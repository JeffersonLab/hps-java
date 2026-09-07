package org.hps.recon.vertexing;

import org.apache.commons.math3.linear.*;
import org.apache.commons.math3.util.FastMath;
import java.util.ArrayList;
import java.util.List;
import hep.physics.matrix.BasicMatrix;
import hep.physics.matrix.Matrix;
import hep.physics.matrix.MatrixOp;
import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;
import org.hps.recon.tracking.CoordinateTransformations;


/**
 * Kalman Filter vertex fitter using Gain Matrix formalism
 * Follows the approach of Frühwirth and Billoir for vertex fitting
 */
public class KalmanVertexFitterGainMatrix {
    
    private double bField;
    private RealVector vertex;
    private RealMatrix vertexCov;
    private double chi2;
    private int ndf;
    private List<TrackMomentum> trackMomenta;
    
    /**
     * Track parameters in perigee representation
     */
    public static class TrackParams {
        public double d0, phi0, omega, z0, tanLambda;
        public RealMatrix cov;

        public TrackParams(double d0, double phi0, double omega, double z0,
                          double tanLambda, RealMatrix cov) {
            this.d0 = d0;
            this.phi0 = phi0;
            this.omega = omega;
            this.z0 = z0;
            this.tanLambda = tanLambda;
            this.cov = cov;
        }

        /** Create a copy of this TrackParams */
        public TrackParams copy() {
            return new TrackParams(d0, phi0, omega, z0, tanLambda, cov.copy());
        }

        /** Get parameters as array [d0, phi0, omega, z0, tanLambda] */
        public double[] toArray() {
            return new double[]{d0, phi0, omega, z0, tanLambda};
        }

        /** Set parameters from array [d0, phi0, omega, z0, tanLambda] */
        public void fromArray(double[] params) {
            this.d0 = params[0];
            this.phi0 = params[1];
            this.omega = params[2];
            this.z0 = params[3];
            this.tanLambda = params[4];
        }
    }
    
    /**
     * A straight line (zero-curvature "pseudo-track"), used to treat an already-fitted
     * neutral V0's momentum direction as a track with no curvature in the cascade
     * (V0 + recoil-track) vertex fit. Parameterized by a point on the line (x0,y0,z0)
     * and a direction vector (dx,dy,dz) (need not be unit-normalized), plus the full
     * 6x6 joint covariance of (x0,y0,z0,dx,dy,dz) -- e.g. the V0 vertex-position/momentum
     * joint covariance, with no block-diagonal approximation.
     */
    public static class LineParams {
        public double x0, y0, z0;
        public double dx, dy, dz;
        public RealMatrix cov;

        public LineParams(double x0, double y0, double z0, double dx, double dy, double dz, RealMatrix cov) {
            this.x0 = x0;
            this.y0 = y0;
            this.z0 = z0;
            this.dx = dx;
            this.dy = dy;
            this.dz = dz;
            this.cov = cov;
        }

        /** Create a copy of this LineParams */
        public LineParams copy() {
            return new LineParams(x0, y0, z0, dx, dy, dz, cov.copy());
        }

        /** Get parameters as array [x0, y0, z0, dx, dy, dz] */
        public double[] toArray() {
            return new double[]{x0, y0, z0, dx, dy, dz};
        }
    }

    /**
     * Track momentum at vertex
     */
    public static class TrackMomentum {
        public RealVector p;
        public RealMatrix pCov;
        public double pt, pMag, theta, phi;
        
        public TrackMomentum(RealVector p, RealMatrix pCov) {
            this.p = p;
            this.pCov = pCov;
            this.pt = FastMath.sqrt(p.getEntry(0) * p.getEntry(0) + 
                                    p.getEntry(1) * p.getEntry(1));
            this.pMag = p.getNorm();
            this.theta = FastMath.atan2(this.pt, p.getEntry(2));
            this.phi = FastMath.atan2(p.getEntry(1), p.getEntry(0));
        }
    }
    
    /**
     * Fit results
     */
    public static class FitResult {
        public RealVector vertex;
        public RealMatrix vertexCov;
        public double chi2;
        public int ndf;
        public List<TrackMomentum> trackMomenta;
        public List<TrackParams> fittedTracks;  // Fitted track parameters (for kinematic fit)
        // Total (summed over all tracks) fitted 3-momentum and its covariance, correctly
        // propagated through the FULL post-fit state covariance (including cross-track and
        // vertex-momentum correlation blocks, not just each track's own diagonal block) --
        // only populated by fitSoftConstrained/fitLagrangeMultiplier; null otherwise.
        public RealVector totalMomentum;
        public RealMatrix totalMomentumCov;

        public FitResult(RealVector vertex, RealMatrix vertexCov, double chi2,
                        int ndf, List<TrackMomentum> trackMomenta) {
            this.vertex = vertex;
            this.vertexCov = vertexCov;
            this.chi2 = chi2;
            this.ndf = ndf;
            this.trackMomenta = trackMomenta;
            this.fittedTracks = null;
        }

        public FitResult(RealVector vertex, RealMatrix vertexCov, double chi2,
                        int ndf, List<TrackMomentum> trackMomenta, List<TrackParams> fittedTracks) {
            this.vertex = vertex;
            this.vertexCov = vertexCov;
            this.chi2 = chi2;
            this.ndf = ndf;
            this.trackMomenta = trackMomenta;
            this.fittedTracks = fittedTracks;
        }
    }
    
    /**
     * Constraint representation: c = constraint residual, H = constraint matrix, V = covariance
     */
    private static class Constraint {
        RealVector c;
        RealMatrix H;
        RealMatrix V;
        
        Constraint(RealVector c, RealMatrix H, RealMatrix V) {
            this.c = c;
            this.H = H;
            this.V = V;
        }
    }
    
    public KalmanVertexFitterGainMatrix(double bField) {
        this.bField = bField;
    }
    
    public KalmanVertexFitterGainMatrix() {
        this(2.0);
    }
    
    /**
     * Convert perigee to vertex parameters
     */
    private static class VertexParams {
        double phiV, zV;
        VertexParams(double phiV, double zV) {
            this.phiV = phiV;
            this.zV = zV;
        }
    }
    
    private VertexParams perigeeToVertexParams(TrackParams track, double xV, double yV) {
        double R = 1.0 / FastMath.abs(track.omega);
        double sign = FastMath.signum(track.omega);
        
        double xc = sign * R * FastMath.sin(track.phi0) - track.d0 * FastMath.sin(track.phi0);
        double yc = -sign * R * FastMath.cos(track.phi0) + track.d0 * FastMath.cos(track.phi0);
        
        double dx = xV - xc;
        double dy = yV - yc;
        
        double phiV = FastMath.atan2(-dx * sign, dy * sign);
        double dphi = phiV - track.phi0;
        // Normalize dphi to (-π, π] to handle the atan2 branch cut.
        // Positrons (phi0 ≈ π) have phiV wrap from just above π to just below -π,
        // giving dphi ≈ -2π instead of the correct small positive value.
        while (dphi >  FastMath.PI) dphi -= 2.0 * FastMath.PI;
        while (dphi < -FastMath.PI) dphi += 2.0 * FastMath.PI;
        // Arc length along the helix from the perigee to (xV,yV): matches the canonical
        // phi(s) = phi0 - s/R convention (org.lcsim TrackUtils/HelixUtils), i.e.
        // s = -sign(omega)*R*dphi, NOT s = R*dphi -- the missing -sign flips the sign of
        // z_v for every track with omega>0 relative to one with omega<0.
        double s = -sign * R * dphi;
        double zV = track.z0 + s * track.tanLambda;
        return new VertexParams(phiV, zV);
    }
    
    /**
     * Compute track constraint using Gain Matrix formalism
     * The track provides a constraint on where the vertex should be
     */
    private Constraint computeTrackConstraint(TrackParams track, RealVector vertex) {
        double xV = vertex.getEntry(0);
        double yV = vertex.getEntry(1);
        double zV = vertex.getEntry(2);
        
        VertexParams vp = perigeeToVertexParams(track, xV, yV);
        
        // Compute H = dc/dvertex
        double R = 1.0 / FastMath.abs(track.omega);
        double sign = FastMath.signum(track.omega);

        double xc = sign * R * FastMath.sin(track.phi0) - track.d0 * FastMath.sin(track.phi0);
        double yc = -sign * R * FastMath.cos(track.phi0) + track.d0 * FastMath.cos(track.phi0);

        double dx = xV - xc;
        double dy = yV - yc;
        double r2 = dx * dx + dy * dy;
        double r  = FastMath.sqrt(r2);

        // Constraint residuals:
        // [0] transverse: distance from vertex to helix circle in XY plane
        // [1] longitudinal: z at vertex vs z predicted from track
        RealVector c = MatrixUtils.createRealVector(new double[]{r - R, zV - vp.zV});

        // Derivatives for longitudinal constraint (via phi at vertex)
        double dphiDx = -dy / r2;
        double dphiDy = dx / r2;
        double dzDx = -sign * R * track.tanLambda * dphiDx;
        double dzDy = -sign * R * track.tanLambda * dphiDy;

        RealMatrix H = MatrixUtils.createRealMatrix(2, 3);
        // Transverse: d(r-R)/d(xV,yV) = (dx/r, dy/r, 0)
        H.setEntry(0, 0, dx / r);
        H.setEntry(0, 1, dy / r);
        H.setEntry(0, 2, 0.0);
        // Longitudinal: unchanged
        H.setEntry(1, 0, -dzDx);
        H.setEntry(1, 1, -dzDy);
        H.setEntry(1, 2, 1.0);
        
        // Propagate covariance
        RealMatrix V = propagateTrackCovariance(track, xV, yV);
        
        return new Constraint(c, H, V);
    }
    
    /**
     * Propagate track covariance to constraint space
     */
    private RealMatrix propagateTrackCovariance(TrackParams track, double xV, double yV) {
        double R = 1.0 / FastMath.abs(track.omega);
        double sign = FastMath.signum(track.omega);
        
        double xc = sign * R * FastMath.sin(track.phi0) - track.d0 * FastMath.sin(track.phi0);
        double yc = -sign * R * FastMath.cos(track.phi0) + track.d0 * FastMath.cos(track.phi0);
        
        double dx = xV - xc;
        double dy = yV - yc;
        double r2 = dx * dx + dy * dy;
        
        double r = FastMath.sqrt(r2);

        // Derivatives of transverse constraint f_t = sqrt((xV-xc)^2+(yV-yc)^2) - R
        // w.r.t. perigee parameters (xc, yc, and R all depend on d0/phi0/omega)
        double dftDd0    = (dx * FastMath.sin(track.phi0) - dy * FastMath.cos(track.phi0)) / r;
        double dftDphi0  = -(sign * R - track.d0) * (dx * FastMath.cos(track.phi0) + dy * FastMath.sin(track.phi0)) / r;
        double dftDomega = (dx * FastMath.sin(track.phi0) - dy * FastMath.cos(track.phi0)) / (r * track.omega * track.omega)
                           + sign / (track.omega * track.omega);

        // Derivatives of phi_v w.r.t. perigee (still needed for z derivatives below)
        double dphiDd0 = -(FastMath.cos(track.phi0) * dx + FastMath.sin(track.phi0) * dy) / r2;
        double dphiDphi0 = (sign * R - track.d0) * (dy * FastMath.cos(track.phi0) - dx * FastMath.sin(track.phi0)) / r2;
        double dphiDomega = -R * R * (FastMath.cos(track.phi0) * dx + FastMath.sin(track.phi0) * dy) / r2;

        double phiV = FastMath.atan2(-dx * sign, dy * sign);
        double dphi = phiV - track.phi0;
        while (dphi >  FastMath.PI) dphi -= 2.0 * FastMath.PI;
        while (dphi < -FastMath.PI) dphi += 2.0 * FastMath.PI;
        double s = -sign * R * dphi;

        // Derivatives of z_v w.r.t. perigee. z_v = z0 + s*tanLambda with
        // s = -sign(omega)*R*dphi, so each R*(dphi-derivative) term below picks up the
        // same -sign factor; the explicit s/omega term (from d/domega of R itself) does not.
        double dzDd0 = -sign * track.tanLambda * R * dphiDd0;
        double dzDphi0 = -sign * (-track.tanLambda * R + track.tanLambda * R * dphiDphi0);
        double dzDomega = -s * track.tanLambda / track.omega - sign * track.tanLambda * R * dphiDomega;
        double dzDz0 = 1.0;
        double dzDtl = s;

        RealMatrix J = MatrixUtils.createRealMatrix(2, 5);
        J.setRow(0, new double[]{dftDd0, dftDphi0, dftDomega, 0.0, 0.0});
        J.setRow(1, new double[]{dzDd0, dzDphi0, dzDomega, dzDz0, dzDtl});
        
        return J.multiply(track.cov).multiply(J.transpose());
    }

    /**
     * Deterministic orthonormal basis {u,v} spanning the plane perpendicular to unit
     * vector n. Picks whichever of e_z/e_x is less parallel to n as the seed for
     * Gram-Schmidt, so the construction is smooth except very near that switch point.
     */
    private static RealVector[] perpendicularBasis(RealVector n) {
        RealVector ref = (FastMath.abs(n.getEntry(2)) < 0.9)
                ? MatrixUtils.createRealVector(new double[]{0, 0, 1})
                : MatrixUtils.createRealVector(new double[]{1, 0, 0});
        RealVector u = ref.subtract(n.mapMultiply(ref.dotProduct(n)));
        u = u.mapDivide(u.getNorm());
        RealVector v = crossProduct(n, u);
        return new RealVector[]{u, v};
    }

    private static RealVector crossProduct(RealVector a, RealVector b) {
        return MatrixUtils.createRealVector(new double[]{
                a.getEntry(1) * b.getEntry(2) - a.getEntry(2) * b.getEntry(1),
                a.getEntry(2) * b.getEntry(0) - a.getEntry(0) * b.getEntry(2),
                a.getEntry(0) * b.getEntry(1) - a.getEntry(1) * b.getEntry(0)
        });
    }

    /**
     * Residual of a vertex position w.r.t. a line, expressed as the 2 components of
     * (vertexGuess - x0) transverse to the line's direction. Zero iff vertexGuess lies
     * exactly on the line. Written to take a raw 6-parameter array (rather than a
     * LineParams) so it can be reused both for the analytic residual and for finite-
     * differencing the Jacobian w.r.t. the line's own parameters below.
     */
    private static RealVector lineResidual(double[] lineParams, RealVector vertexGuess) {
        RealVector x0 = MatrixUtils.createRealVector(new double[]{lineParams[0], lineParams[1], lineParams[2]});
        RealVector d = MatrixUtils.createRealVector(new double[]{lineParams[3], lineParams[4], lineParams[5]});
        RealVector n = d.mapDivide(d.getNorm());
        RealVector[] basis = perpendicularBasis(n);
        RealVector delta = vertexGuess.subtract(x0);
        return MatrixUtils.createRealVector(new double[]{
                delta.dotProduct(basis[0]), delta.dotProduct(basis[1])
        });
    }

    /**
     * Jacobian of lineResidual w.r.t. the line's own 6 parameters, at fixed vertexGuess,
     * by central finite differences. An analytic Jacobian would need d(basis)/d(direction),
     * which is gauge-dependent on the (deterministic but branch-y) perpendicular-basis
     * construction above; finite-differencing the actual residual function sidesteps that
     * while still correctly capturing how the chosen basis responds to changes in direction.
     */
    private static RealMatrix lineResidualJacobianWrtLineParams(double[] lineParams, RealVector vertexGuess) {
        RealMatrix J = MatrixUtils.createRealMatrix(2, 6);
        for (int k = 0; k < 6; k++) {
            double h = 1.0e-6 * FastMath.max(1.0, FastMath.abs(lineParams[k]));
            double[] pPlus = lineParams.clone();
            double[] pMinus = lineParams.clone();
            pPlus[k] += h;
            pMinus[k] -= h;
            RealVector cPlus = lineResidual(pPlus, vertexGuess);
            RealVector cMinus = lineResidual(pMinus, vertexGuess);
            RealVector dcdp = cPlus.subtract(cMinus).mapDivide(2.0 * h);
            J.setColumn(k, dcdp.toArray());
        }
        return J;
    }

    /**
     * Compute the constraint that a vertex must lie on a given line (e.g. a V0's fitted
     * momentum direction, treated as a zero-curvature pseudo-track). Mirrors
     * computeTrackConstraint: H is the residual's Jacobian w.r.t. the vertex position,
     * V is the line's own parameter covariance propagated into residual space.
     */
    private Constraint computeLineConstraint(LineParams line, RealVector vertex) {
        double[] lineParams = line.toArray();

        RealVector c = lineResidual(lineParams, vertex);

        RealVector d = MatrixUtils.createRealVector(new double[]{line.dx, line.dy, line.dz});
        RealVector n = d.mapDivide(d.getNorm());
        RealVector[] basis = perpendicularBasis(n);

        RealMatrix H = MatrixUtils.createRealMatrix(2, 3);
        H.setRow(0, basis[0].toArray());
        H.setRow(1, basis[1].toArray());

        RealMatrix J = lineResidualJacobianWrtLineParams(lineParams, vertex);
        RealMatrix V = J.multiply(line.cov).multiply(J.transpose());

        return new Constraint(c, H, V);
    }

    /**
     * Compute vertex position constraint (beamspot)
     */
    private Constraint computeVertexConstraint(RealVector vertex, 
                                               RealVector vertexConstraint,
                                               RealMatrix vertexConstraintCov) {
        RealVector c = vertex.subtract(vertexConstraint);
        RealMatrix H = MatrixUtils.createRealIdentityMatrix(3);
        RealMatrix V = vertexConstraintCov;
        
        return new Constraint(c, H, V);
    }
    
    /**
     * Compute momentum constraint
     * The covariance V includes both beam momentum uncertainty AND track momentum uncertainties
     */
    private Constraint computeMomentumConstraint(List<TrackParams> tracks,
                                                 RealVector vertex,
                                                 RealVector momentumConstraint,
                                                 RealMatrix momentumConstraintCov) {
        // Calculate total momentum and total momentum covariance from tracks
        RealVector totalP = MatrixUtils.createRealVector(new double[3]);
        RealMatrix dpDvertex = MatrixUtils.createRealMatrix(3, 3);
        RealMatrix totalPCov = MatrixUtils.createRealMatrix(3, 3);

        for (TrackParams track : tracks) {
            RealVector p = computeMomentumAtVertex(track, vertex);
            totalP = totalP.add(p);

            RealMatrix dpDv = computeMomentumVertexDerivatives(track, vertex);
            dpDvertex = dpDvertex.add(dpDv);

            // Add track momentum covariance (propagated from track parameter errors)
            RealMatrix pCov = computeMomentumCovariance(track, vertex);
            totalPCov = totalPCov.add(pCov);
        }

        RealVector c = totalP.subtract(momentumConstraint);
        RealMatrix H = dpDvertex;
        // Total covariance = beam momentum uncertainty + sum of track momentum uncertainties
        RealMatrix V = momentumConstraintCov.add(totalPCov);

        return new Constraint(c, H, V);
    }
    
    /**
     * Compute mass constraint
     * Invariant mass: M² = (ΣE)² - (Σp)²
     * 
     * @param tracks List of track parameters
     * @param vertex Vertex position
     * @param massConstraint Constrained mass value (GeV/c²)
     * @param massConstraintSigma Uncertainty on mass (GeV/c²)
     * @return Constraint object
     */
    private Constraint computeMassConstraint(List<TrackParams> tracks,
                                            RealVector vertex,
                                            double massConstraint,
                                            double massConstraintSigma) {
        // Assume pion mass for all tracks (can be extended)
        double mPi = 0.13957; // GeV/c²
        
        double totalE = 0.0;
        RealVector totalP = MatrixUtils.createRealVector(new double[3]);
        RealVector dEDvertex = MatrixUtils.createRealVector(new double[3]);
        RealMatrix dpDvertex = MatrixUtils.createRealMatrix(3, 3);
        
        for (TrackParams track : tracks) {
            RealVector p = computeMomentumAtVertex(track, vertex);
            double pMag = p.getNorm();
            
            // Energy (assuming pion mass)
            double E = FastMath.sqrt(pMag * pMag + mPi * mPi);
            totalE += E;
            totalP = totalP.add(p);
            
            // Derivatives of E w.r.t. vertex
            // E = sqrt(p² + m²), so dE/dvertex = (p · dp/dvertex) / E
            RealMatrix dpDv = computeMomentumVertexDerivatives(track, vertex);
            
            for (int i = 0; i < 3; i++) {
                double dEDv = 0.0;
                for (int j = 0; j < 3; j++) {
                    dEDv += p.getEntry(j) * dpDv.getEntry(j, i);
                }
                dEDvertex.setEntry(i, dEDvertex.getEntry(i) + dEDv / E);
            }
            
            dpDvertex = dpDvertex.add(dpDv);
        }
        
        // Invariant mass
        double totalPmag = totalP.getNorm();
        double M = FastMath.sqrt(totalE * totalE - totalPmag * totalPmag);
        
        // Constraint residual
        RealVector c = MatrixUtils.createRealVector(new double[]{M - massConstraint});
        
        // Derivatives of M w.r.t. vertex
        // M² = E² - p², so 2M dM = 2E dE - 2p·dp
        // dM/dvertex = (E * dE/dvertex - p · dp/dvertex) / M
        RealVector dMDvertex = MatrixUtils.createRealVector(new double[3]);
        for (int i = 0; i < 3; i++) {
            double dpDotDv = 0.0;
            for (int j = 0; j < 3; j++) {
                dpDotDv += totalP.getEntry(j) * dpDvertex.getEntry(j, i);
            }
            dMDvertex.setEntry(i, (totalE * dEDvertex.getEntry(i) - dpDotDv) / M);
        }
        
        RealMatrix H = MatrixUtils.createRealMatrix(1, 3);
        H.setRowVector(0, dMDvertex);
        
        // Covariance (1x1 matrix)
        RealMatrix V = MatrixUtils.createRealMatrix(1, 1);
        V.setEntry(0, 0, massConstraintSigma * massConstraintSigma);
        
        return new Constraint(c, H, V);
    }
    
    /**
     * Compute momentum at vertex
     */
    RealVector computeMomentumAtVertex(TrackParams track, RealVector vertex) {
        VertexParams vp = perigeeToVertexParams(track, vertex.getEntry(0), vertex.getEntry(1));

        // pT is a magnitude (R = pT/(C*|B|)), so it must use |bField|, not the signed field --
        // direction comes entirely from phiV/tanLambda. Using signed bField here silently
        // flips every reconstructed momentum's direction whenever bField<0 (the real HPS field).
        double pT = 2.99792458e-4 * FastMath.abs(bField) / FastMath.abs(track.omega);
        double px = pT * FastMath.cos(vp.phiV);
        double py = pT * FastMath.sin(vp.phiV);
        double pz = pT * track.tanLambda;
        
        return MatrixUtils.createRealVector(new double[]{px, py, pz});
    }
    
