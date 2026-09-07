package org.hps.analysis.tuple;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.VecOp;

import org.hps.recon.tracking.CoordinateTransformations;
import org.hps.recon.tracking.TrackStateUtils;
import org.hps.recon.tracking.TrackUtils;
import org.hps.recon.vertexing.BilliorTrack;
import org.hps.recon.vertexing.BilliorVertex;
import org.hps.recon.vertexing.BilliorVertexer;
import org.hps.recon.vertexing.KalmanNTrackVertexer;
import org.hps.recon.vertexing.KalmanVertexFitterGainMatrix;
import org.hps.recon.vertexing.KalmanVertexFitterGainMatrix.TrackMomentum;
import org.hps.recon.vertexing.KalmanVertexFitterGainMatrix.TrackParams;
import org.lcsim.event.EventHeader;
import org.lcsim.event.LCRelation;
import org.lcsim.event.MCParticle;
import org.lcsim.event.Track;
import org.lcsim.event.TrackState;
import org.lcsim.event.base.BaseTrackState;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;

import hep.physics.matrix.SymmetricMatrix;

import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;

/**
 * Writes a flat ASCII ntuple comparing, per trident MC event, the legacy Billoir N-track
 * common-vertex fit against the standalone Kalman gain-matrix N-track common-vertex fit
 * ({@code KalmanNTrackVertexer}), for the 3 truth-matched trident tracks (2 e- + 1 e+ from a
 * single common production vertex). Unlike {@code TwoTrackVertexComparisonTupleDriver} and
 * {@code CascadeVertexTupleDriver}, this driver does its own track selection and fitting
 * inline -- it reads raw {@code KalmanFullTracks} plus MC truth directly, rather than
 * pre-built V0/cascade candidate collections from {@code HpsReconParticleDriver} -- since no
 * existing driver builds a true all-tracks-to-one-point common vertex. One row per event
 * (only events where all 3 trident daughters are truth-matched to a reconstructed track).
 * Header line is variable names joined by ":"; data rows are tab-separated, matching the
 * convention used by {@link CascadeVertexTupleDriver}.
 */
public class NTrackVertexComparisonTupleDriver extends Driver {

