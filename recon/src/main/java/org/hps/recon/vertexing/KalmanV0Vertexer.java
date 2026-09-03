package org.hps.recon.vertexing;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.util.FastMath;

import hep.physics.matrix.SymmetricMatrix;
import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.VecOp;

import org.lcsim.event.Track;
import org.lcsim.event.TrackState;

import org.hps.recon.tracking.TrackStateUtils;
import org.hps.recon.vertexing.KalmanVertexFitterGainMatrix.FitResult;
import org.hps.recon.vertexing.KalmanVertexFitterGainMatrix.TrackParams;

/**
 * Fits a plain, unconstrained two-track (e-/e+) V0 vertex using the Kalman gain-matrix
 * fitter ({@link KalmanVertexFitterGainMatrix}), as an alternative to {@link
 * BilliorVertexer} for the same problem. Packages the result as a {@link BilliorVertex}
 * (used here purely as a generic {@code Vertex}-implementing data container, not as a
 * Billoir-algorithm result) so it plugs directly into the existing
 * {@code HpsReconParticleDriver#makeReconstructedParticle} and tuple-reading code.
 */
public class KalmanV0Vertexer {

    private static final double ELECTRON_MASS = 0.000511;

    private final double bField;

    public KalmanV0Vertexer(double bField) {
        this.bField = bField;
    }

    /**
     * Fit the vertex of an electron and a positron track, with no vertex/momentum/mass
     * constraint.
     *
     * @param eleTrack the electron track
     * @param posTrack the positron track
     * @return the fitted vertex, or null if the fit fails
     */
    public BilliorVertex fitVertex(Track eleTrack, Track posTrack) {
        TrackState eleTs = TrackStateUtils.getTrackStatesAtLocation(eleTrack, TrackState.AtPerigee).get(0);
        TrackParams eleParams = trackParamsFromTrack(eleTs);
        TrackParams posParams = trackParamsFromTrack(
                TrackStateUtils.getTrackStatesAtLocation(posTrack, TrackState.AtPerigee).get(0));

        KalmanVertexFitterGainMatrix fitter = new KalmanVertexFitterGainMatrix(bField);
        FitResult result = fitter.fit(Arrays.asList(eleParams, posParams));
        if (result == null) {
            return placeholderVertex();
        }

        return makeVertex(result, eleTs.getReferencePoint());
    }

    /**
     * A sentinel vertex (chi2/mass = -9999, zero position/momenta) used in place of a
     * null result when the fit fails, so that callers pairing this fit's output list
     * index-for-index against another collection (e.g. the Billoir unconstrained V0 fit,
     * for comparison) never see the two lists fall out of sync.
     */
    public static BilliorVertex placeholderVertex() {
        Map<Integer, Hep3Vector> pFitMap = new HashMap<Integer, Hep3Vector>();
        pFitMap.put(0, new BasicHep3Vector(0, 0, 0));
        pFitMap.put(1, new BasicHep3Vector(0, 0, 0));
        BilliorVertex vtxFit = new BilliorVertex(new BasicHep3Vector(0, 0, 0),
                new SymmetricMatrix(3), -9999.0, -9999.0, pFitMap, "KALMAN_UNCONSTRAINED_FAILED");
        vtxFit.setPositionError(new BasicHep3Vector(0, 0, 0));
        vtxFit.setParameter("ndf", -9999.0);
        return vtxFit;
    }