    /**
     * Compute d(momentum)/d(vertex)
     */
    private RealMatrix computeMomentumVertexDerivatives(TrackParams track, RealVector vertex) {
        double xV = vertex.getEntry(0);
        double yV = vertex.getEntry(1);
        
        double R = 1.0 / FastMath.abs(track.omega);
        double sign = FastMath.signum(track.omega);
        double pT = 2.99792458e-4 * FastMath.abs(bField) / FastMath.abs(track.omega);

        double xc = sign * R * FastMath.sin(track.phi0) - track.d0 * FastMath.sin(track.phi0);
        double yc = -sign * R * FastMath.cos(track.phi0) + track.d0 * FastMath.cos(track.phi0);

        double dx = xV - xc;
        double dy = yV - yc;
        double r2 = dx * dx + dy * dy;

        VertexParams vp = perigeeToVertexParams(track, xV, yV);

        double dphiDx = -dy / r2;
        double dphiDy = dx / r2;

        RealMatrix dpDv = MatrixUtils.createRealMatrix(3, 3);
        dpDv.setEntry(0, 0, -pT * FastMath.sin(vp.phiV) * dphiDx);
        dpDv.setEntry(0, 1, -pT * FastMath.sin(vp.phiV) * dphiDy);
        dpDv.setEntry(0, 2, 0.0);
        dpDv.setEntry(1, 0, pT * FastMath.cos(vp.phiV) * dphiDx);
        dpDv.setEntry(1, 1, pT * FastMath.cos(vp.phiV) * dphiDy);
        dpDv.setEntry(1, 2, 0.0);
        dpDv.setEntry(2, 0, 0.0);
        dpDv.setEntry(2, 1, 0.0);
        dpDv.setEntry(2, 2, 0.0);
        
        return dpDv;
    }
    
    /**
     * Compute momentum covariance
     */
    private RealMatrix computeMomentumCovariance(TrackParams track, RealVector vertex) {
        RealMatrix Jp = computeMomentumTrackJacobian(track, vertex);
        return Jp.multiply(track.cov).multiply(Jp.transpose());
    }

    /**
     * Raw momentum and covariance for a single track, with no vertex constraint applied
     * -- evaluated at the track's own point of closest approach (arc length zero), which
     * makes {@link #computeMomentumAtVertex} reduce to the direct px=pT*cos(phi0),
     * py=pT*sin(phi0), pz=pT*tanLambda formula with no extrapolation. This is the "before
     * any vertex fit" baseline used to compare against constrained/unconstrained
     * vertex-fit momenta and their reported errors. Tracking frame.
     */
    public TrackMomentum computeRawMomentum(TrackParams track) {
        RealVector poca = MatrixUtils.createRealVector(new double[]{
                -track.d0 * FastMath.sin(track.phi0), track.d0 * FastMath.cos(track.phi0), track.z0});
        return new TrackMomentum(computeMomentumAtVertex(track, poca), computeMomentumCovariance(track, poca));
    }

    /**
     * Jacobian d(momentum)/d(track parameters) [d0, phi0, omega, z0, tanLambda] at the given
     * vertex, as a 3x5 matrix. Extracted out of {@link #computeMomentumCovariance} so the same
     * per-track momentum Jacobian can be reused (alongside {@link #computeMomentumVertexDerivatives})
     * to build the full state-vector Jacobian needed for a correctly-correlated TOTAL momentum
     * covariance (see the total-momentum computation in {@link #fitSoftConstrained}).
     */
    private RealMatrix computeMomentumTrackJacobian(TrackParams track, RealVector vertex) {
        double xV = vertex.getEntry(0);
        double yV = vertex.getEntry(1);

        VertexParams vp = perigeeToVertexParams(track, xV, yV);

        double R = 1.0 / FastMath.abs(track.omega);
        double sign = FastMath.signum(track.omega);
        double pT = 2.99792458e-4 * FastMath.abs(bField) / FastMath.abs(track.omega);

        double xc = sign * R * FastMath.sin(track.phi0) - track.d0 * FastMath.sin(track.phi0);
        double yc = -sign * R * FastMath.cos(track.phi0) + track.d0 * FastMath.cos(track.phi0);
        double dx = xV - xc;
        double dy = yV - yc;
        double r2 = dx * dx + dy * dy;

        double dphiDd0 = -(FastMath.cos(track.phi0) * dx + FastMath.sin(track.phi0) * dy) / r2;
        double dphiDphi0 = (sign * R - track.d0) * (dy * FastMath.cos(track.phi0) - dx * FastMath.sin(track.phi0)) / r2;
        double dphiDomega = -R * R * (FastMath.cos(track.phi0) * dx + FastMath.sin(track.phi0) * dy) / r2;

        double dpTDomega = -2.99792458e-4 * FastMath.abs(bField) * sign / (track.omega * track.omega);

        double dpxDd0 = -pT * FastMath.sin(vp.phiV) * dphiDd0;
        double dpxDphi0 = -pT * FastMath.sin(vp.phiV) * dphiDphi0;
        double dpxDomega = FastMath.cos(vp.phiV) * dpTDomega - pT * FastMath.sin(vp.phiV) * dphiDomega;

        double dpyDd0 = pT * FastMath.cos(vp.phiV) * dphiDd0;
        double dpyDphi0 = pT * FastMath.cos(vp.phiV) * dphiDphi0;
        double dpyDomega = FastMath.sin(vp.phiV) * dpTDomega + pT * FastMath.cos(vp.phiV) * dphiDomega;

        double dpzDomega = track.tanLambda * dpTDomega;
        double dpzDtl = pT;

        RealMatrix Jp = MatrixUtils.createRealMatrix(3, 5);
        Jp.setRow(0, new double[]{dpxDd0, dpxDphi0, dpxDomega, 0.0, 0.0});
        Jp.setRow(1, new double[]{dpyDd0, dpyDphi0, dpyDomega, 0.0, 0.0});
        Jp.setRow(2, new double[]{0.0, 0.0, dpzDomega, 0.0, dpzDtl});

        return Jp;
    }
    
    /**
     * Fit vertex using Gain Matrix formalism
     */
    public FitResult fit(List<TrackParams> tracks,
                        RealVector initialVertex,
                        RealVector vertexConstraint,
                        RealMatrix vertexConstraintCov,
                        RealVector momentumConstraint,
                        RealMatrix momentumConstraintCov,
                        Double massConstraint,
                        Double massConstraintSigma,
                        int maxIterations,
                        double tolerance) {
        
        int nTracks = tracks.size();
        
        // Initial vertex
        RealVector vertex;
        if (initialVertex != null) {
            vertex = initialVertex.copy();
        } else if (vertexConstraint != null) {
            vertex = vertexConstraint.copy();
        } else {
            double xInit = 0.0, yInit = 0.0, zInit = 0.0;
            for (TrackParams track : tracks) {
                xInit += -track.d0 * FastMath.sin(track.phi0);
                yInit += track.d0 * FastMath.cos(track.phi0);
                zInit += track.z0;
            }
            vertex = MatrixUtils.createRealVector(new double[]{
                xInit / nTracks, yInit / nTracks, zInit / nTracks
            });
        }
        
        // Prior covariance, re-applied at the top of every iteration below (see comment
        // there for why) rather than the initial value of a running C.
        RealMatrix priorC;
        if (vertexConstraint != null && vertexConstraintCov != null) {
            priorC = vertexConstraintCov.copy();
        } else {
            priorC = MatrixUtils.createRealIdentityMatrix(3).scalarMultiply(100.0);
        }
        RealMatrix I = MatrixUtils.createRealIdentityMatrix(3);
        RealMatrix C = priorC;

        // Iterative Gain Matrix updates
        for (int iteration = 0; iteration < maxIterations; iteration++) {
            RealVector vertexOld = vertex.copy();
            // Reset to the vague prior each iteration: the vertex/track/momentum/mass
            // constraints below are the same fixed measurements being re-linearized at
            // successive vertex guesses, not new independent data arriving sequentially.
            // Carrying C forward across iterations would reprocess the same information
            // repeatedly, shrinking the covariance by roughly a factor of (iterations to
            // converge) -- same bug as originally found and fixed in fitCascadeVertex().
            C = priorC;

            // Apply vertex constraint
            if (vertexConstraint != null && vertexConstraintCov != null) {
                Constraint constraint = computeVertexConstraint(vertex, vertexConstraint,
                                                               vertexConstraintCov);

                // Gain matrix: K = C * H^T * (H * C * H^T + V)^-1
                RealMatrix S = constraint.H.multiply(C).multiply(constraint.H.transpose()).add(constraint.V);
                RealMatrix K = C.multiply(constraint.H.transpose()).multiply(
                    new LUDecomposition(S).getSolver().getInverse()
                );

                // Update: vertex = vertex - K * c
                vertex = vertex.subtract(K.operate(constraint.c));

                // Joseph-form covariance update: numerically stable under re-linearization
                // across iterations, unlike the simple (I-KH)*C form which can
                // systematically underestimate C.
                RealMatrix ImKH = I.subtract(K.multiply(constraint.H));
                C = ImKH.multiply(C).multiply(ImKH.transpose())
                        .add(K.multiply(constraint.V).multiply(K.transpose()));
            }

            // Apply track constraints
            for (TrackParams track : tracks) {
                Constraint constraint = computeTrackConstraint(track, vertex);

                RealMatrix S = constraint.H.multiply(C).multiply(constraint.H.transpose()).add(constraint.V);
                RealMatrix K = C.multiply(constraint.H.transpose()).multiply(
                    new LUDecomposition(S).getSolver().getInverse()
                );

                vertex = vertex.subtract(K.operate(constraint.c));
                RealMatrix ImKH = I.subtract(K.multiply(constraint.H));
                C = ImKH.multiply(C).multiply(ImKH.transpose())
                        .add(K.multiply(constraint.V).multiply(K.transpose()));
            }

            // Apply momentum constraint
            if (momentumConstraint != null && momentumConstraintCov != null) {
                try {
                    Constraint constraint = computeMomentumConstraint(tracks, vertex,
                                                                      momentumConstraint,
                                                                      momentumConstraintCov);

                    RealMatrix S = constraint.H.multiply(C).multiply(constraint.H.transpose()).add(constraint.V);
                    RealMatrix K = C.multiply(constraint.H.transpose()).multiply(
                        new LUDecomposition(S).getSolver().getInverse()
                    );

                    vertex = vertex.subtract(K.operate(constraint.c));
                    RealMatrix ImKH = I.subtract(K.multiply(constraint.H));
                    C = ImKH.multiply(C).multiply(ImKH.transpose())
                            .add(K.multiply(constraint.V).multiply(K.transpose()));
                } catch (Exception e) {
                    // Skip if singular
                }
            }

            // Apply mass constraint
            if (massConstraint != null && massConstraintSigma != null) {
                try {
                    Constraint constraint = computeMassConstraint(tracks, vertex,
                                                                 massConstraint,
                                                                 massConstraintSigma);

                    RealMatrix S = constraint.H.multiply(C).multiply(constraint.H.transpose()).add(constraint.V);
                    RealMatrix K = C.multiply(constraint.H.transpose()).multiply(
                        new LUDecomposition(S).getSolver().getInverse()
                    );

                    vertex = vertex.subtract(K.operate(constraint.c));
                    RealMatrix ImKH = I.subtract(K.multiply(constraint.H));
                    C = ImKH.multiply(C).multiply(ImKH.transpose())
                            .add(K.multiply(constraint.V).multiply(K.transpose()));
                } catch (Exception e) {
                    // Skip if singular
                }
            }

            // Check convergence
            if (vertex.subtract(vertexOld).getNorm() < tolerance) {
                break;
            }
        }
        
        // Calculate chi-squared with individual contributions
        double chi2 = 0.0;
        double chi2Vertex = 0.0;
        double chi2Momentum = 0.0;
        double[] chi2Tracks = new double[nTracks];

        if (vertexConstraint != null && vertexConstraintCov != null) {
            Constraint constraint = computeVertexConstraint(vertex, vertexConstraint,
                                                           vertexConstraintCov);
            RealMatrix VInv = new LUDecomposition(constraint.V).getSolver().getInverse();
            chi2Vertex = constraint.c.dotProduct(VInv.operate(constraint.c));
            chi2 += chi2Vertex;
        }

        for (int itrk = 0; itrk < nTracks; itrk++) {
            Constraint constraint = computeTrackConstraint(tracks.get(itrk), vertex);
            RealMatrix VInv = new LUDecomposition(constraint.V).getSolver().getInverse();
            chi2Tracks[itrk] = constraint.c.dotProduct(VInv.operate(constraint.c));
            chi2 += chi2Tracks[itrk];
        }

        if (momentumConstraint != null && momentumConstraintCov != null) {
            try {
                Constraint constraint = computeMomentumConstraint(tracks, vertex,
                                                                  momentumConstraint,
                                                                  momentumConstraintCov);
                RealMatrix VInv = new LUDecomposition(constraint.V).getSolver().getInverse();
                chi2Momentum = constraint.c.dotProduct(VInv.operate(constraint.c));
                chi2 += chi2Momentum;
            } catch (Exception e) {
                // Skip if singular
            }
        }

        if (massConstraint != null && massConstraintSigma != null) {
            try {
                Constraint constraint = computeMassConstraint(tracks, vertex,
                                                             massConstraint,
                                                             massConstraintSigma);
                RealMatrix VInv = new LUDecomposition(constraint.V).getSolver().getInverse();
                chi2 += constraint.c.dotProduct(VInv.operate(constraint.c));
            } catch (Exception e) {
                // Skip if singular
            }
        }

        // Print chi2 contributions
        if(debugFlag){
            System.out.printf("  Chi2 contributions: vertex=%.4f", chi2Vertex);
            for (int itrk = 0; itrk < nTracks; itrk++) {
                System.out.printf("  track%d=%.4f", itrk, chi2Tracks[itrk]);
            }
            System.out.printf("  momentum=%.4f  total=%.4f%n", chi2Momentum, chi2);
        }
        // NDF
        int ndf = 2 * nTracks - 3;
        if (vertexConstraint != null) ndf += 3;
        if (momentumConstraint != null) ndf += 3;
        if (massConstraint != null) ndf += 1;
        
        // Track momenta
        List<TrackMomentum> trackMomenta = new ArrayList<>();
        for (TrackParams track : tracks) {
            RealVector p = computeMomentumAtVertex(track, vertex);
            RealMatrix pCov = computeMomentumCovariance(track, vertex);
            trackMomenta.add(new TrackMomentum(p, pCov));
        }
        
        this.vertex = vertex;
        this.vertexCov = C;
        this.chi2 = chi2;
        this.ndf = ndf;
        this.trackMomenta = trackMomenta;

        return new FitResult(vertex, C, chi2, ndf, trackMomenta);
    }

    /**
     * Fit a "cascade" (production) vertex where a curving recoil track meets an
     * already-fitted neutral V0's momentum direction, treated as a zero-curvature
     * line. Same sequential Kalman gain-matrix update as {@link #fit}, specialized to
     * exactly one track constraint and one line constraint (no vertex/momentum/mass
     * constraints). NDF = (2 track-constraint dof + 2 line-constraint dof) - 3 (vertex
     * position dof) = 1.
     */
    public FitResult fitCascadeVertex(TrackParams recoilTrack, LineParams v0Line,
                                       RealVector initialVertex, int maxIterations, double tolerance) {
        RealVector vertex = (initialVertex != null)
                ? initialVertex.copy()
                : MatrixUtils.createRealVector(new double[]{v0Line.x0, v0Line.y0, v0Line.z0});

        RealMatrix priorC = MatrixUtils.createRealIdentityMatrix(3).scalarMultiply(100.0);
        RealMatrix I = MatrixUtils.createRealIdentityMatrix(3);
        RealMatrix C = priorC;

        for (int iteration = 0; iteration < maxIterations; iteration++) {
            RealVector vertexOld = vertex.copy();
            // Reset to the vague prior each iteration: the track and line constraints are
            // the same two fixed measurements being re-linearized at successive vertex
            // guesses, not new independent data arriving sequentially. Carrying C forward
            // across iterations would reprocess the same information repeatedly, shrinking
            // the covariance by roughly a factor of (iterations to converge) -- exactly the
            // ~20x variance underestimate (~4.5x pull-std inflation) seen before this fix.
            C = priorC;

            Constraint trackConstraint = computeTrackConstraint(recoilTrack, vertex);
            RealMatrix St = trackConstraint.H.multiply(C).multiply(trackConstraint.H.transpose()).add(trackConstraint.V);
            RealMatrix Kt = C.multiply(trackConstraint.H.transpose()).multiply(
                    new LUDecomposition(St).getSolver().getInverse());
            vertex = vertex.subtract(Kt.operate(trackConstraint.c));
            // Joseph-form covariance update: numerically stable under re-linearization across
            // iterations, unlike the simple (I-KH)*C form which can systematically underestimate C.
            RealMatrix ImKHt = I.subtract(Kt.multiply(trackConstraint.H));
            C = ImKHt.multiply(C).multiply(ImKHt.transpose())
                    .add(Kt.multiply(trackConstraint.V).multiply(Kt.transpose()));

            Constraint lineConstraint = computeLineConstraint(v0Line, vertex);
            RealMatrix Sl = lineConstraint.H.multiply(C).multiply(lineConstraint.H.transpose()).add(lineConstraint.V);
            RealMatrix Kl = C.multiply(lineConstraint.H.transpose()).multiply(
                    new LUDecomposition(Sl).getSolver().getInverse());
            vertex = vertex.subtract(Kl.operate(lineConstraint.c));
            RealMatrix ImKHl = I.subtract(Kl.multiply(lineConstraint.H));
            C = ImKHl.multiply(C).multiply(ImKHl.transpose())
                    .add(Kl.multiply(lineConstraint.V).multiply(Kl.transpose()));

            if (vertex.subtract(vertexOld).getNorm() < tolerance) {
                break;
            }
        }

        Constraint trackConstraint = computeTrackConstraint(recoilTrack, vertex);
        RealMatrix VInvT = new LUDecomposition(trackConstraint.V).getSolver().getInverse();
        double chi2Track = trackConstraint.c.dotProduct(VInvT.operate(trackConstraint.c));

        Constraint lineConstraint = computeLineConstraint(v0Line, vertex);
        RealMatrix VInvL = new LUDecomposition(lineConstraint.V).getSolver().getInverse();
        double chi2Line = lineConstraint.c.dotProduct(VInvL.operate(lineConstraint.c));

        double chi2 = chi2Track + chi2Line;
        int ndf = 1;

        List<TrackMomentum> trackMomenta = new ArrayList<>();
        RealVector p = computeMomentumAtVertex(recoilTrack, vertex);
        RealMatrix pCov = computeMomentumCovariance(recoilTrack, vertex);
        trackMomenta.add(new TrackMomentum(p, pCov));

        this.vertex = vertex;
        this.vertexCov = C;
        this.chi2 = chi2;
        this.ndf = ndf;
        this.trackMomenta = trackMomenta;

        return new FitResult(vertex, C, chi2, ndf, trackMomenta);
    }

    public FitResult fitCascadeVertex(TrackParams recoilTrack, LineParams v0Line) {
        return fitCascadeVertex(recoilTrack, v0Line, null, 20, 1.0e-8);
    }

    /**
     * Result of {@link #fitCascadeVertexJoint}: a joint fit of an e-/e+ decay vertex (V1)
     * and a separate V0(=e-+e+)/recoil production vertex (V2), linked by requiring the
     * neutral V0 to fly in a straight line from V1 to V2. Unlike {@link #fitCascadeVertex},
     * which freezes the V0 as a fixed line, this lets V1, V2, and all three track momenta
     * move jointly within their covariances.
     */
    public static class TwoVertexFitResult {
        public RealVector v1, v2;
        public RealMatrix v1Cov, v2Cov;
        public double chi2;
        public int ndf;
        public TrackMomentum eMinusMomentum, ePlusMomentum, recoilMomentum;
        public List<TrackParams> fittedTracks; // [eMinus, ePlus, recoil]

        public TwoVertexFitResult(RealVector v1, RealMatrix v1Cov, RealVector v2, RealMatrix v2Cov,
                double chi2, int ndf, TrackMomentum eMinusMomentum, TrackMomentum ePlusMomentum,
                TrackMomentum recoilMomentum, List<TrackParams> fittedTracks) {
            this.v1 = v1;
            this.v1Cov = v1Cov;
            this.v2 = v2;
            this.v2Cov = v2Cov;
            this.chi2 = chi2;
            this.ndf = ndf;
            this.eMinusMomentum = eMinusMomentum;
            this.ePlusMomentum = ePlusMomentum;
            this.recoilMomentum = recoilMomentum;
            this.fittedTracks = fittedTracks;
        }
    }

    private static final int TVJ_OFF_V1 = 0;
    private static final int TVJ_OFF_THETA = 3;
    private static final int TVJ_OFF_V2 = 4;
    private static final int TVJ_STATE_SIZE = 7;
    private static final double MAX_STEP_NORM = 5.0;

    public static boolean DEBUG_JOINT_FIT = false;

    // The trust-region step cap (MAX_STEP_NORM) trades per-iteration progress for damping a
    // badly-linearized first step, so it needs more outer iterations than the old, uncapped
    // update to fully converge on real events -- confirmed empirically (the real-data
    // regression case below needs ~40, not 20) -- hence 60 as this convenience overload's
    // default rather than the smaller default used before the cap existed.
    private static final int DEFAULT_TVJ_MAX_ITERATIONS = 60;

    /** A candidate (theta, V2) pair picked out of the transverse near/far branch ambiguity. */
    static class ThetaSeed {
        final double theta;
        final RealVector v2;

        ThetaSeed(double theta, RealVector v2) {
            this.theta = theta;
            this.v2 = v2;
        }
    }

    /**
     * The (up to two) theta values where the line through {@code v1} in direction
     * {@code pV0} (tracking-frame x,y bending plane) crosses the recoil track's own
     * transverse helix circle (center xc,yc, radius R). Substituting the line
     * {@code (xV,yV) = (v1x - theta*px, v1y - theta*py)} into the circle equation
     * {@code (xV-xc)^2+(yV-yc)^2 = R^2} gives a quadratic in theta with discriminant
     * {@code Δ/4 = |p_xy|^2 * (R^2 - d_perp^2)}, where {@code d_perp} is the
     * perpendicular distance from the circle's center to the line -- so this returns
     * {@code null} whenever that distance exceeds R (line misses the circle; no branch
     * ambiguity to resolve) or the line has no transverse component at all.
     */
    static double[] transverseCircleRoots(RealVector v1, RealVector pV0, TrackParams recoilTrack) {
        double R = 1.0 / FastMath.abs(recoilTrack.omega);
        double sign = FastMath.signum(recoilTrack.omega);
        double xc = sign * R * FastMath.sin(recoilTrack.phi0) - recoilTrack.d0 * FastMath.sin(recoilTrack.phi0);
        double yc = -sign * R * FastMath.cos(recoilTrack.phi0) + recoilTrack.d0 * FastMath.cos(recoilTrack.phi0);

        double px = pV0.getEntry(0);
        double py = pV0.getEntry(1);
        double a = px * px + py * py;
        if (a < 1.0e-12) {
            return null;
        }
        double ax = v1.getEntry(0) - xc;
        double ay = v1.getEntry(1) - yc;
        double b = -2.0 * (ax * px + ay * py);
        double c = ax * ax + ay * ay - R * R;

        double disc = b * b - 4.0 * a * c;
        if (disc < 0.0) {
            return null;
        }
        double sq = FastMath.sqrt(disc);
        return new double[]{(-b - sq) / (2.0 * a), (-b + sq) / (2.0 * a)};
    }

