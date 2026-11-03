package org.hps.recon.vertexing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import hep.physics.matrix.SymmetricMatrix;
import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;

import org.lcsim.event.Track;
import org.lcsim.event.TrackState;

import org.hps.recon.tracking.TrackStateUtils;
import org.hps.recon.vertexing.TrackConstraintVertexFitter.TrackParams;

/**
 * Fits a plain N-track common vertex (all input tracks constrained to the same (x,y,z)
 * point, all assumed to be electron-mass), using the Billoir-batch algorithm in {@link
 * TrackConstraintVertexFitter}, as an alternative to {@link BilliorVertexer} for the same
 * problem. Handles arbitrary N (N &gt;= 2) uniformly -- e.g. the two-track V0 case as well
 * as validating against trident MC (2 e- + 1 e+ from a single common production vertex).
 * Packages the result as a {@link BilliorVertex} (used here purely as a generic {@code
 * Vertex}-implementing data container, not as a Billoir-algorithm result) so it plugs
 * directly into existing tuple-reading code.
 */
public class NTrackVertexer extends Vertexer {

    public NTrackVertexer(double bField) {
        super(bField);
    }

    /**
     * Fit the common vertex of N tracks with the total 3-momentum constrained to the beam
     * value, using {@link TrackConstraintVertexFitter#fitVertex(List, boolean, boolean,
     * boolean)} directly (no beamspot-position constraint). That richer method already
     * performs the tracking-to-detector frame conversion, invariant mass calculation, and
     * ndf storage, so no separate post-processing is needed here.
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
     *                               below (see {@link TrackConstraintVertexFitter#fitLagrangeMultiplier}).
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
     * see {@link TrackConstraintVertexFitter#setBeamMomentumTransverseNuclearRecoilSigma(double)}.
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

        TrackConstraintVertexFitter fitter = new TrackConstraintVertexFitter(bField);
        fitter.setBeamEnergy(beamEnergy);
        fitter.setBeamRotAngle(beamRotAngle);
        fitter.setBeamMomentumTransverseNuclearRecoilSigma(transverseNuclearRecoilSigma);
        fitter.setReferencePosition(referencePoint);
        // storeCovTrkMomList defaults to false in TrackConstraintVertexFitter (only CascadeVertexer's
        // own hand-built BilliorVertex path sets per-track momentum covariances unconditionally) --
        // without this, fitVertex() silently drops the per-track momentum covariance it already
        // computes internally, and getFittedMomentumError() on the result always returns null.
        fitter.setStoreCovTrkMomList(true);
        BilliorVertex bv = fitter.fitVertex(trackParams, false, true, hardMomentumConstraint);
        return (bv != null) ? bv : placeholderVertex(tracks.size());
    }

    /**
     * Fit the common vertex of N tracks with no beamspot-position or beam-momentum
     * constraint, using {@link TrackConstraintVertexFitter#fitVertex(List, boolean,
     * boolean, boolean)} directly (dispatches internally to {@code fitBillior1985}, which
     * computes a real per-track momentum covariance and, via {@code setStoreCovTrkMomList(true)},
     * attaches it to the returned {@code BilliorVertex} so {@code getFittedMomentumError()} is
     * populated).
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

        TrackConstraintVertexFitter fitter = new TrackConstraintVertexFitter(bField);
        fitter.setReferencePosition(referencePoint);
        // See the comment in fitVertexBeamConstrained -- without this, fitVertex() never attaches
        // the per-track momentum covariance it already computes to the returned BilliorVertex.
        fitter.setStoreCovTrkMomList(true);
        BilliorVertex bv = fitter.fitVertex(trackParams, false, false, false);
        return (bv != null) ? bv : placeholderVertex(tracks.size());
    }

    /**
     * Fit the common vertex of N tracks with a beamspot-position constraint (no beam-momentum
     * constraint), using {@link TrackConstraintVertexFitter#fitVertex(List, boolean, boolean,
     * boolean)} with {@code beamspotConstraint=true} -- a direct 3D Gaussian prior pulling the
     * fitted vertex position itself toward {@code beamPosition} with weight {@code 1/beamSize^2},
     * appropriate for a vertex physically expected to sit at/near the target (unlike {@link
     * BilliorVertexer}'s {@code applyBSconstraint}, which instead projects the fitted momentum
     * direction back through the beamspot -- appropriate for a displaced decay vertex). Kept as
     * a separate method rather than changing {@link #fitVertexNoBeamConstraint} or {@link
     * #fitVertexBeamConstrained} in place, so their existing (unconstrained-position) behavior
     * remains available unchanged for any other caller.
     *
     * @param tracks        the input tracks (any N &gt;= 2)
     * @param beamPosition  beamspot/target position (tracking frame, absolute), or {@code null}
     *                      to use the fitter's own default. Internally shifted by the tracks'
     *                      own reference point before use, since the fit's internal vertex state
     *                      lives in that local frame, not the absolute tracking frame.
     * @param beamSize      beamspot size (tracking frame, x/y/z sigmas), or {@code null} to use
     *                      the fitter's own default
     * @return the fitted vertex, or a sentinel placeholder vertex if the fit fails
     */
    public BilliorVertex fitVertexBeamspotConstrained(List<Track> tracks, double[] beamPosition, double[] beamSize) {
        List<TrackParams> trackParams = new ArrayList<TrackParams>();
        double[] referencePoint = null;
        for (Track track : tracks) {
            TrackState ts = TrackStateUtils.getTrackStatesAtLocation(track, TrackState.AtPerigee).get(0);
            if (referencePoint == null) {
                referencePoint = ts.getReferencePoint();
            }
            trackParams.add(trackParamsFromTrack(ts));
        }

        TrackConstraintVertexFitter fitter = new TrackConstraintVertexFitter(bField);
        if (beamPosition != null) {
            double[] beamPositionLocal = new double[3];
            for (int i = 0; i < 3; i++) {
                beamPositionLocal[i] = beamPosition[i] - referencePoint[i];
            }
            fitter.setBeamPosition(beamPositionLocal);
        }
        if (beamSize != null) {
            fitter.setBeamSize(beamSize);
        }
        fitter.setReferencePosition(referencePoint);
        // See the comment in fitVertexBeamConstrained -- without this, fitVertex() never attaches
        // the per-track momentum covariance it already computes to the returned BilliorVertex.
        fitter.setStoreCovTrkMomList(true);
        BilliorVertex bv = fitter.fitVertex(trackParams, true, false, false);
        return (bv != null) ? bv : placeholderVertex(tracks.size());
    }

