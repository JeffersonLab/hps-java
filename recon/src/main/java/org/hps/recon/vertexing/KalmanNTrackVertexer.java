package org.hps.recon.vertexing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
import org.hps.recon.vertexing.KalmanVertexFitterGainMatrix.TrackMomentum;
import org.hps.recon.vertexing.KalmanVertexFitterGainMatrix.TrackParams;

/**
 * Fits a plain, unconstrained N-track common vertex (all input tracks constrained to the
 * same (x,y,z) point, all assumed to be electron-mass) using the Kalman gain-matrix fitter
 * ({@link KalmanVertexFitterGainMatrix}), as an alternative to {@link BilliorVertexer} for
 * the same problem. This is a generalization of {@link KalmanV0Vertexer} (which is
 * hardcoded to exactly 2 tracks) to arbitrary N -- e.g. for validating against trident MC
 * (2 e- + 1 e+ from a single common production vertex). Packages the result as a {@link
 * BilliorVertex} (used here purely as a generic {@code Vertex}-implementing data container,
 * not as a Billoir-algorithm result) so it plugs directly into existing tuple-reading code.
 */
public class KalmanNTrackVertexer {

    private static final double ELECTRON_MASS = 0.000511;

    private final double bField;

    public KalmanNTrackVertexer(double bField) {
        this.bField = bField;
    }

    /**
     * Fit the common vertex of N tracks, with no vertex/momentum/mass constraint. All
     * tracks are assumed to share the same reference point (true for AtPerigee states as
     * produced by the tracking reconstruction).
     *
     * @param tracks the input tracks (any N &gt;= 2)
     * @return the fitted vertex, or a sentinel placeholder vertex if the fit fails
     */
    public BilliorVertex fitVertex(List<Track> tracks) {
        List<TrackParams> trackParams = new ArrayList<TrackParams>();
        double[] referencePoint = null;
        for (Track track : tracks) {
            TrackState ts = TrackStateUtils.getTrackStatesAtLocation(track, TrackState.AtPerigee).get(0);
            if (referencePoint == null) {
                referencePoint = ts.getReferencePoint();
            }
            trackParams.add(trackParamsFromTrack(ts));
        }

        KalmanVertexFitterGainMatrix fitter = new KalmanVertexFitterGainMatrix(bField);
        FitResult result = fitter.fit(trackParams);
        if (result == null) {
            return placeholderVertex(tracks.size());
        }

        return makeVertex(result, referencePoint);
    }

    /**
     * Fit the common vertex of N tracks with the total 3-momentum constrained to the beam
     * value, using {@link KalmanVertexFitterGainMatrix#fitVertex(List, boolean, boolean,
     * boolean)} directly (no beamspot-position constraint). That richer method already
     * performs the tracking-to-detector frame conversion, invariant mass calculation, and
     * ndf storage, so no separate {@code makeVertex}-style post-processing is needed here.
     *
     * @param tracks                 the input tracks (any N &gt;= 2)
     * @param beamEnergy             beam energy (GeV)
     * @param beamRotAngle           beam crossing angle about the tracking-frame Z axis (rad)
     * @param hardMomentumConstraint if true, enforce the momentum constraint exactly
     *                               (Lagrange multiplier); if false, apply it softly, weighted
     *                               by the beam momentum uncertainty.
     *                               <b>Deprecated:</b> {@code true} (hard mode) is not physically
     *                               correct -- the target nuclear recoil carries real momentum
     *                               away from the tracked leptons -- and, unlike soft mode,
     *                               cannot be corrected via {@code transverseNuclearRecoilSigma}
     *                               below (see {@link KalmanVertexFitterGainMatrix#fitLagrangeMultiplier}).
     *                               Prefer {@code false} for new production use.
     * @return the fitted vertex, or a sentinel placeholder vertex if the fit fails
     */
    public BilliorVertex fitVertexBeamConstrained(List<Track> tracks, double beamEnergy,
            double beamRotAngle, boolean hardMomentumConstraint) {
        return fitVertexBeamConstrained(tracks, beamEnergy, beamRotAngle, hardMomentumConstraint, 0.0);
    }

    /**
     * Same as {@link #fitVertexBeamConstrained(List, double, double, boolean)}, but with an
     * additional transverse beam-momentum-constraint width, combined in quadrature with the
     * beam-divergence term, to account for event-to-event transverse momentum not carried by
     * the tracked leptons -- primarily momentum transferred to the target nucleus during
     * production (nuclear recoil; distinct from a recoil electron from radiative/A' events) --
     * see {@link KalmanVertexFitterGainMatrix#setBeamMomentumTransverseNuclearRecoilSigma(double)}.
     * Kept as a separate overload rather than changing the 4-argument method in place, so that
     * method's existing (recoil-free) behavior remains available unchanged for any other caller.
     *
     * @param hardMomentumConstraint see {@link #fitVertexBeamConstrained(List, double, double, boolean)};
     *                               deprecated (hard mode), prefer {@code false}
     * @param transverseNuclearRecoilSigma additional transverse momentum width (GeV); 0.0
     *                               reproduces the original divergence-only covariance exactly.
     *                               Has no effect when {@code hardMomentumConstraint} is true.
     */
    public BilliorVertex fitVertexBeamConstrained(List<Track> tracks, double beamEnergy,
            double beamRotAngle, boolean hardMomentumConstraint, double transverseNuclearRecoilSigma) {
        List<TrackParams> trackParams = new ArrayList<TrackParams>();
        double[] referencePoint = null;
        for (Track track : tracks) {
            TrackState ts = TrackStateUtils.getTrackStatesAtLocation(track, TrackState.AtPerigee).get(0);
            if (referencePoint == null) {
                referencePoint = ts.getReferencePoint();
            }
            trackParams.add(trackParamsFromTrack(ts));
        }

        KalmanVertexFitterGainMatrix fitter = new KalmanVertexFitterGainMatrix(bField);
        fitter.setBeamEnergy(beamEnergy);
        fitter.setBeamRotAngle(beamRotAngle);
        fitter.setBeamMomentumTransverseNuclearRecoilSigma(transverseNuclearRecoilSigma);
        fitter.setReferencePosition(referencePoint);
        BilliorVertex bv = fitter.fitVertex(trackParams, false, true, hardMomentumConstraint);
        return (bv != null) ? bv : placeholderVertex(tracks.size());
    }