    /**
     * Of the (up to two) theta roots where the V0 line through {@code v1} crosses the
     * recoil track's own transverse circle ({@link #transverseCircleRoots}), pick
     * whichever also matches the recoil track's own longitudinal (z) prediction --
     * see the class Javadoc on {@link #fitCascadeVertexJoint} for the near/far branch
     * ambiguity this resolves. Returns {@code null} (defer to the caller's fallback
     * theta) when the line misses the circle entirely.
     */
    ThetaSeed selectPhysicalThetaSeed(RealVector v1, RealVector pV0, TrackParams recoilTrack) {
        double[] roots = transverseCircleRoots(v1, pV0, recoilTrack);
        if (roots == null) {
            return null;
        }
        ThetaSeed best = null;
        double bestResidual = Double.POSITIVE_INFINITY;
        for (double theta : roots) {
            RealVector v2 = v1.subtract(pV0.mapMultiply(theta));
            VertexParams vp = perigeeToVertexParams(recoilTrack, v2.getEntry(0), v2.getEntry(1));
            double residual = FastMath.abs(v2.getEntry(2) - vp.zV);
            if (residual < bestResidual) {
                bestResidual = residual;
                best = new ThetaSeed(theta, v2);
            }
        }
        return best;
    }

    public TwoVertexFitResult fitCascadeVertexJoint(TrackParams eMinusIn, TrackParams ePlusIn, TrackParams recoilIn,
            RealVector v1Init, RealVector v2Init) {
        return fitCascadeVertexJoint(eMinusIn, ePlusIn, recoilIn, v1Init, v2Init, null, null, DEFAULT_TVJ_MAX_ITERATIONS, 1.0e-8);
    }

    /**
     * As above, but with explicit V1/V2 prior covariances -- e.g. the already-fitted V0's own
     * vertex covariance for V1, and that V0's line propagated to the target plane
     * ({@link #propagateLineToPlane}) for V2, instead of the flat/beamspot defaults below. Pass
     * {@code null} for either to fall back to this class's original default for that vertex.
     */
    public TwoVertexFitResult fitCascadeVertexJoint(TrackParams eMinusIn, TrackParams ePlusIn, TrackParams recoilIn,
            RealVector v1Init, RealVector v2Init, RealMatrix v1Cov, RealMatrix v2Cov) {
        return fitCascadeVertexJoint(eMinusIn, ePlusIn, recoilIn, v1Init, v2Init, v1Cov, v2Cov, DEFAULT_TVJ_MAX_ITERATIONS, 1.0e-8);
    }

    /**
     * Joint two-vertex fit: e-/e+ decay vertex V1 and V0/recoil production vertex V2,
     * linked by a straight-line-flight collinearity constraint. Unlike the earlier
     * Newton-Raphson/KKT formulation (which represented that constraint as a symmetric,
     * direction-blind projection of (V2-V1) onto the plane perpendicular to
     * p_v0(V1) = p_e-(V1) + p_e+(V1), and consequently let the solver converge to a
     * physically impossible branch -- V1 on the wrong side of the production point -- in
     * roughly half of truth-matched candidates), this follows Hulsbergen's decay-chain
     * Kalman filter (NIM A552 (2005) 566-575, Eq. 33): the flight from V1 to V2 gets an
     * explicit, signed decay-length parameter theta = l/|p|, related to the two vertices
     * by the *exact* constraint g(V1,theta,V2) = V2 - V1 + theta*p_v0(V1) = 0. Because
     * theta carries its own sign, prior, and row in the covariance, the fit never has to
     * implicitly "discover" which side of V1 the production vertex sits on; a good initial
     * guess plus a weak prior on theta is enough to pin the correct branch.
     *
     * State is the 7-vector [V1(0-2), theta(3), V2(4-6)]; the eMinus/ePlus/recoil track
     * parameters are held fixed at their input values throughout (momenta are read off
     * those fixed tracks at the fitted vertices via {@link #computeMomentumAtVertex}, same
     * convention as {@link #fitCascadeVertex}/{@link CascadeVertexer}) -- there is no
     * 21-dim track sub-state to fit. Each outer iteration applies four constraints in
     * sequence via a single Kalman-gain update shared by measurement-type constraints
     * (track-at-vertex, Eq. 7-19) and the exact geometric constraint (Eq. 24-31, which the
     * paper notes -- after Eq. 32 -- is simply the V-&gt;0 limit of the measurement update):
     * eMinus-at-V1, ePlus-at-V1, recoil-at-V2, then the geometric decay-length constraint.
     * As in {@link #fitCascadeVertex}, the covariance C is reset to its prior each outer
     * pass (the four constraints are the same fixed measurements being re-linearized
     * around a better expansion point, not new independent data), and the update uses the
     * numerically-stable Joseph-form covariance update throughout.
     *
     * @param v2Init initial guess for the production vertex; if null, defaults to this
     *               fitter's own {@code beamPosition} field (tracking frame), matching the
     *               convention used as the initial vertex guess in {@link #fitVertex}.
     */
    public TwoVertexFitResult fitCascadeVertexJoint(TrackParams eMinusIn, TrackParams ePlusIn, TrackParams recoilIn,
            RealVector v1Init, RealVector v2Init, int maxIterations, double tolerance) {
        return fitCascadeVertexJoint(eMinusIn, ePlusIn, recoilIn, v1Init, v2Init, null, null, maxIterations, tolerance);
    }

    /**
     * As above, but with explicit V1/V2 prior covariances (nullable -- {@code null} falls back
     * to this class's original flat/beamspot default for that vertex).
     */
    public TwoVertexFitResult fitCascadeVertexJoint(TrackParams eMinusIn, TrackParams ePlusIn, TrackParams recoilIn,
            RealVector v1Init, RealVector v2Init, RealMatrix v1CovIn, RealMatrix v2CovIn,
            int maxIterations, double tolerance) {

        TrackParams eMinus = eMinusIn.copy();
        TrackParams ePlus = ePlusIn.copy();
        TrackParams recoil = recoilIn.copy();

        // V2 -- the production vertex -- is genuinely known to sit at the beamspot when the
        // caller doesn't supply its own guess (the case used by ThreeTrackVertexer); V1 --
        // the decay vertex -- is not otherwise constrained and is purely determined by the
        // eMinus-/ePlus-at-V1 track constraints, so it always gets a weak/flat prior.
        boolean useBeamspotPriorForV2 = (v2Init == null);
        RealVector v2InitVec = useBeamspotPriorForV2 ? MatrixUtils.createRealVector(beamPosition) : v2Init;

        // Seed theta as the least-squares projection of the initial vertex separation onto
        // the initial flight direction -- this gives the fit the right *sign* immediately
        // from the existing v1Init/v2Init guesses, which is what actually fixes the branch
        // ambiguity that the old symmetric collinearity residual couldn't resolve. Sign
        // matches the constraint g = V2 - V1 + theta*p_v0(V1) = 0 solved for theta at the
        // initial guess: theta = (V1Init-V2Init).pV0Init / |pV0Init|^2.
        RealVector pV0Init = computeMomentumAtVertex(eMinus, v1Init).add(computeMomentumAtVertex(ePlus, v1Init));
        double pV0InitNormSq = pV0Init.dotProduct(pV0Init);
        double thetaInit = (pV0InitNormSq > 0)
                ? v1Init.subtract(v2InitVec).dotProduct(pV0Init) / pV0InitNormSq
                : 0.0;

        // The V0 flight line through v1Init (direction pV0Init) generically crosses the
        // recoil track's own transverse (bending-plane) circle at two points, not one --
        // a true near/far branch ambiguity, distinct from the least-squares projection
        // above (which only fixes the sign relative to the *caller's* v2Init guess, and
        // has no information about the recoil track at all). Newton can walk to either
        // branch once iterating, self-consistently converging to a wrong-but-stable fixed
        // point (large chi2, but no signal in the iteration itself to avoid it). Break the
        // tie analytically before iterating: of the two transverse roots, keep whichever
        // also matches the recoil track's own longitudinal (z) prediction -- the two
        // branches generically disagree on that sharply, since z is fixed only by arc
        // length along the recoil's own helix, independent of which transverse root was
        // used to get there. When the line misses the circle entirely (no real root),
        // there's no branch ambiguity to resolve -- the recoil-at-V2 constraint is a soft
        // measurement, not exact, so the least-squares theta above already sits at the
        // unique, well-posed closest-approach minimum.
        ThetaSeed branchSeed = selectPhysicalThetaSeed(v1Init, pV0Init, recoil);
        if (branchSeed != null) {
            thetaInit = branchSeed.theta;
            v2InitVec = branchSeed.v2;
        }

        return fitCascadeVertexJointCore(eMinus, ePlus, recoil, v1Init, thetaInit, v2InitVec,
                v1CovIn, v2CovIn, useBeamspotPriorForV2, maxIterations, tolerance);
    }

    /**
     * Run the joint fit from an explicitly forced (theta, V2) seed, bypassing
     * {@link #selectPhysicalThetaSeed}'s own branch choice entirely -- lets a caller fit both
     * {@link #transverseCircleRoots} candidates to convergence and choose between them using
     * its own external discriminator, since neither chi2 nor selectPhysicalThetaSeed can tell
     * the two branches apart (see {@link org.hps.recon.vertexing.ThreeTrackVertexer#fit}, which
     * picks between them by comparing each branch's V1 to the independently-fitted V0 vertex).
     * Package-private: not part of the general-purpose API, only used alongside
     * {@link org.hps.recon.vertexing.ThreeTrackVertexer}.
     */
    TwoVertexFitResult fitCascadeVertexJointForcedBranch(TrackParams eMinusIn, TrackParams ePlusIn, TrackParams recoilIn,
            RealVector v1Init, double thetaInit, RealVector v2InitVec, RealMatrix v1CovIn, RealMatrix v2CovIn,
            int maxIterations, double tolerance) {
        TrackParams eMinus = eMinusIn.copy();
        TrackParams ePlus = ePlusIn.copy();
        TrackParams recoil = recoilIn.copy();
        return fitCascadeVertexJointCore(eMinus, ePlus, recoil, v1Init, thetaInit, v2InitVec,
                v1CovIn, v2CovIn, false, maxIterations, tolerance);
    }

    private TwoVertexFitResult fitCascadeVertexJointCore(TrackParams eMinus, TrackParams ePlus, TrackParams recoil,
            RealVector v1Init, double thetaInit, RealVector v2InitVec, RealMatrix v1CovIn, RealMatrix v2CovIn,
            boolean useBeamspotPriorForV2, int maxIterations, double tolerance) {

        RealVector y0 = MatrixUtils.createRealVector(new double[TVJ_STATE_SIZE]);
        y0.setSubVector(TVJ_OFF_V1, v1Init);
        y0.setEntry(TVJ_OFF_THETA, thetaInit);
        y0.setSubVector(TVJ_OFF_V2, v2InitVec);

        RealMatrix priorC = MatrixUtils.createRealMatrix(TVJ_STATE_SIZE, TVJ_STATE_SIZE);
        if (v1CovIn != null) {
            priorC.setSubMatrix(v1CovIn.getData(), TVJ_OFF_V1, TVJ_OFF_V1);
        } else {
            for (int i = 0; i < 3; i++) {
                priorC.setEntry(TVJ_OFF_V1 + i, TVJ_OFF_V1 + i, 100.0);
            }
        }
        priorC.setEntry(TVJ_OFF_THETA, TVJ_OFF_THETA, 100.0);
        if (v2CovIn != null) {
            priorC.setSubMatrix(v2CovIn.getData(), TVJ_OFF_V2, TVJ_OFF_V2);
        } else {
            for (int i = 0; i < 3; i++) {
                double var = useBeamspotPriorForV2 ? beamSize[i] * beamSize[i] : 100.0;
                priorC.setEntry(TVJ_OFF_V2 + i, TVJ_OFF_V2 + i, var);
            }
        }

        RealVector y = y0.copy();
        RealMatrix C = priorC.copy();

        if (DEBUG_JOINT_FIT) {
            System.err.println("JFDEBUG start y0=" + y0 + " thetaInit=" + thetaInit);
        }

        // All four constraint blocks (eMinus-at-V1, ePlus-at-V1, recoil-at-V2, geometric) are
        // stacked into a single simultaneous Kalman update per outer iteration, all evaluated
        // at the same linearization point y. Applying them as separate sequential sub-steps
        // (as tried previously) relinearizes the later sub-steps around whatever wildly wrong
        // point the earlier, nearly-degenerate eMinus/ePlus-only V1 update jumped to -- since
        // that 2-track-only sub-step is poorly conditioned for low-opening-angle pairs, it can
        // walk V1 tens of mm away before the geometric constraint gets a chance to pull it
        // back, and repeating that per iteration produces either a stable wrong fixed point or
        // an undamped 2-cycle oscillation (both observed on real bad events). A single joint
        // update conditions the near-degenerate track direction on the geometric constraint
        // from the start of each pass, removing that failure mode.
        final int TVJ_N_ROWS = 9;
        for (int iteration = 0; iteration < maxIterations; iteration++) {
            RealVector yOld = y.copy();
            C = priorC.copy();
            try {
                RealVector v1 = y.getSubVector(TVJ_OFF_V1, 3);
                double theta = y.getEntry(TVJ_OFF_THETA);
                RealVector v2 = y.getSubVector(TVJ_OFF_V2, 3);

                Constraint cEm = computeTrackConstraint(eMinus, v1);
                Constraint cEp = computeTrackConstraint(ePlus, v1);
                Constraint cRc = computeTrackConstraint(recoil, v2);
                GeometricConstraint gc = computeGeometricConstraint(v1, theta, v2, eMinus, ePlus);

                RealVector cStack = MatrixUtils.createRealVector(new double[TVJ_N_ROWS]);
                cStack.setSubVector(0, cEm.c);
                cStack.setSubVector(2, cEp.c);
                cStack.setSubVector(4, cRc.c);
                cStack.setSubVector(6, gc.g);

                RealMatrix HStack = MatrixUtils.createRealMatrix(TVJ_N_ROWS, TVJ_STATE_SIZE);
                HStack.setSubMatrix(embed(cEm.H, TVJ_OFF_V1).getData(), 0, 0);
                HStack.setSubMatrix(embed(cEp.H, TVJ_OFF_V1).getData(), 2, 0);
                HStack.setSubMatrix(embed(cRc.H, TVJ_OFF_V2).getData(), 4, 0);
                HStack.setSubMatrix(gc.H.getData(), 6, 0);

                // Geometric block (rows 6-8): carries gc.V, the eMinus/ePlus momentum-direction
                // uncertainty propagated through the theta-scaled Jacobian (see
                // computeGeometricConstraint) -- not an exact (V=0) constraint, since pV0(V1)
                // has real uncertainty that must be reflected here or the update over-trusts
                // this block and underestimates V1/V2's reported covariance.
                RealMatrix VStack = MatrixUtils.createRealMatrix(TVJ_N_ROWS, TVJ_N_ROWS);
                VStack.setSubMatrix(cEm.V.getData(), 0, 0);
                VStack.setSubMatrix(cEp.V.getData(), 2, 2);
                VStack.setSubMatrix(cRc.V.getData(), 4, 4);
                VStack.setSubMatrix(gc.V.getData(), 6, 6);

                SequentialUpdate u = sequentialUpdate(y, C, cStack, HStack, VStack);

                // Trust-region cap: the geometric block is an exact (V=0) constraint, so
                // nothing in the linear update damps a bad first step when the initial
                // linearization is poor (near-degenerate eMinus/ePlus opening angle) --
                // observed concretely on real data as V1 leaping ~100 mm and flipping to
                // the unphysical branch within iteration 0, then just self-consistently
                // refining around that wrong point (large, non-vanishing track-at-vertex
                // residuals alongside a near-zero geometric residual). Capping the full
                // state step's norm forces the fit to re-linearize closer to its starting
                // point instead of leaping on the first, least-trustworthy iteration.
                RealVector step = u.y.subtract(y);
                double stepNorm = step.getNorm();
                if (stepNorm > MAX_STEP_NORM) {
                    step = step.mapMultiply(MAX_STEP_NORM / stepNorm);
                }
                y = y.add(step);
                C = u.C;

                if (DEBUG_JOINT_FIT) {
                    System.err.println("JFDEBUG it=" + iteration + " v1=" + y.getSubVector(TVJ_OFF_V1, 3)
                            + " theta=" + y.getEntry(TVJ_OFF_THETA) + " v2=" + y.getSubVector(TVJ_OFF_V2, 3)
                            + " |cEm|=" + cEm.c.getNorm() + " |cEp|=" + cEp.c.getNorm()
                            + " |cRc|=" + cRc.c.getNorm() + " |g|=" + gc.g.getNorm());
                }

                if (!Double.isFinite(y.getNorm())) {
                    y = yOld;
                    break;
                }
                if (y.subtract(yOld).getNorm() < tolerance) {
                    break;
                }
            } catch (Exception e) {
                if (DEBUG_JOINT_FIT) {
                    System.err.println("JFDEBUG it=" + iteration + " EXCEPTION " + e);
                }
                y = yOld;
                break;
            }
        }

        RealVector v1Final = y.getSubVector(TVJ_OFF_V1, 3);
        double thetaFinal = y.getEntry(TVJ_OFF_THETA);
        RealVector v2Final = y.getSubVector(TVJ_OFF_V2, 3);

        RealMatrix v1Cov = C.getSubMatrix(TVJ_OFF_V1, TVJ_OFF_V1 + 2, TVJ_OFF_V1, TVJ_OFF_V1 + 2);
        RealMatrix v2Cov = C.getSubMatrix(TVJ_OFF_V2, TVJ_OFF_V2 + 2, TVJ_OFF_V2, TVJ_OFF_V2 + 2);

        // chi2/ndf: recompute each of the 4 constraint blocks at the converged state and
        // sum r^T*Vblock^-1*r (Hulsbergen Eq. 19/31); ndf follows the standard
        // measurements-minus-free-parameters convention used elsewhere in this class
        // (fit(): ndf=2*nTracks-3; fitCascadeVertex(): ndf=1) -- 3 track constraints x 2
        // rows + 1 geometric constraint x 3 rows = 9 measurements, minus 7 free
        // parameters = 2.
        Constraint cEmFinal = computeTrackConstraint(eMinus, v1Final);
        Constraint cEpFinal = computeTrackConstraint(ePlus, v1Final);
        Constraint cRcFinal = computeTrackConstraint(recoil, v2Final);
        GeometricConstraint gcFinal = computeGeometricConstraint(v1Final, thetaFinal, v2Final, eMinus, ePlus);

        double chi2 = 0.0;
        chi2 += chi2Contribution(cEmFinal.c, cEmFinal.V);
        chi2 += chi2Contribution(cEpFinal.c, cEpFinal.V);
        chi2 += chi2Contribution(cRcFinal.c, cRcFinal.V);
        chi2 += chi2Contribution(gcFinal.g, gcFinal.V);

        int ndf = 2;

        TrackMomentum eMinusMomentum = new TrackMomentum(
                computeMomentumAtVertex(eMinus, v1Final), computeMomentumCovariance(eMinus, v1Final));
        TrackMomentum ePlusMomentum = new TrackMomentum(
                computeMomentumAtVertex(ePlus, v1Final), computeMomentumCovariance(ePlus, v1Final));
        TrackMomentum recoilMomentum = new TrackMomentum(
                computeMomentumAtVertex(recoil, v2Final), computeMomentumCovariance(recoil, v2Final));

        List<TrackParams> fittedTracks = new ArrayList<>();
        fittedTracks.add(eMinus);
        fittedTracks.add(ePlus);
        fittedTracks.add(recoil);

        return new TwoVertexFitResult(v1Final, v1Cov, v2Final, v2Cov, chi2, ndf,
                eMinusMomentum, ePlusMomentum, recoilMomentum, fittedTracks);
    }

    private static final int TVJX_OFF_V1 = 0;
    private static final int TVJX_OFF_THETA = 3;
    private static final int TVJX_OFF_V2FREE = 4; // holds only v2[1], v2[2] (the free transverse coords)
    private static final int TVJX_STATE_SIZE = 6;

    /**
     * As {@link #fitCascadeVertexJoint}, but V2's tracking-index-0 (beam-direction/target-z)
     * coordinate is held fixed at {@code v2FixedX} instead of being a free fit parameter --
     * for topologies (e.g. near-collinear V0+recoil, which lack the ~30mrad minimum opening
     * angle that ordinary top/bottom-paired 2-track vertexing enforces) where that coordinate
     * is only very weakly constrained by the track geometry, fixing it to the known target
     * position removes the poorly-determined degree of freedom entirely rather than merely
     * down-weighting it via a soft prior. V1 stays fully free in all 3 dimensions, and V2's
     * other two (transverse) coordinates keep fitting freely. State is the 6-vector
     * [V1(0-2), theta(3), v2free(4-5)] where v2free=[v2.y,v2.z]; the returned
     * {@link TwoVertexFitResult} still carries a full 3-vector/3x3-cov V2 (fixed
     * coordinate reported as exactly {@code v2FixedX} with exactly-zero variance/covariance
     * in that row/column), so downstream consumers need no changes.
     */
    public TwoVertexFitResult fitCascadeVertexJointFixedV2X(TrackParams eMinusIn, TrackParams ePlusIn, TrackParams recoilIn,
            RealVector v1Init, RealVector v2Init, double v2FixedX, RealMatrix v1Cov, RealMatrix v2Cov) {
        return fitCascadeVertexJointFixedV2X(eMinusIn, ePlusIn, recoilIn, v1Init, v2Init, v2FixedX, v1Cov, v2Cov,
                DEFAULT_TVJ_MAX_ITERATIONS, 1.0e-8);
    }

    public TwoVertexFitResult fitCascadeVertexJointFixedV2X(TrackParams eMinusIn, TrackParams ePlusIn, TrackParams recoilIn,
            RealVector v1Init, RealVector v2Init, double v2FixedX, RealMatrix v1CovIn, RealMatrix v2CovIn,
            int maxIterations, double tolerance) {

        TrackParams eMinus = eMinusIn.copy();
        TrackParams ePlus = ePlusIn.copy();
        TrackParams recoil = recoilIn.copy();

        boolean useBeamspotPriorForV2 = (v2Init == null);
        RealVector v2InitVec = (useBeamspotPriorForV2 ? MatrixUtils.createRealVector(beamPosition) : v2Init).copy();
        v2InitVec.setEntry(0, v2FixedX);

        // Same theta seeding as fitCascadeVertexJoint (least-squares projection, then the
        // transverseCircleRoots/selectPhysicalThetaSeed branch override) -- both operate on
        // full 3-vectors and are unaffected by fixing V2's index-0 coordinate afterward.
        RealVector pV0Init = computeMomentumAtVertex(eMinus, v1Init).add(computeMomentumAtVertex(ePlus, v1Init));
        double pV0InitNormSq = pV0Init.dotProduct(pV0Init);
        double thetaInit = (pV0InitNormSq > 0)
                ? v1Init.subtract(v2InitVec).dotProduct(pV0Init) / pV0InitNormSq
                : 0.0;

        ThetaSeed branchSeed = selectPhysicalThetaSeed(v1Init, pV0Init, recoil);
        if (branchSeed != null) {
            thetaInit = branchSeed.theta;
            v2InitVec = branchSeed.v2.copy();
            v2InitVec.setEntry(0, v2FixedX);
        }

        return fitCascadeVertexJointFixedV2XCore(eMinus, ePlus, recoil, v1Init, thetaInit, v2InitVec, v2FixedX,
                v1CovIn, v2CovIn, useBeamspotPriorForV2, maxIterations, tolerance);
    }