    private static final List<String> VARIABLES = Arrays.asList(
            "run/I", "event/I",
            "vtxX/D", "vtxY/D", "vtxZ/D",
            "vtxXErr/D", "vtxYErr/D", "vtxZErr/D",
            "chi2/D", "mass/D",
            "kalVtxX/D", "kalVtxY/D", "kalVtxZ/D",
            "kalVtxXErr/D", "kalVtxYErr/D", "kalVtxZErr/D",
            "kalChi2/D", "kalNdf/I", "kalMass/D",
            "kalUncEle1Px/D", "kalUncEle1Py/D", "kalUncEle1Pz/D",
            "kalUncEle1PxErr/D", "kalUncEle1PyErr/D", "kalUncEle1PzErr/D",
            "kalUncEle2Px/D", "kalUncEle2Py/D", "kalUncEle2Pz/D",
            "kalUncEle2PxErr/D", "kalUncEle2PyErr/D", "kalUncEle2PzErr/D",
            "kalUncPosPx/D", "kalUncPosPy/D", "kalUncPosPz/D",
            "kalUncPosPxErr/D", "kalUncPosPyErr/D", "kalUncPosPzErr/D",
            "kalSoftVtxX/D", "kalSoftVtxY/D", "kalSoftVtxZ/D",
            "kalSoftVtxXErr/D", "kalSoftVtxYErr/D", "kalSoftVtxZErr/D",
            "kalSoftChi2/D", "kalSoftNdf/I", "kalSoftMass/D",
            "kalSoftPx/D", "kalSoftPy/D", "kalSoftPz/D",
            "kalSoftPxErr/D", "kalSoftPyErr/D", "kalSoftPzErr/D",
            "kalSoftEle1Px/D", "kalSoftEle1Py/D", "kalSoftEle1Pz/D",
            "kalSoftEle1PxErr/D", "kalSoftEle1PyErr/D", "kalSoftEle1PzErr/D",
            "kalSoftEle2Px/D", "kalSoftEle2Py/D", "kalSoftEle2Pz/D",
            "kalSoftEle2PxErr/D", "kalSoftEle2PyErr/D", "kalSoftEle2PzErr/D",
            "kalSoftPosPx/D", "kalSoftPosPy/D", "kalSoftPosPz/D",
            "kalSoftPosPxErr/D", "kalSoftPosPyErr/D", "kalSoftPosPzErr/D",
            "ele1TruthMatched/I", "ele2TruthMatched/I", "posTruthMatched/I", "allTruthMatched/I",
            "apVtxXMC/D", "apVtxYMC/D", "apVtxZMC/D",
            "mcTotalPx/D", "mcTotalPy/D", "mcTotalPz/D",
            "mcEle1Px/D", "mcEle1Py/D", "mcEle1Pz/D",
            "mcEle2Px/D", "mcEle2Py/D", "mcEle2Pz/D",
            "mcPosPx/D", "mcPosPy/D", "mcPosPz/D",
            "recoEle1Px/D", "recoEle1Py/D", "recoEle1Pz/D",
            "recoEle1PxErr/D", "recoEle1PyErr/D", "recoEle1PzErr/D",
            "recoEle2Px/D", "recoEle2Py/D", "recoEle2Pz/D",
            "recoEle2PxErr/D", "recoEle2PyErr/D", "recoEle2PzErr/D",
            "recoPosPx/D", "recoPosPy/D", "recoPosPz/D",
            "recoPosPxErr/D", "recoPosPyErr/D", "recoPosPzErr/D");

    private String trackCollectionName = "KalmanFullTracks";
    private String mcParticlesColName = "MCParticle";
    private String trackToMCParticleRelationsColName = "KalmanFullTracksToMCParticleRelations";
    private String tupleFile = null;
    private PrintWriter tupleWriter = null;
    private double bField;
    private double beamEnergy = 3.74;
    private double beamRotAngle = -0.0305;
    private double minTruthMatchPurity = 0.9;

    public void setTrackCollectionName(String trackCollectionName) {
        this.trackCollectionName = trackCollectionName;
    }

    public void setMinTruthMatchPurity(double minTruthMatchPurity) {
        this.minTruthMatchPurity = minTruthMatchPurity;
    }

    public void setBeamEnergy(double beamEnergy) {
        this.beamEnergy = beamEnergy;
    }

    public void setBeamRotAngle(double beamRotAngle) {
        this.beamRotAngle = beamRotAngle;
    }

    public void setMcParticlesColName(String mcParticlesColName) {
        this.mcParticlesColName = mcParticlesColName;
    }

    public void setTrackToMCParticleRelationsColName(String trackToMCParticleRelationsColName) {
        this.trackToMCParticleRelationsColName = trackToMCParticleRelationsColName;
    }

    public void setTupleFile(String tupleFile) {
        this.tupleFile = tupleFile;
    }

    @Override
    protected void detectorChanged(Detector detector) {
        bField = detector.getFieldMap().getField(new BasicHep3Vector(0., 0., -4.3)).y();
        if (tupleFile == null) {
            return;
        }
        try {
            tupleWriter = new PrintWriter(tupleFile);
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Could not open N-track vertex comparison tuple file " + tupleFile, e);
        }
        tupleWriter.println(String.join(":", VARIABLES));
    }