    /**
     * Fit the common vertex of N tracks with no beamspot-position or beam-momentum
     * constraint, using {@link KalmanVertexFitterGainMatrix#fitVertex(List, boolean,
     * boolean, boolean)} directly (dispatches internally to {@code fitBillior1985}, which
     * -- unlike the simpler {@code fit()} algorithm used by {@link #fitVertex(List)} --
     * already computes a real per-track momentum covariance). Kept as a separate method
     * rather than changing {@link #fitVertex(List)} in place, so that method's existing
     * (covariance-free) behavior remains available unchanged for any other caller.
     *
     * @param tracks the input tracks (any N &gt;= 2)
     * @return the fitted vertex, or a sentinel placeholder vertex if the fit fails
     */
    public BilliorVertex fitVertexNoBeamConstraint(List<Track> tracks) {
        List<TrackParams> trackParams = new ArrayList<TrackParams>();
        double[] referencePoint = null;
        for (Track track : tracks) {
            TrackState ts = TrackStateUtils.getTrackStatesAtLocation(track, TrackState.AtPerigee).get(0);
            if (referencePoint == null) {
                referencePoint = ts.getReferencePoint();
            }
            trackParams.add(trackParamsFromTrack(ts));
        }

        KalmanVertexFitterGainMatrix fitter = new KalmanVertexFitterGainMatrix(bField);
        fitter.setReferencePosition(referencePoint);
        BilliorVertex bv = fitter.fitVertex(trackParams, false, false, false);
        return (bv != null) ? bv : placeholderVertex(tracks.size());
    }

    /**
     * A sentinel vertex (chi2/mass = -9999, zero position/momenta) used in place of a null
     * result when the fit fails, so that callers pairing this fit's output index-for-index
     * against another collection (e.g. the Billoir N-track fit, for comparison) never see
     * the two lists fall out of sync.
     */
    public static BilliorVertex placeholderVertex(int nTracks) {
        Map<Integer, Hep3Vector> pFitMap = new HashMap<Integer, Hep3Vector>();
        for (int i = 0; i < nTracks; i++) {
            pFitMap.put(i, new BasicHep3Vector(0, 0, 0));
        }
        BilliorVertex vtxFit = new BilliorVertex(new BasicHep3Vector(0, 0, 0),
                new SymmetricMatrix(3), -9999.0, -9999.0, pFitMap, "KALMAN_NTRACK_FAILED");
        vtxFit.setPositionError(new BasicHep3Vector(0, 0, 0));
        vtxFit.setParameter("ndf", -9999.0);
        return vtxFit;
    }

    /**
     * Extract perigee TrackParams (d0, phi0, omega, z0, tanLambda) directly from an
     * AtPerigee TrackState, with no reparametrization -- see {@link KalmanV0Vertexer}'s
     * copy of this method for the full rationale (same convention here).
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
     * Convert a Kalman {@code FitResult} for N tracks into a {@code BilliorVertex}, using
     * the same tracking-frame -> detector-frame convention (det(x,y,z) = trk(y,z,x)) as
     * {@link KalmanV0Vertexer#makeVertex}. All tracks are assumed electron-mass (valid for
     * the trident use case: 2 e- + 1 e+).
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

        Map<Integer, Hep3Vector> pFitMap = new HashMap<Integer, Hep3Vector>();
        double totalE = 0.0;
        Hep3Vector totalP = new BasicHep3Vector(0, 0, 0);
        for (int i = 0; i < result.trackMomenta.size(); i++) {
            TrackMomentum tm = result.trackMomenta.get(i);
            RealVector pTrk = tm.p;
            Hep3Vector p = new BasicHep3Vector(pTrk.getEntry(1), pTrk.getEntry(2), pTrk.getEntry(0));
            pFitMap.put(i, p);
            totalE += FastMath.sqrt(p.magnitudeSquared() + ELECTRON_MASS * ELECTRON_MASS);
            totalP = VecOp.add(totalP, p);
        }
        double massSq = totalE * totalE - totalP.magnitudeSquared();
        double invMass = massSq > 0 ? FastMath.sqrt(massSq) : -99.0;

        BilliorVertex vtxFit = new BilliorVertex(vtxPos, covVtx, result.chi2, invMass, pFitMap, "KALMAN_NTRACK");
        vtxFit.setPositionError(vtxPosErr);
        vtxFit.setProbability(result.ndf);
        vtxFit.setParameter("ndf", (double) result.ndf);
        return vtxFit;
    }
}