    /**
     * Run the fixed-V2-coordinate joint fit from an explicitly forced (theta, V2) seed,
     * bypassing {@link #selectPhysicalThetaSeed}'s own branch choice -- the fixed-V2X
     * counterpart of {@link #fitCascadeVertexJointForcedBranch}, used the same way by
     * {@link ThreeTrackVertexer#fit} to fit both {@link #transverseCircleRoots} candidates
     * and choose between them externally. Package-private, not part of the general-purpose
     * API.
     */
    TwoVertexFitResult fitCascadeVertexJointFixedV2XForcedBranch(TrackParams eMinusIn, TrackParams ePlusIn, TrackParams recoilIn,
            RealVector v1Init, double thetaInit, RealVector v2InitVec, double v2FixedX,
            RealMatrix v1CovIn, RealMatrix v2CovIn, int maxIterations, double tolerance) {
        TrackParams eMinus = eMinusIn.copy();
        TrackParams ePlus = ePlusIn.copy();
        TrackParams recoil = recoilIn.copy();
        RealVector v2Fixed = v2InitVec.copy();
        v2Fixed.setEntry(0, v2FixedX);
        return fitCascadeVertexJointFixedV2XCore(eMinus, ePlus, recoil, v1Init, thetaInit, v2Fixed, v2FixedX,
                v1CovIn, v2CovIn, false, maxIterations, tolerance);
    }

    private TwoVertexFitResult fitCascadeVertexJointFixedV2XCore(TrackParams eMinus, TrackParams ePlus, TrackParams recoil,
            RealVector v1Init, double thetaInit, RealVector v2InitVec, double v2FixedX, RealMatrix v1CovIn, RealMatrix v2CovIn,
            boolean useBeamspotPriorForV2, int maxIterations, double tolerance) {

        RealVector y0 = MatrixUtils.createRealVector(new double[TVJX_STATE_SIZE]);
        y0.setSubVector(TVJX_OFF_V1, v1Init);
        y0.setEntry(TVJX_OFF_THETA, thetaInit);
        y0.setEntry(TVJX_OFF_V2FREE, v2InitVec.getEntry(1));
        y0.setEntry(TVJX_OFF_V2FREE + 1, v2InitVec.getEntry(2));

        RealMatrix priorC = MatrixUtils.createRealMatrix(TVJX_STATE_SIZE, TVJX_STATE_SIZE);
        if (v1CovIn != null) {
            priorC.setSubMatrix(v1CovIn.getData(), TVJX_OFF_V1, TVJX_OFF_V1);
        } else {
            for (int i = 0; i < 3; i++) {
                priorC.setEntry(TVJX_OFF_V1 + i, TVJX_OFF_V1 + i, 100.0);
            }
        }
        priorC.setEntry(TVJX_OFF_THETA, TVJX_OFF_THETA, 100.0);
        if (v2CovIn != null) {
            priorC.setSubMatrix(v2CovIn.getSubMatrix(1, 2, 1, 2).getData(), TVJX_OFF_V2FREE, TVJX_OFF_V2FREE);
        } else {
            for (int i = 0; i < 2; i++) {
                double var = useBeamspotPriorForV2 ? beamSize[i + 1] * beamSize[i + 1] : 100.0;
                priorC.setEntry(TVJX_OFF_V2FREE + i, TVJX_OFF_V2FREE + i, var);
            }
        }

        RealVector y = y0.copy();
        RealMatrix C = priorC.copy();

        if (DEBUG_JOINT_FIT) {
            System.err.println("JFXDEBUG start y0=" + y0 + " thetaInit=" + thetaInit + " v2FixedX=" + v2FixedX);
        }

        final int TVJX_N_ROWS = 9;
        for (int iteration = 0; iteration < maxIterations; iteration++) {
            RealVector yOld = y.copy();
            C = priorC.copy();
            try {
                RealVector v1 = y.getSubVector(TVJX_OFF_V1, 3);
                double theta = y.getEntry(TVJX_OFF_THETA);
                RealVector v2Full = MatrixUtils.createRealVector(new double[]{
                        v2FixedX, y.getEntry(TVJX_OFF_V2FREE), y.getEntry(TVJX_OFF_V2FREE + 1)});

                Constraint cEm = computeTrackConstraint(eMinus, v1);
                Constraint cEp = computeTrackConstraint(ePlus, v1);
                Constraint cRc = computeTrackConstraint(recoil, v2Full);
                GeometricConstraint gc = computeGeometricConstraint(v1, theta, v2Full, eMinus, ePlus);

                RealVector cStack = MatrixUtils.createRealVector(new double[TVJX_N_ROWS]);
                cStack.setSubVector(0, cEm.c);
                cStack.setSubVector(2, cEp.c);
                cStack.setSubVector(4, cRc.c);
                cStack.setSubVector(6, gc.g);

                // cRc.H (2x3, columns = v2.x,y,z) has its column 0 (the now-fixed coordinate)
                // dropped before embedding; gc.H (3x7, in the original TVJ_OFF_* column
                // convention) has its single V2-x column (index TVJ_OFF_V2) dropped, which
                // shifts the V2-y/z columns left to land exactly at TVJX_OFF_V2FREE.
                RealMatrix HStack = MatrixUtils.createRealMatrix(TVJX_N_ROWS, TVJX_STATE_SIZE);
                HStack.setSubMatrix(embedFixedV2X(cEm.H, TVJX_OFF_V1).getData(), 0, 0);
                HStack.setSubMatrix(embedFixedV2X(cEp.H, TVJX_OFF_V1).getData(), 2, 0);
                HStack.setSubMatrix(embedFixedV2X(dropColumn(cRc.H, 0), TVJX_OFF_V2FREE).getData(), 4, 0);
                HStack.setSubMatrix(dropColumn(gc.H, TVJ_OFF_V2).getData(), 6, 0);

                RealMatrix VStack = MatrixUtils.createRealMatrix(TVJX_N_ROWS, TVJX_N_ROWS);
                VStack.setSubMatrix(cEm.V.getData(), 0, 0);
                VStack.setSubMatrix(cEp.V.getData(), 2, 2);
                VStack.setSubMatrix(cRc.V.getData(), 4, 4);
                VStack.setSubMatrix(gc.V.getData(), 6, 6);

                SequentialUpdate u = sequentialUpdate(y, C, cStack, HStack, VStack);

                RealVector step = u.y.subtract(y);
                double stepNorm = step.getNorm();
                if (stepNorm > MAX_STEP_NORM) {
                    step = step.mapMultiply(MAX_STEP_NORM / stepNorm);
                }
                y = y.add(step);
                C = u.C;

                if (DEBUG_JOINT_FIT) {
                    System.err.println("JFXDEBUG it=" + iteration + " v1=" + y.getSubVector(TVJX_OFF_V1, 3)
                            + " theta=" + y.getEntry(TVJX_OFF_THETA) + " v2free=" + y.getSubVector(TVJX_OFF_V2FREE, 2)
                            + " |cEm|=" + cEm.c.getNorm() + " |cEp|=" + cEp.c.getNorm()
                            + " |cRc|=" + cRc.c.getNorm() + " |g|=" + gc.g.getNorm());
                }

                if (!Double.isFinite(y.getNorm())) {
                    y = yOld;
                    break;
                }
                if (y.subtract(yOld).getNorm() < tolerance) {
                    break;
                }
            } catch (Exception e) {
                if (DEBUG_JOINT_FIT) {
                    System.err.println("JFXDEBUG it=" + iteration + " EXCEPTION " + e);
                }
                y = yOld;
                break;
            }
        }

        RealVector v1Final = y.getSubVector(TVJX_OFF_V1, 3);
        double thetaFinal = y.getEntry(TVJX_OFF_THETA);
        RealVector v2Final = MatrixUtils.createRealVector(new double[]{
                v2FixedX, y.getEntry(TVJX_OFF_V2FREE), y.getEntry(TVJX_OFF_V2FREE + 1)});

        RealMatrix v1Cov = C.getSubMatrix(TVJX_OFF_V1, TVJX_OFF_V1 + 2, TVJX_OFF_V1, TVJX_OFF_V1 + 2);
        // Fixed coordinate's row/col (index 0) stays exactly 0, per the confirmed reporting
        // convention -- only the free (y,z) 2x2 sub-block comes from the converged fit.
        RealMatrix v2Cov = MatrixUtils.createRealMatrix(3, 3);
        v2Cov.setSubMatrix(
                C.getSubMatrix(TVJX_OFF_V2FREE, TVJX_OFF_V2FREE + 1, TVJX_OFF_V2FREE, TVJX_OFF_V2FREE + 1).getData(),
                1, 1);

        Constraint cEmFinal = computeTrackConstraint(eMinus, v1Final);
        Constraint cEpFinal = computeTrackConstraint(ePlus, v1Final);
        Constraint cRcFinal = computeTrackConstraint(recoil, v2Final);
        GeometricConstraint gcFinal = computeGeometricConstraint(v1Final, thetaFinal, v2Final, eMinus, ePlus);

        double chi2 = 0.0;
        chi2 += chi2Contribution(cEmFinal.c, cEmFinal.V);
        chi2 += chi2Contribution(cEpFinal.c, cEpFinal.V);
        chi2 += chi2Contribution(cRcFinal.c, cRcFinal.V);
        chi2 += chi2Contribution(gcFinal.g, gcFinal.V);

        // ndf: 3 track constraints x 2 rows + 1 geometric constraint x 3 rows = 9
        // measurements, minus 6 free parameters (V1 fully free, theta, V2's 2 free
        // transverse coords) = 3.
        int ndf = 3;

        TrackMomentum eMinusMomentum = new TrackMomentum(
                computeMomentumAtVertex(eMinus, v1Final), computeMomentumCovariance(eMinus, v1Final));
        TrackMomentum ePlusMomentum = new TrackMomentum(
                computeMomentumAtVertex(ePlus, v1Final), computeMomentumCovariance(ePlus, v1Final));
        TrackMomentum recoilMomentum = new TrackMomentum(
                computeMomentumAtVertex(recoil, v2Final), computeMomentumCovariance(recoil, v2Final));

        List<TrackParams> fittedTracks = new ArrayList<>();
        fittedTracks.add(eMinus);
        fittedTracks.add(ePlus);
        fittedTracks.add(recoil);

        return new TwoVertexFitResult(v1Final, v1Cov, v2Final, v2Cov, chi2, ndf,
                eMinusMomentum, ePlusMomentum, recoilMomentum, fittedTracks);
    }

    /** Zero-pad a Jacobian's columns into a {@code TVJX_STATE_SIZE}-wide state, starting at colOffset. */
    private static RealMatrix embedFixedV2X(RealMatrix H, int colOffset) {
        RealMatrix full = MatrixUtils.createRealMatrix(H.getRowDimension(), TVJX_STATE_SIZE);
        full.setSubMatrix(H.getData(), 0, colOffset);
        return full;
    }

    /** Drop column {@code dropCol} from H, shifting later columns left by one. */
    private static RealMatrix dropColumn(RealMatrix H, int dropCol) {
        int rows = H.getRowDimension();
        int cols = H.getColumnDimension();
        RealMatrix reduced = MatrixUtils.createRealMatrix(rows, cols - 1);
        int destCol = 0;
        for (int c = 0; c < cols; c++) {
            if (c == dropCol) {
                continue;
            }
            reduced.setColumnVector(destCol++, H.getColumnVector(c));
        }
        return reduced;
    }

    /** r^T*V^-1*r, or 0.0 if V is singular (mirrors the defensive pattern used elsewhere in this class). */
    private static double chi2Contribution(RealVector c, RealMatrix V) {
        try {
            RealMatrix VInv = new LUDecomposition(V).getSolver().getInverse();
            return c.dotProduct(VInv.operate(c));
        } catch (Exception e) {
            return 0.0;
        }
    }

    /** Zero-pad a Jacobian's columns into the full {@code TVJ_STATE_SIZE}-wide state, starting at colOffset. */
    private static RealMatrix embed(RealMatrix H3, int colOffset) {
        RealMatrix full = MatrixUtils.createRealMatrix(H3.getRowDimension(), TVJ_STATE_SIZE);
        full.setSubMatrix(H3.getData(), 0, colOffset);
        return full;
    }

    private static class SequentialUpdate {
        final RealVector y;
        final RealMatrix C;

        SequentialUpdate(RealVector y, RealMatrix C) {
            this.y = y;
            this.C = C;
        }
    }

    /**
     * One step of Hulsbergen's sequential Kalman-gain recursion, shared by
     * measurement-type constraints (Eq. 7-19, V = real measurement covariance) and exact
     * constraints (Eq. 24-31, V = 0) -- the paper notes after Eq. 32 that the two are
     * identical once V (for a measurement) or 0 (for an exact constraint) is substituted
     * in, so a single implementation covers both. Uses the same numerically-stable
     * Joseph-form covariance update as {@link #fitCascadeVertex}.
     */
    private static SequentialUpdate sequentialUpdate(RealVector y, RealMatrix C, RealVector c, RealMatrix H, RealMatrix V) {
        RealMatrix S = H.multiply(C).multiply(H.transpose()).add(V);
        RealMatrix K = C.multiply(H.transpose()).multiply(new LUDecomposition(S).getSolver().getInverse());
        RealVector yNew = y.subtract(K.operate(c));
        RealMatrix I = MatrixUtils.createRealIdentityMatrix(y.getDimension());
        RealMatrix ImKH = I.subtract(K.multiply(H));
        RealMatrix Cnew = ImKH.multiply(C).multiply(ImKH.transpose()).add(K.multiply(V).multiply(K.transpose()));
        return new SequentialUpdate(yNew, Cnew);
    }

    private static class GeometricConstraint {
        final RealVector g;
        final RealMatrix H;
        final RealMatrix V;

        GeometricConstraint(RealVector g, RealMatrix H, RealMatrix V) {
            this.g = g;
            this.H = H;
            this.V = V;
        }
    }

    /**
     * Hulsbergen's Eq. 33 decay-length constraint g(V1,theta,V2) = V2 - V1 + theta*p_v0(V1)
     * = 0, with a fully analytic Jacobian (no finite differences): dg/dV2 = I,
     * dg/dtheta = p_v0(V1), dg/dV1 = -I + theta * d(p_v0)/dV1, where d(p_v0)/dV1 is the sum
     * of the two daughters' {@link #computeMomentumVertexDerivatives}.
     *
     * <p>Unlike a true Eq. 32 "exact" constraint (V=0), g depends on p_v0(V1), which carries
     * real uncertainty from the eMinus/ePlus track parameters (direction/curvature), amplified
     * by theta -- for a near-collinear V0+recoil topology theta can be large, so this term is
     * not negligible. dg/dpEm = dg/dpEp = theta*I (each daughter's momentum enters g solely
     * through pV0 = pEm+pEp, linearly scaled by theta), so the propagated covariance is
     * theta^2 * (covariance of pEm + covariance of pEp), reusing the same per-track momentum
     * covariance already computed for reporting fitted momenta ({@link #computeMomentumCovariance}).
     * Leaving V=0 here (the prior behavior) causes the Kalman update to over-trust this
     * constraint, underestimating the reported V1/V2 position covariances.
     */
    private GeometricConstraint computeGeometricConstraint(RealVector v1, double theta, RealVector v2,
            TrackParams eMinus, TrackParams ePlus) {
        RealVector pEm = computeMomentumAtVertex(eMinus, v1);
        RealVector pEp = computeMomentumAtVertex(ePlus, v1);
        RealVector pV0 = pEm.add(pEp);
        RealVector g = v2.subtract(v1).add(pV0.mapMultiply(theta));

        RealMatrix dpV0dV1 = computeMomentumVertexDerivatives(eMinus, v1).add(computeMomentumVertexDerivatives(ePlus, v1));
        RealMatrix dGdV1 = MatrixUtils.createRealIdentityMatrix(3).scalarMultiply(-1.0).add(dpV0dV1.scalarMultiply(theta));

        RealMatrix H = MatrixUtils.createRealMatrix(3, TVJ_STATE_SIZE);
        H.setSubMatrix(dGdV1.getData(), 0, TVJ_OFF_V1);
        H.setColumnVector(TVJ_OFF_THETA, pV0);
        H.setSubMatrix(MatrixUtils.createRealIdentityMatrix(3).getData(), 0, TVJ_OFF_V2);

        RealMatrix pCov = computeMomentumCovariance(eMinus, v1).add(computeMomentumCovariance(ePlus, v1));
        RealMatrix V = pCov.scalarMultiply(theta * theta);

        return new GeometricConstraint(g, H, V);
    }