    @Override
    public void process(EventHeader event) {
        if (tupleWriter == null || !event.hasCollection(Track.class, trackCollectionName)
                || !event.hasCollection(MCParticle.class, mcParticlesColName)
                || !event.hasCollection(LCRelation.class, trackToMCParticleRelationsColName)) {
            return;
        }

        // Trident truth identification: PDGID 623 is the trident/"reaction" pseudo-particle
        // and has 3 direct daughters (2 e- + 1 e+ from a single common production vertex) --
        // architecturally different from the A' sample, where 622 is the A' signal record
        // (2 daughters: signal e+e-) and 623 is a separate, unrelated single-daughter recoil
        // electron record (confirmed via direct LCIO inspection of tritrig_pulser_100.slcio:
        // zero PDGID-622 particles present; every event has exactly one PDGID-623 top-level
        // particle with the expected 2x11 + 1x(-11) daughter topology). Ranks electron
        // daughters by energy, same convention as MCTupleMaker.fillMCTridentVariables (which
        // has this same PDGID mixup for tritrig-only samples).
        MCParticle ele1MC = null;
        MCParticle ele2MC = null;
        MCParticle posMC = null;
        for (MCParticle mcp : event.get(MCParticle.class, mcParticlesColName)) {
            if (mcp.getPDGID() == 623) {
                for (MCParticle daughter : mcp.getDaughters()) {
                    switch (daughter.getPDGID()) {
                        case -11:
                            if (posMC == null || daughter.getEnergy() > posMC.getEnergy()) {
                                posMC = daughter;
                            }
                            break;
                        case 11:
                            if (ele1MC == null || daughter.getEnergy() > ele1MC.getEnergy()) {
                                ele2MC = ele1MC;
                                ele1MC = daughter;
                            } else if (ele2MC == null || daughter.getEnergy() > ele2MC.getEnergy()) {
                                ele2MC = daughter;
                            }
                            break;
                    }
                }
                break;
            }
        }
        if (ele1MC == null || ele2MC == null || posMC == null) {
            return;
        }

        // Truth vertex: unlike the A' sample, this generator's PDGID-622 daughters' own
        // getOrigin() is not contaminated by any upstream converter bug (confirmed via
        // MCTupleMaker.fillMCParticleVariables, which uses particle.getOrigin() directly),
        // so no daughter-production-point workaround is needed here.
        Hep3Vector apVtxMC = ele1MC.getOrigin();

        // Truth total momentum of the 3 trident daughters, used as the pull denominator's
        // truth term for the fitted total-momentum pull plots -- same "no frame-conversion
        // workaround needed" situation as apVtxMC above (MCParticle.getMomentum() is already
        // in detector frame).
        Hep3Vector ele1MCMom = ele1MC.getMomentum();
        Hep3Vector ele2MCMom = ele2MC.getMomentum();
        Hep3Vector posMCMom = posMC.getMomentum();
        Hep3Vector mcTotalP = VecOp.add(VecOp.add(ele1MCMom, ele2MCMom), posMCMom);

        // A single MCParticle can receive relations from more than one Track (e.g. a
        // ghost/duplicate track sharing hits with the true particle), so pick the
        // highest-purity (rel.getWeight()) match rather than whichever relation the
        // collection happens to iterate last -- same disambiguation signal used by
        // CascadeVertexTupleDriver/TwoTrackVertexComparisonTupleDriver's trackPurity maps.
        Map<MCParticle, Track> mcToTrack = new HashMap<MCParticle, Track>();
        Map<MCParticle, Double> mcToWeight = new HashMap<MCParticle, Double>();
        for (LCRelation rel : event.get(LCRelation.class, trackToMCParticleRelationsColName)) {
            MCParticle mcp = (MCParticle) rel.getTo();
            if (mcp == ele1MC || mcp == ele2MC || mcp == posMC) {
                double weight = rel.getWeight();
                if (weight < minTruthMatchPurity) {
                    continue;
                }
                Double best = mcToWeight.get(mcp);
                if (best == null || weight > best) {
                    mcToWeight.put(mcp, weight);
                    mcToTrack.put(mcp, (Track) rel.getFrom());
                }
            }
        }

        Track ele1Track = mcToTrack.get(ele1MC);
        Track ele2Track = mcToTrack.get(ele2MC);
        Track posTrack = mcToTrack.get(posMC);
        boolean ele1Matched = ele1Track != null;
        boolean ele2Matched = ele2Track != null;
        boolean posMatched = posTrack != null;
        if (!ele1Matched || !ele2Matched || !posMatched) {
            return;
        }

        List<Track> tracks = Arrays.asList(ele1Track, ele2Track, posTrack);

        // Raw reconstructed momentum (+ its own, no-vertex-constraint error) at each
        // track's own point of closest approach, before any vertex fit is applied -- the
        // pre-fit baseline the vertex-fit momenta (and MC truth) are compared against.
        // Computed in the tracking frame (same convention as TrackDataDriver.java) then
        // converted to detector frame to match apVtxMC/mcEle1Px etc.
        Hep3Vector[] recoEle1 = getRawMomentumAndError(ele1Track);
        Hep3Vector[] recoEle2 = getRawMomentumAndError(ele2Track);
        Hep3Vector[] recoPos = getRawMomentumAndError(posTrack);
        Hep3Vector recoEle1Mom = recoEle1[0], recoEle1MomErr = recoEle1[1];
        Hep3Vector recoEle2Mom = recoEle2[0], recoEle2MomErr = recoEle2[1];
        Hep3Vector recoPosMom = recoPos[0], recoPosMomErr = recoPos[1];

        List<BilliorTrack> billTracks = new ArrayList<BilliorTrack>();
        for (Track track : tracks) {
            billTracks.add(new BilliorTrack(track));
        }

        // BilliorTrack(Track) copies the AtPerigee track parameters verbatim but drops the
        // TrackState's own (fixed, non-zero) reference point, and BilliorVertexer's linear
        // approximation is only accurate near its assumed reference point -- exactly the gap
        // HpsReconParticleDriver.fitVertex()/shiftTracksToVertex() (production V0 fitting)
        // works around: fit once from the naive (uncorrected) tracks, then re-derive each
        // track's helix parameters at that first-pass vertex via
        // TrackUtils.getParametersAtNewRefPoint/getCovarianceAtNewRefPoint, and refit from
        // there. Mirrored here verbatim (generalized to N=3 tracks) rather than just adding
        // the reference point back once, for full consistency with the production pattern.
        BilliorVertexer firstPassVertexer = new BilliorVertexer(bField);
        BilliorVertex firstPassVtx = firstPassVertexer.fitVertex(billTracks);

        double[] newRef = {firstPassVtx.getPosition().z(), firstPassVtx.getPosition().x(), 0.0};
        List<BilliorTrack> shiftedTracks = new ArrayList<BilliorTrack>();
        for (Track track : tracks) {
            BaseTrackState oldTs = (BaseTrackState) TrackStateUtils.getTrackStatesAtLocation(track, TrackState.AtPerigee).get(0);
            double[] newParams = TrackUtils.getParametersAtNewRefPoint(newRef, oldTs);
            SymmetricMatrix newCov = TrackUtils.getCovarianceAtNewRefPoint(newRef, oldTs.getReferencePoint(), oldTs.getParameters(),
                    new SymmetricMatrix(5, oldTs.getCovMatrix(), true));
            BaseTrackState newTs = new BaseTrackState(newParams, newRef, newCov.asPackedArray(true), TrackState.AtPerigee, oldTs.getBLocal());
            shiftedTracks.add(new BilliorTrack(newTs, 0, 0));
        }

        BilliorVertexer billiorVertexer = new BilliorVertexer(bField);
        billiorVertexer.setReferencePosition(newRef);
        BilliorVertex billoirVtx = billiorVertexer.fitVertex(shiftedTracks);
        BilliorVertex kalmanVtx = new KalmanNTrackVertexer(bField).fitVertex(tracks);
        BilliorVertex kalmanVtxUnconstrained = new KalmanNTrackVertexer(bField).fitVertexNoBeamConstraint(tracks);
        // Hard (Lagrange-multiplier/exact) beam-momentum-constrained mode is deprecated (see
        // KalmanVertexFitterGainMatrix.fitLagrangeMultiplier) and no longer computed here.
        // sigmaTNuclearRecoil accounts for momentum carried away by the target nuclear recoil
        // in trident production, not modeled by the beam-divergence-only covariance alone (see
        // KalmanVertexFitterGainMatrix.setBeamMomentumTransverseNuclearRecoilSigma). Value is
        // 18.6 MeV, measured from std(mcTotalPx/Py) truth spread on a single 10-file real-MC
        // pilot; TODO: retune on a larger/full sample.
        double sigmaTNuclearRecoil = 0.0186; // GeV
        BilliorVertex kalmanVtxSoft = new KalmanNTrackVertexer(bField).fitVertexBeamConstrained(
                tracks, beamEnergy, beamRotAngle, false, sigmaTNuclearRecoil);

        Hep3Vector vtxPos = billoirVtx.getPosition();
        Hep3Vector vtxPosErr = billoirVtx.getPositionError();
        Hep3Vector kalVtxPos = kalmanVtx.getPosition();
        Hep3Vector kalVtxPosErr = kalmanVtx.getPositionError();
        Double kalNdf = kalmanVtx.getCustomParameters().get("ndf");
        Hep3Vector kalSoftVtxPos = kalmanVtxSoft.getPosition();
        Hep3Vector kalSoftVtxPosErr = kalmanVtxSoft.getPositionError();
        Double kalSoftNdf = kalmanVtxSoft.getCustomParameters().get("ndf");
        Hep3Vector kalSoftP = kalmanVtxSoft.getV0Momentum();
        Hep3Vector kalSoftPErr = kalmanVtxSoft.getV0MomentumError();

        Map<String, Double> row = new HashMap<String, Double>();
        row.put("run/I", (double) event.getRunNumber());
        row.put("event/I", (double) event.getEventNumber());
        row.put("vtxX/D", vtxPos.x());
        row.put("vtxY/D", vtxPos.y());
        row.put("vtxZ/D", vtxPos.z());
        row.put("vtxXErr/D", vtxPosErr.x());
        row.put("vtxYErr/D", vtxPosErr.y());
        row.put("vtxZErr/D", vtxPosErr.z());
        row.put("chi2/D", billoirVtx.getChi2());
        row.put("mass/D", billoirVtx.getInvMass());
        row.put("kalVtxX/D", kalVtxPos.x());
        row.put("kalVtxY/D", kalVtxPos.y());
        row.put("kalVtxZ/D", kalVtxPos.z());
        row.put("kalVtxXErr/D", kalVtxPosErr.x());
        row.put("kalVtxYErr/D", kalVtxPosErr.y());
        row.put("kalVtxZErr/D", kalVtxPosErr.z());
        row.put("kalChi2/D", kalmanVtx.getChi2());
        row.put("kalNdf/I", kalNdf != null ? kalNdf : -9999.0);
        row.put("kalMass/D", kalmanVtx.getInvMass());
        row.put("kalSoftVtxX/D", kalSoftVtxPos.x());
        row.put("kalSoftVtxY/D", kalSoftVtxPos.y());
        row.put("kalSoftVtxZ/D", kalSoftVtxPos.z());
        row.put("kalSoftVtxXErr/D", kalSoftVtxPosErr.x());
        row.put("kalSoftVtxYErr/D", kalSoftVtxPosErr.y());
        row.put("kalSoftVtxZErr/D", kalSoftVtxPosErr.z());
        row.put("kalSoftChi2/D", kalmanVtxSoft.getChi2());
        row.put("kalSoftNdf/I", kalSoftNdf != null ? kalSoftNdf : -9999.0);
        row.put("kalSoftMass/D", kalmanVtxSoft.getInvMass());
        if (kalSoftP != null && kalSoftPErr != null) {
            row.put("kalSoftPx/D", kalSoftP.x());
            row.put("kalSoftPy/D", kalSoftP.y());
            row.put("kalSoftPz/D", kalSoftP.z());
            row.put("kalSoftPxErr/D", kalSoftPErr.x());
            row.put("kalSoftPyErr/D", kalSoftPErr.y());
            row.put("kalSoftPzErr/D", kalSoftPErr.z());
        }
        putTrackMomentum(row, kalmanVtxUnconstrained, 0, "kalUncEle1");
        putTrackMomentum(row, kalmanVtxUnconstrained, 1, "kalUncEle2");
        putTrackMomentum(row, kalmanVtxUnconstrained, 2, "kalUncPos");
        putTrackMomentum(row, kalmanVtxSoft, 0, "kalSoftEle1");
        putTrackMomentum(row, kalmanVtxSoft, 1, "kalSoftEle2");
        putTrackMomentum(row, kalmanVtxSoft, 2, "kalSoftPos");
        row.put("ele1TruthMatched/I", ele1Matched ? 1.0 : 0.0);
        row.put("ele2TruthMatched/I", ele2Matched ? 1.0 : 0.0);
        row.put("posTruthMatched/I", posMatched ? 1.0 : 0.0);
        row.put("allTruthMatched/I", (ele1Matched && ele2Matched && posMatched) ? 1.0 : 0.0);
        row.put("apVtxXMC/D", apVtxMC.x());
        row.put("apVtxYMC/D", apVtxMC.y());
        row.put("apVtxZMC/D", apVtxMC.z());
        row.put("mcTotalPx/D", mcTotalP.x());
        row.put("mcTotalPy/D", mcTotalP.y());
        row.put("mcTotalPz/D", mcTotalP.z());
        row.put("mcEle1Px/D", ele1MCMom.x());
        row.put("mcEle1Py/D", ele1MCMom.y());
        row.put("mcEle1Pz/D", ele1MCMom.z());
        row.put("mcEle2Px/D", ele2MCMom.x());
        row.put("mcEle2Py/D", ele2MCMom.y());
        row.put("mcEle2Pz/D", ele2MCMom.z());
        row.put("mcPosPx/D", posMCMom.x());
        row.put("mcPosPy/D", posMCMom.y());
        row.put("mcPosPz/D", posMCMom.z());
        row.put("recoEle1Px/D", recoEle1Mom.x());
        row.put("recoEle1Py/D", recoEle1Mom.y());
        row.put("recoEle1Pz/D", recoEle1Mom.z());
        row.put("recoEle1PxErr/D", recoEle1MomErr.x());
        row.put("recoEle1PyErr/D", recoEle1MomErr.y());
        row.put("recoEle1PzErr/D", recoEle1MomErr.z());
        row.put("recoEle2Px/D", recoEle2Mom.x());
        row.put("recoEle2Py/D", recoEle2Mom.y());
        row.put("recoEle2Pz/D", recoEle2Mom.z());
        row.put("recoEle2PxErr/D", recoEle2MomErr.x());
        row.put("recoEle2PyErr/D", recoEle2MomErr.y());
        row.put("recoEle2PzErr/D", recoEle2MomErr.z());
        row.put("recoPosPx/D", recoPosMom.x());
        row.put("recoPosPy/D", recoPosMom.y());
        row.put("recoPosPz/D", recoPosMom.z());
        row.put("recoPosPxErr/D", recoPosMomErr.x());
        row.put("recoPosPyErr/D", recoPosMomErr.y());
        row.put("recoPosPzErr/D", recoPosMomErr.z());

        writeRow(row);
    }

