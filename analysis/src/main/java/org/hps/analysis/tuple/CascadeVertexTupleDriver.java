package org.hps.analysis.tuple;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import hep.physics.vec.Hep3Vector;

import org.hps.recon.vertexing.BilliorVertex;
import org.lcsim.event.EventHeader;
import org.lcsim.event.LCRelation;
import org.lcsim.event.MCParticle;
import org.lcsim.event.ReconstructedParticle;
import org.lcsim.event.Track;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;

/**
 * Writes a flat ASCII ntuple of cascade (V0 + recoil-electron production-vertex) fit
 * quantities, for offline inspection/plotting of the {@code CascadeVertexer} output.
 * One row per cascade candidate. Header line is variable names joined by ":"; data
 * rows are tab-separated, matching the convention used by {@link TupleMaker#writeTuple}
 * (not reused directly here since that class requires a hardware trigger bank and other
 * DQM-specific setup that doesn't apply to this validation driver).
 */
public class CascadeVertexTupleDriver extends Driver {

    private static final List<String> VARIABLES = Arrays.asList(
            "run/I", "event/I",
            "cascadeVtxX/D", "cascadeVtxY/D", "cascadeVtxZ/D",
            "cascadeVtxXErr/D", "cascadeVtxYErr/D", "cascadeVtxZErr/D",
            "cascadeChi2/D", "cascadeNdf/I", "cascadeMass/D",
            "v0PX/D", "v0PY/D", "v0PZ/D",
            "v0UncPX/D", "v0UncPY/D", "v0UncPZ/D",
            "recoilPX/D", "recoilPY/D", "recoilPZ/D",
            "recoilUncPX/D", "recoilUncPY/D", "recoilUncPZ/D",
            "v0VtxX/D", "v0VtxY/D", "v0VtxZ/D",
            "v0VtxXErr/D", "v0VtxYErr/D", "v0VtxZErr/D", "v0Mass/D", "v0Chi2/D",
            "v0InputVtxX/D", "v0InputVtxY/D", "v0InputVtxZ/D",
            "v0InputVtxXErr/D", "v0InputVtxYErr/D", "v0InputVtxZErr/D", "v0InputMass/D", "v0InputChi2/D",
            "v0ProjX/D", "v0ProjY/D", "v0ProjXErr/D", "v0ProjYErr/D",
            "recoilProjX/D", "recoilProjY/D", "recoilProjXErr/D", "recoilProjYErr/D",
            "apMassMC/D", "apVtxXMC/D", "apVtxYMC/D", "apVtxZMC/D",
            "apOriginXMC/D", "apOriginYMC/D", "apOriginZMC/D",
            "v0EleTruthMatched/I", "v0PosTruthMatched/I", "v0BothTruthMatchedToAp/I",
            "recoilTruthMatched/I",
            "v0ElePurity/D", "v0PosPurity/D", "recoilPurity/D");

    private String cascadeVertexCandidatesColName = "CascadeVertexCandidates";
    private String mcParticlesColName = null;
    private String trackToMCParticleRelationsColName = null;
    private String tupleFile = null;
    private PrintWriter tupleWriter = null;