    /**
     * Fit the common vertex of N tracks with both the beamspot-position constraint ({@link
     * #fitVertexBeamspotConstrained}) and the beam-momentum constraint ({@link
     * #fitVertexBeamConstrained}) applied together, using {@link
     * TrackConstraintVertexFitter#fitVertex(List, boolean, boolean, boolean)} with both
     * {@code beamspotConstraint} and {@code beamMomentumConstraint} true. Kept as a separate
     * method rather than changing either of those two in place, so their existing (single-
     * constraint) behavior remains available unchanged for any other caller.
     *
     * @param tracks                        the input tracks (any N &gt;= 2)
     * @param beamPosition                  beamspot/target position (tracking frame, absolute),
     *                                       or {@code null} to use the fitter's own default
     * @param beamSize                      beamspot size (tracking frame, x/y/z sigmas), or
     *                                       {@code null} to use the fitter's own default
     * @param beamEnergy                    beam energy (GeV)
     * @param beamRotAngle                  beam crossing angle about the tracking-frame Z axis (rad)
     * @param transverseNuclearRecoilSigma  additional transverse beam-momentum-constraint width
     *                                       (GeV); see {@link #fitVertexBeamConstrained(List,
     *                                       double, double, boolean, double)}
     * @return the fitted vertex, or a sentinel placeholder vertex if the fit fails
     */
    public BilliorVertex fitVertexBothConstrained(List<Track> tracks, double[] beamPosition, double[] beamSize,
            double beamEnergy, double beamRotAngle, double transverseNuclearRecoilSigma) {
        List<TrackParams> trackParams = new ArrayList<TrackParams>();
        double[] referencePoint = null;
        for (Track track : tracks) {
            TrackState ts = TrackStateUtils.getTrackStatesAtLocation(track, TrackState.AtPerigee).get(0);
            if (referencePoint == null) {
                referencePoint = ts.getReferencePoint();
            }
            trackParams.add(trackParamsFromTrack(ts));
        }

        TrackConstraintVertexFitter fitter = new TrackConstraintVertexFitter(bField);
        if (beamPosition != null) {
            double[] beamPositionLocal = new double[3];
            for (int i = 0; i < 3; i++) {
                beamPositionLocal[i] = beamPosition[i] - referencePoint[i];
            }
            fitter.setBeamPosition(beamPositionLocal);
        }
        if (beamSize != null) {
            fitter.setBeamSize(beamSize);
        }
        fitter.setBeamEnergy(beamEnergy);
        fitter.setBeamRotAngle(beamRotAngle);
        fitter.setBeamMomentumTransverseNuclearRecoilSigma(transverseNuclearRecoilSigma);
        fitter.setReferencePosition(referencePoint);
        // See the comment in fitVertexBeamConstrained -- without this, fitVertex() never attaches
        // the per-track momentum covariance it already computes to the returned BilliorVertex.
        fitter.setStoreCovTrkMomList(true);
        BilliorVertex bv = fitter.fitVertex(trackParams, true, true, false);
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
}