    /**
     * Reads track {@code trackIndex}'s fitted momentum + diagonal error off {@code bv} (set
     * unconditionally by {@code KalmanVertexFitterGainMatrix.fitVertex}'s dispatcher for every
     * track, via {@code getFittedMomentum(i)} and the {@code fitMom{i}_pxErr} custom
     * parameters) and writes them into {@code row} under {@code prefix}. No-ops (leaving the
     * sentinel fill in {@code writeRow} to apply) if {@code bv} is a failed-fit placeholder,
     * signaled by the custom-parameter errors being absent.
     */
    private static void putTrackMomentum(Map<String, Double> row, BilliorVertex bv, int trackIndex, String prefix) {
        Hep3Vector p = bv.getFittedMomentum(trackIndex);
        Double pxErr = bv.getCustomParameters().get("fitMom" + trackIndex + "_pxErr");
        Double pyErr = bv.getCustomParameters().get("fitMom" + trackIndex + "_pyErr");
        Double pzErr = bv.getCustomParameters().get("fitMom" + trackIndex + "_pzErr");
        if (p == null || pxErr == null || pyErr == null || pzErr == null) {
            return;
        }
        row.put(prefix + "Px/D", p.x());
        row.put(prefix + "Py/D", p.y());
        row.put(prefix + "Pz/D", p.z());
        row.put(prefix + "PxErr/D", pxErr);
        row.put(prefix + "PyErr/D", pyErr);
        row.put(prefix + "PzErr/D", pzErr);
    }

