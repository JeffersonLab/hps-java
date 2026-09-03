package org.hps.analysis.tuple;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import hep.physics.vec.Hep3Vector;

import org.hps.recon.tracking.TrackStateUtils;
import org.hps.recon.vertexing.BilliorVertex;
import org.lcsim.event.EventHeader;
import org.lcsim.event.LCRelation;
import org.lcsim.event.MCParticle;
import org.lcsim.event.ReconstructedParticle;
import org.lcsim.event.Track;
import org.lcsim.event.TrackState;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;

/**
 * Writes a flat ASCII ntuple pairing, per unconstrained V0 (e-/e+) candidate, the
 * existing Billoir vertex fit against the standalone unconstrained two-track Kalman
 * vertex fit ({@code KalmanV0Vertexer}, wired through {@code HpsReconParticleDriver}),
 * for offline comparison of the two algorithms. One row per V0 candidate. Header line is
 * variable names joined by ":"; data rows are tab-separated, matching the convention
 * used by {@link CascadeVertexTupleDriver}.
 */
public class TwoTrackVertexComparisonTupleDriver extends Driver {

    private static final Logger LOGGER = Logger.getLogger(TwoTrackVertexComparisonTupleDriver.class.getPackage().getName());

    private static final List<String> VARIABLES = Arrays.asList(
            "run/I", "event/I",
            "vtxX/D", "vtxY/D", "vtxZ/D",
            "vtxXErr/D", "vtxYErr/D", "vtxZErr/D",
            "chi2/D", "mass/D",
            "kalVtxX/D", "kalVtxY/D", "kalVtxZ/D",
            "kalVtxXErr/D", "kalVtxYErr/D", "kalVtxZErr/D",
            "kalChi2/D", "kalNdf/I", "kalMass/D",
            "eleTruthMatched/I", "posTruthMatched/I", "bothTruthMatchedToAp/I",
            "apVtxXMC/D", "apVtxYMC/D", "apVtxZMC/D");

    private String unconstrainedV0CandidatesColName = "UnconstrainedV0Candidates";
    private String kalmanV0CandidatesColName = "KalmanUnconstrainedV0Candidates";
    private String mcParticlesColName = null;
    private String trackToMCParticleRelationsColName = null;
    private String tupleFile = null;
    private PrintWriter tupleWriter = null;

    public void setUnconstrainedV0CandidatesColName(String unconstrainedV0CandidatesColName) {
        this.unconstrainedV0CandidatesColName = unconstrainedV0CandidatesColName;
    }

    public void setKalmanV0CandidatesColName(String kalmanV0CandidatesColName) {
        this.kalmanV0CandidatesColName = kalmanV0CandidatesColName;
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
        if (tupleFile == null) {
            return;
        }
        try {
            tupleWriter = new PrintWriter(tupleFile);
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Could not open two-track vertex comparison tuple file " + tupleFile, e);
        }
        tupleWriter.println(String.join(":", VARIABLES));
    }