    /**
     * Build a LineParams representing an already-fitted V0's momentum direction, carrying
     * the full 6x6 position-momentum joint covariance (no block-diagonal approximation),
     * for use as the "line" input to {@link #fitCascadeVertex}. BilliorVertex stores its
     * position/momentum/covariances in the detector frame; this fitter's helix/line math
     * operates in the tracking frame (same convention as BilliorVertexer's internal fit),
     * so everything is rotated to tracking frame here.
     */
    public static LineParams lineFromV0(BilliorVertex v0Vertex) {
        Hep3Vector vDet = v0Vertex.getPosition();
        Hep3Vector p1Det = v0Vertex.getFittedMomentum(0);
        Hep3Vector p2Det = v0Vertex.getFittedMomentum(1);
        Hep3Vector pDet = new BasicHep3Vector(p1Det.x() + p2Det.x(), p1Det.y() + p2Det.y(), p1Det.z() + p2Det.z());

        Hep3Vector vTrk = CoordinateTransformations.transformVectorToTracking(vDet);
        Hep3Vector pTrk = CoordinateTransformations.transformVectorToTracking(pDet);

        Matrix covVVDet = v0Vertex.getCovMatrix();
        List<Matrix> covTrkMom = v0Vertex.getFittedMomentumCovariance();
        Matrix covPPDet = MatrixOp.add(MatrixOp.add(covTrkMom.get(0), covTrkMom.get(1)),
                MatrixOp.add(covTrkMom.get(2), MatrixOp.transposed(covTrkMom.get(2))));
        Matrix covVPDet = v0Vertex.getVertexV0MomentumCovariance();

        Matrix covVVTrk = CoordinateTransformations.transformMatrixToTracking(covVVDet);
        Matrix covPPTrk = CoordinateTransformations.transformMatrixToTracking(covPPDet);
        Matrix covVPTrk = CoordinateTransformations.transformMatrixToTracking(covVPDet);

        RealMatrix cov6 = MatrixUtils.createRealMatrix(6, 6);
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                cov6.setEntry(i, j, covVVTrk.e(i, j));
                cov6.setEntry(3 + i, 3 + j, covPPTrk.e(i, j));
                cov6.setEntry(i, 3 + j, covVPTrk.e(i, j));
                cov6.setEntry(3 + j, i, covVPTrk.e(i, j));
            }
        }

        return new LineParams(vTrk.x(), vTrk.y(), vTrk.z(), pTrk.x(), pTrk.y(), pTrk.z(), cov6);
    }

    /** Position and covariance where a {@link LineParams} line crosses a fixed plane. */
    public static class LinePlaneProjection {
        public final RealVector position;
        public final RealMatrix cov;

        public LinePlaneProjection(RealVector position, RealMatrix cov) {
            this.position = position;
            this.cov = cov;
        }
    }

    /**
     * Propagate a line (e.g. an already-fitted V0's flight line from {@link #lineFromV0}) to
     * the plane x = xPlane (tracking frame), carrying its covariance along. The line crosses
     * the plane at a fixed, deterministic x = xPlane by construction, so the propagated
     * covariance's x-row/column is exactly zero from the Jacobian alone -- {@code sigmaXFloor}
     * is substituted in for that entry so a caller using this as a Kalman prior doesn't rigidly
     * pin x forever. When used as a Kalman prior, this must be a weak/uninformative value
     * (e.g. the variance=100 -> sigma=10 convention used elsewhere in this class for V1/theta),
     * NOT {@link #beamSize}[0] -- that field is a disconnected, hardcoded ~1-micron default
     * never intended as a covariance floor, and using it here rigidly pins the propagated x to
     * xPlane on every outer iteration (priorC is rebuilt from this covariance each time),
     * preventing the fit from ever moving away from the target plane.
     */
    public static LinePlaneProjection propagateLineToPlane(LineParams line, double xPlane, double sigmaXFloor) {
        double s = (xPlane - line.x0) / line.dx;
        double yProp = line.y0 + s * line.dy;
        double zProp = line.z0 + s * line.dz;

        RealVector position = MatrixUtils.createRealVector(new double[]{xPlane, yProp, zProp});

        RealMatrix J = MatrixUtils.createRealMatrix(3, 6);
        // Row 0 (x): always exactly xPlane, independent of the line's parameters -> all zero.
        // Row 1 (y): yProp = y0 + s*dy, s = (xPlane-x0)/dx
        J.setEntry(1, 0, -line.dy / line.dx);          // d(yProp)/d(x0)
        J.setEntry(1, 1, 1.0);                          // d(yProp)/d(y0)
        J.setEntry(1, 3, -s * line.dy / line.dx);       // d(yProp)/d(dx)
        J.setEntry(1, 4, s);                            // d(yProp)/d(dy)
        // Row 2 (z): zProp = z0 + s*dz
        J.setEntry(2, 0, -line.dz / line.dx);           // d(zProp)/d(x0)
        J.setEntry(2, 2, 1.0);                          // d(zProp)/d(z0)
        J.setEntry(2, 3, -s * line.dz / line.dx);       // d(zProp)/d(dx)
        J.setEntry(2, 5, s);                            // d(zProp)/d(dz)

        RealMatrix cov = J.multiply(line.cov).multiply(J.transpose());
        cov.setEntry(0, 0, sigmaXFloor * sigmaXFloor);

        return new LinePlaneProjection(position, cov);
    }

    /**
     * Propagate a curved track (perigee helix, e.g. the recoil electron) to the plane
     * x = xPlane (tracking frame), carrying its covariance along via the same
     * turning-angle parameterization used by {@link #perigeeToVertexParams}/
     * {@link #propagateTrackCovariance} -- but solving for the turning angle phiV that
     * puts the helix at a given x, instead of the one nearest a given (x,y) vertex guess.
     * As with {@link #propagateLineToPlane}, x is fixed by construction so the propagated
     * covariance's x-row/column is set from {@code sigmaXFloor} rather than the (otherwise
     * exactly zero) Jacobian entry.
     */
    public static LinePlaneProjection propagateTrackToPlane(TrackParams track, double xPlane, double sigmaXFloor) {
        double omega = track.omega;
        double R = 1.0 / FastMath.abs(omega);
        double sign = FastMath.signum(omega);
        double d0 = track.d0;
        double phi0 = track.phi0;
        double tanLambda = track.tanLambda;
        double z0 = track.z0;

        // xc,yc: helix center; xV = xc - sin(phiV)/omega, yV = yc + cos(phiV)/omega (inverting
        // the same relations used in perigeeToVertexParams, using the identity sign*R = 1/omega).
        double xc = FastMath.sin(phi0) * (1.0 / omega - d0);
        double yc = -FastMath.cos(phi0) * (1.0 / omega - d0);

        double sinPhiV = omega * (xc - xPlane);
        sinPhiV = FastMath.min(1.0, FastMath.max(-1.0, sinPhiV));
        double cand1 = FastMath.asin(sinPhiV);
        double cand2 = FastMath.PI - cand1;
        double dphi1 = normalizeAngle(cand1 - phi0);
        double dphi2 = normalizeAngle(cand2 - phi0);
        // Two candidate crossings of the plane exist (helix circle meets the line twice);
        // pick the one reached by the smaller turning angle from the perigee, i.e. the first
        // crossing along the track's flight path.
        boolean useCand1 = FastMath.abs(dphi1) <= FastMath.abs(dphi2);
        double phiV = useCand1 ? cand1 : cand2;
        double dphi = useCand1 ? dphi1 : dphi2;

        // See perigeeToVertexParams: s = -sign(omega)*R*dphi, not R*dphi.
        double s = -sign * R * dphi;
        double cosPhiV = FastMath.cos(phiV);
        double sinPhiV2 = FastMath.sin(phiV);
        double yProp = yc + cosPhiV / omega;
        double zProp = z0 + s * tanLambda;

        RealVector position = MatrixUtils.createRealVector(new double[]{xPlane, yProp, zProp});

        double dPhiVDd0 = -omega * FastMath.sin(phi0) / cosPhiV;
        double dPhiVDphi0 = FastMath.cos(phi0) * (1.0 - omega * d0) / cosPhiV;
        double dPhiVDomega = (sinPhiV2 - FastMath.sin(phi0)) / (omega * cosPhiV);

        double dyDd0 = FastMath.cos(phi0) - (sinPhiV2 / omega) * dPhiVDd0;
        double dyDphi0 = FastMath.sin(phi0) * (1.0 / omega - d0) - (sinPhiV2 / omega) * dPhiVDphi0;
        double dyDomega = (FastMath.cos(phi0) - cosPhiV) / (omega * omega) - (sinPhiV2 / omega) * dPhiVDomega;

        // z = z0 + s*tanLambda with s = -sign(omega)*R*dphi: each R*(phiV-derivative) term
        // below picks up the same -sign factor; the explicit s/omega term does not.
        double dzDd0 = -sign * tanLambda * R * dPhiVDd0;
        double dzDphi0 = -sign * (-tanLambda * R + tanLambda * R * dPhiVDphi0);
        double dzDomega = -s * tanLambda / omega - sign * tanLambda * R * dPhiVDomega;
        double dzDz0 = 1.0;
        double dzDtl = s;

        RealMatrix J = MatrixUtils.createRealMatrix(2, 5);
        J.setRow(0, new double[]{dyDd0, dyDphi0, dyDomega, 0.0, 0.0});
        J.setRow(1, new double[]{dzDd0, dzDphi0, dzDomega, dzDz0, dzDtl});

        RealMatrix covYZ = J.multiply(track.cov).multiply(J.transpose());

        RealMatrix cov = MatrixUtils.createRealMatrix(3, 3);
        cov.setEntry(0, 0, sigmaXFloor * sigmaXFloor);
        cov.setEntry(1, 1, covYZ.getEntry(0, 0));
        cov.setEntry(1, 2, covYZ.getEntry(0, 1));
        cov.setEntry(2, 1, covYZ.getEntry(1, 0));
        cov.setEntry(2, 2, covYZ.getEntry(1, 1));

        return new LinePlaneProjection(position, cov);
    }

    private static double normalizeAngle(double a) {
        while (a > FastMath.PI) {
            a -= 2.0 * FastMath.PI;
        }
        while (a < -FastMath.PI) {
            a += 2.0 * FastMath.PI;
        }
        return a;
    }

    /**
     * Joint kinematic fit using soft (penalized) constraints via the Gain Matrix formalism.
     * All constraints (track + 4-momentum) are satisfied simultaneously.
     *
     * This solves: minimize (x - x0)^T W (x - x0) + h(x)^T V^{-1} h(x)
     * where x = [vertex, track1_params, track2_params, ...] is the full state vector,
     * x0 is the initial/measured values, W is the weight matrix (inverse covariance),
     * h(x) are the constraint residuals, and V is the constraint covariance.
     *
     * Because V > 0, constraints are soft (approximately satisfied, weighted by their
     * uncertainty). This is equivalent to a Kalman filter update treating h(x)=0 as
     * a measurement with noise V. True Lagrange multipliers (hard constraints) are
     * recovered only in the limit V -> 0.
     *
     * Constraints:
     * - Track constraints: each track must pass through the vertex (z matching)
     * - 4-momentum constraint: sum of track momenta = beam momentum
     */
    public FitResult fitSoftConstrained(List<TrackParams> inputTracks,
                                           RealVector vertexConstraint,
                                           RealMatrix vertexConstraintCov,
                                           RealVector fourMomentumConstraint,
                                           RealMatrix fourMomentumConstraintCov,
                                           int maxIterations,
                                           double tolerance) {

        int nTracks = inputTracks.size();
        int nTrackParams = 5;
        int nVertexParams = 3;
        int stateSize = nVertexParams + nTracks * nTrackParams;

        // Number of constraints: 2 per track (transverse + longitudinal) + 3 momentum constraints
        // Note: We only constrain 3-momentum, not energy, because for non-collinear tracks
        // sum(E_i) > E_beam due to triangle inequality, making energy constraint unphysical
        //
        // Track constraints are SOFT (covariance propagated from track errors):
        //   transverse:   r - R = 0  (vertex must lie on the helix circle in XY)
        //   longitudinal: zV - z_predicted = 0
        int nTrackConstraints = 2 * nTracks;
        int nMomConstraints = (fourMomentumConstraint != null) ? 3 : 0;
        int nConstraints = nTrackConstraints + nMomConstraints;

        // Make working copies of tracks
        List<TrackParams> tracks = new ArrayList<>();
        for (TrackParams t : inputTracks) {
            tracks.add(t.copy());
        }

        // Build initial state vector x0 = [vertex, track1, track2, ...]
        RealVector x0 = MatrixUtils.createRealVector(new double[stateSize]);

        // Initial vertex from constraint or average of track perigees
        if (vertexConstraint != null) {
            x0.setEntry(0, vertexConstraint.getEntry(0));
            x0.setEntry(1, vertexConstraint.getEntry(1));
            x0.setEntry(2, vertexConstraint.getEntry(2));
        } else {
            double xInit = 0, yInit = 0, zInit = 0;
            for (TrackParams t : tracks) {
                xInit += -t.d0 * FastMath.sin(t.phi0);
                yInit += t.d0 * FastMath.cos(t.phi0);
                zInit += t.z0;
            }
            x0.setEntry(0, xInit / nTracks);
            x0.setEntry(1, yInit / nTracks);
            x0.setEntry(2, zInit / nTracks);
        }

        // Initial track parameters
        for (int i = 0; i < nTracks; i++) {
            TrackParams t = tracks.get(i);
            int offset = nVertexParams + i * nTrackParams;
            x0.setEntry(offset + 0, t.d0);
            x0.setEntry(offset + 1, t.phi0);
            x0.setEntry(offset + 2, t.omega);
            x0.setEntry(offset + 3, t.z0);
            x0.setEntry(offset + 4, t.tanLambda);
        }

        // Build weight matrix W = inverse of initial covariance (block diagonal)
        RealMatrix W = MatrixUtils.createRealMatrix(stateSize, stateSize);

        // Vertex part
        if (vertexConstraintCov != null) {
            RealMatrix vertexCovInv = new LUDecomposition(vertexConstraintCov).getSolver().getInverse();
            for (int i = 0; i < 3; i++)
                for (int j = 0; j < 3; j++)
                    W.setEntry(i, j, vertexCovInv.getEntry(i, j));
        } else {
            // Weak constraint if no beamspot
            for (int i = 0; i < 3; i++)
                W.setEntry(i, i, 0.01);
        }

        // Track parts
        for (int i = 0; i < nTracks; i++) {
            int offset = nVertexParams + i * nTrackParams;
            RealMatrix trackCovInv;
            try {
                trackCovInv = new LUDecomposition(tracks.get(i).cov).getSolver().getInverse();
            } catch (SingularMatrixException e) {
                return null;  // Cannot build weight matrix; skip this event silently
            }
            for (int a = 0; a < nTrackParams; a++)
                for (int b = 0; b < nTrackParams; b++)
                    W.setEntry(offset + a, offset + b, trackCovInv.getEntry(a, b));
        }

        // Also need W^-1 for the solution
        RealMatrix WInv = MatrixUtils.createRealMatrix(stateSize, stateSize);
        if (vertexConstraintCov != null) {
            for (int i = 0; i < 3; i++)
                for (int j = 0; j < 3; j++)
                    WInv.setEntry(i, j, vertexConstraintCov.getEntry(i, j));
        } else {
            for (int i = 0; i < 3; i++)
                WInv.setEntry(i, i, 100.0);
        }
        for (int i = 0; i < nTracks; i++) {
            int offset = nVertexParams + i * nTrackParams;
            RealMatrix tCov = tracks.get(i).cov;
            for (int a = 0; a < nTrackParams; a++)
                for (int b = 0; b < nTrackParams; b++)
                    WInv.setEntry(offset + a, offset + b, tCov.getEntry(a, b));
        }

        // Current state (start at x0)
        RealVector x = x0.copy();

        // Storage for final-iteration constraint system (used for post-fit covariance and chi2)
        RealMatrix finalH    = null;
        RealMatrix finalV    = null;
        RealVector finalHvec = null;

        if (debugFlag) {
            System.out.println("=== fitSoftConstrained ===");
            System.out.println("  State size: " + stateSize + ", Constraints: " + nConstraints);
        }

        // Iterative solution using Newton-Raphson
        for (int iteration = 0; iteration < maxIterations; iteration++) {
            RealVector xOld = x.copy();

            // Extract vertex from state
            RealVector vertex = x.getSubVector(0, 3);

            // Update track parameters from state
            for (int i = 0; i < nTracks; i++) {
                int offset = nVertexParams + i * nTrackParams;
                tracks.get(i).d0 = x.getEntry(offset + 0);
                tracks.get(i).phi0 = x.getEntry(offset + 1);
                tracks.get(i).omega = x.getEntry(offset + 2);
                tracks.get(i).z0 = x.getEntry(offset + 3);
                tracks.get(i).tanLambda = x.getEntry(offset + 4);
            }

            // Compute constraint residuals h(x) and Jacobian H = dh/dx
            RealVector h = MatrixUtils.createRealVector(new double[nConstraints]);
            RealMatrix H = MatrixUtils.createRealMatrix(nConstraints, stateSize);

            // Track constraints: 2 per track
            //   row 2*i:   transverse   h_t = r - R  (vertex on helix circle in XY)
            //   row 2*i+1: longitudinal h_z = zV - z_predicted
            for (int i = 0; i < nTracks; i++) {
                TrackParams track = tracks.get(i);
                int offset = nVertexParams + i * nTrackParams;
                int rowT = 2 * i;
                int rowZ = 2 * i + 1;

                double xV = vertex.getEntry(0);
                double yV = vertex.getEntry(1);
                double zV = vertex.getEntry(2);

                VertexParams vp = perigeeToVertexParams(track, xV, yV);

                double R = 1.0 / FastMath.abs(track.omega);
                double sign = FastMath.signum(track.omega);

                double xc = sign * R * FastMath.sin(track.phi0) - track.d0 * FastMath.sin(track.phi0);
                double yc = -sign * R * FastMath.cos(track.phi0) + track.d0 * FastMath.cos(track.phi0);
                double dx = xV - xc;
                double dy = yV - yc;
                double r2 = dx * dx + dy * dy;
                double r  = FastMath.sqrt(r2);

                // Constraint residuals
                h.setEntry(rowT, r - R);
                h.setEntry(rowZ, zV - vp.zV);

                // --- Transverse constraint Jacobian: d(r-R)/d(state) ---
                // w.r.t. vertex
                H.setEntry(rowT, 0, dx / r);
                H.setEntry(rowT, 1, dy / r);
                H.setEntry(rowT, 2, 0.0);
                // w.r.t. track params
                double dftDd0    = (dx * FastMath.sin(track.phi0) - dy * FastMath.cos(track.phi0)) / r;
                double dftDphi0  = -(sign * R - track.d0) * (dx * FastMath.cos(track.phi0) + dy * FastMath.sin(track.phi0)) / r;
                double dftDomega = (dx * FastMath.sin(track.phi0) - dy * FastMath.cos(track.phi0)) / (r * track.omega * track.omega)
                                   + sign / (track.omega * track.omega);
                H.setEntry(rowT, offset + 0, dftDd0);
                H.setEntry(rowT, offset + 1, dftDphi0);
                H.setEntry(rowT, offset + 2, dftDomega);
                H.setEntry(rowT, offset + 3, 0.0);
                H.setEntry(rowT, offset + 4, 0.0);

                // --- Longitudinal constraint Jacobian: d(zV - zPred)/d(state) ---
                // w.r.t. vertex
                double dphiDx = -dy / r2;
                double dphiDy = dx / r2;
                double dzPredDx = -sign * R * track.tanLambda * dphiDx;
                double dzPredDy = -sign * R * track.tanLambda * dphiDy;
                H.setEntry(rowZ, 0, -dzPredDx);
                H.setEntry(rowZ, 1, -dzPredDy);
                H.setEntry(rowZ, 2, 1.0);
                // w.r.t. track params
                double dphiDd0 = -(FastMath.cos(track.phi0) * dx + FastMath.sin(track.phi0) * dy) / r2;
                double dphiDphi0 = (sign * R - track.d0) * (dy * FastMath.cos(track.phi0) - dx * FastMath.sin(track.phi0)) / r2;
                double dphiDomega = -R * R * (FastMath.cos(track.phi0) * dx + FastMath.sin(track.phi0) * dy) / r2;
                double phiV = FastMath.atan2(-dx * sign, dy * sign);
                double dphi_s = phiV - track.phi0;
                while (dphi_s >  FastMath.PI) dphi_s -= 2.0 * FastMath.PI;
                while (dphi_s < -FastMath.PI) dphi_s += 2.0 * FastMath.PI;
                double s = -sign * R * dphi_s;
                // z_pred = z0 + s*tanLambda with s = -sign(omega)*R*dphi: each R*(dphi-derivative)
                // term below picks up the same -sign factor; the explicit s/omega term does not.
                double dzPredDd0    = -sign * track.tanLambda * R * dphiDd0;
                double dzPredDphi0  = -sign * (-track.tanLambda * R + track.tanLambda * R * dphiDphi0);
                double dzPredDomega = -s * track.tanLambda / track.omega - sign * track.tanLambda * R * dphiDomega;
                H.setEntry(rowZ, offset + 0, -dzPredDd0);
                H.setEntry(rowZ, offset + 1, -dzPredDphi0);
                H.setEntry(rowZ, offset + 2, -dzPredDomega);
                H.setEntry(rowZ, offset + 3, -1.0);
                H.setEntry(rowZ, offset + 4, -s);
            }

            // 3-momentum constraints: totalP - beamP = 0 (no energy constraint)
            if (fourMomentumConstraint != null) {
                RealVector totalP = MatrixUtils.createRealVector(new double[3]);

                // First pass: compute total 3-momentum
                for (int itrk = 0; itrk < nTracks; itrk++) {
                    TrackParams track = tracks.get(itrk);
                    RealVector p = computeMomentumAtVertex(track, vertex);

                    totalP.addToEntry(0, p.getEntry(0));
                    totalP.addToEntry(1, p.getEntry(1));
                    totalP.addToEntry(2, p.getEntry(2));
                }

                // Constraint residuals (3-momentum only)
                for (int j = 0; j < 3; j++) {
                    h.setEntry(nTrackConstraints + j, totalP.getEntry(j) - fourMomentumConstraint.getEntry(j));
                }

                // Second pass: compute Jacobian for 3-momentum constraints
                for (int itrk = 0; itrk < nTracks; itrk++) {
                    TrackParams track = tracks.get(itrk);
                    int offset = nVertexParams + itrk * nTrackParams;

                    double xV = vertex.getEntry(0);
                    double yV = vertex.getEntry(1);
                    VertexParams vp = perigeeToVertexParams(track, xV, yV);
                    double R = 1.0 / FastMath.abs(track.omega);
                    double sign = FastMath.signum(track.omega);
                    double pT = 2.99792458e-4 * FastMath.abs(bField) / FastMath.abs(track.omega);

                    double xc = sign * R * FastMath.sin(track.phi0) - track.d0 * FastMath.sin(track.phi0);
                    double yc = -sign * R * FastMath.cos(track.phi0) + track.d0 * FastMath.cos(track.phi0);
                    double dx = xV - xc;
                    double dy = yV - yc;
                    double r2 = dx * dx + dy * dy;

                    // dp/dvertex
                    double dphiDx = -dy / r2;
                    double dphiDy = dx / r2;

                    double dpxDxV = -pT * FastMath.sin(vp.phiV) * dphiDx;
                    double dpxDyV = -pT * FastMath.sin(vp.phiV) * dphiDy;
                    double dpyDxV = pT * FastMath.cos(vp.phiV) * dphiDx;
                    double dpyDyV = pT * FastMath.cos(vp.phiV) * dphiDy;

                    H.addToEntry(nTrackConstraints + 0, 0, dpxDxV);
                    H.addToEntry(nTrackConstraints + 0, 1, dpxDyV);
                    H.addToEntry(nTrackConstraints + 1, 0, dpyDxV);
                    H.addToEntry(nTrackConstraints + 1, 1, dpyDyV);
                    // pz doesn't depend on vertex position

                    // dp/dtrack (phi0, omega, tanLambda)
                    double dphiDphi0 = (sign * R - track.d0) * (dy * FastMath.cos(track.phi0) - dx * FastMath.sin(track.phi0)) / r2;
                    double dphiDomega = -R * R * (FastMath.cos(track.phi0) * dx + FastMath.sin(track.phi0) * dy) / r2;
                    double dpTDomega = -2.99792458e-4 * FastMath.abs(bField) * sign / (track.omega * track.omega);

                    double dpxDphi0 = -pT * FastMath.sin(vp.phiV) * dphiDphi0;
                    double dpxDomega = FastMath.cos(vp.phiV) * dpTDomega - pT * FastMath.sin(vp.phiV) * dphiDomega;
                    double dpyDphi0 = pT * FastMath.cos(vp.phiV) * dphiDphi0;
                    double dpyDomega = FastMath.sin(vp.phiV) * dpTDomega + pT * FastMath.cos(vp.phiV) * dphiDomega;
                    double dpzDomega = track.tanLambda * dpTDomega;
                    double dpzDtl = pT;

                    H.addToEntry(nTrackConstraints + 0, offset + 1, dpxDphi0);
                    H.addToEntry(nTrackConstraints + 0, offset + 2, dpxDomega);
                    H.addToEntry(nTrackConstraints + 1, offset + 1, dpyDphi0);
                    H.addToEntry(nTrackConstraints + 1, offset + 2, dpyDomega);
                    H.addToEntry(nTrackConstraints + 2, offset + 2, dpzDomega);
                    H.addToEntry(nTrackConstraints + 2, offset + 4, dpzDtl);
                }
            }

            if (debugFlag) {
                System.out.printf("  Iteration %d: |h| = %.6f%n", iteration, h.getNorm());
                for (int i = 0; i < nConstraints; i++) {
                    System.out.printf("    h[%d] = %.6f%n", i, h.getEntry(i));
                }
            }

            // Build constraint covariance matrix V for soft constraints.
            // The geometric track constraints (h_t = r-R, h_z = zV-zPred) are exact
            // functions of the *same* state x whose track-parameter block is already
            // weighted by W via the (x-x0) measurement term. Softening them with V =
            // J_h*trackCov*J_h^T (as this used to do) double-counts that same track
            // covariance a second time -- J_h here is exactly the track-parameter block
            // of H above, so V was just H_trk*trackCov*H_trk^T, the same information
            // already present in W^-1. That inflated the reported posterior vertex
            // covariance (confirmed via toy-MC pulls: std ~0.7 instead of 1, for both
            // hard and soft momentum-constraint modes, since this track-level V was
            // added unconditionally regardless of the momentum-constraint softness).
            // These constraints are therefore treated as effectively hard instead,
            // regularized only by a tiny fixed epsilon (a small fraction of H*WInv*H^T's
            // own diagonal scale) purely to avoid the near-singular case the original
            // code was guarding against: two tracks with nearly identical |tanLambda|
            // make the two longitudinal constraints nearly degenerate. Only the momentum
            // constraint, when a real fourMomentumConstraintCov is supplied, represents
            // genuinely independent information (the beam momentum uncertainty) and
            // keeps its own physical covariance below.
            RealMatrix constraintCov = MatrixUtils.createRealMatrix(nConstraints, nConstraints);

            RealMatrix HWInvHTforEps = H.multiply(WInv).multiply(H.transpose());
            double diagScale = 0.0;
            for (int i = 0; i < nConstraints; i++) {
                diagScale += HWInvHTforEps.getEntry(i, i);
            }
            diagScale = (nConstraints > 0) ? diagScale / nConstraints : 1.0;
            double epsilon = (diagScale > 0) ? diagScale * 1e-6 : 1e-12;

            for (int i = 0; i < nTrackConstraints; i++) {
                constraintCov.setEntry(i, i, epsilon);
            }

            if (debugFlag && iteration == 0) {
                System.out.printf("    Track constraint regularization epsilon = %.3e%n", epsilon);
            }

            // Momentum constraint covariances (if applicable)
            if (fourMomentumConstraint != null && fourMomentumConstraintCov != null) {
                for (int i = 0; i < 3; i++) {
                    for (int j = 0; j < 3; j++) {
                        constraintCov.setEntry(nTrackConstraints + i, nTrackConstraints + j,
                                               fourMomentumConstraintCov.getEntry(i, j));
                    }
                }
            }

            // Save constraint system at current x for post-fit covariance and chi2
            finalH    = H;
            finalV    = constraintCov;
            finalHvec = h;

            // Solve the KKT system using block elimination with SOFT CONSTRAINTS:
            // For soft constraints with covariance V, we solve:
            //   min (x-x0)^T W (x-x0) + h^T V^-1 h
            //
            // This modifies the standard Lagrange multiplier solution by adding V to H W^-1 H^T:
            //   λ = (H W^-1 H^T + V)^-1 (h + H(x0 - x))
            //   dx = (x0 - x) - W^-1 H^T λ
            //
            // V = J·Cov·J^T is needed for numerical regularization: for a V0 where both tracks
            // have similar |tanλ|, the two longitudinal vertex-Z constraints are nearly degenerate
            // and H W^-1 H^T becomes singular without V on the diagonal.

            try {
                RealMatrix HWInvHT = H.multiply(WInv).multiply(H.transpose());
                // Add constraint covariance for regularization
                RealMatrix HWInvHT_plus_V = HWInvHT.add(constraintCov);
                RealVector rhs = h.add(H.operate(x0.subtract(x)));

                RealVector lambda = new LUDecomposition(HWInvHT_plus_V).getSolver().solve(rhs);
                RealVector deltaX = x0.subtract(x).subtract(WInv.multiply(H.transpose()).operate(lambda));

                x = x.add(deltaX);

                if (debugFlag) {
                    System.out.printf("    |deltaX| = %.6f%n", deltaX.getNorm());
                }

                // Check convergence
                if (deltaX.getNorm() < tolerance && h.getNorm() < tolerance * 10) {
                    if (debugFlag) {
                        System.out.println("  Converged at iteration " + iteration);
                    }
                    break;
                }
            } catch (Exception e) {
                if (debugFlag) {
                    System.out.println("  Matrix inversion failed: " + e.getMessage());
                }
                break;
            }
        }

        // Extract final results
        RealVector vertex = x.getSubVector(0, 3);

        // Post-fit covariance: C = W^{-1} - W^{-1} H^T (H W^{-1} H^T + V)^{-1} H W^{-1}
        // This is the full stateSize x stateSize covariance after all constraints are applied.
        // K = W^{-1} H^T S^{-1}  where S = H W^{-1} H^T + V
        RealMatrix C_fitted = WInv.copy();
        if (finalH != null) {
            try {
                RealMatrix S    = finalH.multiply(WInv).multiply(finalH.transpose()).add(finalV);
                RealMatrix K    = WInv.multiply(finalH.transpose())
                                      .multiply(new LUDecomposition(S).getSolver().getInverse());
                C_fitted = WInv.subtract(K.multiply(finalH).multiply(WInv));
            } catch (Exception e) {
                if (debugFlag) System.out.println("  Post-fit covariance failed: " + e.getMessage());
            }
        }

        // Extract fitted track parameters with post-fit covariances
        List<TrackParams> fittedTracks = new ArrayList<>();
        for (int i = 0; i < nTracks; i++) {
            int offset = nVertexParams + i * nTrackParams;
            double d0        = x.getEntry(offset + 0);
            double phi0      = x.getEntry(offset + 1);
            double omega     = x.getEntry(offset + 2);
            double z0        = x.getEntry(offset + 3);
            double tanLambda = x.getEntry(offset + 4);
            RealMatrix trackCovFitted = C_fitted.getSubMatrix(
                    offset, offset + nTrackParams - 1,
                    offset, offset + nTrackParams - 1);
            fittedTracks.add(new TrackParams(d0, phi0, omega, z0, tanLambda, trackCovFitted));
        }

        // Full chi2 = parameter pulls + constraint residuals
        //   (x-x0)^T W (x-x0)  +  h^T V^{-1} h
        // The second term is computed per block for numerical stability.
        RealVector dx = x.subtract(x0);
        double chi2 = dx.dotProduct(W.operate(dx));
        if (finalHvec != null && finalV != null) {
            for (int i = 0; i < nTracks; i++) {
                int rowT = 2 * i, rowZ = 2 * i + 1;
                RealMatrix Vblock = finalV.getSubMatrix(rowT, rowZ, rowT, rowZ);
                RealVector hblock = MatrixUtils.createRealVector(new double[]{
                        finalHvec.getEntry(rowT), finalHvec.getEntry(rowZ)});
                try {
                    chi2 += hblock.dotProduct(new LUDecomposition(Vblock).getSolver().solve(hblock));
                } catch (Exception e) { /* skip singular block */ }
            }
            if (fourMomentumConstraint != null && fourMomentumConstraintCov != null) {
                RealMatrix Vblock = finalV.getSubMatrix(
                        nTrackConstraints, nTrackConstraints + 2,
                        nTrackConstraints, nTrackConstraints + 2);
                RealVector hblock = finalHvec.getSubVector(nTrackConstraints, 3);
                try {
                    chi2 += hblock.dotProduct(new LUDecomposition(Vblock).getSolver().solve(hblock));
                } catch (Exception e) { /* skip singular block */ }
            }
        }

        // NDF = number of constraints minus the vertex-position dof they determine
        // (vertex has only a weak/free prior in W, so all 3 of its dof are absorbed
        // by the constraints rather than by a measurement -- matches the ndf convention
        // used elsewhere in this file, e.g. fitCascadeVertex's `2*nTracks-3`).
        int ndf = nConstraints - nVertexParams;

        // Compute final track momenta using fitted track parameters and their post-fit covariances
        List<TrackMomentum> trackMomenta = new ArrayList<>();
        double me = 0.000511;
        for (TrackParams track : fittedTracks) {
            RealVector p = computeMomentumAtVertex(track, vertex);
            RealMatrix pCov = computeMomentumCovariance(track, vertex);
            trackMomenta.add(new TrackMomentum(p, pCov));
        }

        // Total (summed) fitted momentum and its covariance, correctly propagated through the
        // FULL post-fit state covariance C_fitted -- unlike each track's own pCov above (which
        // only uses that track's diagonal block), this includes the cross-track and
        // vertex-momentum correlation blocks induced by the shared vertex and (for the
        // momentum-constrained fits) shared momentum-sum constraint. Built via
        // Cov(totalP) = J^T C_fitted J, where J (stateSize x 3) stacks the vertex-block
        // Jacobian (dP_total/d(vertex), summed over tracks) and each track's own
        // dP_track/d(track params) block (computeMomentumTrackJacobian).
        RealVector totalPTracking = MatrixUtils.createRealVector(new double[3]);
        RealMatrix vertexPBlock = MatrixUtils.createRealMatrix(3, 3);
        RealMatrix Jtotal = MatrixUtils.createRealMatrix(stateSize, 3);
        for (int i = 0; i < nTracks; i++) {
            TrackParams track = fittedTracks.get(i);
            totalPTracking = totalPTracking.add(computeMomentumAtVertex(track, vertex));
            vertexPBlock = vertexPBlock.add(computeMomentumVertexDerivatives(track, vertex));
            RealMatrix Jp = computeMomentumTrackJacobian(track, vertex);
            int offset = nVertexParams + i * nTrackParams;
            Jtotal.setSubMatrix(Jp.transpose().getData(), offset, 0);
        }
        Jtotal.setSubMatrix(vertexPBlock.transpose().getData(), 0, 0);
        RealMatrix totalPCovTracking = Jtotal.transpose().multiply(C_fitted).multiply(Jtotal);

        // Vertex covariance: upper-left 3x3 block of the post-fit covariance
        RealMatrix vertexCov = C_fitted.getSubMatrix(0, 2, 0, 2);

        if (debugFlag) {
            System.out.printf("  Final vertex: [%.4f, %.4f, %.4f]%n",
                              vertex.getEntry(0), vertex.getEntry(1), vertex.getEntry(2));
            System.out.printf("  Chi2: %.4f, NDF: %d, Chi2/NDF: %.2f%n", chi2, ndf, chi2/ndf);

            // Show track parameter changes vs uncertainties
            String[] paramNames = {"d0", "phi0", "omega", "z0", "tanL"};
            for (int i = 0; i < nTracks; i++) {
                int offset = nVertexParams + i * nTrackParams;
                System.out.printf("  Track %d parameter pulls (change/sigma):%n", i);
                double trackChi2 = 0;
                for (int p = 0; p < nTrackParams; p++) {
                    double change = x.getEntry(offset + p) - x0.getEntry(offset + p);
                    double sigma = FastMath.sqrt(inputTracks.get(i).cov.getEntry(p, p));
                    double pull = change / sigma;
                    trackChi2 += pull * pull;
                    System.out.printf("    %5s: change=%12.6f, sigma=%12.6f, pull=%8.2f%n",
                                      paramNames[p], change, sigma, pull);
                }
                System.out.printf("    Track %d chi2 contribution (diagonal only): %.2f%n", i, trackChi2);
            }

            // Show vertex change
            System.out.printf("  Vertex change: [%.4f, %.4f, %.4f]%n",
                              x.getEntry(0) - x0.getEntry(0),
                              x.getEntry(1) - x0.getEntry(1),
                              x.getEntry(2) - x0.getEntry(2));

            RealVector totalP = MatrixUtils.createRealVector(new double[3]);
            double totalE = 0;
            for (TrackMomentum tm : trackMomenta) {
                totalP = totalP.add(tm.p);
                totalE += FastMath.sqrt(tm.pMag * tm.pMag + me * me);
            }
            System.out.printf("  Total fitted 4-momentum: [%.4f, %.4f, %.4f, %.4f]%n",
                              totalP.getEntry(0), totalP.getEntry(1), totalP.getEntry(2), totalE);
            if (fourMomentumConstraint != null) {
                System.out.printf("  Beam 4-momentum:         [%.4f, %.4f, %.4f, %.4f]%n",
                                  fourMomentumConstraint.getEntry(0), fourMomentumConstraint.getEntry(1),
                                  fourMomentumConstraint.getEntry(2), fourMomentumConstraint.getEntry(3));
            }
        }

        FitResult fitResult = new FitResult(vertex, vertexCov, chi2, ndf, trackMomenta, fittedTracks);
        fitResult.totalMomentum = totalPTracking;
        fitResult.totalMomentumCov = totalPCovTracking;
        return fitResult;
    }

    /**
     * Joint kinematic fit with an EXACT (hard) beam momentum constraint via the
     * Lagrange multiplier method, combined with soft track-helix constraints.
     *
     * <p>Solves the mixed constrained optimisation problem:
     * <pre>
     *   minimise  (x - x0)^T W (x - x0) + sum_i h_track_i^T V_track_i^{-1} h_track_i
     *   subject to  h_mom(x) = total_p(x) - beamMomentum = 0  (exactly)
     * </pre>
     * where x = [vertex, track_1, ..., track_N] is the full state vector.
     *
     * <p>Implemented via the same unified KKT system as {@link #fitSoftConstrained}:
     * <pre>
     *   (H W^{-1} H^T + V_mixed) lambda = rhs
     * </pre>
     * but with V_mixed = diag(V_track_1, ..., V_track_N, 0): the zero block on the
     * momentum rows enforces that constraint exactly rather than softly weighting it
     * by a beam momentum uncertainty.  This recovers the classical Lagrange multiplier
     * solution for the momentum constraint while keeping the track-helix constraints
     * soft (as is physically appropriate given measurement errors).
     *
     * @param inputTracks        List of track parameters
     * @param vertexConstraint   Beamspot position prior (null for weak 100 mm prior)
     * @param vertexConstraintCov Beamspot position covariance (null for weak prior)
     * @param beamMomentum       Exact beam 3-momentum [px, py, pz] in GeV (4-vector accepted; only first 3 used)
     * @param maxIterations      Maximum Newton-Raphson iterations
     * @param tolerance          Convergence tolerance
     * @return FitResult with vertex, track parameters, chi2, and momenta
     *
     * @deprecated The beam momentum is not actually conserved exactly by the tracked
     * leptons alone -- some momentum (~18.6 MeV transverse, see
     * {@link #setBeamMomentumTransverseNuclearRecoilSigma(double)}) is carried away by
     * the target nuclear recoil. Enforcing V_mom=0 exactly therefore fits an equality
     * that is not physically true, which structurally cannot be fixed by covariance
     * tuning (see {@code ntrack_beam_momentum_constraint_result.md}: n=123 real
     * candidates showed hard-mode chi2/ndf completely unchanged by the nuclear-recoil
     * covariance widening that fixes soft mode, since that widening has no effect when
     * V_mom is hardcoded to zero). Use {@link #fitSoftConstrained} (or
     * {@link KalmanNTrackVertexer#fitVertexBeamConstrained} with
     * {@code hardMomentumConstraint=false}) with a tuned
     * {@code setBeamMomentumTransverseNuclearRecoilSigma} instead. Kept for
     * reference/regression comparison, not recommended for new production use.
     */
    @Deprecated
    public FitResult fitLagrangeMultiplier(List<TrackParams> inputTracks,
                                           RealVector vertexConstraint,
                                           RealMatrix vertexConstraintCov,
                                           RealVector beamMomentum,
                                           int maxIterations,
                                           double tolerance) {
        // Passing null for fourMomentumConstraintCov leaves the momentum block of the
        // KKT constraint covariance matrix as zero (V_mom = 0), which enforces the
        // momentum constraint exactly as a hard Lagrange multiplier constraint, in
        // contrast to fitSoftConstrained() which fills that block with the beam
        // momentum uncertainty and satisfies the constraint only approximately.
        return fitSoftConstrained(inputTracks, vertexConstraint, vertexConstraintCov,
                                  beamMomentum, null, maxIterations, tolerance);
    }

    // Simplified methods
    public FitResult fit(List<TrackParams> tracks) {
        return fit(tracks, null, null, null, null, null, null, null, 10, 1e-6);
    }
    
    public FitResult fit(List<TrackParams> tracks, int maxIterations, double tolerance) {
        return fit(tracks, null, null, null, null, null, null, null, maxIterations, tolerance);
    }
    
    // Configurable fields for BilliorVertexer-style interface
    private double[] beamSize = {0.001, 0.150, 0.050};
    private double[] beamPosition = {-1.1, 0, 0};
    private double[] referencePosition = {0.0, 0.0, 0.0}; // tracking frame offset added to output vertex
    private double pBeam = 3.74;
    private double rotAngle = -0.030;
    private boolean debugFlag = false;
    private boolean storeCovTrkMomList = false;
    // Additional transverse beam-momentum-constraint width (GeV), combined in quadrature with
    // the beam-divergence term below. Default 0 reproduces the original divergence-only
    // covariance exactly. Non-zero values represent event-to-event transverse momentum not
    // carried by the tracked leptons -- primarily momentum transferred to the target nucleus
    // during production (nuclear recoil; distinct from a recoil electron from radiative/A'
    // events) -- measured directly from trident MC truth (std(mcTotalPx), std(mcTotalPy)) at
    // ~18.5-18.8 MeV, vs. the ~0.37 MeV implied by 100 urad beam divergence alone.
    private double sigmaTNuclearRecoil = 0.0;

    public void setBeamSize(double[] bs) { this.beamSize = bs; }
    public void setBeamPosition(double[] bp) { this.beamPosition = bp; }
    public double[] getBeamSize() { return beamSize; }
    public double[] getBeamPosition() { return beamPosition; }
    public void setReferencePosition(double[] rp) { this.referencePosition = rp.clone(); }
    public void setBeamEnergy(double energy) { this.pBeam = energy; }
    public void setBeamRotAngle(double angle) { this.rotAngle = angle; }
    public void setBeamMomentumTransverseNuclearRecoilSigma(double sigma) { this.sigmaTNuclearRecoil = sigma; }
    public void setDebug(boolean debug) { this.debugFlag = debug; }
    public void setStoreCovTrkMomList(boolean value) { this.storeCovTrkMomList = value; }

    /**
     * Vertex-only fit following Billior (NIM A225, 1984) / Billior &amp; Qian (NIM A311, 1992).
     * Direct translation of BilliorVertexer.follow1985Paper, using TrackParams (LCIO helix).
     *
     * TrackParams (LCIO: d0, phi0, omega, z0, tanL) are converted internally to Billior
     * parameterization (eps=-d0, z0, theta=PI/2-atan(tanL), phi0, rho=omega).  The vertex
     * is solved in the tracking frame (x=beam, y=horiz, z=vert) by marginalising the three
     * momentum parameters (theta, phiV, rho) analytically via the Schur complement of the
     * full 5x5 track weight matrix.
     *
     * @param tracks               Track parameters (LCIO helix, reference near vertex).
     * @param vertexConstraintVec  Beamspot centre in tracking frame, or null.
     * @param vertexConstraintCov  Beamspot covariance, or null.
     * @return FitResult, or null on numerical failure.
     */
    public FitResult fitBillior1985(List<TrackParams> tracks,
                                    RealVector vertexConstraintVec,
                                    RealMatrix vertexConstraintCov) {
        int nTracks = tracks.size();
        if (nTracks < 2) return null;

        // fieldConversion: same as org.lcsim.constants.Constants.fieldConversion
        final double fieldConv = 2.99792458e-4;

        // -----------------------------------------------------------------------
        // Step 1: Convert LCIO TrackParams → Billior parameterization per track
        //
        // LCIO order:    (d0, phi0, omega, z0, tanL)   indices 0,1,2,3,4
        // Billior order: (eps=-d0, z0, theta, phi0, rho=omega) indices 0,1,2,3,4
        //   where theta = PI/2 - atan(tanL)
        //
        // Covariance transforms via diagonal Jacobian J:
        //   J[0][0]=-1  J[1][3]=1  J[2][4]=-1/(1+tanL^2)  J[3][1]=1  J[4][2]=1
        // -----------------------------------------------------------------------
        double[][] bPar = new double[nTracks][5];
        BasicMatrix[] bCov = new BasicMatrix[nTracks];

        for (int t = 0; t < nTracks; t++) {
            TrackParams tp = tracks.get(t);
            double tanL = tp.tanLambda;
            bPar[t][0] = -tp.d0;
            bPar[t][1] =  tp.z0;
            bPar[t][2] =  Math.PI / 2.0 - Math.atan(tanL);
            bPar[t][3] =  tp.phi0;
            bPar[t][4] =  tp.omega;

            // Build Jacobian J (Billior row, LCIO column)
            BasicMatrix J = new BasicMatrix(5, 5);
            J.setElement(0, 0, -1.0);
            J.setElement(1, 3,  1.0);
            J.setElement(2, 4, -1.0 / (1.0 + tanL * tanL));
            J.setElement(3, 1,  1.0);
            J.setElement(4, 2,  1.0);

            // Copy LCIO covariance into BasicMatrix
            BasicMatrix lcioC = new BasicMatrix(5, 5);
            for (int r = 0; r < 5; r++)
                for (int c = 0; c < 5; c++)
                    lcioC.setElement(r, c, tp.cov.getEntry(r, c));

            // Billior covariance = J * lcioC * J^T
            bCov[t] = (BasicMatrix) MatrixOp.mult(J, MatrixOp.mult(lcioC, MatrixOp.transposed(J)));
        }

        // -----------------------------------------------------------------------
        // Step 2: Build per-track matrices at linearisation point v0 = (0,0,0)
        //         following Billior & Qian NIM A311 (1992) eqs. 3-9.
        //
        // At v0=(0,0,0): uu=vv=0, phiVert=phi0, ci=0, pis=measured Billior params.
        //
        // A (5x3): d(helix params)/d(vertex position)
        // B (5x3): d(helix params)/d(momentum params theta,phiV,rho)
        // G (5x5): inverse of Billior covariance
        // Di (3x3) = A^T G B,  Ei (3x3) = B^T G B
        // -----------------------------------------------------------------------
        List<BasicMatrix> As  = new ArrayList<>();
        List<BasicMatrix> Bs  = new ArrayList<>();
        List<BasicMatrix> Gs  = new ArrayList<>();
        List<BasicMatrix> pis = new ArrayList<>();  // measured Billior params as 5x1
        List<BasicMatrix> Ds  = new ArrayList<>();  // Di = A^T G B
        List<BasicMatrix> Es  = new ArrayList<>();  // Ei = B^T G B

        BasicMatrix D0 = new BasicMatrix(3, 3);     // D0 = sum A_i^T G_i A_i

        for (int t = 0; t < nTracks; t++) {
            double theta = bPar[t][2];
            double phi0  = bPar[t][3];  // == phiVert at v0=0
            double rho   = bPar[t][4];
            double cotth = 1.0 / Math.tan(theta);
            double cosf  = Math.cos(phi0);
            double sinf  = Math.sin(phi0);

            // A matrix (5x3): partial derivatives of helix params w.r.t. vertex (x,y,z)
            // At v0=0, phiVert=phi0.  Non-zero entries:
            //   eps row (0):   d(eps)/dx = sin(f), d(eps)/dy = -cos(f)
            //   z0  row (1):   d(z0)/dx  = -cot*cos(f), d(z0)/dy = -cot*sin(f), d(z0)/dz=1
            //   phi row (3):   d(phi)/dx = -rho*cos(f), d(phi)/dy = -rho*sin(f)
            BasicMatrix A = new BasicMatrix(5, 3);
            A.setElement(0, 0,  sinf);
            A.setElement(0, 1, -cosf);
            A.setElement(1, 0, -cotth * cosf);
            A.setElement(1, 1, -cotth * sinf);
            A.setElement(1, 2,  1.0);
            A.setElement(3, 0, -rho * cosf);
            A.setElement(3, 1, -rho * sinf);

            // B matrix (5x3): partial derivatives of helix params w.r.t. (theta, phiV, rho)
            // At v0=0 (uu=vv=0) all uu/vv terms vanish.
            BasicMatrix B = new BasicMatrix(5, 3);
            B.setElement(2, 0, 1.0);   // d(theta)/d(theta)
            B.setElement(3, 1, 1.0);   // d(phiV)/d(phiV)
            B.setElement(4, 2, 1.0);   // d(rho)/d(rho)
            // B[0,1]=uu=0, B[0,2]=-uu^2/2=0, B[1,0]=uu*(1+cot^2)=0,
            // B[1,1]=-vv*cot=0, B[1,2]=uu*vv*cot=0, B[3,2]=-uu=0

            // G = inverse of Billior covariance
            BasicMatrix G;
            try { G = (BasicMatrix) MatrixOp.inverse(bCov[t]); }
            catch (Exception e) { return null; }

            // Measured Billior params as 5x1 column
            BasicMatrix pi = new BasicMatrix(5, 1);
            for (int k = 0; k < 5; k++) pi.setElement(k, 0, bPar[t][k]);

            As.add(A); Bs.add(B); Gs.add(G); pis.add(pi);

            // Di = A^T G B  (3x3),  Ei = B^T G B  (3x3)
            BasicMatrix Di = (BasicMatrix) MatrixOp.mult(MatrixOp.transposed(A), MatrixOp.mult(G, B));
            BasicMatrix Ei = (BasicMatrix) MatrixOp.mult(MatrixOp.transposed(B), MatrixOp.mult(G, B));
            Ds.add(Di); Es.add(Ei);

            // Accumulate D0 = sum A^T G A  (3x3)
            BasicMatrix contrib = (BasicMatrix) MatrixOp.mult(MatrixOp.transposed(A), MatrixOp.mult(G, A));
            D0 = (BasicMatrix) MatrixOp.add(D0, contrib);
        }

        // -----------------------------------------------------------------------
        // Step 3: Solve for vertex position
        //
        // Vertex information matrix = D0 - sum Di Ei^{-1} Di^T  (Schur complement)
        // Plus optional Gaussian vertex prior (beamspot constraint).
        //
        // bigsum (RHS) = sum (A^T G - A^T G B Ei^{-1} B^T G) p
        // -----------------------------------------------------------------------
        BasicMatrix tmpInfoVtx = D0;
        BasicMatrix bigsum = new BasicMatrix(3, 1);

        // Beamspot prior: add to vertex information matrix and RHS
        if (vertexConstraintVec != null && vertexConstraintCov != null) {
            BasicMatrix priorCov = new BasicMatrix(3, 3);
            for (int r = 0; r < 3; r++)
                for (int c = 0; c < 3; c++)
                    priorCov.setElement(r, c, vertexConstraintCov.getEntry(r, c));
            BasicMatrix priorInv;
            try { priorInv = (BasicMatrix) MatrixOp.inverse(priorCov); }
            catch (Exception e) { return null; }
            tmpInfoVtx = (BasicMatrix) MatrixOp.add(tmpInfoVtx, priorInv);
            // RHS contribution: priorInv * priorPos
            BasicMatrix priorPos = new BasicMatrix(3, 1);
            for (int r = 0; r < 3; r++) priorPos.setElement(r, 0, vertexConstraintVec.getEntry(r));
            bigsum = (BasicMatrix) MatrixOp.add(bigsum, MatrixOp.mult(priorInv, priorPos));
        }

        for (int i = 0; i < nTracks; i++) {
            BasicMatrix A  = As.get(i);
            BasicMatrix B  = Bs.get(i);
            BasicMatrix G  = Gs.get(i);
            BasicMatrix p  = pis.get(i);
            BasicMatrix Di = Ds.get(i);
            BasicMatrix Ei = Es.get(i);
            BasicMatrix EiInv;
            try { EiInv = (BasicMatrix) MatrixOp.inverse(Ei); }
            catch (Exception e) { return null; }

            // Subtract Schur complement contribution: Di Ei^{-1} Di^T
            tmpInfoVtx = (BasicMatrix) MatrixOp.add(tmpInfoVtx,
                MatrixOp.mult(-1, MatrixOp.mult(Di, MatrixOp.mult(EiInv, MatrixOp.transposed(Di)))));

            // RHS: (A^T G  -  A^T G B Ei^{-1} B^T G) * p
            BasicMatrix ATG    = (BasicMatrix) MatrixOp.mult(MatrixOp.transposed(A), G);
            BasicMatrix BEIBtG = (BasicMatrix) MatrixOp.mult(B,
                                    MatrixOp.mult(EiInv, MatrixOp.mult(MatrixOp.transposed(B), G)));
            BasicMatrix coeff  = (BasicMatrix) MatrixOp.add(ATG, MatrixOp.mult(-1,
                                    MatrixOp.mult(ATG, BEIBtG)));
            bigsum = (BasicMatrix) MatrixOp.add(bigsum, MatrixOp.mult(coeff, p));
        }

        BasicMatrix covVtx;
        try { covVtx = (BasicMatrix) MatrixOp.inverse(tmpInfoVtx); }
        catch (Exception e) { return null; }

        BasicMatrix xtilde = (BasicMatrix) MatrixOp.mult(covVtx, bigsum);

        // -----------------------------------------------------------------------
        // Step 4: Compute fitted momenta and chi2
        //         Following BilliorVertexer.follow1985Paper lines 1130-1188.
        //
        // qtilde (3x1) = -Ei^{-1} Di^T xtilde + Ei^{-1} B^T G p   [eqs 22b,d]
        // ptilde (5x1) = A xtilde + B qtilde                        [fitted helix params]
        // chi2 += (p - ptilde)^T G (p - ptilde)
        // pfit = (theta_fit, phiV_fit, rho_fit) = qtilde   (since ci=0 at v0=0)
        // -----------------------------------------------------------------------
        double chi2 = 0.0;
        List<TrackMomentum> trackMomenta = new ArrayList<>();

        for (int j = 0; j < nTracks; j++) {
            BasicMatrix A  = As.get(j);
            BasicMatrix B  = Bs.get(j);
            BasicMatrix G  = Gs.get(j);
            BasicMatrix p  = pis.get(j);
            BasicMatrix Di = Ds.get(j);
            BasicMatrix Ei = Es.get(j);
            BasicMatrix EiInv;
            try { EiInv = (BasicMatrix) MatrixOp.inverse(Ei); }
            catch (Exception e) { return null; }

            // qtilde: fitted momentum params (theta, phiV, rho)
            BasicMatrix qtilde = (BasicMatrix) MatrixOp.add(
                MatrixOp.mult(-1, MatrixOp.mult(EiInv, MatrixOp.mult(MatrixOp.transposed(Di), xtilde))),
                MatrixOp.mult(EiInv, MatrixOp.mult(MatrixOp.transposed(B), MatrixOp.mult(G, p))));

            // ptilde: predicted 5-parameter helix at fitted vertex
            BasicMatrix ptilde = (BasicMatrix) MatrixOp.add(
                MatrixOp.mult(A, xtilde), MatrixOp.mult(B, qtilde));

            // Chi2 contribution: (p - ptilde)^T G (p - ptilde)
            BasicMatrix residual = (BasicMatrix) MatrixOp.add(p, MatrixOp.mult(-1, ptilde));
            chi2 += MatrixOp.mult(MatrixOp.transposed(residual),
                                  MatrixOp.mult(G, residual)).e(0, 0);

            // pfit = qtilde (ci=0 at v0=0, so qtilde_j + ci_j[2..4] = qtilde_j)
            double thetaFit = qtilde.e(0, 0);
            double phiVFit  = qtilde.e(1, 0);
            double rhoFit   = qtilde.e(2, 0);

            // Convert (theta, phiV, rho) → (px, py, pz) in tracking frame
            // Pt = |fieldConv * B / rho|,  px = Pt*cos(phiV), py = Pt*sin(phiV), pz = Pt/tan(theta)
            double Pt = Math.abs(fieldConv * bField / rhoFit);
            double px = Pt * Math.cos(phiVFit);
            double py = Pt * Math.sin(phiVFit);
            double pz = Pt / Math.tan(thetaFit);

            // Fitted momentum covariance: propagate Cij[j][j] (theta,phiV,rho) → (px,py,pz)
            // Cij[j][j] = Ei^{-1} + Ei^{-1} Di^T covVtx Di Ei^{-1}  (from eq 22c)
            BasicMatrix C0j    = (BasicMatrix) MatrixOp.mult(-1,
                                    MatrixOp.mult(covVtx, MatrixOp.mult(Di, EiInv)));
            BasicMatrix CjjTmp = (BasicMatrix) MatrixOp.mult(-1,
                                    MatrixOp.mult(EiInv, MatrixOp.mult(MatrixOp.transposed(Di), C0j)));
            BasicMatrix Cjj    = (BasicMatrix) MatrixOp.add(EiInv, CjjTmp);

            // Jacobian d(px,py,pz)/d(theta,phiV,rho)
            double Bsig = fieldConv * bField;  // signed B
            BasicMatrix Jmom = new BasicMatrix(3, 3);
            Jmom.setElement(0, 0,  0.0);
            Jmom.setElement(0, 1, -Pt * Math.sin(phiVFit));
            Jmom.setElement(0, 2, -(Bsig * Math.cos(phiVFit)) / (rhoFit * rhoFit));
            Jmom.setElement(1, 0,  0.0);
            Jmom.setElement(1, 1,  Pt * Math.cos(phiVFit));
            Jmom.setElement(1, 2, -(Bsig * Math.sin(phiVFit)) / (rhoFit * rhoFit));
            Jmom.setElement(2, 0, -Pt * Math.pow(1.0 / Math.sin(thetaFit), 2));
            Jmom.setElement(2, 1,  0.0);
            Jmom.setElement(2, 2, -(Bsig / Math.tan(thetaFit)) / (rhoFit * rhoFit));

            BasicMatrix pCovBillior = (BasicMatrix) MatrixOp.mult(Jmom,
                                        MatrixOp.mult(Cjj, MatrixOp.transposed(Jmom)));

            RealMatrix pCovRM = MatrixUtils.createRealMatrix(3, 3);
            for (int r = 0; r < 3; r++)
                for (int c = 0; c < 3; c++)
                    pCovRM.setEntry(r, c, pCovBillior.e(r, c));

            RealVector pVec = MatrixUtils.createRealVector(new double[]{px, py, pz});
            trackMomenta.add(new TrackMomentum(pVec, pCovRM));
        }

        // Wrap vertex covariance into RealMatrix
        RealMatrix covVtxRM = MatrixUtils.createRealMatrix(3, 3);
        for (int r = 0; r < 3; r++)
            for (int c = 0; c < 3; c++)
                covVtxRM.setEntry(r, c, covVtx.e(r, c));
        RealVector xVtx = MatrixUtils.createRealVector(new double[]{
            xtilde.e(0, 0), xtilde.e(1, 0), xtilde.e(2, 0)});

        int ndf = 2 * nTracks - 3;
        return new FitResult(xVtx, covVtxRM, chi2, ndf, trackMomenta);
    }

    /**
     * Fit vertex and return a BilliorVertex for compatibility with existing code.
     * Applies beamspot position constraint always; optionally applies beam momentum constraint.
     *
     * @param tracks         List of track parameters
     * @param beamConstraint If true, apply beam 4-momentum constraint in addition to beamspot
     * @return BilliorVertex with fitted results
     */
    public BilliorVertex fitVertex(List<TrackParams> tracks, boolean beamConstraint) {
        return fitVertex(tracks, true, beamConstraint, false);
    }

    /**
     * Fit vertex with independent control over beamspot position and beam momentum constraints.
     *
     * <p>Four modes are supported:
     * <ul>
     *   <li>(false, false) – unconstrained: only track-helix constraints on vertex position</li>
     *   <li>(true,  false) – beamspot only: vertex position constrained to beam spot</li>
     *   <li>(false, true)  – beam momentum only: total 3-momentum constrained to beam value,
     *                         no position constraint beyond the track helices</li>
     *   <li>(true,  true)  – full: beamspot position + beam 4-momentum constraints</li>
     * </ul>
     *
     * @param tracks                 List of track parameters
     * @param beamspotConstraint     If true, constrain vertex position to beam spot
     * @param beamMomentumConstraint If true, constrain total 3-momentum to beam value
     * @return BilliorVertex with fitted results
     */
    public BilliorVertex fitVertex(List<TrackParams> tracks, boolean beamspotConstraint, boolean beamMomentumConstraint) {
        return fitVertex(tracks, beamspotConstraint, beamMomentumConstraint, false);
    }

    /**
     * Fit vertex with independent control over beamspot, beam momentum, and whether
     * the momentum constraint is applied exactly (hard/Lagrange multiplier) or softly
     * (weighted by beam momentum uncertainty).
     *
     * <p>When {@code hardMomentumConstraint} is true and {@code beamMomentumConstraint}
     * is true, {@link #fitLagrangeMultiplier} is called so that total 3-momentum equals
     * the beam value exactly.  Otherwise {@link #fitSoftConstrained} is called and the
     * beam momentum uncertainty is folded into the constraint weight.
     *
     * @param tracks                  List of track parameters
     * @param beamspotConstraint      If true, constrain vertex position to beam spot
     * @param beamMomentumConstraint  If true, constrain total 3-momentum to beam value
     * @param hardMomentumConstraint  If true (and beamMomentumConstraint is true), enforce
     *                                the momentum constraint exactly via Lagrange multipliers.
     *                                <b>Deprecated:</b> see {@link #fitLagrangeMultiplier} --
     *                                the exact constraint is not physically correct (target
     *                                nuclear recoil carries real momentum away) and this mode
     *                                cannot be fixed by {@link #setBeamMomentumTransverseNuclearRecoilSigma}.
     *                                Prefer {@code false} (soft mode) for new production use.
     * @return BilliorVertex with fitted results
     */
    public BilliorVertex fitVertex(List<TrackParams> tracks, boolean beamspotConstraint, boolean beamMomentumConstraint, boolean hardMomentumConstraint) {
        if (debugFlag)
            System.out.println("     *********   starting new fitVertex    *********     ");

        // Print input track 4-momenta
        if (debugFlag) {
            double me = 0.000511;
            RealVector initVertex = MatrixUtils.createRealVector(beamPosition);
            RealVector totalP = MatrixUtils.createRealVector(new double[3]);
            double totalE = 0;
            for (int i = 0; i < tracks.size(); i++) {
                TrackParams t = tracks.get(i);
                RealVector p = computeMomentumAtVertex(t, initVertex);
                double pMag = p.getNorm();
                double E = FastMath.sqrt(pMag * pMag + me * me);
                totalP = totalP.add(p);
                totalE += E;
                System.out.printf("  Input track %d: p=[%.4f, %.4f, %.4f] |p|=%.4f E=%.4f%n",
                                  i, p.getEntry(0), p.getEntry(1), p.getEntry(2), pMag, E);
                System.out.printf("                 params: d0=%.4f phi0=%.4f omega=%.6f z0=%.4f tanL=%.4f%n",
                                  t.d0, t.phi0, t.omega, t.z0, t.tanLambda);
            }
            System.out.printf("  Input total 4-momentum: [%.4f, %.4f, %.4f, %.4f]%n",
                              totalP.getEntry(0), totalP.getEntry(1), totalP.getEntry(2), totalE);
        }

        // Set up vertex (beamspot) constraint
        RealVector vertexConstraintVec = null;
        RealMatrix vertexConstraintCovMat = null;
        if (beamspotConstraint) {
            vertexConstraintVec = MatrixUtils.createRealVector(beamPosition);
            vertexConstraintCovMat = MatrixUtils.createRealMatrix(3, 3);
            vertexConstraintCovMat.setEntry(0, 0, beamSize[0] * beamSize[0]);
            vertexConstraintCovMat.setEntry(1, 1, beamSize[1] * beamSize[1]);
            vertexConstraintCovMat.setEntry(2, 2, beamSize[2] * beamSize[2]);
        }

        // Set up beam 4-momentum constraint
        RealVector fourMomentumConstraintVec = null;
        RealMatrix fourMomentumConstraintCovMat = null;
        if (beamMomentumConstraint) {
            // Beam momentum vector in tracking frame
            // Detector frame: beam along HPS Z, rotated by rotAngle in HPS X-Z plane
            // Tracking frame: X=HPS_Z, Y=HPS_X, Z=HPS_Y
            double pxBeam = pBeam * FastMath.cos(rotAngle);  // tracking X = HPS Z
            double pyBeam = -pBeam * FastMath.sin(rotAngle); // tracking Y = HPS X
            double pzBeam = 0.0;                             // tracking Z = HPS Y
            double me = 0.000511;  // electron mass in GeV
            double eBeam = FastMath.sqrt(pBeam * pBeam + me * me);
            fourMomentumConstraintVec = MatrixUtils.createRealVector(new double[]{pxBeam, pyBeam, pzBeam, eBeam});

            double dpOverP = 1e-2;
            double sigmaTheta = 100e-6;           // beam angular divergence (rad)
            double sigmaL = dpOverP * pBeam;
            double sigmaT = FastMath.hypot(sigmaTheta * pBeam, sigmaTNuclearRecoil);
            double cosR = FastMath.cos(rotAngle);
            double sinR = FastMath.sin(rotAngle);
            double sL2 = sigmaL * sigmaL;
            double sT2 = sigmaT * sigmaT;
            fourMomentumConstraintCovMat = MatrixUtils.createRealMatrix(4, 4);
            fourMomentumConstraintCovMat.setEntry(0, 0, sL2 * cosR * cosR + sT2 * sinR * sinR);
            fourMomentumConstraintCovMat.setEntry(0, 1, (sT2 - sL2) * sinR * cosR);
            fourMomentumConstraintCovMat.setEntry(1, 0, (sT2 - sL2) * sinR * cosR);
            fourMomentumConstraintCovMat.setEntry(1, 1, sL2 * sinR * sinR + sT2 * cosR * cosR);
            fourMomentumConstraintCovMat.setEntry(2, 2, sT2);
            double sigmaE = dpOverP * eBeam;
            fourMomentumConstraintCovMat.setEntry(3, 3, sigmaE * sigmaE);
        }

        // Dispatch to the appropriate fitting method.
        // fitBillior1985:        Billior (NIM A225, 1984) / Billior & Qian (NIM A311, 1992) —
        //                        default for position-only fits.  Full 5-param helix linearisation
        //                        marginalising the 3 momentum DOF via Schur complement.
        // fitLagrangeMultiplier: hard (exact) momentum constraint, V_mom = 0.
        // fitSoftConstrained:    full state-vector NR; used when a beam-momentum constraint
        //                        is needed (adds momentum rows to the constraint system).
        FitResult result;
        if (beamMomentumConstraint && hardMomentumConstraint) {
            result = fitLagrangeMultiplier(tracks, vertexConstraintVec, vertexConstraintCovMat,
                                           fourMomentumConstraintVec, 10, 1e-6);
        } else if (beamMomentumConstraint) {
            result = fitSoftConstrained(tracks, vertexConstraintVec, vertexConstraintCovMat,
                                        fourMomentumConstraintVec, fourMomentumConstraintCovMat,
                                        10, 1e-6);
        } else {
            // Position-only fit: Billior 1985 algorithm.
            result = fitBillior1985(tracks, vertexConstraintVec, vertexConstraintCovMat);
        }

        // Determine label for BilliorVertex
        String label;
        if (beamspotConstraint && beamMomentumConstraint && hardMomentumConstraint) {
            label = "ThreeProngBSBeamHardConstrained";
        } else if (beamspotConstraint && beamMomentumConstraint) {
            label = "ThreeProngBSBeamConstrained";
        } else if (beamspotConstraint) {
            label = "ThreeProngBSConstrained";
        } else if (beamMomentumConstraint && hardMomentumConstraint) {
            label = "ThreeProngMomHardConstrained";
        } else if (beamMomentumConstraint) {
            label = "ThreeProngMomConstrained";
        } else {
            label = "ThreeProngUnconstrained";
        }

        // If the fit failed (e.g. singular track covariance), return null so callers can skip.
        if (result == null) return null;

        // Convert FitResult to BilliorVertex
        // Tracking frame to detector frame: HPS X = TRACK Y, HPS Y = TRACK Z, HPS Z = TRACK X
        // Add referencePosition offset (tracking frame) before converting — mirrors BilliorVertexer
        double vtxX = result.vertex.getEntry(1) + referencePosition[1]; // tracking Y -> HPS X
        double vtxY = result.vertex.getEntry(2) + referencePosition[2]; // tracking Z -> HPS Y
        double vtxZ = result.vertex.getEntry(0) + referencePosition[0]; // tracking X -> HPS Z
        hep.physics.vec.Hep3Vector vtxPos = new hep.physics.vec.BasicHep3Vector(vtxX, vtxY, vtxZ);

        // Convert covariance matrix (tracking -> detector frame)
        // Reorder: (0,1,2) tracking -> (1,2,0) detector
        double[] covPacked = new double[6];
        // Symmetric matrix packed: (0,0), (1,0), (1,1), (2,0), (2,1), (2,2)
        // In detector frame: x=trk_y(1), y=trk_z(2), z=trk_x(0)
        covPacked[0] = result.vertexCov.getEntry(1, 1); // xx = trk(1,1)
        covPacked[1] = result.vertexCov.getEntry(2, 1); // yx = trk(2,1)
        covPacked[2] = result.vertexCov.getEntry(2, 2); // yy = trk(2,2)
        covPacked[3] = result.vertexCov.getEntry(0, 1); // zx = trk(0,1)
        covPacked[4] = result.vertexCov.getEntry(0, 2); // zy = trk(0,2)
        covPacked[5] = result.vertexCov.getEntry(0, 0); // zz = trk(0,0)
        hep.physics.matrix.SymmetricMatrix covVtx = new hep.physics.matrix.SymmetricMatrix(3, covPacked, true);

        // Vertex position error
        hep.physics.vec.Hep3Vector vtxPosErr = new hep.physics.vec.BasicHep3Vector(
            FastMath.sqrt(result.vertexCov.getEntry(1, 1)),
            FastMath.sqrt(result.vertexCov.getEntry(2, 2)),
            FastMath.sqrt(result.vertexCov.getEntry(0, 0))
        );

        // Fitted momenta (convert tracking -> detector frame)
        java.util.Map<Integer, hep.physics.vec.Hep3Vector> pFitMap = new java.util.HashMap<>();
        double me = 0.000511;
        double totalE = 0.0;
        double totalPx = 0.0, totalPy = 0.0, totalPz = 0.0;

        for (int i = 0; i < result.trackMomenta.size(); i++) {
            RealVector p = result.trackMomenta.get(i).p;
            // tracking (px,py,pz) -> detector (py, pz, px)
            double detPx = p.getEntry(1);
            double detPy = p.getEntry(2);
            double detPz = p.getEntry(0);
            pFitMap.put(i, new hep.physics.vec.BasicHep3Vector(detPx, detPy, detPz));
            double pMag = p.getNorm();
            totalE += FastMath.sqrt(pMag * pMag + me * me);
            totalPx += detPx;
            totalPy += detPy;
            totalPz += detPz;
        }

        double pSumSq = totalPx * totalPx + totalPy * totalPy + totalPz * totalPz;
        double massSq = totalE * totalE - pSumSq;
        double invMass = massSq > 0 ? FastMath.sqrt(massSq) : -99.0;

        BilliorVertex bv = new BilliorVertex(vtxPos, covVtx, result.chi2, invMass, pFitMap, label);
        bv.setPositionError(vtxPosErr);
        bv.setProbability(result.ndf);
        bv.setParameter("ndf", (double) result.ndf);

        // Total (summed) fitted momentum + diagonal error, converted tracking -> detector frame
        // with the same {1,2,0} reindex used for the per-track momenta above. Reuses the
        // existing (Kalman-unused until now) BilliorVertex V0-momentum slot rather than adding
        // new custom-parameter keys -- this is exactly the "total momentum + error" slot it was
        // designed for, and it is already wired into getParameters() (V0Px/y/z, V0PxErr/etc,
        // V0PErr). Only the diagonal error is stored, matching the diagonal-only convention
        // already used for fitMom{i}_pxErr below.
        if (result.totalMomentum != null && result.totalMomentumCov != null) {
            int[] map = {1, 2, 0}; // detector index -> tracking index
            double detTotalPx = result.totalMomentum.getEntry(map[0]);
            double detTotalPy = result.totalMomentum.getEntry(map[1]);
            double detTotalPz = result.totalMomentum.getEntry(map[2]);
            hep.physics.vec.Hep3Vector detTotalP =
                    new hep.physics.vec.BasicHep3Vector(detTotalPx, detTotalPy, detTotalPz);
            hep.physics.vec.Hep3Vector detTotalPErr = new hep.physics.vec.BasicHep3Vector(
                    FastMath.sqrt(FastMath.abs(result.totalMomentumCov.getEntry(map[0], map[0]))),
                    FastMath.sqrt(FastMath.abs(result.totalMomentumCov.getEntry(map[1], map[1]))),
                    FastMath.sqrt(FastMath.abs(result.totalMomentumCov.getEntry(map[2], map[2]))));
            bv.setV0Momentum(detTotalP, detTotalPErr);
        }

        // Store momentum covariances in detector frame (for the _covTrkMomList accessor path)
        if (storeCovTrkMomList) {
            java.util.List<hep.physics.matrix.Matrix> covTrkMomList = new java.util.ArrayList<>();
            for (int i = 0; i < result.trackMomenta.size(); i++) {
                RealMatrix pCov = result.trackMomenta.get(i).pCov;
                // Reorder tracking -> detector frame: det (x,y,z) = trk (y,z,x)
                double[][] detCov = new double[3][3];
                int[] map = {1, 2, 0}; // detector index -> tracking index
                for (int a = 0; a < 3; a++)
                    for (int b = 0; b < 3; b++)
                        detCov[a][b] = pCov.getEntry(map[a], map[b]);
                double[] packed = new double[6];
                packed[0] = detCov[0][0];
                packed[1] = detCov[1][0];
                packed[2] = detCov[1][1];
                packed[3] = detCov[2][0];
                packed[4] = detCov[2][1];
                packed[5] = detCov[2][2];
                covTrkMomList.add(new hep.physics.matrix.SymmetricMatrix(3, packed, true));
            }
            bv.setTrackMomentumCovariances(covTrkMomList);
        }

        // Always store fitted momentum errors and fitted track parameters + errors as named
        // custom parameters.  This uses the same proven getParameters()/setParameter() path
        // as vXErr, invMass, and the predicted-track quantities, so they are accessible to
        // any downstream analyser without relying on the _covTrkMomList / _fitTrkParsList fields.
        for (int i = 0; i < result.trackMomenta.size(); i++) {
            RealMatrix pCov = result.trackMomenta.get(i).pCov;
            // Tracking -> detector: det x = trk y (index 1), det y = trk z (index 2), det z = trk x (index 0)
            String mpfx = "fitMom" + i + "_";
            bv.setParameter(mpfx + "pxErr", FastMath.sqrt(FastMath.abs(pCov.getEntry(1, 1))));
            bv.setParameter(mpfx + "pyErr", FastMath.sqrt(FastMath.abs(pCov.getEntry(2, 2))));
            bv.setParameter(mpfx + "pzErr", FastMath.sqrt(FastMath.abs(pCov.getEntry(0, 0))));
        }
        // fitSoftConstrained() always populates result.fittedTracks.
        // The fallback to input tracks guards against any future code path that returns null.
        List<TrackParams> tracksForOutput = (result.fittedTracks != null) ? result.fittedTracks : tracks;
        for (int i = 0; i < tracksForOutput.size(); i++) {
            TrackParams ft = tracksForOutput.get(i);
            String tpfx = "fitTrk" + i + "_";
            bv.setParameter(tpfx + "d0",       ft.d0);
            bv.setParameter(tpfx + "phi0",     ft.phi0);
            bv.setParameter(tpfx + "omega",    ft.omega);
            bv.setParameter(tpfx + "z0",       ft.z0);
            bv.setParameter(tpfx + "tanL",     ft.tanLambda);
            bv.setParameter(tpfx + "d0Err",    FastMath.sqrt(FastMath.abs(ft.cov.getEntry(0, 0))));
            bv.setParameter(tpfx + "phi0Err",  FastMath.sqrt(FastMath.abs(ft.cov.getEntry(1, 1))));
            bv.setParameter(tpfx + "omegaErr", FastMath.sqrt(FastMath.abs(ft.cov.getEntry(2, 2))));
            bv.setParameter(tpfx + "z0Err",    FastMath.sqrt(FastMath.abs(ft.cov.getEntry(3, 3))));
            bv.setParameter(tpfx + "tanLErr",  FastMath.sqrt(FastMath.abs(ft.cov.getEntry(4, 4))));
        }

        if (debugFlag) {
            System.out.println("=== KalmanVertexFitterGainMatrix::fitVertex ===");
            System.out.println("  B field: " + bField);
            System.out.println("  Beamspot constraint: " + beamspotConstraint);
            System.out.println("  Beam momentum constraint: " + beamMomentumConstraint);
            System.out.println("  Hard momentum constraint: " + hardMomentumConstraint);
            System.out.println("  Label: " + label);
            System.out.println("  Number of tracks: " + tracks.size());
            for (int i = 0; i < tracks.size(); i++) {
                TrackParams t = tracks.get(i);
                System.out.printf("  Input Track %d: d0=%.4f phi0=%.4f omega=%.6f z0=%.4f tanLambda=%.4f%n",
                                  i, t.d0, t.phi0, t.omega, t.z0, t.tanLambda);
            }
            if (fourMomentumConstraintVec != null) {
                System.out.printf("  Beam 4-momentum constraint: [%.4f, %.4f, %.4f, %.4f]%n",
                                  fourMomentumConstraintVec.getEntry(0),
                                  fourMomentumConstraintVec.getEntry(1),
                                  fourMomentumConstraintVec.getEntry(2),
                                  fourMomentumConstraintVec.getEntry(3));
            }
            if (vertexConstraintVec != null) {
                System.out.printf("  Vertex constraint: [%.4f, %.4f, %.4f]%n",
                                  vertexConstraintVec.getEntry(0),
                                  vertexConstraintVec.getEntry(1),
                                  vertexConstraintVec.getEntry(2));
            }
            System.out.println("  --- Fit Results ---");
            System.out.printf("  Vertex (tracking frame): [%.4f, %.4f, %.4f]%n",
                              result.vertex.getEntry(0), result.vertex.getEntry(1), result.vertex.getEntry(2));
            System.out.println("  Vertex (det frame): " + vtxPos);
            System.out.println("  Vertex error (det frame): " + vtxPosErr);
            System.out.printf("  Vertex covariance (det frame): [%.6f, %.6f, %.6f; %.6f, %.6f; %.6f]%n",
                              covPacked[0], covPacked[1], covPacked[2],
                              covPacked[3], covPacked[4], covPacked[5]);
            System.out.printf("  Chi2: %.4f  NDF: %d  Chi2/NDF: %.4f%n",
                              result.chi2, result.ndf,
                              result.ndf > 0 ? result.chi2 / result.ndf : -1.0);
            System.out.println("  InvMass: " + invMass);
            for (int i = 0; i < result.trackMomenta.size(); i++) {
                RealVector pTrk = result.trackMomenta.get(i).p;
                hep.physics.vec.Hep3Vector pDet = pFitMap.get(i);
                double pMagI = pTrk.getNorm();
                System.out.printf("  Track %d momentum (trk frame): [%.4f, %.4f, %.4f] |p|=%.4f%n",
                                  i, pTrk.getEntry(0), pTrk.getEntry(1), pTrk.getEntry(2), pMagI);
                System.out.printf("  Track %d momentum (det frame): [%.4f, %.4f, %.4f] |p|=%.4f%n",
                                  i, pDet.x(), pDet.y(), pDet.z(), pDet.magnitude());
            }
            double totalPMag = FastMath.sqrt(totalPx * totalPx + totalPy * totalPy + totalPz * totalPz);
            System.out.printf("  Total momentum (det frame): [%.4f, %.4f, %.4f] |p|=%.4f  E=%.4f%n",
                              totalPx, totalPy, totalPz, totalPMag, totalE);
            System.out.println("=== End KalmanVertexFitterGainMatrix::fitVertex ===");
        }

        return bv;
    }

    // Getters
    public RealVector getVertex() { return vertex; }
    public RealMatrix getVertexCov() { return vertexCov; }
    public double getChi2() { return chi2; }
    public int getNdf() { return ndf; }
    public List<TrackMomentum> getTrackMomenta() { return trackMomenta; }

    /**
     * Fit vertex using N-1 tracks and predict the momentum of the Nth track
     * 
     * This method uses vertex and/or total momentum constraints to fit with a subset
     * of tracks, then predicts the momentum of the excluded track.
     * 
     * @param tracks All track parameters including the one to predict
     * @param predictTrackIdx Index of track to predict (0-based)
     * @param vertexConstraint Known vertex position
     * @param vertexConstraintCov Vertex constraint covariance
     * @param totalMomentumConstraint Known total momentum of ALL tracks
     * @param totalMomentumConstraintCov Total momentum constraint covariance
     * @param massConstraint Invariant mass constraint for ALL tracks
     * @param massConstraintSigma Mass constraint uncertainty
     * @param maxIterations Maximum iterations
     * @param tolerance Convergence tolerance
     * @return PredictedTrackResult containing vertex, predicted momentum, and all track momenta
     */
    public PredictedTrackResult fitWithPredictedTrack(
            List<TrackParams> tracks,
            int predictTrackIdx,
            RealVector vertexConstraint,
            RealMatrix vertexConstraintCov,
            RealVector totalMomentumConstraint,
            RealMatrix totalMomentumConstraintCov,
            Double massConstraint,
            Double massConstraintSigma,
            int maxIterations,
            double tolerance) {
        
        int nTracks = tracks.size();
        
        if (predictTrackIdx < 0 || predictTrackIdx >= nTracks) {
            throw new IllegalArgumentException(
                "predictTrackIdx must be between 0 and " + (nTracks - 1));
        }
        
        // Fit all N tracks to get the best vertex - do NOT use momentum constraint here;
        // we use momentum conservation to PREDICT the excluded track's momentum
        FitResult fitResult = fit(
            tracks, null, vertexConstraint, vertexConstraintCov,
            null, null,  // No momentum constraint for N-track fit
            null, null,  // No mass constraint either
            maxIterations, tolerance
        );

        return fitWithPredictedTrack(fitResult, predictTrackIdx,
            totalMomentumConstraint, totalMomentumConstraintCov,
            massConstraint, massConstraintSigma);
    }

    /**
     * Predict the momentum of one track using momentum conservation, given a pre-computed
     * vertex fit result. The vertex fit must already include all tracks.
     * Use this overload to avoid recomputing the vertex fit for each predicted track.
     *
     * @param precomputedFit  Result of a prior fit to all N tracks
     * @param predictTrackIdx Index of the track whose momentum is to be predicted
     * @param totalMomentumConstraint Known total momentum (beam momentum)
     * @param totalMomentumConstraintCov Covariance on total momentum
     * @param massConstraint  Invariant mass constraint (null to skip)
     * @param massConstraintSigma Mass constraint uncertainty (null to skip)
     * @return PredictedTrackResult containing vertex, predicted momentum, and all track momenta
     */
    public PredictedTrackResult fitWithPredictedTrack(
            FitResult precomputedFit,
            int predictTrackIdx,
            RealVector totalMomentumConstraint,
            RealMatrix totalMomentumConstraintCov,
            Double massConstraint,
            Double massConstraintSigma) {

        int nTracks = precomputedFit.trackMomenta.size();

        if (predictTrackIdx < 0 || predictTrackIdx >= nTracks) {
            throw new IllegalArgumentException(
                "predictTrackIdx must be between 0 and " + (nTracks - 1));
        }

        FitResult fitResult = precomputedFit;

        // Predict momentum of excluded track
        RealVector predictedP;
        RealMatrix predictedPCov;
        
        if (totalMomentumConstraint != null) {
            // Use momentum conservation: p_predicted = p_total - sum(p_fitted for other tracks)
            RealVector fittedTotalP = MatrixUtils.createRealVector(new double[3]);
            RealMatrix fittedPCov = MatrixUtils.createRealMatrix(3, 3);
            for (int i = 0; i < nTracks; i++) {
                if (i == predictTrackIdx) continue;
                TrackMomentum mom = fitResult.trackMomenta.get(i);
                fittedTotalP = fittedTotalP.add(mom.p);
                fittedPCov = fittedPCov.add(mom.pCov);
                if(debugFlag)
                    System.out.printf("DEBUG fitWithPredictedTrack: fitted track %d p (tracking) = [%.4f, %.4f, %.4f] |p|=%.4f%n",
                                      i, mom.p.getEntry(0), mom.p.getEntry(1), mom.p.getEntry(2), mom.p.getNorm());
            }
            predictedP = totalMomentumConstraint.subtract(fittedTotalP);
            if(debugFlag){
                System.out.printf("DEBUG fitWithPredictedTrack: total fitted p (tracking) = [%.4f, %.4f, %.4f] |p|=%.4f%n",
                                  fittedTotalP.getEntry(0), fittedTotalP.getEntry(1), fittedTotalP.getEntry(2), fittedTotalP.getNorm());
                System.out.printf("DEBUG fitWithPredictedTrack: momentum constraint (tracking) = [%.4f, %.4f, %.4f] |p|=%.4f%n",
                                  totalMomentumConstraint.getEntry(0), totalMomentumConstraint.getEntry(1), totalMomentumConstraint.getEntry(2),
                                  totalMomentumConstraint.getNorm());
                System.out.printf("DEBUG fitWithPredictedTrack: predicted p (tracking) = [%.4f, %.4f, %.4f] |p|=%.4f%n",
                                  predictedP.getEntry(0), predictedP.getEntry(1), predictedP.getEntry(2), predictedP.getNorm());
            }
            // Propagate uncertainty: Cov(p_pred) = Cov(p_total) + Cov(sum p_fitted for other tracks)
            // Note: This assumes uncorrelated track momenta (ignores vertex correlations)

            if (totalMomentumConstraintCov != null) {
                predictedPCov = totalMomentumConstraintCov.add(fittedPCov);
            } else {
                predictedPCov = fittedPCov;
            }

        } else {
            // Without momentum constraint, use the fitted momentum of the predicted track
            predictedP = fitResult.trackMomenta.get(predictTrackIdx).p;
            predictedPCov = fitResult.trackMomenta.get(predictTrackIdx).pCov;
        }
        
        // Create momentum info for predicted track
        TrackMomentum predictedMom = new TrackMomentum(predictedP, predictedPCov);
        
        // All track momenta in original order; replace predicted track entry with
        // the conservation-predicted momentum (actual fitted momentum stored in actualP)
        List<TrackMomentum> allTrackMomenta = new ArrayList<>(fitResult.trackMomenta);
        allTrackMomenta.set(predictTrackIdx, predictedMom);
        
        // Calculate total chi2:
        // Start with chi2 from fitting all N tracks
        double chi2Total = fitResult.chi2;
        if(debugFlag)
            System.out.printf("DEBUG fitWithPredictedTrack: N-track fit chi2=%.2f ndf=%d%n", fitResult.chi2, fitResult.ndf);

        // Add chi2 contribution from comparing predicted momentum to actual fitted track momentum
        RealVector actualP = fitResult.trackMomenta.get(predictTrackIdx).p;
        RealMatrix actualPCov = fitResult.trackMomenta.get(predictTrackIdx).pCov;

        if(debugFlag)
            System.out.printf("DEBUG fitWithPredictedTrack: actual p (tracking) = [%.4f, %.4f, %.4f] |p|=%.4f%n",
                          actualP.getEntry(0), actualP.getEntry(1), actualP.getEntry(2), actualP.getNorm());

        // Residual: predicted - actual
        RealVector pResidual = predictedP.subtract(actualP);
        if(debugFlag)
            System.out.printf("DEBUG fitWithPredictedTrack: residual (pred-act) = [%.4f, %.4f, %.4f]%n",
                              pResidual.getEntry(0), pResidual.getEntry(1), pResidual.getEntry(2));

        // Combined covariance for the comparison
        RealMatrix combinedCov = predictedPCov.add(actualPCov);

        // Chi2 contribution from momentum comparison
        double chi2Momentum = 0;
        try {
            RealMatrix combinedCovInv = new LUDecomposition(combinedCov).getSolver().getInverse();
            chi2Momentum = pResidual.dotProduct(combinedCovInv.operate(pResidual));
            chi2Total += chi2Momentum;
            if(debugFlag)                
                System.out.printf("DEBUG fitWithPredictedTrack: chi2 from momentum comparison = %.2f%n", chi2Momentum);
        } catch (SingularMatrixException e) {
            // Combined covariance is singular; skip this chi2 contribution silently
        }
        if(debugFlag)
            System.out.printf("DEBUG fitWithPredictedTrack: total chi2 = %.2f%n", chi2Total);
        
        // ndf: from N-track fit plus 3 for the 3-component momentum comparison
        int ndfTotal = fitResult.ndf + 3;

        this.vertex = fitResult.vertex;
        this.vertexCov = fitResult.vertexCov;
        this.chi2 = chi2Total;
        this.ndf = ndfTotal;
        this.trackMomenta = allTrackMomenta;

        return new PredictedTrackResult(
            fitResult.vertex, fitResult.vertexCov,
            predictedP, predictedPCov,
            actualP, actualPCov,
            pResidual,
            chi2Total, ndfTotal, allTrackMomenta
        );
    }

    /**
     * Convert a PredictedTrackResult to a BilliorVertex
     *
     * @param result The predicted track result
     * @param predictedTrackIdx Index of the predicted track (0=ele, 1=pos, 2=rec)
     * @param label Label for the vertex type
     * @return BilliorVertex containing the fit results
     */
    public BilliorVertex predictedResultToBilliorVertex(PredictedTrackResult result, int predictedTrackIdx, String label) {
        // Tracking frame to detector frame: HPS X = TRACK Y, HPS Y = TRACK Z, HPS Z = TRACK X
        double vtxX = result.vertex.getEntry(1); // tracking Y -> HPS X
        double vtxY = result.vertex.getEntry(2); // tracking Z -> HPS Y
        double vtxZ = result.vertex.getEntry(0); // tracking X -> HPS Z
        hep.physics.vec.Hep3Vector vtxPos = new hep.physics.vec.BasicHep3Vector(vtxX, vtxY, vtxZ);

        // Convert covariance matrix (tracking -> detector frame)
        double[] covPacked = new double[6];
        covPacked[0] = result.vertexCov.getEntry(1, 1); // xx = trk(1,1)
        covPacked[1] = result.vertexCov.getEntry(2, 1); // yx = trk(2,1)
        covPacked[2] = result.vertexCov.getEntry(2, 2); // yy = trk(2,2)
        covPacked[3] = result.vertexCov.getEntry(0, 1); // zx = trk(0,1)
        covPacked[4] = result.vertexCov.getEntry(0, 2); // zy = trk(0,2)
        covPacked[5] = result.vertexCov.getEntry(0, 0); // zz = trk(0,0)
        hep.physics.matrix.SymmetricMatrix covVtx = new hep.physics.matrix.SymmetricMatrix(3, covPacked, true);

        // Vertex position error
        hep.physics.vec.Hep3Vector vtxPosErr = new hep.physics.vec.BasicHep3Vector(
            FastMath.sqrt(result.vertexCov.getEntry(1, 1)),
            FastMath.sqrt(result.vertexCov.getEntry(2, 2)),
            FastMath.sqrt(result.vertexCov.getEntry(0, 0))
        );

        // Fitted momenta (convert tracking -> detector frame)
        java.util.Map<Integer, hep.physics.vec.Hep3Vector> pFitMap = new java.util.HashMap<>();
        double me = 0.000511;
        double totalE = 0.0;
        double totalPx = 0.0, totalPy = 0.0, totalPz = 0.0;

        for (int i = 0; i < result.allTrackMomenta.size(); i++) {
            RealVector p = result.allTrackMomenta.get(i).p;
            // tracking (px,py,pz) -> detector (py, pz, px)
            double detPx = p.getEntry(1);
            double detPy = p.getEntry(2);
            double detPz = p.getEntry(0);
            pFitMap.put(i, new hep.physics.vec.BasicHep3Vector(detPx, detPy, detPz));
            double pMag = p.getNorm();
            totalE += FastMath.sqrt(pMag * pMag + me * me);
            totalPx += detPx;
            totalPy += detPy;
            totalPz += detPz;
        }

        double pSumSq = totalPx * totalPx + totalPy * totalPy + totalPz * totalPz;
        double massSq = totalE * totalE - pSumSq;
        double invMass = massSq > 0 ? FastMath.sqrt(massSq) : -99.0;

        BilliorVertex bv = new BilliorVertex(vtxPos, covVtx, result.chi2, invMass, pFitMap, label);
        bv.setProbability(result.ndf);
        bv.setParameter("ndf", (double) result.ndf);
        bv.setPositionError(vtxPosErr);

        // Store predicted momentum (converted to detector frame)
        // Tracking frame: (px_trk, py_trk, pz_trk) -> Detector frame: (py_trk, pz_trk, px_trk)
        double predPx = result.predictedMomentum.getEntry(1);  // trk Y -> det X
        double predPy = result.predictedMomentum.getEntry(2);  // trk Z -> det Y
        double predPz = result.predictedMomentum.getEntry(0);  // trk X -> det Z
        double predPtot = Math.sqrt(predPx*predPx + predPy*predPy + predPz*predPz);
        if(debugFlag){
            System.out.printf("DEBUG toBilliorVertex: predicted p (tracking) = [%.4f, %.4f, %.4f]%n",
                              result.predictedMomentum.getEntry(0), result.predictedMomentum.getEntry(1), result.predictedMomentum.getEntry(2));
            System.out.printf("DEBUG toBilliorVertex: predicted p (detector) = [%.4f, %.4f, %.4f] |p|=%.4f%n",
                              predPx, predPy, predPz, predPtot);
        }
        bv.setParameter("predictedPx", predPx);
        bv.setParameter("predictedPy", predPy);
        bv.setParameter("predictedPz", predPz);

        // Store actual momentum (converted to detector frame)
        double actPx = result.actualMomentum.getEntry(1);
        double actPy = result.actualMomentum.getEntry(2);
        double actPz = result.actualMomentum.getEntry(0);
        double actPtot = Math.sqrt(actPx*actPx + actPy*actPy + actPz*actPz);
        if(debugFlag){
            System.out.printf("DEBUG toBilliorVertex: actual p (tracking) = [%.4f, %.4f, %.4f]%n",
                              result.actualMomentum.getEntry(0), result.actualMomentum.getEntry(1), result.actualMomentum.getEntry(2));
            System.out.printf("DEBUG toBilliorVertex: actual p (detector) = [%.4f, %.4f, %.4f] |p|=%.4f%n",
                              actPx, actPy, actPz, actPtot);
        }
        bv.setParameter("actualPx", actPx);
        bv.setParameter("actualPy", actPy);
        bv.setParameter("actualPz", actPz);

        // Store residuals (converted to detector frame)
        double resPx = result.momentumResidual.getEntry(1);
        double resPy = result.momentumResidual.getEntry(2);
        double resPz = result.momentumResidual.getEntry(0);
        bv.setParameter("residualPx", resPx);
        bv.setParameter("residualPy", resPy);
        bv.setParameter("residualPz", resPz);

        // Store predicted momentum covariance (converted to detector frame)
        {
            RealMatrix pCov = result.predictedMomentumCov;
            int[] map = {1, 2, 0}; // detector index -> tracking index
            bv.setParameter("predictedMomCovXX", pCov.getEntry(map[0], map[0]));
            bv.setParameter("predictedMomCovXY", pCov.getEntry(map[0], map[1]));
            bv.setParameter("predictedMomCovXZ", pCov.getEntry(map[0], map[2]));
            bv.setParameter("predictedMomCovYY", pCov.getEntry(map[1], map[1]));
            bv.setParameter("predictedMomCovYZ", pCov.getEntry(map[1], map[2]));
            bv.setParameter("predictedMomCovZZ", pCov.getEntry(map[2], map[2]));
        }

        // Store actual momentum covariance (converted to detector frame)
        {
            RealMatrix pCov = result.actualMomentumCov;
            int[] map = {1, 2, 0}; // detector index -> tracking index
            bv.setParameter("actualMomCovXX", pCov.getEntry(map[0], map[0]));
            bv.setParameter("actualMomCovXY", pCov.getEntry(map[0], map[1]));
            bv.setParameter("actualMomCovXZ", pCov.getEntry(map[0], map[2]));
            bv.setParameter("actualMomCovYY", pCov.getEntry(map[1], map[1]));
            bv.setParameter("actualMomCovYZ", pCov.getEntry(map[1], map[2]));
            bv.setParameter("actualMomCovZZ", pCov.getEntry(map[2], map[2]));
        }

        // Store which track was predicted
        bv.setParameter("predictedTrackIdx", (double) predictedTrackIdx);

        // Store momentum covariances if requested
        if (storeCovTrkMomList) {
            java.util.List<hep.physics.matrix.Matrix> covTrkMomList = new java.util.ArrayList<>();
            for (int i = 0; i < result.allTrackMomenta.size(); i++) {
                RealMatrix pCov = result.allTrackMomenta.get(i).pCov;
                // Reorder tracking -> detector frame
                double[][] detCov = new double[3][3];
                int[] map = {1, 2, 0}; // detector index -> tracking index
                for (int a = 0; a < 3; a++)
                    for (int b = 0; b < 3; b++)
                        detCov[a][b] = pCov.getEntry(map[a], map[b]);
                double[] packed = new double[6];
                packed[0] = detCov[0][0];
                packed[1] = detCov[1][0];
                packed[2] = detCov[1][1];
                packed[3] = detCov[2][0];
                packed[4] = detCov[2][1];
                packed[5] = detCov[2][2];
                covTrkMomList.add(new hep.physics.matrix.SymmetricMatrix(3, packed, true));
            }
            bv.setTrackMomentumCovariances(covTrkMomList);
        }

        return bv;
    }

    /**
     * Result from fitting with predicted track
     */
    public static class PredictedTrackResult {
        public RealVector vertex;
        public RealMatrix vertexCov;
        public RealVector predictedMomentum;
        public RealMatrix predictedMomentumCov;
        public RealVector actualMomentum;       // Measured momentum of excluded track
        public RealMatrix actualMomentumCov;
        public RealVector momentumResidual;     // predicted - actual
        public double chi2;
        public int ndf;
        public List<TrackMomentum> allTrackMomenta;

        public PredictedTrackResult(RealVector vertex, RealMatrix vertexCov,
                                   RealVector predictedMomentum, RealMatrix predictedMomentumCov,
                                   RealVector actualMomentum, RealMatrix actualMomentumCov,
                                   RealVector momentumResidual,
                                   double chi2, int ndf, List<TrackMomentum> allTrackMomenta) {
            this.vertex = vertex;
            this.vertexCov = vertexCov;
            this.predictedMomentum = predictedMomentum;
            this.predictedMomentumCov = predictedMomentumCov;
            this.actualMomentum = actualMomentum;
            this.actualMomentumCov = actualMomentumCov;
            this.momentumResidual = momentumResidual;
            this.chi2 = chi2;
            this.ndf = ndf;
            this.allTrackMomenta = allTrackMomenta;
        }
    }
}