    public void setCascadeVertexCandidatesColName(String cascadeVertexCandidatesColName) {
        this.cascadeVertexCandidatesColName = cascadeVertexCandidatesColName;
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
            throw new RuntimeException("Could not open cascade vertex tuple file " + tupleFile, e);
        }
        tupleWriter.println(String.join(":", VARIABLES));
    }

    @Override
    public void process(EventHeader event) {
        if (tupleWriter == null || !event.hasCollection(ReconstructedParticle.class, cascadeVertexCandidatesColName)) {
            return;
        }

        Map<Track, MCParticle> trackToMC = new HashMap<Track, MCParticle>();
        Map<Track, Double> trackPurity = new HashMap<Track, Double>();
        if (trackToMCParticleRelationsColName != null
                && event.hasCollection(LCRelation.class, trackToMCParticleRelationsColName)) {
            for (LCRelation rel : event.get(LCRelation.class, trackToMCParticleRelationsColName)) {
                trackToMC.put((Track) rel.getFrom(), (MCParticle) rel.getTo());
                trackPurity.put((Track) rel.getFrom(), (double) rel.getWeight());
            }
        }

        MCParticle apMC = null;
        MCParticle eleMC = null;
        MCParticle posMC = null;
        MCParticle recoilMC = null;
        if (mcParticlesColName != null && event.hasCollection(MCParticle.class, mcParticlesColName)) {
            List<MCParticle> mcParticles = event.get(MCParticle.class, mcParticlesColName);
            for (MCParticle mcp : mcParticles) {
                if (mcp.getPDGID() == 622 && mcp.getDaughters().size() == 2) {
                    apMC = mcp;
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
            // Recoil electron convention (verified against real ap_pulser MC truth):
            // the A' (622) is its own top-level record with no parent, and the recoil
            // electron is the single PDGID-11 daughter of a separate top-level PDGID
            // 623 "reaction" particle -- the two top-level records are not linked to
            // each other, so the recoil cannot be found via the A''s parent chain.
            if (apMC != null) {
                for (MCParticle mcp : mcParticles) {
                    if (mcp.getPDGID() == 623) {
                        for (MCParticle daughter : mcp.getDaughters()) {
                            if (daughter.getPDGID() == 11) {
                                recoilMC = daughter;
                                break;
                            }
                        }
                        break;
                    }
                }
            }
        }

        List<ReconstructedParticle> cascadeCandidates = event.get(ReconstructedParticle.class, cascadeVertexCandidatesColName);
        for (ReconstructedParticle cascade : cascadeCandidates) {
            BilliorVertex cascadeVtx = (BilliorVertex) cascade.getStartVertex();
            ReconstructedParticle v0Particle = cascade.getParticles().get(0);
            ReconstructedParticle recoilElectron = cascade.getParticles().get(1);
            BilliorVertex v0Vtx = (BilliorVertex) v0Particle.getStartVertex();

            Hep3Vector cascadePos = cascadeVtx.getPosition();
            Hep3Vector pV0 = cascadeVtx.getFittedMomentum(0);
            Hep3Vector pRecoil = cascadeVtx.getFittedMomentum(1);
            Hep3Vector pV0Unc = v0Particle.getMomentum();
            Hep3Vector pRecoilUnc = recoilElectron.getMomentum();
            Double ndf = cascadeVtx.getCustomParameters().get("ndf");
            Hep3Vector v0Pos = v0Vtx.getPosition();

            List<ReconstructedParticle> v0Daughters = v0Particle.getParticles();
            ReconstructedParticle v0EleDaughter = v0Daughters.get(0).getCharge() < 0 ? v0Daughters.get(0) : v0Daughters.get(1);
            ReconstructedParticle v0PosDaughter = v0Daughters.get(0).getCharge() < 0 ? v0Daughters.get(1) : v0Daughters.get(0);
            boolean eleMatched = eleMC != null && eleMC.equals(trackToMC.get(v0EleDaughter.getTracks().get(0)));
            boolean posMatched = posMC != null && posMC.equals(trackToMC.get(v0PosDaughter.getTracks().get(0)));
            boolean recoilMatched = recoilMC != null && recoilMC.equals(trackToMC.get(recoilElectron.getTracks().get(0)));
            // apMC.getEndPoint() is unreliable for this generator sample: BaseMCParticle's
            // endpoint field defaults to (0,0,0) and is only ever set by an external writer
            // (e.g. Geant4 propagation), which doesn't apply to a generator-level, promptly-
            // decaying A'. Use a daughter's own production point instead -- daughters are
            // produced exactly at the parent's decay vertex (same convention already used in
            // APrimeMCAnalysisDriver).
            Hep3Vector apVtxMC = eleMC != null ? eleMC.getOrigin() : (posMC != null ? posMC.getOrigin() : null);
            // apMC.getOrigin() is ALSO contaminated for this sample: the .dat/LHE-to-stdhep
            // converters (DatFileToStdhepTVM/DatFileToStdhep/ConvertToStdhep) apply the
            // decay-length vertex shift to the A' record itself, not just its daughters, so
            // apMC.getOrigin() actually returns the decay vertex too. The recoil electron's
            // own vertex is never touched by that shift and still holds the true, unshifted
            // production point -- same convention already used in APrimeMCAnalysisDriver
            // (recoilMC.getOrigin() as the interaction/production point).
            Hep3Vector apOriginMC = recoilMC != null ? recoilMC.getOrigin() : null;

            Map<String, Double> row = new HashMap<String, Double>();
            row.put("run/I", (double) event.getRunNumber());
            row.put("event/I", (double) event.getEventNumber());
            row.put("cascadeVtxX/D", cascadePos.x());
            row.put("cascadeVtxY/D", cascadePos.y());
            row.put("cascadeVtxZ/D", cascadePos.z());
            row.put("cascadeVtxXErr/D", Math.sqrt(Math.abs(cascadeVtx.getCovMatrix().e(0, 0))));
            row.put("cascadeVtxYErr/D", Math.sqrt(Math.abs(cascadeVtx.getCovMatrix().e(1, 1))));
            row.put("cascadeVtxZErr/D", Math.sqrt(Math.abs(cascadeVtx.getCovMatrix().e(2, 2))));
            row.put("cascadeChi2/D", cascadeVtx.getChi2());
            row.put("cascadeNdf/I", ndf != null ? ndf : -9999.0);
            row.put("cascadeMass/D", cascadeVtx.getInvMass());
            row.put("v0PX/D", pV0.x());
            row.put("v0PY/D", pV0.y());
            row.put("v0PZ/D", pV0.z());
            row.put("v0UncPX/D", pV0Unc.x());
            row.put("v0UncPY/D", pV0Unc.y());
            row.put("v0UncPZ/D", pV0Unc.z());
            row.put("recoilPX/D", pRecoil.x());
            row.put("recoilPY/D", pRecoil.y());
            row.put("recoilPZ/D", pRecoil.z());
            row.put("recoilUncPX/D", pRecoilUnc.x());
            row.put("recoilUncPY/D", pRecoilUnc.y());
            row.put("recoilUncPZ/D", pRecoilUnc.z());
            row.put("v0VtxX/D", v0Pos.x());
            row.put("v0VtxY/D", v0Pos.y());
            row.put("v0VtxZ/D", v0Pos.z());
            row.put("v0VtxXErr/D", Math.sqrt(Math.abs(v0Vtx.getCovMatrix().e(0, 0))));
            row.put("v0VtxYErr/D", Math.sqrt(Math.abs(v0Vtx.getCovMatrix().e(1, 1))));
            row.put("v0VtxZErr/D", Math.sqrt(Math.abs(v0Vtx.getCovMatrix().e(2, 2))));
            row.put("v0Mass/D", v0Vtx.getInvMass());
            row.put("v0Chi2/D", v0Vtx.getChi2());
            Map<String, Double> cascadeParams = cascadeVtx.getCustomParameters();
            row.put("v0InputVtxX/D", cascadeParams.get("v0InputVtxX"));
            row.put("v0InputVtxY/D", cascadeParams.get("v0InputVtxY"));
            row.put("v0InputVtxZ/D", cascadeParams.get("v0InputVtxZ"));
            row.put("v0InputVtxXErr/D", cascadeParams.get("v0InputVtxXErr"));
            row.put("v0InputVtxYErr/D", cascadeParams.get("v0InputVtxYErr"));
            row.put("v0InputVtxZErr/D", cascadeParams.get("v0InputVtxZErr"));
            row.put("v0InputMass/D", cascadeParams.get("v0InputMass"));
            row.put("v0InputChi2/D", cascadeParams.get("v0InputChi2"));
            row.put("v0ProjX/D", cascadeParams.get("v0ProjX"));
            row.put("v0ProjY/D", cascadeParams.get("v0ProjY"));
            row.put("v0ProjXErr/D", cascadeParams.get("v0ProjXErr"));
            row.put("v0ProjYErr/D", cascadeParams.get("v0ProjYErr"));
            row.put("recoilProjX/D", cascadeParams.get("recoilProjX"));
            row.put("recoilProjY/D", cascadeParams.get("recoilProjY"));
            row.put("recoilProjXErr/D", cascadeParams.get("recoilProjXErr"));
            row.put("recoilProjYErr/D", cascadeParams.get("recoilProjYErr"));
            row.put("apMassMC/D", apMC != null ? apMC.getMass() : Double.NaN);
            row.put("apVtxXMC/D", apVtxMC != null ? apVtxMC.x() : Double.NaN);
            row.put("apVtxYMC/D", apVtxMC != null ? apVtxMC.y() : Double.NaN);
            row.put("apVtxZMC/D", apVtxMC != null ? apVtxMC.z() : Double.NaN);
            row.put("apOriginXMC/D", apOriginMC != null ? apOriginMC.x() : Double.NaN);
            row.put("apOriginYMC/D", apOriginMC != null ? apOriginMC.y() : Double.NaN);
            row.put("apOriginZMC/D", apOriginMC != null ? apOriginMC.z() : Double.NaN);
            row.put("v0EleTruthMatched/I", eleMatched ? 1.0 : 0.0);
            row.put("v0PosTruthMatched/I", posMatched ? 1.0 : 0.0);
            row.put("v0BothTruthMatchedToAp/I", eleMatched && posMatched ? 1.0 : 0.0);
            row.put("recoilTruthMatched/I", recoilMatched ? 1.0 : 0.0);
            row.put("v0ElePurity/D", trackPurity.get(v0EleDaughter.getTracks().get(0)));
            row.put("v0PosPurity/D", trackPurity.get(v0PosDaughter.getTracks().get(0)));
            row.put("recoilPurity/D", trackPurity.get(recoilElectron.getTracks().get(0)));

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
}