    @Override
    public void process(EventHeader event) {
        if (tupleWriter == null || !event.hasCollection(ReconstructedParticle.class, unconstrainedV0CandidatesColName)
                || !event.hasCollection(ReconstructedParticle.class, kalmanV0CandidatesColName)) {
            return;
        }

        Map<Track, MCParticle> trackToMC = new HashMap<Track, MCParticle>();
        if (trackToMCParticleRelationsColName != null
                && event.hasCollection(LCRelation.class, trackToMCParticleRelationsColName)) {
            for (LCRelation rel : event.get(LCRelation.class, trackToMCParticleRelationsColName)) {
                trackToMC.put((Track) rel.getFrom(), (MCParticle) rel.getTo());
            }
        }

        MCParticle eleMC = null;
        MCParticle posMC = null;
        if (mcParticlesColName != null && event.hasCollection(MCParticle.class, mcParticlesColName)) {
            for (MCParticle mcp : event.get(MCParticle.class, mcParticlesColName)) {
                if (mcp.getPDGID() == 622 && mcp.getDaughters().size() == 2) {
                    for (MCParticle daughter : mcp.getDaughters()) {
                        if (daughter.getPDGID() == 11) {
                            eleMC = daughter;
                        } else if (daughter.getPDGID() == -11) {
                            posMC = daughter;
                        }
                    }
                    break;
                }
            }
        }

        // apMC.getOrigin()/getEndPoint() are unreliable for this generator sample: the
        // .dat/LHE-to-stdhep converters apply the decay-length vertex shift to the A' record
        // itself, and BaseMCParticle's endpoint field defaults to (0,0,0) unless set by an
        // external writer. Use a daughter's own production point instead -- daughters are
        // produced exactly at the parent's decay vertex (same convention as CascadeVertexTupleDriver).
        Hep3Vector apVtxMC = eleMC != null ? eleMC.getOrigin() : (posMC != null ? posMC.getOrigin() : null);

        List<ReconstructedParticle> billoirV0s = event.get(ReconstructedParticle.class, unconstrainedV0CandidatesColName);
        List<ReconstructedParticle> kalmanV0s = event.get(ReconstructedParticle.class, kalmanV0CandidatesColName);
        if (billoirV0s.size() != kalmanV0s.size()) {
            LOGGER.warning("Billoir/Kalman unconstrained V0 collection size mismatch ("
                    + billoirV0s.size() + " vs " + kalmanV0s.size() + "), skipping event "
                    + event.getEventNumber());
            return;
        }

        for (int i = 0; i < billoirV0s.size(); i++) {
            ReconstructedParticle billoirV0 = billoirV0s.get(i);
            ReconstructedParticle kalmanV0 = kalmanV0s.get(i);
            BilliorVertex billoirVtx = (BilliorVertex) billoirV0.getStartVertex();
            BilliorVertex kalmanVtx = (BilliorVertex) kalmanV0.getStartVertex();

            List<ReconstructedParticle> daughters = billoirV0.getParticles();
            ReconstructedParticle eleDaughter = daughters.get(0).getCharge() < 0 ? daughters.get(0) : daughters.get(1);
            ReconstructedParticle posDaughter = daughters.get(0).getCharge() < 0 ? daughters.get(1) : daughters.get(0);

            if (event.getRunNumber() == 14596 && event.getEventNumber() == 734) {
                System.out.println("CANDIDATE i=" + i + " billoirVtxZ=" + billoirVtx.getPosition().z()
                        + " billoirChi2=" + billoirVtx.getChi2()
                        + " kalmanVtxZ=" + kalmanVtx.getPosition().z()
                        + " kalmanChi2=" + kalmanVtx.getChi2());
                dumpTrack("ELECTRON", eleDaughter.getTracks().get(0));
                dumpTrack("POSITRON", posDaughter.getTracks().get(0));
            }
            boolean eleMatched = eleMC != null && eleMC.equals(trackToMC.get(eleDaughter.getTracks().get(0)));
            boolean posMatched = posMC != null && posMC.equals(trackToMC.get(posDaughter.getTracks().get(0)));

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
            row.put("eleTruthMatched/I", eleMatched ? 1.0 : 0.0);
            row.put("posTruthMatched/I", posMatched ? 1.0 : 0.0);
            row.put("bothTruthMatchedToAp/I", eleMatched && posMatched ? 1.0 : 0.0);
            row.put("apVtxXMC/D", apVtxMC != null ? apVtxMC.x() : Double.NaN);
            row.put("apVtxYMC/D", apVtxMC != null ? apVtxMC.y() : Double.NaN);
            row.put("apVtxZMC/D", apVtxMC != null ? apVtxMC.z() : Double.NaN);

            writeRow(row);
        }
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

    private static void dumpTrack(String label, Track track) {
        TrackState ts = TrackStateUtils.getTrackStatesAtLocation(track, TrackState.AtPerigee).get(0);
        double[] par = ts.getParameters();
        double[] cov = ts.getCovMatrix();
        System.out.println("=== " + label + " AtPerigee ===");
        System.out.println("  d0=" + par[0] + " phi0=" + par[1] + " omega=" + par[2]
                + " z0=" + par[3] + " tanLambda=" + par[4] + " bLocal=" + ts.getBLocal());
        System.out.println("  covPacked=" + Arrays.toString(cov));
        System.out.println("  referencePoint=" + Arrays.toString(ts.getReferencePoint()));

        System.out.println("  track.getTrackStates().size()=" + track.getTrackStates().size());
        for (int k = 0; k < track.getTrackStates().size(); k++) {
            TrackState tsk = track.getTrackStates().get(k);
            System.out.println("  state[" + k + "] location=" + tsk.getLocation()
                    + " sameObjAsPerigeeLookup=" + (tsk == ts)
                    + " d0=" + tsk.getParameter(0) + " z0=" + tsk.getParameter(3));
        }
        TrackState ts0 = track.getTrackStates().get(0);
        System.out.println("  getTrackStates().get(0): d0=" + ts0.getParameter(0)
                + " phi0=" + ts0.getParameter(1) + " omega=" + ts0.getParameter(2)
                + " z0=" + ts0.getParameter(3) + " tanLambda=" + ts0.getParameter(4)
                + " bLocal=" + ts0.getBLocal() + " location=" + ts0.getLocation());
        System.out.println("  ts0==tsPerigee: " + (ts0 == ts));
    }
}