    /**
     * Raw pre-vertex-fit momentum and its diagonal error of {@code track}, evaluated at
     * its own point of closest approach (no vertex constraint applied), converted to
     * detector frame. The error comes from propagating the track's own helix-parameter
     * covariance through the same momentum Jacobian the vertex fitters use
     * ({@link KalmanVertexFitterGainMatrix#computeRawMomentum}), so it is directly
     * comparable to the kalSoftXPxErr columns -- the "before any vertex fit"
     * baseline for momentum pull plots. Returns {momentum, momentumError}.
     */
    private Hep3Vector[] getRawMomentumAndError(Track track) {
        TrackState ts = TrackStateUtils.getTrackStatesAtLocation(track, TrackState.AtPerigee).get(0);
        double[] par = ts.getParameters();
        SymmetricMatrix sm = new SymmetricMatrix(5, ts.getCovMatrix(), true);
        RealMatrix cov = MatrixUtils.createRealMatrix(5, 5);
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 5; j++) {
                cov.setEntry(i, j, sm.e(i, j));
            }
        }
        TrackParams tp = new TrackParams(par[0], par[1], par[2], par[3], par[4], cov);
        TrackMomentum tm = new KalmanVertexFitterGainMatrix(bField).computeRawMomentum(tp);

        Hep3Vector pDet = CoordinateTransformations.transformVectorToDetector(
                new BasicHep3Vector(tm.p.getEntry(0), tm.p.getEntry(1), tm.p.getEntry(2)));
        SymmetricMatrix pCovTrk = new SymmetricMatrix(3);
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                pCovTrk.setElement(i, j, tm.pCov.getEntry(i, j));
            }
        }
        SymmetricMatrix pCovDet = CoordinateTransformations.transformCovarianceToDetector(pCovTrk);
        Hep3Vector pErrDet = new BasicHep3Vector(
                Math.sqrt(Math.abs(pCovDet.e(0, 0))),
                Math.sqrt(Math.abs(pCovDet.e(1, 1))),
                Math.sqrt(Math.abs(pCovDet.e(2, 2))));
        return new Hep3Vector[]{pDet, pErrDet};
    }

    private void writeRow(Map<String, Double> row) {
        for (String variable : VARIABLES) {
            Double value = row.get(variable);
            if (value == null || Double.isNaN(value)) {
                value = -9999.0;
            }
            if (variable.endsWith("/I") || variable.endsWith("/B")) {
                tupleWriter.format("%d\t", Math.round(value));
            } else {
                tupleWriter.format("%g\t", value);
            }
        }
        tupleWriter.println();
    }

    @Override
    public void endOfData() {
        if (tupleWriter != null) {
            tupleWriter.close();
        }
    }
}
