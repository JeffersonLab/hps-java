package org.hps.recon.vertexing;

import org.apache.commons.math3.linear.*;
import org.apache.commons.math3.util.FastMath;
import java.util.ArrayList;
import java.util.List;


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
        double s = R * dphi;
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
        double dzDx = R * track.tanLambda * dphiDx;
        double dzDy = R * track.tanLambda * dphiDy;

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
        double s = R * dphi;

        // Derivatives of z_v w.r.t. perigee
        double dzDd0 = track.tanLambda * R * dphiDd0;
        double dzDphi0 = -track.tanLambda * R + track.tanLambda * R * dphiDphi0;
        double dzDomega = -s * track.tanLambda / track.omega + track.tanLambda * R * dphiDomega;
        double dzDz0 = 1.0;
        double dzDtl = s;

        RealMatrix J = MatrixUtils.createRealMatrix(2, 5);
        J.setRow(0, new double[]{dftDd0, dftDphi0, dftDomega, 0.0, 0.0});
        J.setRow(1, new double[]{dzDd0, dzDphi0, dzDomega, dzDz0, dzDtl});
        
        return J.multiply(track.cov).multiply(J.transpose());
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
    private RealVector computeMomentumAtVertex(TrackParams track, RealVector vertex) {
        VertexParams vp = perigeeToVertexParams(track, vertex.getEntry(0), vertex.getEntry(1));
        
        double pT = 2.99792458e-4 * bField / FastMath.abs(track.omega);
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
        double pT = 2.99792458e-4 * bField / FastMath.abs(track.omega);
        
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
        double xV = vertex.getEntry(0);
        double yV = vertex.getEntry(1);
        
        VertexParams vp = perigeeToVertexParams(track, xV, yV);
        
        double R = 1.0 / FastMath.abs(track.omega);
        double sign = FastMath.signum(track.omega);
        double pT = 2.99792458e-4 * bField / FastMath.abs(track.omega);
        
        double xc = sign * R * FastMath.sin(track.phi0) - track.d0 * FastMath.sin(track.phi0);
        double yc = -sign * R * FastMath.cos(track.phi0) + track.d0 * FastMath.cos(track.phi0);
        double dx = xV - xc;
        double dy = yV - yc;
        double r2 = dx * dx + dy * dy;
        
        double dphiDd0 = -(FastMath.cos(track.phi0) * dx + FastMath.sin(track.phi0) * dy) / r2;
        double dphiDphi0 = (sign * R - track.d0) * (dy * FastMath.cos(track.phi0) - dx * FastMath.sin(track.phi0)) / r2;
        double dphiDomega = -R * R * (FastMath.cos(track.phi0) * dx + FastMath.sin(track.phi0) * dy) / r2;
        
        double dpTDomega = -2.99792458e-4 * bField * sign / (track.omega * track.omega);
        
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
        
        return Jp.multiply(track.cov).multiply(Jp.transpose());
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
        
        // Initial covariance
        RealMatrix C;
        if (vertexConstraint != null && vertexConstraintCov != null) {
            C = vertexConstraintCov.copy();
        } else {
            C = MatrixUtils.createRealIdentityMatrix(3).scalarMultiply(100.0);
        }
        
        // Iterative Gain Matrix updates
        for (int iteration = 0; iteration < maxIterations; iteration++) {
            RealVector vertexOld = vertex.copy();
            
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
                
                // Update covariance: C = (I - K * H) * C
                RealMatrix I = MatrixUtils.createRealIdentityMatrix(3);
                C = I.subtract(K.multiply(constraint.H)).multiply(C);
            }
            
            // Apply track constraints
            for (TrackParams track : tracks) {
                Constraint constraint = computeTrackConstraint(track, vertex);
                
                RealMatrix S = constraint.H.multiply(C).multiply(constraint.H.transpose()).add(constraint.V);
                RealMatrix K = C.multiply(constraint.H.transpose()).multiply(
                    new LUDecomposition(S).getSolver().getInverse()
                );
                
                vertex = vertex.subtract(K.operate(constraint.c));
                RealMatrix I = MatrixUtils.createRealIdentityMatrix(3);
                C = I.subtract(K.multiply(constraint.H)).multiply(C);
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
                    RealMatrix I = MatrixUtils.createRealIdentityMatrix(3);
                    C = I.subtract(K.multiply(constraint.H)).multiply(C);
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
                    RealMatrix I = MatrixUtils.createRealIdentityMatrix(3);
                    C = I.subtract(K.multiply(constraint.H)).multiply(C);
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
        System.out.printf("  Chi2 contributions: vertex=%.4f", chi2Vertex);
        for (int itrk = 0; itrk < nTracks; itrk++) {
            System.out.printf("  track%d=%.4f", itrk, chi2Tracks[itrk]);
        }
        System.out.printf("  momentum=%.4f  total=%.4f%n", chi2Momentum, chi2);
        
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
            RealMatrix trackCovInv = new LUDecomposition(tracks.get(i).cov).getSolver().getInverse();
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
                double dzPredDx = R * track.tanLambda * dphiDx;
                double dzPredDy = R * track.tanLambda * dphiDy;
                H.setEntry(rowZ, 0, -dzPredDx);
                H.setEntry(rowZ, 1, -dzPredDy);
                H.setEntry(rowZ, 2, 1.0);
                // w.r.t. track params
                double dphiDd0 = -(FastMath.cos(track.phi0) * dx + FastMath.sin(track.phi0) * dy) / r2;
                double dphiDphi0 = (sign * R - track.d0) * (dy * FastMath.cos(track.phi0) - dx * FastMath.sin(track.phi0)) / r2;
                double dphiDomega = -R * R * (FastMath.cos(track.phi0) * dx + FastMath.sin(track.phi0) * dy) / r2;
                double phiV = FastMath.atan2(-dx * sign, dy * sign);
                double s = R * (phiV - track.phi0);
                double dzPredDd0    = track.tanLambda * R * dphiDd0;
                double dzPredDphi0  = -track.tanLambda * R + track.tanLambda * R * dphiDphi0;
                double dzPredDomega = -s * track.tanLambda / track.omega + track.tanLambda * R * dphiDomega;
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
                    double pT = 2.99792458e-4 * bField / FastMath.abs(track.omega);

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
                    double dpTDomega = -2.99792458e-4 * bField * sign / (track.omega * track.omega);

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
            // For each track, V is a 2x2 block: V = J_h * trackCov * J_h^T
            // where J_h has rows [d(h_t)/d(params), d(h_z)/d(params)]:
            //   h_t = r - R        -> d(h_t)/d(params) = +Jft  (positive)
            //   h_z = zV - zPred   -> d(h_z)/d(params) = -Jz   (negative)
            // For momentum constraints: use the provided fourMomentumConstraintCov
            RealMatrix constraintCov = MatrixUtils.createRealMatrix(nConstraints, nConstraints);

            // Track constraint covariances: 2x2 block per track
            for (int i = 0; i < nTracks; i++) {
                TrackParams track = tracks.get(i);
                int rowT = 2 * i;
                int rowZ = 2 * i + 1;

                double xV = vertex.getEntry(0);
                double yV = vertex.getEntry(1);

                double R = 1.0 / FastMath.abs(track.omega);
                double sign = FastMath.signum(track.omega);

                double xc = sign * R * FastMath.sin(track.phi0) - track.d0 * FastMath.sin(track.phi0);
                double yc = -sign * R * FastMath.cos(track.phi0) + track.d0 * FastMath.cos(track.phi0);
                double dx = xV - xc;
                double dy = yV - yc;
                double r2 = dx * dx + dy * dy;
                double r  = FastMath.sqrt(r2);

                // d(h_t)/d(track params): Jft (positive)
                double[] Jht = new double[5];
                Jht[0] = (dx * FastMath.sin(track.phi0) - dy * FastMath.cos(track.phi0)) / r;
                Jht[1] = -(sign * R - track.d0) * (dx * FastMath.cos(track.phi0) + dy * FastMath.sin(track.phi0)) / r;
                Jht[2] = (dx * FastMath.sin(track.phi0) - dy * FastMath.cos(track.phi0)) / (r * track.omega * track.omega)
                         + sign / (track.omega * track.omega);
                Jht[3] = 0.0;
                Jht[4] = 0.0;

                // d(h_z)/d(track params): -Jz (negative, since h_z = zV - zPred)
                double dphiDd0 = -(FastMath.cos(track.phi0) * dx + FastMath.sin(track.phi0) * dy) / r2;
                double dphiDphi0 = (sign * R - track.d0) * (dy * FastMath.cos(track.phi0) - dx * FastMath.sin(track.phi0)) / r2;
                double dphiDomega = -R * R * (FastMath.cos(track.phi0) * dx + FastMath.sin(track.phi0) * dy) / r2;
                double phiV = FastMath.atan2(-dx * sign, dy * sign);
                double s = R * (phiV - track.phi0);
                double[] Jhz = new double[5];
                Jhz[0] = -(track.tanLambda * R * dphiDd0);
                Jhz[1] = -(-track.tanLambda * R + track.tanLambda * R * dphiDphi0);
                Jhz[2] = -(-s * track.tanLambda / track.omega + track.tanLambda * R * dphiDomega);
                Jhz[3] = -1.0;
                Jhz[4] = -s;

                // V_2x2 = J_h * trackCov * J_h^T
                double V_tt = 0, V_zz = 0, V_tz = 0;
                for (int a = 0; a < 5; a++) {
                    for (int b = 0; b < 5; b++) {
                        double c = track.cov.getEntry(a, b);
                        V_tt += Jht[a] * c * Jht[b];
                        V_zz += Jhz[a] * c * Jhz[b];
                        V_tz += Jht[a] * c * Jhz[b];
                    }
                }
                constraintCov.setEntry(rowT, rowT, V_tt);
                constraintCov.setEntry(rowT, rowZ, V_tz);
                constraintCov.setEntry(rowZ, rowT, V_tz);
                constraintCov.setEntry(rowZ, rowZ, V_zz);

                if (debugFlag && iteration == 0) {
                    System.out.printf("    Track %d transverse sigma = %.4f mm, z sigma = %.4f mm%n",
                                      i, FastMath.sqrt(V_tt), FastMath.sqrt(V_zz));
                }
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
            // The constraint covariance V allows the constraints to be satisfied approximately,
            // with the deviation weighted by the constraint uncertainty. This is mathematically
            // correct for the track Z constraints which have uncertainty from z0 and tanLambda errors.

            try {
                RealMatrix HWInvHT = H.multiply(WInv).multiply(H.transpose());
                // Add constraint covariance for soft constraints
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

        // NDF = number of constraints
        int ndf = nConstraints;

        // Compute final track momenta using fitted track parameters and their post-fit covariances
        List<TrackMomentum> trackMomenta = new ArrayList<>();
        double me = 0.000511;
        for (TrackParams track : fittedTracks) {
            RealVector p = computeMomentumAtVertex(track, vertex);
            RealMatrix pCov = computeMomentumCovariance(track, vertex);
            trackMomenta.add(new TrackMomentum(p, pCov));
        }

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

        return new FitResult(vertex, vertexCov, chi2, ndf, trackMomenta, fittedTracks);
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
     */
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
    private double pBeam = 3.74;
    private double rotAngle = -0.030;
    private boolean debugFlag = false;
    private boolean storeCovTrkMomList = false;

    public void setBeamSize(double[] bs) { this.beamSize = bs; }
    public void setBeamPosition(double[] bp) { this.beamPosition = bp; }
    public void setBeamEnergy(double energy) { this.pBeam = energy; }
    public void setBeamRotAngle(double angle) { this.rotAngle = angle; }
    public void setDebug(boolean debug) { this.debugFlag = debug; }
    public void setStoreCovTrkMomList(boolean value) { this.storeCovTrkMomList = value; }

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
     *                                the momentum constraint exactly via Lagrange multipliers
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
            double sigmaT = sigmaTheta * pBeam;
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
        // Both methods perform a joint fit of vertex position AND track parameters
        // simultaneously, so result.fittedTracks is always populated.
        // fitLagrangeMultiplier: hard (exact) momentum constraint, V_mom = 0.
        // fitSoftConstrained:    soft momentum constraint, weighted by beam uncertainty.
        FitResult result;
        if (beamMomentumConstraint && hardMomentumConstraint) {
            result = fitLagrangeMultiplier(tracks, vertexConstraintVec, vertexConstraintCovMat,
                                           fourMomentumConstraintVec, 10, 1e-6);
        } else {
            result = fitSoftConstrained(tracks, vertexConstraintVec, vertexConstraintCovMat,
                                        fourMomentumConstraintVec, fourMomentumConstraintCovMat,
                                        10, 1e-6);
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

        // Convert FitResult to BilliorVertex
        // Tracking frame to detector frame: HPS X = TRACK Y, HPS Y = TRACK Z, HPS Z = TRACK X
        double vtxX = result.vertex.getEntry(1); // tracking Y -> HPS X
        double vtxY = result.vertex.getEntry(2); // tracking Z -> HPS Y
        double vtxZ = result.vertex.getEntry(0); // tracking X -> HPS Z
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
            // If covariance is singular, skip this contribution
            System.out.println("Warning: Combined momentum covariance is singular, skipping chi2 contribution");
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
