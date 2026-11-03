package org.hps.recon.vertexing;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.linear.LUDecomposition;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.util.FastMath;

import org.hps.recon.vertexing.TrackConstraintVertexFitter.Constraint;
import org.hps.recon.vertexing.TrackConstraintVertexFitter.FitResult;
import org.hps.recon.vertexing.TrackConstraintVertexFitter.TrackMomentum;
import org.hps.recon.vertexing.TrackConstraintVertexFitter.TrackParams;

/**
 * The original Kalman gain-matrix vertex fit (iterative sequential updates, Joseph-form
 * covariance), superseded in all production/analysis code by the Billoir-batch linear-algebra
 * path ({@code TrackConstraintVertexFitter.fitBillior1985}/{@code fitVertex}). Kept here for
 * reference/regression testing only -- it lives under {@code src/test}, so it cannot be
 * referenced from any {@code src/main} code, structurally enforcing that nothing in production
 * or analysis code depends on it.
 *
 * <p>Holds a {@link TrackConstraintVertexFitter} instance and calls its package-private
 * per-track constraint helpers ({@code computeTrackConstraint}, {@code
 * computeMomentumVertexDerivatives}, {@code computeMomentumCovariance}, {@code
 * computeMomentumTrackJacobian}, {@code perigeeToVertexParams}), which are shared with that
 * class's own {@code fitCascadeVertexJoint*} family.
 */
public class GainMatrixVertexer {

    private final TrackConstraintVertexFitter fitter;
    private boolean debugFlag = false;

    public GainMatrixVertexer(double bField) {
        this.fitter = new TrackConstraintVertexFitter(bField);
    }

    public void setDebugFlag(boolean debugFlag) {
        this.debugFlag = debugFlag;
    }

    private Constraint computeVertexConstraint(RealVector vertex,
                                               RealVector vertexConstraint,
                                               RealMatrix vertexConstraintCov) {
        RealVector c = vertex.subtract(vertexConstraint);
        RealMatrix H = MatrixUtils.createRealIdentityMatrix(3);
        RealMatrix V = vertexConstraintCov;

        return new Constraint(c, H, V);
    }

    /**
     * Compute momentum constraint. The covariance V includes both beam momentum uncertainty
     * AND track momentum uncertainties.
     */
    private Constraint computeMomentumConstraint(List<TrackParams> tracks,
                                                 RealVector vertex,
                                                 RealVector momentumConstraint,
                                                 RealMatrix momentumConstraintCov) {
        RealVector totalP = MatrixUtils.createRealVector(new double[3]);
        RealMatrix dpDvertex = MatrixUtils.createRealMatrix(3, 3);
        RealMatrix totalPCov = MatrixUtils.createRealMatrix(3, 3);

        for (TrackParams track : tracks) {
            RealVector p = fitter.computeMomentumAtVertex(track, vertex);
            totalP = totalP.add(p);

            RealMatrix dpDv = fitter.computeMomentumVertexDerivatives(track, vertex);
            dpDvertex = dpDvertex.add(dpDv);

            RealMatrix pCov = fitter.computeMomentumCovariance(track, vertex);
            totalPCov = totalPCov.add(pCov);
        }

        RealVector c = totalP.subtract(momentumConstraint);
        RealMatrix H = dpDvertex;
        RealMatrix V = momentumConstraintCov.add(totalPCov);

        return new Constraint(c, H, V);
    }

    /**
     * Compute mass constraint. Invariant mass: M^2 = (Sum E)^2 - (Sum p)^2.
     */
    private Constraint computeMassConstraint(List<TrackParams> tracks,
                                            RealVector vertex,
                                            double massConstraint,
                                            double massConstraintSigma) {
        double mPi = 0.13957; // GeV/c^2

        double totalE = 0.0;
        RealVector totalP = MatrixUtils.createRealVector(new double[3]);
        RealVector dEDvertex = MatrixUtils.createRealVector(new double[3]);
        RealMatrix dpDvertex = MatrixUtils.createRealMatrix(3, 3);

        for (TrackParams track : tracks) {
            RealVector p = fitter.computeMomentumAtVertex(track, vertex);
            double pMag = p.getNorm();

            double E = FastMath.sqrt(pMag * pMag + mPi * mPi);
            totalE += E;
            totalP = totalP.add(p);

            RealMatrix dpDv = fitter.computeMomentumVertexDerivatives(track, vertex);

            for (int i = 0; i < 3; i++) {
                double dEDv = 0.0;
                for (int j = 0; j < 3; j++) {
                    dEDv += p.getEntry(j) * dpDv.getEntry(j, i);
                }
                dEDvertex.setEntry(i, dEDvertex.getEntry(i) + dEDv / E);
            }

            dpDvertex = dpDvertex.add(dpDv);
        }

        double totalPmag = totalP.getNorm();
        double M = FastMath.sqrt(totalE * totalE - totalPmag * totalPmag);

        RealVector c = MatrixUtils.createRealVector(new double[]{M - massConstraint});

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

        RealMatrix V = MatrixUtils.createRealMatrix(1, 1);
        V.setEntry(0, 0, massConstraintSigma * massConstraintSigma);

        return new Constraint(c, H, V);
    }

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

                RealMatrix S = constraint.H.multiply(C).multiply(constraint.H.transpose()).add(constraint.V);
                RealMatrix K = C.multiply(constraint.H.transpose()).multiply(
                    new LUDecomposition(S).getSolver().getInverse()
                );

                vertex = vertex.subtract(K.operate(constraint.c));

                RealMatrix ImKH = I.subtract(K.multiply(constraint.H));
                C = ImKH.multiply(C).multiply(ImKH.transpose())
                        .add(K.multiply(constraint.V).multiply(K.transpose()));
            }

            // Apply track constraints
            for (TrackParams track : tracks) {
                Constraint constraint = fitter.computeTrackConstraint(track, vertex);

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
            Constraint constraint = fitter.computeTrackConstraint(tracks.get(itrk), vertex);
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

        if (debugFlag) {
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
        List<TrackMomentum> trackMomenta = new ArrayList<TrackMomentum>();
        for (TrackParams track : tracks) {
            RealVector p = fitter.computeMomentumAtVertex(track, vertex);
            RealMatrix pCov = fitter.computeMomentumCovariance(track, vertex);
            trackMomenta.add(new TrackMomentum(p, pCov));
        }

        return new FitResult(vertex, C, chi2, ndf, trackMomenta);
    }

    public FitResult fit(List<TrackParams> tracks) {
        return fit(tracks, null, null, null, null, null, null, null, 10, 1e-6);
    }

    public FitResult fit(List<TrackParams> tracks, int maxIterations, double tolerance) {
        return fit(tracks, null, null, null, null, null, null, null, maxIterations, tolerance);
    }
}