    /**
     * Extract perigee TrackParams (d0, phi0, omega, z0, tanLambda) directly from an
     * AtPerigee TrackState, with no reparametrization. {@link
     * KalmanVertexFitterGainMatrix}'s track-constraint math implicitly treats the vertex
     * unknown as living in the same frame as the raw d0/phi0/omega/z0, so the fitted
     * vertex it returns is relative to the track's reference point, not the absolute
     * tracking/detector frame; {@link #makeVertex} adds that reference point back in
     * afterward. Electron and positron tracks are assumed to share the same reference
     * point (true for AtPerigee states as produced by the tracking reconstruction).
     */
    private static TrackParams trackParamsFromTrack(TrackState ts) {
        double[] par = ts.getParameters();
        SymmetricMatrix sm = new SymmetricMatrix(5, ts.getCovMatrix(), true);
        RealMatrix cov = MatrixUtils.createRealMatrix(5, 5);
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 5; j++) {
                cov.setEntry(i, j, sm.e(i, j));
            }
        }
        return new TrackParams(par[0], par[1], par[2], par[3], par[4], cov);
    }

    /**
     * Convert a Kalman {@code FitResult} for the (electron, positron) track pair into a
     * {@code BilliorVertex}, using the same tracking-frame -> detector-frame convention
     * (det(x,y,z) = trk(y,z,x)) as {@code CascadeVertexer#makeReconstructedParticle}.
     *
     * @param referencePoint the (shared) reference point of the input tracks, tracking
     *                        frame; added back to {@code result.vertex} since the fitter's
     *                        raw output is relative to that point, not the absolute frame
     */
    private static BilliorVertex makeVertex(FitResult result, double[] referencePoint) {
        Hep3Vector vtxPos = new BasicHep3Vector(
                result.vertex.getEntry(1) + referencePoint[1],
                result.vertex.getEntry(2) + referencePoint[2],
                result.vertex.getEntry(0) + referencePoint[0]);

        double[] covPacked = new double[6];
        covPacked[0] = result.vertexCov.getEntry(1, 1);
        covPacked[1] = result.vertexCov.getEntry(2, 1);
        covPacked[2] = result.vertexCov.getEntry(2, 2);
        covPacked[3] = result.vertexCov.getEntry(0, 1);
        covPacked[4] = result.vertexCov.getEntry(0, 2);
        covPacked[5] = result.vertexCov.getEntry(0, 0);
        SymmetricMatrix covVtx = new SymmetricMatrix(3, covPacked, true);

        Hep3Vector vtxPosErr = new BasicHep3Vector(
                FastMath.sqrt(FastMath.abs(result.vertexCov.getEntry(1, 1))),
                FastMath.sqrt(FastMath.abs(result.vertexCov.getEntry(2, 2))),
                FastMath.sqrt(FastMath.abs(result.vertexCov.getEntry(0, 0))));

        RealVector pEleTrk = result.trackMomenta.get(0).p;
        Hep3Vector pEle = new BasicHep3Vector(pEleTrk.getEntry(1), pEleTrk.getEntry(2), pEleTrk.getEntry(0));
        RealVector pPosTrk = result.trackMomenta.get(1).p;
        Hep3Vector pPos = new BasicHep3Vector(pPosTrk.getEntry(1), pPosTrk.getEntry(2), pPosTrk.getEntry(0));

        double eEle = FastMath.sqrt(pEle.magnitudeSquared() + ELECTRON_MASS * ELECTRON_MASS);
        double ePos = FastMath.sqrt(pPos.magnitudeSquared() + ELECTRON_MASS * ELECTRON_MASS);
        double totalE = eEle + ePos;
        Hep3Vector totalP = VecOp.add(pEle, pPos);
        double massSq = totalE * totalE - totalP.magnitudeSquared();
        double invMass = massSq > 0 ? FastMath.sqrt(massSq) : -99.0;

        Map<Integer, Hep3Vector> pFitMap = new HashMap<Integer, Hep3Vector>();
        pFitMap.put(0, pEle);
        pFitMap.put(1, pPos);

        BilliorVertex vtxFit = new BilliorVertex(vtxPos, covVtx, result.chi2, invMass, pFitMap, "KALMAN_UNCONSTRAINED");
        vtxFit.setPositionError(vtxPosErr);
        vtxFit.setProbability(result.ndf);
        vtxFit.setParameter("ndf", (double) result.ndf);
        return vtxFit;
    }
}
