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
        
        // Constraint residual: vertex z - predicted z from track
        RealVector c = MatrixUtils.createRealVector(new double[]{0.0, zV - vp.zV});
        
        // Compute H = dc/dvertex
        double R = 1.0 / FastMath.abs(track.omega);
        double sign = FastMath.signum(track.omega);
        
        double xc = sign * R * FastMath.sin(track.phi0) - track.d0 * FastMath.sin(track.phi0);
        double yc = -sign * R * FastMath.cos(track.phi0) + track.d0 * FastMath.cos(track.phi0);
        
        double dx = xV - xc;
        double dy = yV - yc;
        double r2 = dx * dx + dy * dy;
        
        // Derivatives
        double dphiDx = -dy / r2;
        double dphiDy = dx / r2;
        
        double dzDx = R * track.tanLambda * dphiDx;
        double dzDy = R * track.tanLambda * dphiDy;
        
        RealMatrix H = MatrixUtils.createRealMatrix(2, 3);
        H.setEntry(0, 0, dphiDx);
        H.setEntry(0, 1, dphiDy);
        H.setEntry(0, 2, 0.0);
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
        
        // Derivatives of phi_v w.r.t. perigee
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
        J.setRow(0, new double[]{dphiDd0, dphiDphi0, dphiDomega, 0.0, 0.0});
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
     * Kinematic fit with 4-momentum constraint using Lagrange multipliers.
     * This method properly updates both vertex AND track parameters to satisfy the constraint.
     *
     * The state vector is: [vertex(3), track1_params(5), track2_params(5), ...]
     * The 4-momentum constraint is: sum(px_i, py_i, pz_i, E_i) = (px_beam, py_beam, pz_beam, E_beam)
     * All particles are assumed to have electron mass.
     *
     * Uses iterative linearized least squares with Lagrange multipliers.
     */
    public FitResult fitKinematic(List<TrackParams> inputTracks,
                                  RealVector vertexConstraint,
                                  RealMatrix vertexConstraintCov,
                                  RealVector fourMomentumConstraint,
                                  RealMatrix fourMomentumConstraintCov,
                                  int maxIterations,
                                  double tolerance) {

        int nTracks = inputTracks.size();
        int nTrackParams = 5;
        int stateSize = 3 + nTracks * nTrackParams;  // vertex(3) + tracks(5 each)

        // Make working copies of tracks
        List<TrackParams> tracks = new ArrayList<>();
        for (TrackParams t : inputTracks) {
            tracks.add(t.copy());
        }

        // Build initial state vector: [vertex, track1, track2, ...]
        RealVector state = MatrixUtils.createRealVector(new double[stateSize]);

        // Initial vertex estimate
        double xInit = 0.0, yInit = 0.0, zInit = 0.0;
        for (TrackParams track : tracks) {
            xInit += -track.d0 * FastMath.sin(track.phi0);
            yInit += track.d0 * FastMath.cos(track.phi0);
            zInit += track.z0;
        }
        if (vertexConstraint != null) {
            state.setEntry(0, vertexConstraint.getEntry(0));
            state.setEntry(1, vertexConstraint.getEntry(1));
            state.setEntry(2, vertexConstraint.getEntry(2));
        } else {
            state.setEntry(0, xInit / nTracks);
            state.setEntry(1, yInit / nTracks);
            state.setEntry(2, zInit / nTracks);
        }

        // Initial track parameters
        for (int i = 0; i < nTracks; i++) {
            TrackParams t = tracks.get(i);
            int offset = 3 + i * nTrackParams;
            state.setEntry(offset + 0, t.d0);
            state.setEntry(offset + 1, t.phi0);
            state.setEntry(offset + 2, t.omega);
            state.setEntry(offset + 3, t.z0);
            state.setEntry(offset + 4, t.tanLambda);
        }

        // Build initial covariance matrix (block diagonal)
        RealMatrix stateCov = MatrixUtils.createRealMatrix(stateSize, stateSize);
        // Vertex covariance
        if (vertexConstraintCov != null) {
            for (int i = 0; i < 3; i++)
                for (int j = 0; j < 3; j++)
                    stateCov.setEntry(i, j, vertexConstraintCov.getEntry(i, j));
        } else {
            for (int i = 0; i < 3; i++)
                stateCov.setEntry(i, i, 100.0);
        }
        // Track covariances
        for (int i = 0; i < nTracks; i++) {
            int offset = 3 + i * nTrackParams;
            RealMatrix tCov = tracks.get(i).cov;
            for (int a = 0; a < nTrackParams; a++)
                for (int b = 0; b < nTrackParams; b++)
                    stateCov.setEntry(offset + a, offset + b, tCov.getEntry(a, b));
        }

        // Store original measured values for chi2 calculation
        RealVector state0 = state.copy();
        RealMatrix stateCov0 = stateCov.copy();

        // Iterative fit
        for (int iteration = 0; iteration < maxIterations; iteration++) {
            RealVector stateOld = state.copy();

            // Extract current vertex
            RealVector vertex = state.getSubVector(0, 3);

            // Update track objects from state
            for (int i = 0; i < nTracks; i++) {
                int offset = 3 + i * nTrackParams;
                tracks.get(i).d0 = state.getEntry(offset + 0);
                tracks.get(i).phi0 = state.getEntry(offset + 1);
                tracks.get(i).omega = state.getEntry(offset + 2);
                tracks.get(i).z0 = state.getEntry(offset + 3);
                tracks.get(i).tanLambda = state.getEntry(offset + 4);
            }

            // Apply vertex constraint (beamspot)
            if (vertexConstraint != null && vertexConstraintCov != null) {
                // c = vertex - vertexConstraint
                RealVector c = vertex.subtract(vertexConstraint);

                // H is identity for vertex part, zero for track parts
                RealMatrix H = MatrixUtils.createRealMatrix(3, stateSize);
                for (int i = 0; i < 3; i++) H.setEntry(i, i, 1.0);

                RealMatrix V = vertexConstraintCov;

                // Kalman update
                RealMatrix S = H.multiply(stateCov).multiply(H.transpose()).add(V);
                RealMatrix K = stateCov.multiply(H.transpose()).multiply(
                    new LUDecomposition(S).getSolver().getInverse());

                state = state.subtract(K.operate(c));
                RealMatrix I = MatrixUtils.createRealIdentityMatrix(stateSize);
                stateCov = I.subtract(K.multiply(H)).multiply(stateCov);

                // Update vertex variable
                vertex = state.getSubVector(0, 3);
            }

            // Apply track constraints (each track must pass through vertex)
            for (int itrk = 0; itrk < nTracks; itrk++) {
                int offset = 3 + itrk * nTrackParams;

                // Update track from state
                TrackParams track = tracks.get(itrk);
                track.d0 = state.getEntry(offset + 0);
                track.phi0 = state.getEntry(offset + 1);
                track.omega = state.getEntry(offset + 2);
                track.z0 = state.getEntry(offset + 3);
                track.tanLambda = state.getEntry(offset + 4);

                vertex = state.getSubVector(0, 3);
                double xV = vertex.getEntry(0);
                double yV = vertex.getEntry(1);
                double zV = vertex.getEntry(2);

                VertexParams vp = perigeeToVertexParams(track, xV, yV);

                // Constraint: [0, zV - zV_predicted]
                RealVector c = MatrixUtils.createRealVector(new double[]{0.0, zV - vp.zV});

                // Compute Jacobian w.r.t. full state
                RealMatrix H = MatrixUtils.createRealMatrix(2, stateSize);

                // Derivatives w.r.t. vertex
                double R = 1.0 / FastMath.abs(track.omega);
                double sign = FastMath.signum(track.omega);
                double xc = sign * R * FastMath.sin(track.phi0) - track.d0 * FastMath.sin(track.phi0);
                double yc = -sign * R * FastMath.cos(track.phi0) + track.d0 * FastMath.cos(track.phi0);
                double dx = xV - xc;
                double dy = yV - yc;
                double r2 = dx * dx + dy * dy;

                double dphiDx = -dy / r2;
                double dphiDy = dx / r2;
                double dzDx = R * track.tanLambda * dphiDx;
                double dzDy = R * track.tanLambda * dphiDy;

                H.setEntry(0, 0, dphiDx);
                H.setEntry(0, 1, dphiDy);
                H.setEntry(1, 0, -dzDx);
                H.setEntry(1, 1, -dzDy);
                H.setEntry(1, 2, 1.0);

                // Derivatives w.r.t. track parameters
                double dphiDd0 = -(FastMath.cos(track.phi0) * dx + FastMath.sin(track.phi0) * dy) / r2;
                double dphiDphi0 = (sign * R - track.d0) * (dy * FastMath.cos(track.phi0) - dx * FastMath.sin(track.phi0)) / r2;
                double dphiDomega = -R * R * (FastMath.cos(track.phi0) * dx + FastMath.sin(track.phi0) * dy) / r2;

                double phiV = FastMath.atan2(-dx * sign, dy * sign);
                double dphi = phiV - track.phi0;
                double s = R * dphi;

                double dzDd0 = track.tanLambda * R * dphiDd0;
                double dzDphi0 = -track.tanLambda * R + track.tanLambda * R * dphiDphi0;
                double dzDomega = -s * track.tanLambda / track.omega + track.tanLambda * R * dphiDomega;
                double dzDz0 = 1.0;
                double dzDtl = s;

                H.setEntry(0, offset + 0, dphiDd0);
                H.setEntry(0, offset + 1, dphiDphi0);
                H.setEntry(0, offset + 2, dphiDomega);
                H.setEntry(1, offset + 0, -dzDd0);
                H.setEntry(1, offset + 1, -dzDphi0);
                H.setEntry(1, offset + 2, -dzDomega);
                H.setEntry(1, offset + 3, -dzDz0 + 1.0);  // Note: dzDz0 = 1, so this cancels
                H.setEntry(1, offset + 4, -dzDtl);

                // Covariance in constraint space (small, since constraint should be exact)
                RealMatrix V = MatrixUtils.createRealMatrix(2, 2);
                V.setEntry(0, 0, 1e-6);
                V.setEntry(1, 1, 1e-6);

                // Kalman update
                RealMatrix S = H.multiply(stateCov).multiply(H.transpose()).add(V);
                RealMatrix K = stateCov.multiply(H.transpose()).multiply(
                    new LUDecomposition(S).getSolver().getInverse());

                state = state.subtract(K.operate(c));
                RealMatrix I = MatrixUtils.createRealIdentityMatrix(stateSize);
                stateCov = I.subtract(K.multiply(H)).multiply(stateCov);
            }

            // Apply 4-momentum constraint (px, py, pz, E)
            // All particles assumed to have electron mass
            if (fourMomentumConstraint != null && fourMomentumConstraintCov != null) {
                // Update tracks from state
                vertex = state.getSubVector(0, 3);
                for (int i = 0; i < nTracks; i++) {
                    int offset = 3 + i * nTrackParams;
                    tracks.get(i).d0 = state.getEntry(offset + 0);
                    tracks.get(i).phi0 = state.getEntry(offset + 1);
                    tracks.get(i).omega = state.getEntry(offset + 2);
                    tracks.get(i).z0 = state.getEntry(offset + 3);
                    tracks.get(i).tanLambda = state.getEntry(offset + 4);
                }

                double me = 0.000511;  // electron mass in GeV

                // Compute total 4-momentum and Jacobian (4 constraints x stateSize)
                RealVector totalP4 = MatrixUtils.createRealVector(new double[4]);  // px, py, pz, E
                RealMatrix H = MatrixUtils.createRealMatrix(4, stateSize);

                for (int itrk = 0; itrk < nTracks; itrk++) {
                    TrackParams track = tracks.get(itrk);
                    int offset = 3 + itrk * nTrackParams;

                    RealVector p = computeMomentumAtVertex(track, vertex);
                    double pMag = p.getNorm();
                    double E = FastMath.sqrt(pMag * pMag + me * me);

                    totalP4.addToEntry(0, p.getEntry(0));  // px
                    totalP4.addToEntry(1, p.getEntry(1));  // py
                    totalP4.addToEntry(2, p.getEntry(2));  // pz
                    totalP4.addToEntry(3, E);              // E

                    // dp/dtrack derivatives (NOT dp/dvertex - vertex affects momentum through phi at vertex)
                    // The 4-momentum constraint should primarily adjust track momenta, not vertex position
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

                    // Compute derivatives w.r.t. phi0, omega, and tanLambda
                    // d0 and z0 derivatives are negligible (O(ω²) smaller) and omitted
                    // phi0 affects momentum direction and IS significant near perigee
                    double dphiDphi0 = (sign * R - track.d0) * (dy * FastMath.cos(track.phi0) - dx * FastMath.sin(track.phi0)) / r2;
                    double dphiDomega = -R * R * (FastMath.cos(track.phi0) * dx + FastMath.sin(track.phi0) * dy) / r2;
                    double dpTDomega = -2.99792458e-4 * bField * sign / (track.omega * track.omega);

                    double dpxDphi0 = -pT * FastMath.sin(vp.phiV) * dphiDphi0;
                    double dpxDomega = FastMath.cos(vp.phiV) * dpTDomega - pT * FastMath.sin(vp.phiV) * dphiDomega;

                    double dpyDphi0 = pT * FastMath.cos(vp.phiV) * dphiDphi0;
                    double dpyDomega = FastMath.sin(vp.phiV) * dpTDomega + pT * FastMath.cos(vp.phiV) * dphiDomega;

                    double dpzDomega = track.tanLambda * dpTDomega;
                    double dpzDtl = pT;

                    // Momentum derivatives w.r.t. track parameters
                    // Include phi0 (affects momentum direction), omega (affects pT), tanLambda (affects pz/pT)
                    // Exclude d0, z0 (negligible effect on momentum)
                    H.addToEntry(0, offset + 1, dpxDphi0);
                    H.addToEntry(0, offset + 2, dpxDomega);
                    H.addToEntry(1, offset + 1, dpyDphi0);
                    H.addToEntry(1, offset + 2, dpyDomega);
                    H.addToEntry(2, offset + 2, dpzDomega);
                    H.addToEntry(2, offset + 4, dpzDtl);

                    // Energy derivatives: phi0, omega, tanLambda
                    double dEDphi0 = (p.getEntry(0) * dpxDphi0 + p.getEntry(1) * dpyDphi0) / E;
                    double dEDomega = (p.getEntry(0) * dpxDomega + p.getEntry(1) * dpyDomega + p.getEntry(2) * dpzDomega) / E;
                    double dEDtl = p.getEntry(2) * dpzDtl / E;

                    H.addToEntry(3, offset + 1, dEDphi0);
                    H.addToEntry(3, offset + 2, dEDomega);
                    H.addToEntry(3, offset + 4, dEDtl);

                    if (debugFlag && iteration == 0) {
                        System.out.printf("  Track %d Jacobian: dpx/dphi0=%.4f dpy/dphi0=%.4f dE/dphi0=%.4f%n",
                                          itrk, dpxDphi0, dpyDphi0, dEDphi0);
                        System.out.printf("             dpx/domega=%.4f dpy/domega=%.4f dpz/domega=%.4f dE/domega=%.4f%n",
                                          dpxDomega, dpyDomega, dpzDomega, dEDomega);
                        System.out.printf("             dpT/domega=%.4f pT=%.4f omega=%.6f phi0=%.4f%n", dpTDomega, pT, track.omega, track.phi0);
                    }
                }

                RealVector c = totalP4.subtract(fourMomentumConstraint);

                // Use beam 4-momentum covariance only (don't add track covariances dynamically,
                // as this can create instability when track parameters are modified)
                // The track measurement uncertainties are already captured in stateCov through H*stateCov*H^T
                RealMatrix V = fourMomentumConstraintCov;

                //                if (debugFlag && iteration == 0) {
                if (debugFlag) {
                    System.out.printf("  Iteration %d: totalP4=[%.4f, %.4f, %.4f, %.4f] constraint=[%.4f, %.4f, %.4f, %.4f]%n",
                                      iteration, totalP4.getEntry(0), totalP4.getEntry(1), totalP4.getEntry(2), totalP4.getEntry(3),
                                      fourMomentumConstraint.getEntry(0), fourMomentumConstraint.getEntry(1),
                                      fourMomentumConstraint.getEntry(2), fourMomentumConstraint.getEntry(3));
                    System.out.printf("  Residual c=[%.4f, %.4f, %.4f, %.4f]%n",
                                      c.getEntry(0), c.getEntry(1), c.getEntry(2), c.getEntry(3));
                    System.out.printf("  V diagonal: [%.6f, %.6f, %.6f, %.6f]%n",
                                      V.getEntry(0,0), V.getEntry(1,1), V.getEntry(2,2), V.getEntry(3,3));
                }

                // Kalman update
                try {
                    RealMatrix S = H.multiply(stateCov).multiply(H.transpose()).add(V);
                    RealMatrix K = stateCov.multiply(H.transpose()).multiply(
                        new LUDecomposition(S).getSolver().getInverse());

                    RealVector correction = K.operate(c);

                    //                    if (debugFlag && iteration == 0) {
                    if (debugFlag) {
                        System.out.printf("  State correction: vertex=[%.4f, %.4f, %.4f]%n",
                                          correction.getEntry(0), correction.getEntry(1), correction.getEntry(2));
                        for (int i = 0; i < nTracks; i++) {
                            int offset = 3 + i * nTrackParams;
                            System.out.printf("  Track %d correction: d0=%.6f phi0=%.6f omega=%.8f z0=%.6f tanL=%.6f%n",
                                              i, correction.getEntry(offset), correction.getEntry(offset+1),
                                              correction.getEntry(offset+2), correction.getEntry(offset+3),
                                              correction.getEntry(offset+4));
                        }
                    }

                    state = state.subtract(correction);
                    RealMatrix I = MatrixUtils.createRealIdentityMatrix(stateSize);
                    stateCov = I.subtract(K.multiply(H)).multiply(stateCov);
                } catch (Exception e) {
                    if (debugFlag) {
                        System.out.println("  4-momentum constraint failed: " + e.getMessage());
                    }
                }
            }

            // Check convergence
            if (state.subtract(stateOld).getNorm() < tolerance) {
                break;
            }
        }

        // Extract final vertex
        RealVector vertex = state.getSubVector(0, 3);
        RealMatrix vertexCov = stateCov.getSubMatrix(0, 2, 0, 2);

        // Extract final track parameters
        List<TrackParams> fittedTracks = new ArrayList<>();
        for (int i = 0; i < nTracks; i++) {
            int offset = 3 + i * nTrackParams;
            double d0 = state.getEntry(offset + 0);
            double phi0 = state.getEntry(offset + 1);
            double omega = state.getEntry(offset + 2);
            double z0 = state.getEntry(offset + 3);
            double tanLambda = state.getEntry(offset + 4);
            RealMatrix tCov = stateCov.getSubMatrix(offset, offset + 4, offset, offset + 4);
            fittedTracks.add(new TrackParams(d0, phi0, omega, z0, tanLambda, tCov));
        }

        // Compute chi2
        RealVector delta = state.subtract(state0);
        double chi2 = 0.0;

        // Track contribution to chi2 (use original covariances)
        for (int i = 0; i < nTracks; i++) {
            int offset = 3 + i * nTrackParams;
            RealVector dTrack = delta.getSubVector(offset, nTrackParams);
            RealMatrix tCov0 = stateCov0.getSubMatrix(offset, offset + 4, offset, offset + 4);
            try {
                RealMatrix tCovInv = new LUDecomposition(tCov0).getSolver().getInverse();
                chi2 += dTrack.dotProduct(tCovInv.operate(dTrack));
            } catch (Exception e) {
                // Skip if singular
            }
        }

        // 4-momentum constraint contribution
        double me = 0.000511;  // electron mass
        if (fourMomentumConstraint != null) {
            RealVector totalP4 = MatrixUtils.createRealVector(new double[4]);
            for (TrackParams track : fittedTracks) {
                RealVector p = computeMomentumAtVertex(track, vertex);
                double pMag = p.getNorm();
                double E = FastMath.sqrt(pMag * pMag + me * me);
                totalP4.addToEntry(0, p.getEntry(0));
                totalP4.addToEntry(1, p.getEntry(1));
                totalP4.addToEntry(2, p.getEntry(2));
                totalP4.addToEntry(3, E);
            }
            RealVector p4Residual = totalP4.subtract(fourMomentumConstraint);
            try {
                RealMatrix p4CovInv = new LUDecomposition(fourMomentumConstraintCov).getSolver().getInverse();
                chi2 += p4Residual.dotProduct(p4CovInv.operate(p4Residual));
            } catch (Exception e) {
                // Skip if singular
            }
        }

        // NDF: 2 measurements per track (phi, z at vertex) + 4 momentum constraints - 3 vertex params
        int ndf = 2 * nTracks - 3;
        if (fourMomentumConstraint != null) ndf += 4;

        // Compute track momenta from fitted parameters
        List<TrackMomentum> trackMomenta = new ArrayList<>();
        for (TrackParams track : fittedTracks) {
            RealVector p = computeMomentumAtVertex(track, vertex);
            RealMatrix pCov = computeMomentumCovariance(track, vertex);
            trackMomenta.add(new TrackMomentum(p, pCov));
        }

        if (debugFlag) {
            System.out.println("=== KalmanVertexFitterGainMatrix::fitKinematic ===");
            System.out.println("  Number of tracks: " + nTracks);
            System.out.printf("  Fitted vertex: [%.4f, %.4f, %.4f]%n",
                              vertex.getEntry(0), vertex.getEntry(1), vertex.getEntry(2));
            System.out.printf("  Chi2: %.4f  NDF: %d%n", chi2, ndf);

            RealVector totalInP = MatrixUtils.createRealVector(new double[3]);
            RealVector totalP = MatrixUtils.createRealVector(new double[3]);
            double totalInE = 0.0;
            double totalE = 0.0;

            for (int i = 0; i < nTracks; i++) {
                TrackParams orig = inputTracks.get(i);
                TrackParams fitted = fittedTracks.get(i);
                RealVector pOrig = computeMomentumAtVertex(orig, vertex);
                RealVector pFit = computeMomentumAtVertex(fitted, vertex);
                double pOrigMag = pOrig.getNorm();
                double pFitMag = pFit.getNorm();
                double eOrig = FastMath.sqrt(pOrigMag * pOrigMag + me * me);
                double eFit = FastMath.sqrt(pFitMag * pFitMag + me * me);
                totalP = totalP.add(pFit);
                totalInP = totalInP.add(pOrig);
                totalE += eFit;
                totalInE += eOrig;
                System.out.printf("  Track %d: omega %.6f -> %.6f, tanL %.4f -> %.4f%n",
                                  i, orig.omega, fitted.omega, orig.tanLambda, fitted.tanLambda);
                System.out.printf("           p: [%.4f, %.4f, %.4f] -> [%.4f, %.4f, %.4f]%n",
                                  pOrig.getEntry(0), pOrig.getEntry(1), pOrig.getEntry(2),
                                  pFit.getEntry(0), pFit.getEntry(1), pFit.getEntry(2));
                System.out.printf("           E: %.4f -> %.4f%n", eOrig, eFit);
            }
            System.out.printf("  Total input 4-momentum:  [%.4f, %.4f, %.4f, %.4f]%n",
                              totalInP.getEntry(0), totalInP.getEntry(1), totalInP.getEntry(2), totalInE);
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
     * Kinematic fit using Lagrange multiplier approach.
     * All constraints (track + 4-momentum) are satisfied simultaneously.
     *
     * This solves: minimize (x - x0)^T W (x - x0) subject to h(x) = 0
     * where x = [vertex, track1_params, track2_params, ...] is the full state vector,
     * x0 is the initial/measured values, W is the weight matrix (inverse covariance),
     * and h(x) = 0 are the constraint equations.
     *
     * Constraints:
     * - Track constraints: each track must pass through the vertex (z matching)
     * - 4-momentum constraint: sum of track momenta = beam momentum
     */
    public FitResult fitLagrangeMultiplier(List<TrackParams> inputTracks,
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

        // Number of constraints: 1 z-constraint per track + 3 momentum constraints (px, py, pz only)
        // Note: We only constrain 3-momentum, not energy, because for non-collinear tracks
        // sum(E_i) > E_beam due to triangle inequality, making energy constraint unphysical
        //
        // Track Z constraints are now SOFT constraints with covariance propagated from track errors.
        // This is mathematically correct: z_predicted = z0 + s*tanLambda has uncertainty from
        // the track parameter covariance, so the constraint should not be exact.
        int nTrackConstraints = nTracks;
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

        if (debugFlag) {
            System.out.println("=== fitLagrangeMultiplier ===");
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

            // Track constraints: zV - z_predicted = 0 for each track
            for (int i = 0; i < nTracks; i++) {
                TrackParams track = tracks.get(i);
                int offset = nVertexParams + i * nTrackParams;

                double xV = vertex.getEntry(0);
                double yV = vertex.getEntry(1);
                double zV = vertex.getEntry(2);

                VertexParams vp = perigeeToVertexParams(track, xV, yV);

                // Constraint residual: zV - z_predicted
                h.setEntry(i, zV - vp.zV);

                // Jacobian computation
                double R = 1.0 / FastMath.abs(track.omega);
                double sign = FastMath.signum(track.omega);

                double xc = sign * R * FastMath.sin(track.phi0) - track.d0 * FastMath.sin(track.phi0);
                double yc = -sign * R * FastMath.cos(track.phi0) + track.d0 * FastMath.cos(track.phi0);
                double dx = xV - xc;
                double dy = yV - yc;
                double r2 = dx * dx + dy * dy;

                // dh/d(vertex) where h = zV - z_predicted
                double dphiDx = -dy / r2;
                double dphiDy = dx / r2;
                double dzPredDx = R * track.tanLambda * dphiDx;
                double dzPredDy = R * track.tanLambda * dphiDy;

                H.setEntry(i, 0, -dzPredDx);      // dh/dxV = -d(z_pred)/dxV
                H.setEntry(i, 1, -dzPredDy);      // dh/dyV
                H.setEntry(i, 2, 1.0);            // dh/dzV = 1

                // dh/d(track params)
                double dphiDd0 = -(FastMath.cos(track.phi0) * dx + FastMath.sin(track.phi0) * dy) / r2;
                double dphiDphi0 = (sign * R - track.d0) * (dy * FastMath.cos(track.phi0) - dx * FastMath.sin(track.phi0)) / r2;
                double dphiDomega = -R * R * (FastMath.cos(track.phi0) * dx + FastMath.sin(track.phi0) * dy) / r2;

                double phiV = FastMath.atan2(-dx * sign, dy * sign);
                double dphi = phiV - track.phi0;
                double s = R * dphi;

                double dzPredDd0 = track.tanLambda * R * dphiDd0;
                double dzPredDphi0 = -track.tanLambda * R + track.tanLambda * R * dphiDphi0;
                double dzPredDomega = -s * track.tanLambda / track.omega + track.tanLambda * R * dphiDomega;
                double dzPredDz0 = 1.0;
                double dzPredDtl = s;

                H.setEntry(i, offset + 0, -dzPredDd0);
                H.setEntry(i, offset + 1, -dzPredDphi0);
                H.setEntry(i, offset + 2, -dzPredDomega);
                H.setEntry(i, offset + 3, -dzPredDz0);
                H.setEntry(i, offset + 4, -dzPredDtl);
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

            // Build constraint covariance matrix V for soft constraints
            // For track Z constraints: V_i = J_i * trackCov * J_i^T where J_i = dz_pred/d(track_params)
            // For momentum constraints: use the provided fourMomentumConstraintCov
            RealMatrix constraintCov = MatrixUtils.createRealMatrix(nConstraints, nConstraints);

            // Track Z constraint covariances
            for (int i = 0; i < nTracks; i++) {
                TrackParams track = tracks.get(i);
                int offset = nVertexParams + i * nTrackParams;

                double xV = vertex.getEntry(0);
                double yV = vertex.getEntry(1);

                double R = 1.0 / FastMath.abs(track.omega);
                double sign = FastMath.signum(track.omega);

                double xc = sign * R * FastMath.sin(track.phi0) - track.d0 * FastMath.sin(track.phi0);
                double yc = -sign * R * FastMath.cos(track.phi0) + track.d0 * FastMath.cos(track.phi0);
                double dx = xV - xc;
                double dy = yV - yc;
                double r2 = dx * dx + dy * dy;

                double dphiDd0 = -(FastMath.cos(track.phi0) * dx + FastMath.sin(track.phi0) * dy) / r2;
                double dphiDphi0 = (sign * R - track.d0) * (dy * FastMath.cos(track.phi0) - dx * FastMath.sin(track.phi0)) / r2;
                double dphiDomega = -R * R * (FastMath.cos(track.phi0) * dx + FastMath.sin(track.phi0) * dy) / r2;

                double phiV = FastMath.atan2(-dx * sign, dy * sign);
                double dphi = phiV - track.phi0;
                double s = R * dphi;

                // Jacobian of z_predicted w.r.t. track parameters [d0, phi0, omega, z0, tanLambda]
                double[] Jz = new double[5];
                Jz[0] = track.tanLambda * R * dphiDd0;
                Jz[1] = -track.tanLambda * R + track.tanLambda * R * dphiDphi0;
                Jz[2] = -s * track.tanLambda / track.omega + track.tanLambda * R * dphiDomega;
                Jz[3] = 1.0;  // dz_pred/dz0
                Jz[4] = s;    // dz_pred/dtanLambda

                // Compute variance: V_i = J * Cov * J^T (scalar since constraint is 1D)
                double variance = 0.0;
                for (int a = 0; a < 5; a++) {
                    for (int b = 0; b < 5; b++) {
                        variance += Jz[a] * track.cov.getEntry(a, b) * Jz[b];
                    }
                }

                // Set constraint covariance (diagonal element for this track's Z constraint)
                constraintCov.setEntry(i, i, variance);

                if (debugFlag && iteration == 0) {
                    System.out.printf("    Track %d z_pred sigma = %.4f mm%n", i, FastMath.sqrt(variance));
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

        // Extract fitted track parameters
        List<TrackParams> fittedTracks = new ArrayList<>();
        for (int i = 0; i < nTracks; i++) {
            int offset = nVertexParams + i * nTrackParams;
            double d0 = x.getEntry(offset + 0);
            double phi0 = x.getEntry(offset + 1);
            double omega = x.getEntry(offset + 2);
            double z0 = x.getEntry(offset + 3);
            double tanLambda = x.getEntry(offset + 4);
            // Use original covariance for now (proper covariance requires more work)
            fittedTracks.add(new TrackParams(d0, phi0, omega, z0, tanLambda, inputTracks.get(i).cov));
        }

        // Compute chi2 = (x - x0)^T W (x - x0)
        RealVector dx = x.subtract(x0);
        double chi2 = dx.dotProduct(W.operate(dx));

        // NDF = number of constraints
        int ndf = nConstraints;

        // Compute final track momenta
        List<TrackMomentum> trackMomenta = new ArrayList<>();
        double me = 0.000511;
        for (TrackParams track : fittedTracks) {
            RealVector p = computeMomentumAtVertex(track, vertex);
            RealMatrix pCov = computeMomentumCovariance(track, vertex);
            trackMomenta.add(new TrackMomentum(p, pCov));
        }

        // Compute vertex covariance (simplified: use projected covariance)
        RealMatrix vertexCov = WInv.getSubMatrix(0, 2, 0, 2);

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
    private double pBeam = 3.7;
    private double rotAngle = -0.030;
    private boolean debugFlag = false;
    private boolean storeCovTrkMomList = false;
    private boolean useLagrangeMultiplier = true;  // Use Lagrange multiplier method by default

    public void setBeamSize(double[] bs) { this.beamSize = bs; }
    public void setBeamPosition(double[] bp) { this.beamPosition = bp; }
    public void setBeamEnergy(double energy) { this.pBeam = energy; }
    public void setBeamRotAngle(double angle) { this.rotAngle = angle; }
    public void setDebug(boolean debug) { this.debugFlag = debug; }
    public void setStoreCovTrkMomList(boolean value) { this.storeCovTrkMomList = value; }
    public void setUseLagrangeMultiplier(boolean value) { this.useLagrangeMultiplier = value; }

    /**
     * Fit vertex and return a BilliorVertex for compatibility with existing code.
     *
     * @param tracks List of track parameters
     * @param beamConstraint If true, apply beam momentum constraint
     * @return BilliorVertex with fitted results
     */
    public BilliorVertex fitVertex(List<TrackParams> tracks, boolean beamConstraint) {
        if(debugFlag)
            System.out.println("     *********   starting new fitVertex    *********     ");

        // Print input track 4-momenta
        if (debugFlag) {
            double me = 0.000511;
            // Use beamPosition as initial vertex estimate for momentum calculation
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

        // Set up vertex constraint from beam position/size
        RealVector vertexConstraintVec = MatrixUtils.createRealVector(beamPosition);
        RealMatrix vertexConstraintCovMat = MatrixUtils.createRealMatrix(3, 3);
        vertexConstraintCovMat.setEntry(0, 0, beamSize[0] * beamSize[0]);
        vertexConstraintCovMat.setEntry(1, 1, beamSize[1] * beamSize[1]);
        vertexConstraintCovMat.setEntry(2, 2, beamSize[2] * beamSize[2]);

        // Set up 4-momentum constraint from beam energy and rotation angle
        // Constraint is (px, py, pz, E) where E = sqrt(p^2 + m_e^2) for electron beam
        RealVector fourMomentumConstraintVec = null;
        RealMatrix fourMomentumConstraintCovMat = null;
        if (beamConstraint) {
            // Beam momentum vector in tracking frame
            // Detector frame: beam along HPS Z, rotated by rotAngle in HPS X-Z plane
            //   det: (pBeam*sin(rotAngle), 0, pBeam*cos(rotAngle))
            // Tracking frame: X=HPS_Z, Y=HPS_X, Z=HPS_Y
            double pxBeam = pBeam * FastMath.cos(rotAngle);  // tracking X = HPS Z
            //            double pyBeam = pBeam * FastMath.sin(rotAngle);  // tracking Y = HPS X
            double pyBeam = -pBeam * FastMath.sin(rotAngle);  // tracking Y = HPS X
            double pzBeam = 0.0;                              // tracking Z = HPS Y
            double me = 0.000511;  // electron mass in GeV
            double eBeam = FastMath.sqrt(pBeam * pBeam + me * me);
            fourMomentumConstraintVec = MatrixUtils.createRealVector(new double[]{pxBeam, pyBeam, pzBeam, eBeam});
            // Tight covariance on beam 4-momentum (small uncertainty)
            fourMomentumConstraintCovMat = MatrixUtils.createRealMatrix(4, 4);
            double sigmaP = 0.01 * pBeam; // 0.1% momentum spread
            double sigmaE = 0.01 * eBeam; // 0.1% energy spread
            fourMomentumConstraintCovMat.setEntry(0, 0, sigmaP * sigmaP);
            fourMomentumConstraintCovMat.setEntry(1, 1, sigmaP * sigmaP);
            fourMomentumConstraintCovMat.setEntry(2, 2, sigmaP * sigmaP);
            fourMomentumConstraintCovMat.setEntry(3, 3, sigmaE * sigmaE);
        }

        // Use kinematic fit when beam constraint is requested (properly updates track momenta)
        // Use regular fit otherwise
        FitResult result;
        if (beamConstraint) {
            if (useLagrangeMultiplier) {
                // Lagrange multiplier method: all constraints satisfied simultaneously
                result = fitLagrangeMultiplier(tracks, vertexConstraintVec, vertexConstraintCovMat,
                                               fourMomentumConstraintVec, fourMomentumConstraintCovMat,
                                               10, 1e-6);
            } else {
                // Sequential Kalman filter method
                result = fitKinematic(tracks, vertexConstraintVec, vertexConstraintCovMat,
                                      fourMomentumConstraintVec, fourMomentumConstraintCovMat,
                                      10, 1e-6);
            }
        } else {
            result = fit(tracks, null, vertexConstraintVec, vertexConstraintCovMat,
                         null, null, null, null, 10, 1e-6);
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

        BilliorVertex bv = new BilliorVertex(vtxPos, covVtx, result.chi2, invMass, pFitMap,
                                              beamConstraint ? "ThreeProngBeamConstrained" : "ThreeProngUnconstrained");
        bv.setPositionError(vtxPosErr);

        // Store momentum covariances if requested
        if (storeCovTrkMomList) {
            java.util.List<hep.physics.matrix.Matrix> covTrkMomList = new java.util.ArrayList<>();
            for (int i = 0; i < result.trackMomenta.size(); i++) {
                RealMatrix pCov = result.trackMomenta.get(i).pCov;
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

        if (debugFlag) {
            System.out.println("=== KalmanVertexFitterGainMatrix::fitVertex ===");
            System.out.println("  B field: " + bField);
            System.out.println("  Beam constraint: " + beamConstraint);
            System.out.println("  Number of tracks: " + tracks.size());
            for (int i = 0; i < tracks.size(); i++) {
                TrackParams t = tracks.get(i);
                System.out.printf("  Input Track %d: d0=%.4f phi0=%.4f omega=%.6f z0=%.4f tanLambda=%.4f%n",
                                  i, t.d0, t.phi0, t.omega, t.z0, t.tanLambda);
            }
            if (beamConstraint && fourMomentumConstraintVec != null) {
                System.out.printf("  Beam 4-momentum constraint: [%.4f, %.4f, %.4f, %.4f]%n",
                                  fourMomentumConstraintVec.getEntry(0),
                                  fourMomentumConstraintVec.getEntry(1),
                                  fourMomentumConstraintVec.getEntry(2),
                                  fourMomentumConstraintVec.getEntry(3));
            }
            System.out.printf("  Vertex constraint: [%.4f, %.4f, %.4f]%n",
                              vertexConstraintVec.getEntry(0),
                              vertexConstraintVec.getEntry(1),
                              vertexConstraintVec.getEntry(2));
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
        
        // Separate tracks into fitting tracks and predicted track
        List<TrackParams> fitTracks = new ArrayList<>();
        for (int i = 0; i < nTracks; i++) {
            if (i != predictTrackIdx) {
                fitTracks.add(tracks.get(i));
            }
        }
        TrackParams predictedTrack = tracks.get(predictTrackIdx);

        // Fit with N-1 tracks - NOTE: Do NOT use momentum constraint here
        // The N-1 tracks don't satisfy the total momentum constraint;
        // we use momentum conservation to PREDICT the missing track's momentum
        FitResult fitResult = fit(
            fitTracks, null, vertexConstraint, vertexConstraintCov,
            null, null,  // No momentum constraint for N-1 track fit
            null, null,  // No mass constraint either
            maxIterations, tolerance
        );
        
        // Predict momentum of excluded track
        RealVector predictedP;
        RealMatrix predictedPCov;
        
        if (totalMomentumConstraint != null) {
            // Use momentum conservation: p_predicted = p_total - sum(p_fitted)
            RealVector fittedTotalP = MatrixUtils.createRealVector(new double[3]);
            for (int i = 0; i < fitResult.trackMomenta.size(); i++) {
                TrackMomentum mom = fitResult.trackMomenta.get(i);
                fittedTotalP = fittedTotalP.add(mom.p);
                System.out.printf("DEBUG fitWithPredictedTrack: fitted track %d p (tracking) = [%.4f, %.4f, %.4f] |p|=%.4f%n",
                    i, mom.p.getEntry(0), mom.p.getEntry(1), mom.p.getEntry(2), mom.p.getNorm());
            }
            System.out.printf("DEBUG fitWithPredictedTrack: total fitted p (tracking) = [%.4f, %.4f, %.4f] |p|=%.4f%n",
                fittedTotalP.getEntry(0), fittedTotalP.getEntry(1), fittedTotalP.getEntry(2), fittedTotalP.getNorm());
            System.out.printf("DEBUG fitWithPredictedTrack: momentum constraint (tracking) = [%.4f, %.4f, %.4f] |p|=%.4f%n",
                totalMomentumConstraint.getEntry(0), totalMomentumConstraint.getEntry(1), totalMomentumConstraint.getEntry(2),
                totalMomentumConstraint.getNorm());
            predictedP = totalMomentumConstraint.subtract(fittedTotalP);
            System.out.printf("DEBUG fitWithPredictedTrack: predicted p (tracking) = [%.4f, %.4f, %.4f] |p|=%.4f%n",
                predictedP.getEntry(0), predictedP.getEntry(1), predictedP.getEntry(2), predictedP.getNorm());

            // Propagate uncertainty: Cov(p_pred) = Cov(p_total) + Cov(sum p_fitted)
            // Use the fitted track momentum covariances from the fit result
            // Note: This assumes uncorrelated track momenta (ignores vertex correlations)
            RealMatrix fittedPCov = MatrixUtils.createRealMatrix(3, 3);
            for (TrackMomentum mom : fitResult.trackMomenta) {
                fittedPCov = fittedPCov.add(mom.pCov);
            }

            if (totalMomentumConstraintCov != null) {
                predictedPCov = totalMomentumConstraintCov.add(fittedPCov);
            } else {
                predictedPCov = fittedPCov;
            }

        } else {
            // Without momentum constraint, predict from track parameters at fitted vertex
            predictedP = computeMomentumAtVertex(predictedTrack, fitResult.vertex);
            predictedPCov = computeMomentumCovariance(predictedTrack, fitResult.vertex);
        }
        
        // Create momentum info for predicted track
        TrackMomentum predictedMom = new TrackMomentum(predictedP, predictedPCov);
        
        // Combine all track momenta in original order
        List<TrackMomentum> allTrackMomenta = new ArrayList<>();
        int fitIdx = 0;
        for (int i = 0; i < nTracks; i++) {
            if (i == predictTrackIdx) {
                allTrackMomenta.add(predictedMom);
            } else {
                allTrackMomenta.add(fitResult.trackMomenta.get(fitIdx));
                fitIdx++;
            }
        }
        
        // Calculate total chi2:
        // Start with chi2 from fitting N-1 tracks
        double chi2Total = fitResult.chi2;
        System.out.printf("DEBUG fitWithPredictedTrack: N-1 fit chi2=%.2f ndf=%d%n", fitResult.chi2, fitResult.ndf);

        // Add chi2 contribution from comparing predicted momentum to actual track
        // The actual track's momentum at the fitted vertex
        RealVector actualP = computeMomentumAtVertex(predictedTrack, fitResult.vertex);
        RealMatrix actualPCov = computeMomentumCovariance(predictedTrack, fitResult.vertex);

        System.out.printf("DEBUG fitWithPredictedTrack: actual p (tracking) = [%.4f, %.4f, %.4f] |p|=%.4f%n",
            actualP.getEntry(0), actualP.getEntry(1), actualP.getEntry(2), actualP.getNorm());

        // Residual: predicted - actual
        RealVector pResidual = predictedP.subtract(actualP);
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
            System.out.printf("DEBUG fitWithPredictedTrack: chi2 from momentum comparison = %.2f%n", chi2Momentum);
        } catch (SingularMatrixException e) {
            // If covariance is singular, skip this contribution
            System.out.println("Warning: Combined momentum covariance is singular, skipping chi2 contribution");
        }
        System.out.printf("DEBUG fitWithPredictedTrack: total chi2 = %.2f%n", chi2Total);
        
        // ndf: from N-1 track fit plus 3 for the 3-component momentum comparison
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
        System.out.printf("DEBUG toBilliorVertex: predicted p (tracking) = [%.4f, %.4f, %.4f]%n",
            result.predictedMomentum.getEntry(0), result.predictedMomentum.getEntry(1), result.predictedMomentum.getEntry(2));
        System.out.printf("DEBUG toBilliorVertex: predicted p (detector) = [%.4f, %.4f, %.4f] |p|=%.4f%n",
            predPx, predPy, predPz, predPtot);
        bv.setParameter("predictedPx", predPx);
        bv.setParameter("predictedPy", predPy);
        bv.setParameter("predictedPz", predPz);

        // Store actual momentum (converted to detector frame)
        double actPx = result.actualMomentum.getEntry(1);
        double actPy = result.actualMomentum.getEntry(2);
        double actPz = result.actualMomentum.getEntry(0);
        double actPtot = Math.sqrt(actPx*actPx + actPy*actPy + actPz*actPz);
        System.out.printf("DEBUG toBilliorVertex: actual p (tracking) = [%.4f, %.4f, %.4f]%n",
            result.actualMomentum.getEntry(0), result.actualMomentum.getEntry(1), result.actualMomentum.getEntry(2));
        System.out.printf("DEBUG toBilliorVertex: actual p (detector) = [%.4f, %.4f, %.4f] |p|=%.4f%n",
            actPx, actPy, actPz, actPtot);
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
