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

import org.hps.recon.tracking.TrackStateUtils;
import org.hps.recon.tracking.TrackUtils;
import org.hps.recon.vertexing.BilliorTrack;
import org.hps.recon.vertexing.BilliorVertex;
import org.hps.recon.vertexing.BilliorVertexer;
import org.hps.recon.vertexing.KalmanNTrackVertexer;
import org.lcsim.event.EventHeader;
import org.lcsim.event.LCRelation;
import org.lcsim.event.MCParticle;
import org.lcsim.event.Track;
import org.lcsim.event.TrackState;
import org.lcsim.event.base.BaseTrackState;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;

import hep.physics.matrix.SymmetricMatrix;

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
            "ele1TruthMatched/I", "ele2TruthMatched/I", "posTruthMatched/I", "allTruthMatched/I",
            "apVtxXMC/D", "apVtxYMC/D", "apVtxZMC/D");

    private String trackCollectionName = "KalmanFullTracks";
    private String mcParticlesColName = "MCParticle";
    private String trackToMCParticleRelationsColName = "KalmanFullTracksToMCParticleRelations";
    private String tupleFile = null;
    private PrintWriter tupleWriter = null;
    private double bField;

    public void setTrackCollectionName(String trackCollectionName) {
        this.trackCollectionName = trackCollectionName;
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

        Hep3Vector vtxPos = billoirVtx.getPosition();
        Hep3Vector vtxPosErr = billoirVtx.getPositionError();
        Hep3Vector kalVtxPos = kalmanVtx.getPosition();
        Hep3Vector kalVtxPosErr = kalmanVtx.getPositionError();
        Double kalNdf = kalmanVtx.getCustomParameters().get("ndf");

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
        row.put("ele1TruthMatched/I", ele1Matched ? 1.0 : 0.0);
        row.put("ele2TruthMatched/I", ele2Matched ? 1.0 : 0.0);
        row.put("posTruthMatched/I", posMatched ? 1.0 : 0.0);
        row.put("allTruthMatched/I", (ele1Matched && ele2Matched && posMatched) ? 1.0 : 0.0);
        row.put("apVtxXMC/D", apVtxMC.x());
        row.put("apVtxYMC/D", apVtxMC.y());
        row.put("apVtxZMC/D", apVtxMC.z());

        writeRow(row);
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
