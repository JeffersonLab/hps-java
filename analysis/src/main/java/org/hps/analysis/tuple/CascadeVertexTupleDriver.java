package org.hps.analysis.tuple;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import hep.physics.vec.Hep3Vector;

import org.hps.recon.vertexing.BilliorVertex;
import org.hps.recon.vertexing.CascadeVertexer;
import org.lcsim.event.EventHeader;
import org.lcsim.event.LCRelation;
import org.lcsim.event.MCParticle;
import org.lcsim.event.ReconstructedParticle;
import org.lcsim.event.Track;
import org.lcsim.event.Vertex;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;

/**
 * Writes a flat ASCII ntuple of cascade (V0 + recoil-electron production-vertex) fit
 * quantities, for offline inspection/plotting of {@link CascadeVertexer} output.
 * One row per cascade candidate. Header line is variable names joined by ":"; data
 * rows are tab-separated, matching the convention used by {@link TupleMaker#writeTuple}
 * (not reused directly here since that class requires a hardware trigger bank and other
 * DQM-specific setup that doesn't apply to this validation driver).
 */
public class CascadeVertexTupleDriver extends Driver {

    private static final double ELECTRON_MASS = 0.000511;

    private static final List<String> VARIABLES = Arrays.asList(
            "run/I", "event/I",
            "cascadeVtxX/D", "cascadeVtxY/D", "cascadeVtxZ/D",
            "cascadeVtxXErr/D", "cascadeVtxYErr/D", "cascadeVtxZErr/D",
            "cascadeChi2/D", "cascadeNdf/I", "cascadeMass/D",
            "v0PX/D", "v0PY/D", "v0PZ/D", "v0PXErr/D", "v0PYErr/D", "v0PZErr/D",
            "v0UncPX/D", "v0UncPY/D", "v0UncPZ/D", "v0UncMass/D",
            "recoilPX/D", "recoilPY/D", "recoilPZ/D", "recoilPXErr/D", "recoilPYErr/D", "recoilPZErr/D",
            "recoilUncPX/D", "recoilUncPY/D", "recoilUncPZ/D",
            "eleFitPX/D", "eleFitPY/D", "eleFitPZ/D", "eleFitPXErr/D", "eleFitPYErr/D", "eleFitPZErr/D",
            "posFitPX/D", "posFitPY/D", "posFitPZ/D", "posFitPXErr/D", "posFitPYErr/D", "posFitPZErr/D",
            "eleUncPX/D", "eleUncPY/D", "eleUncPZ/D", "posUncPX/D", "posUncPY/D", "posUncPZ/D",
            "v0VtxX/D", "v0VtxY/D", "v0VtxZ/D",
            "v0VtxXErr/D", "v0VtxYErr/D", "v0VtxZErr/D", "v0Mass/D", "v0Chi2/D",
            "v0InputVtxX/D", "v0InputVtxY/D", "v0InputVtxZ/D",
            "v0InputVtxXErr/D", "v0InputVtxYErr/D", "v0InputVtxZErr/D", "v0InputMass/D", "v0InputChi2/D",
            "v0ProjX/D", "v0ProjY/D", "v0ProjXErr/D", "v0ProjYErr/D",
            "recoilProjX/D", "recoilProjY/D", "recoilProjXErr/D", "recoilProjYErr/D",
            "apMassMC/D", "apVtxXMC/D", "apVtxYMC/D", "apVtxZMC/D",
            "apOriginXMC/D", "apOriginYMC/D", "apOriginZMC/D",
            "eleMomXMC/D", "eleMomYMC/D", "eleMomZMC/D",
            "posMomXMC/D", "posMomYMC/D", "posMomZMC/D",
            "recoilMomXMC/D", "recoilMomYMC/D", "recoilMomZMC/D",
            "v0EleTruthMatched/I", "v0PosTruthMatched/I", "v0BothTruthMatchedToAp/I",
            "recoilTruthMatched/I",
            "v0ElePurity/D", "v0PosPurity/D", "recoilPurity/D",
            "cascadeBeamMomConstrainedVtxX/D", "cascadeBeamMomConstrainedVtxY/D", "cascadeBeamMomConstrainedVtxZ/D",
            "cascadeBeamMomConstrainedVtxXErr/D", "cascadeBeamMomConstrainedVtxYErr/D", "cascadeBeamMomConstrainedVtxZErr/D",
            "cascadeBeamMomConstrainedChi2/D", "cascadeBeamMomConstrainedNdf/I", "cascadeBeamMomConstrainedMass/D",
            "cascadeBeamMomConstrainedV0PX/D", "cascadeBeamMomConstrainedV0PY/D", "cascadeBeamMomConstrainedV0PZ/D",
            "cascadeBeamMomConstrainedV0PXErr/D", "cascadeBeamMomConstrainedV0PYErr/D", "cascadeBeamMomConstrainedV0PZErr/D",
            "cascadeBeamMomConstrainedRecoilPX/D", "cascadeBeamMomConstrainedRecoilPY/D", "cascadeBeamMomConstrainedRecoilPZ/D",
            "cascadeBeamMomConstrainedRecoilPXErr/D", "cascadeBeamMomConstrainedRecoilPYErr/D", "cascadeBeamMomConstrainedRecoilPZErr/D",
            "cascadeBeamMomConstrainedElePX/D", "cascadeBeamMomConstrainedElePY/D", "cascadeBeamMomConstrainedElePZ/D",
            "cascadeBeamMomConstrainedElePXErr/D", "cascadeBeamMomConstrainedElePYErr/D", "cascadeBeamMomConstrainedElePZErr/D",
            "cascadeBeamMomConstrainedPosPX/D", "cascadeBeamMomConstrainedPosPY/D", "cascadeBeamMomConstrainedPosPZ/D",
            "cascadeBeamMomConstrainedPosPXErr/D", "cascadeBeamMomConstrainedPosPYErr/D", "cascadeBeamMomConstrainedPosPZErr/D",
            "cascadeBeamMomConstrainedV0VtxX/D", "cascadeBeamMomConstrainedV0VtxY/D", "cascadeBeamMomConstrainedV0VtxZ/D",
            "cascadeBeamMomConstrainedV0VtxXErr/D", "cascadeBeamMomConstrainedV0VtxYErr/D", "cascadeBeamMomConstrainedV0VtxZErr/D",
            "cascadeBeamMomConstrainedV0Mass/D", "cascadeBeamMomConstrainedV0Chi2/D",
            "cascadeBeamspotConstrainedVtxX/D", "cascadeBeamspotConstrainedVtxY/D", "cascadeBeamspotConstrainedVtxZ/D",
            "cascadeBeamspotConstrainedVtxXErr/D", "cascadeBeamspotConstrainedVtxYErr/D", "cascadeBeamspotConstrainedVtxZErr/D",
            "cascadeBeamspotConstrainedChi2/D", "cascadeBeamspotConstrainedNdf/I", "cascadeBeamspotConstrainedMass/D",
            "cascadeBeamspotConstrainedV0PX/D", "cascadeBeamspotConstrainedV0PY/D", "cascadeBeamspotConstrainedV0PZ/D",
            "cascadeBeamspotConstrainedV0PXErr/D", "cascadeBeamspotConstrainedV0PYErr/D", "cascadeBeamspotConstrainedV0PZErr/D",
            "cascadeBeamspotConstrainedRecoilPX/D", "cascadeBeamspotConstrainedRecoilPY/D", "cascadeBeamspotConstrainedRecoilPZ/D",
            "cascadeBeamspotConstrainedRecoilPXErr/D", "cascadeBeamspotConstrainedRecoilPYErr/D", "cascadeBeamspotConstrainedRecoilPZErr/D",
            "cascadeBeamspotConstrainedElePX/D", "cascadeBeamspotConstrainedElePY/D", "cascadeBeamspotConstrainedElePZ/D",
            "cascadeBeamspotConstrainedElePXErr/D", "cascadeBeamspotConstrainedElePYErr/D", "cascadeBeamspotConstrainedElePZErr/D",
            "cascadeBeamspotConstrainedPosPX/D", "cascadeBeamspotConstrainedPosPY/D", "cascadeBeamspotConstrainedPosPZ/D",
            "cascadeBeamspotConstrainedPosPXErr/D", "cascadeBeamspotConstrainedPosPYErr/D", "cascadeBeamspotConstrainedPosPZErr/D",
            "cascadeBeamspotConstrainedV0VtxX/D", "cascadeBeamspotConstrainedV0VtxY/D", "cascadeBeamspotConstrainedV0VtxZ/D",
            "cascadeBeamspotConstrainedV0VtxXErr/D", "cascadeBeamspotConstrainedV0VtxYErr/D", "cascadeBeamspotConstrainedV0VtxZErr/D",
            "cascadeBeamspotConstrainedV0Mass/D", "cascadeBeamspotConstrainedV0Chi2/D",
            "cascadeBothConstrainedVtxX/D", "cascadeBothConstrainedVtxY/D", "cascadeBothConstrainedVtxZ/D",
            "cascadeBothConstrainedVtxXErr/D", "cascadeBothConstrainedVtxYErr/D", "cascadeBothConstrainedVtxZErr/D",
            "cascadeBothConstrainedChi2/D", "cascadeBothConstrainedNdf/I", "cascadeBothConstrainedMass/D",
            "cascadeBothConstrainedV0PX/D", "cascadeBothConstrainedV0PY/D", "cascadeBothConstrainedV0PZ/D",
            "cascadeBothConstrainedV0PXErr/D", "cascadeBothConstrainedV0PYErr/D", "cascadeBothConstrainedV0PZErr/D",
            "cascadeBothConstrainedRecoilPX/D", "cascadeBothConstrainedRecoilPY/D", "cascadeBothConstrainedRecoilPZ/D",
            "cascadeBothConstrainedRecoilPXErr/D", "cascadeBothConstrainedRecoilPYErr/D", "cascadeBothConstrainedRecoilPZErr/D",
            "cascadeBothConstrainedElePX/D", "cascadeBothConstrainedElePY/D", "cascadeBothConstrainedElePZ/D",
            "cascadeBothConstrainedElePXErr/D", "cascadeBothConstrainedElePYErr/D", "cascadeBothConstrainedElePZErr/D",
            "cascadeBothConstrainedPosPX/D", "cascadeBothConstrainedPosPY/D", "cascadeBothConstrainedPosPZ/D",
            "cascadeBothConstrainedPosPXErr/D", "cascadeBothConstrainedPosPYErr/D", "cascadeBothConstrainedPosPZErr/D",
            "cascadeBothConstrainedV0VtxX/D", "cascadeBothConstrainedV0VtxY/D", "cascadeBothConstrainedV0VtxZ/D",
            "cascadeBothConstrainedV0VtxXErr/D", "cascadeBothConstrainedV0VtxYErr/D", "cascadeBothConstrainedV0VtxZErr/D",
            "cascadeBothConstrainedV0Mass/D", "cascadeBothConstrainedV0Chi2/D",
            "ntrackVtxX/D", "ntrackVtxY/D", "ntrackVtxZ/D",
            "ntrackVtxXErr/D", "ntrackVtxYErr/D", "ntrackVtxZErr/D",
            "ntrackChi2/D", "ntrackNdf/I", "ntrackMass/D",
            "ntrackElePX/D", "ntrackElePY/D", "ntrackElePZ/D",
            "ntrackElePXErr/D", "ntrackElePYErr/D", "ntrackElePZErr/D",
            "ntrackPosPX/D", "ntrackPosPY/D", "ntrackPosPZ/D",
            "ntrackPosPXErr/D", "ntrackPosPYErr/D", "ntrackPosPZErr/D",
            "ntrackRecoilPX/D", "ntrackRecoilPY/D", "ntrackRecoilPZ/D",
            "ntrackRecoilPXErr/D", "ntrackRecoilPYErr/D", "ntrackRecoilPZErr/D",
            "ntrackBeamMomConstrainedVtxX/D", "ntrackBeamMomConstrainedVtxY/D", "ntrackBeamMomConstrainedVtxZ/D",
            "ntrackBeamMomConstrainedVtxXErr/D", "ntrackBeamMomConstrainedVtxYErr/D", "ntrackBeamMomConstrainedVtxZErr/D",
            "ntrackBeamMomConstrainedChi2/D", "ntrackBeamMomConstrainedNdf/I", "ntrackBeamMomConstrainedMass/D",
            "ntrackBeamMomConstrainedElePX/D", "ntrackBeamMomConstrainedElePY/D", "ntrackBeamMomConstrainedElePZ/D",
            "ntrackBeamMomConstrainedElePXErr/D", "ntrackBeamMomConstrainedElePYErr/D", "ntrackBeamMomConstrainedElePZErr/D",
            "ntrackBeamMomConstrainedPosPX/D", "ntrackBeamMomConstrainedPosPY/D", "ntrackBeamMomConstrainedPosPZ/D",
            "ntrackBeamMomConstrainedPosPXErr/D", "ntrackBeamMomConstrainedPosPYErr/D", "ntrackBeamMomConstrainedPosPZErr/D",
            "ntrackBeamMomConstrainedRecoilPX/D", "ntrackBeamMomConstrainedRecoilPY/D", "ntrackBeamMomConstrainedRecoilPZ/D",
            "ntrackBeamMomConstrainedRecoilPXErr/D", "ntrackBeamMomConstrainedRecoilPYErr/D", "ntrackBeamMomConstrainedRecoilPZErr/D",
            "ntrackBeamspotConstrainedVtxX/D", "ntrackBeamspotConstrainedVtxY/D", "ntrackBeamspotConstrainedVtxZ/D",
            "ntrackBeamspotConstrainedVtxXErr/D", "ntrackBeamspotConstrainedVtxYErr/D", "ntrackBeamspotConstrainedVtxZErr/D",
            "ntrackBeamspotConstrainedChi2/D", "ntrackBeamspotConstrainedNdf/I", "ntrackBeamspotConstrainedMass/D",
            "ntrackBeamspotConstrainedElePX/D", "ntrackBeamspotConstrainedElePY/D", "ntrackBeamspotConstrainedElePZ/D",
            "ntrackBeamspotConstrainedElePXErr/D", "ntrackBeamspotConstrainedElePYErr/D", "ntrackBeamspotConstrainedElePZErr/D",
            "ntrackBeamspotConstrainedPosPX/D", "ntrackBeamspotConstrainedPosPY/D", "ntrackBeamspotConstrainedPosPZ/D",
            "ntrackBeamspotConstrainedPosPXErr/D", "ntrackBeamspotConstrainedPosPYErr/D", "ntrackBeamspotConstrainedPosPZErr/D",
            "ntrackBeamspotConstrainedRecoilPX/D", "ntrackBeamspotConstrainedRecoilPY/D", "ntrackBeamspotConstrainedRecoilPZ/D",
            "ntrackBeamspotConstrainedRecoilPXErr/D", "ntrackBeamspotConstrainedRecoilPYErr/D", "ntrackBeamspotConstrainedRecoilPZErr/D",
            "ntrackBothConstrainedVtxX/D", "ntrackBothConstrainedVtxY/D", "ntrackBothConstrainedVtxZ/D",
            "ntrackBothConstrainedVtxXErr/D", "ntrackBothConstrainedVtxYErr/D", "ntrackBothConstrainedVtxZErr/D",
            "ntrackBothConstrainedChi2/D", "ntrackBothConstrainedNdf/I", "ntrackBothConstrainedMass/D",
            "ntrackBothConstrainedElePX/D", "ntrackBothConstrainedElePY/D", "ntrackBothConstrainedElePZ/D",
            "ntrackBothConstrainedElePXErr/D", "ntrackBothConstrainedElePYErr/D", "ntrackBothConstrainedElePZErr/D",
            "ntrackBothConstrainedPosPX/D", "ntrackBothConstrainedPosPY/D", "ntrackBothConstrainedPosPZ/D",
            "ntrackBothConstrainedPosPXErr/D", "ntrackBothConstrainedPosPYErr/D", "ntrackBothConstrainedPosPZErr/D",
            "ntrackBothConstrainedRecoilPX/D", "ntrackBothConstrainedRecoilPY/D", "ntrackBothConstrainedRecoilPZ/D",
            "ntrackBothConstrainedRecoilPXErr/D", "ntrackBothConstrainedRecoilPYErr/D", "ntrackBothConstrainedRecoilPZErr/D");

    private String cascadeVertexCandidatesColName = "CascadeVertexCandidates";
    // Beam-momentum-constrained refit of cascadeVertexCandidatesColName, index-aligned with
    // it (see ReconParticleDriver#findCascadeVertices / CascadeVertexer#placeholderCascade).
    // Null/off by default, matching cascadeVertexCandidatesColName's own opt-in convention.
    private String cascadeVertexCandidatesBeamConstrainedColName = null;
    // Beamspot-position-constrained refit of cascadeVertexCandidatesColName, index-aligned with
    // it. Null/off by default, matching cascadeVertexCandidatesColName's own opt-in convention.
    private String cascadeVertexCandidatesBeamspotConstrainedColName = null;
    // Refit of cascadeVertexCandidatesColName with both the beamspot-position and
    // beam-momentum constraints applied together, index-aligned with it. Null/off by default.
    private String cascadeVertexCandidatesBothConstrainedColName = null;
    // Single-common-vertex ("N-track") fit of the same three tracks as
    // cascadeVertexCandidatesColName, index-aligned with it (see
    // ReconParticleDriver#findCascadeVertices / NTrackVertexer). Null/off by default,
    // matching cascadeVertexCandidatesColName's own opt-in convention.
    private String ntrackVertexCandidatesColName = null;
    // Beam-momentum-constrained refit of ntrackVertexCandidatesColName, index-aligned with it.
    private String ntrackVertexCandidatesBeamConstrainedColName = null;
    // Beamspot-position-constrained refit of ntrackVertexCandidatesColName, index-aligned with
    // it (see ReconParticleDriver#findCascadeVertices / NTrackVertexer#fitVertexBeamspotConstrained).
    // Null/off by default, matching ntrackVertexCandidatesColName's own opt-in convention.
    private String ntrackVertexCandidatesBeamspotConstrainedColName = null;
    // Refit of ntrackVertexCandidatesColName with both the beamspot-position and
    // beam-momentum constraints applied together, index-aligned with it. Null/off by default.
    private String ntrackVertexCandidatesBothConstrainedColName = null;
    private String mcParticlesColName = null;
    private String trackToMCParticleRelationsColName = null;
    private String tupleFile = null;
    private PrintWriter tupleWriter = null;

    public void setCascadeVertexCandidatesColName(String cascadeVertexCandidatesColName) {
        this.cascadeVertexCandidatesColName = cascadeVertexCandidatesColName;
    }

    public void setCascadeVertexCandidatesBeamConstrainedColName(String cascadeVertexCandidatesBeamConstrainedColName) {
        this.cascadeVertexCandidatesBeamConstrainedColName = cascadeVertexCandidatesBeamConstrainedColName;
    }

    public void setCascadeVertexCandidatesBeamspotConstrainedColName(String cascadeVertexCandidatesBeamspotConstrainedColName) {
        this.cascadeVertexCandidatesBeamspotConstrainedColName = cascadeVertexCandidatesBeamspotConstrainedColName;
    }

    public void setCascadeVertexCandidatesBothConstrainedColName(String cascadeVertexCandidatesBothConstrainedColName) {
        this.cascadeVertexCandidatesBothConstrainedColName = cascadeVertexCandidatesBothConstrainedColName;
    }

    public void setNtrackVertexCandidatesColName(String ntrackVertexCandidatesColName) {
        this.ntrackVertexCandidatesColName = ntrackVertexCandidatesColName;
    }

    public void setNtrackVertexCandidatesBeamConstrainedColName(String ntrackVertexCandidatesBeamConstrainedColName) {
        this.ntrackVertexCandidatesBeamConstrainedColName = ntrackVertexCandidatesBeamConstrainedColName;
    }

    public void setNtrackVertexCandidatesBeamspotConstrainedColName(String ntrackVertexCandidatesBeamspotConstrainedColName) {
        this.ntrackVertexCandidatesBeamspotConstrainedColName = ntrackVertexCandidatesBeamspotConstrainedColName;
    }

    public void setNtrackVertexCandidatesBothConstrainedColName(String ntrackVertexCandidatesBothConstrainedColName) {
        this.ntrackVertexCandidatesBothConstrainedColName = ntrackVertexCandidatesBothConstrainedColName;
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
        // For trident MC (no PDGID-622 A' present -- see below), the two truth electrons
        // are indistinguishable at truth level; which one plays "ele" (paired with posMC to
        // form the V0) vs "recoil" is only decidable per-candidate, once we know which
        // reconstructed track a given cascade candidate assigned to v0EleDaughter. These two
        // hold the pair of truth electrons pending that per-candidate disambiguation below.
        MCParticle tridentEle1MC = null;
        MCParticle tridentEle2MC = null;
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
            } else {
                // Trident convention (verified against tritrig_pulser MC truth, same scheme
                // already used by NTrackVertexComparisonTupleDriver): with no PDGID-622 A'
                // present, PDGID 623 is instead the trident "reaction" pseudo-particle itself,
                // with 3 direct daughters (2 e- + 1 e+) from a single common production
                // vertex -- architecturally different from the A' sample's 623 (a separate,
                // unrelated single-daughter recoil-electron record).
                for (MCParticle mcp : mcParticles) {
                    if (mcp.getPDGID() == 623) {
                        List<MCParticle> daughters = mcp.getDaughters();
                        if (daughters.size() == 3) {
                            MCParticle e1 = null;
                            MCParticle e2 = null;
                            MCParticle p1 = null;
                            for (MCParticle daughter : daughters) {
                                if (daughter.getPDGID() == 11) {
                                    if (e1 == null) {
                                        e1 = daughter;
                                    } else {
                                        e2 = daughter;
                                    }
                                } else if (daughter.getPDGID() == -11) {
                                    p1 = daughter;
                                }
                            }
                            if (e1 != null && e2 != null && p1 != null) {
                                tridentEle1MC = e1;
                                tridentEle2MC = e2;
                                posMC = p1;
                            }
                        }
                        break;
                    }
                }
            }
        }

        List<ReconstructedParticle> cascadeCandidates = event.get(ReconstructedParticle.class, cascadeVertexCandidatesColName);
        List<ReconstructedParticle> bcCandidates = null;
        if (cascadeVertexCandidatesBeamConstrainedColName != null
                && event.hasCollection(ReconstructedParticle.class, cascadeVertexCandidatesBeamConstrainedColName)) {
            bcCandidates = event.get(ReconstructedParticle.class, cascadeVertexCandidatesBeamConstrainedColName);
        }
        List<ReconstructedParticle> bsCandidates = null;
        if (cascadeVertexCandidatesBeamspotConstrainedColName != null
                && event.hasCollection(ReconstructedParticle.class, cascadeVertexCandidatesBeamspotConstrainedColName)) {
            bsCandidates = event.get(ReconstructedParticle.class, cascadeVertexCandidatesBeamspotConstrainedColName);
        }
        List<ReconstructedParticle> bothCandidates = null;
        if (cascadeVertexCandidatesBothConstrainedColName != null
                && event.hasCollection(ReconstructedParticle.class, cascadeVertexCandidatesBothConstrainedColName)) {
            bothCandidates = event.get(ReconstructedParticle.class, cascadeVertexCandidatesBothConstrainedColName);
        }
        List<Vertex> ntrackCandidates = null;
        if (ntrackVertexCandidatesColName != null
                && event.hasCollection(Vertex.class, ntrackVertexCandidatesColName)) {
            ntrackCandidates = event.get(Vertex.class, ntrackVertexCandidatesColName);
        }
        List<Vertex> ntrackBcCandidates = null;
        if (ntrackVertexCandidatesBeamConstrainedColName != null
                && event.hasCollection(Vertex.class, ntrackVertexCandidatesBeamConstrainedColName)) {
            ntrackBcCandidates = event.get(Vertex.class, ntrackVertexCandidatesBeamConstrainedColName);
        }
        List<Vertex> ntrackBscCandidates = null;
        if (ntrackVertexCandidatesBeamspotConstrainedColName != null
                && event.hasCollection(Vertex.class, ntrackVertexCandidatesBeamspotConstrainedColName)) {
            ntrackBscCandidates = event.get(Vertex.class, ntrackVertexCandidatesBeamspotConstrainedColName);
        }
        List<Vertex> ntrackBothCandidates = null;
        if (ntrackVertexCandidatesBothConstrainedColName != null
                && event.hasCollection(Vertex.class, ntrackVertexCandidatesBothConstrainedColName)) {
            ntrackBothCandidates = event.get(Vertex.class, ntrackVertexCandidatesBothConstrainedColName);
        }
        for (int candidateIndex = 0; candidateIndex < cascadeCandidates.size(); candidateIndex++) {
            ReconstructedParticle cascade = cascadeCandidates.get(candidateIndex);
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
            // Per-daughter fitted momenta/errors from the two vertex fits (v0Vtx: e-/e+,
            // index 0/1 by construction -- see CascadeVertexer.fit's charge-sorted
            // eleDaughter/posDaughter; cascadeVtx: V0(combined)/recoil, index 0/1). Errors
            // are null (-> NaN -> -9999 sentinel) unless the producing vertexer populated
            // the corresponding track-momentum covariance.
            Hep3Vector eleFitP = v0Vtx.getFittedMomentum(0);
            Hep3Vector posFitP = v0Vtx.getFittedMomentum(1);
            Hep3Vector eleFitPErr = v0Vtx.getFittedMomentumError(0);
            Hep3Vector posFitPErr = v0Vtx.getFittedMomentumError(1);
            Hep3Vector v0PErr = cascadeVtx.getFittedMomentumError(0);
            Hep3Vector recoilPErr = cascadeVtx.getFittedMomentumError(1);

            List<ReconstructedParticle> v0Daughters = v0Particle.getParticles();
            ReconstructedParticle v0EleDaughter = v0Daughters.get(0).getCharge() < 0 ? v0Daughters.get(0) : v0Daughters.get(1);
            ReconstructedParticle v0PosDaughter = v0Daughters.get(0).getCharge() < 0 ? v0Daughters.get(1) : v0Daughters.get(0);
            // Trident truth-electron disambiguation: with no PDGID-622 A' present, the two
            // truth electrons (tridentEle1MC/tridentEle2MC) are only distinguishable as
            // "ele" (paired with posMC to form the V0) vs "recoil" via which reconstructed
            // track this candidate assigned to v0EleDaughter -- whichever truth electron that
            // track is matched to (via the real LCRelation-based trackToMC map, not an
            // arbitrary energy-ranking) becomes eleMC for this candidate; the other becomes
            // recoilMC. Left null (unmatched) if v0EleDaughter's track matches neither.
            if (apMC == null && tridentEle1MC != null && tridentEle2MC != null) {
                MCParticle v0EleTruth = trackToMC.get(v0EleDaughter.getTracks().get(0));
                if (v0EleTruth == tridentEle1MC) {
                    eleMC = tridentEle1MC;
                    recoilMC = tridentEle2MC;
                } else if (v0EleTruth == tridentEle2MC) {
                    eleMC = tridentEle2MC;
                    recoilMC = tridentEle1MC;
                } else {
                    eleMC = null;
                    recoilMC = null;
                }
            }
            Hep3Vector eleMomMC = eleMC != null ? eleMC.getMomentum() : null;
            Hep3Vector posMomMC = posMC != null ? posMC.getMomentum() : null;
            Hep3Vector recoilMomMC = recoilMC != null ? recoilMC.getMomentum() : null;
            // Raw, pre-vertex-fit daughter momenta and their invariant mass (mirrors the
            // existing v0Unc/recoilUnc "raw reco" convention above, but per-daughter and
            // as a mass rather than just a momentum sum). Computed directly from the raw
            // momenta under the electron-mass assumption (same ELECTRON_MASS value used in
            // CascadeVertexer/NTrackVertexer/TridentAnalysis) rather than
            // via ReconstructedParticle.asFourVector(), since that relies on whatever mass the
            // upstream particle-builder happened to assign each daughter.
            Hep3Vector eleUncP = v0EleDaughter.getMomentum();
            Hep3Vector posUncP = v0PosDaughter.getMomentum();
            double eEleUnc = Math.sqrt(eleUncP.magnitudeSquared() + ELECTRON_MASS * ELECTRON_MASS);
            double ePosUnc = Math.sqrt(posUncP.magnitudeSquared() + ELECTRON_MASS * ELECTRON_MASS);
            double v0UncPxSum = eleUncP.x() + posUncP.x();
            double v0UncPySum = eleUncP.y() + posUncP.y();
            double v0UncPzSum = eleUncP.z() + posUncP.z();
            double v0UncMass = Math.sqrt(Math.max(0.0, (eEleUnc + ePosUnc) * (eEleUnc + ePosUnc)
                    - v0UncPxSum * v0UncPxSum - v0UncPySum * v0UncPySum - v0UncPzSum * v0UncPzSum));
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
            // (recoilMC.getOrigin() as the interaction/production point). For trident (no
            // A'), all 3 daughters share one common production vertex and getOrigin() is
            // never contaminated by that converter, so apVtxMC/apOriginMC come out identical
            // -- expected, not a bug.
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
            row.put("v0PXErr/D", v0PErr != null ? v0PErr.x() : Double.NaN);
            row.put("v0PYErr/D", v0PErr != null ? v0PErr.y() : Double.NaN);
            row.put("v0PZErr/D", v0PErr != null ? v0PErr.z() : Double.NaN);
            row.put("v0UncPX/D", pV0Unc.x());
            row.put("v0UncPY/D", pV0Unc.y());
            row.put("v0UncPZ/D", pV0Unc.z());
            row.put("v0UncMass/D", v0UncMass);
            row.put("recoilPX/D", pRecoil.x());
            row.put("recoilPY/D", pRecoil.y());
            row.put("recoilPZ/D", pRecoil.z());
            row.put("recoilPXErr/D", recoilPErr != null ? recoilPErr.x() : Double.NaN);
            row.put("recoilPYErr/D", recoilPErr != null ? recoilPErr.y() : Double.NaN);
            row.put("recoilPZErr/D", recoilPErr != null ? recoilPErr.z() : Double.NaN);
            row.put("recoilUncPX/D", pRecoilUnc.x());
            row.put("recoilUncPY/D", pRecoilUnc.y());
            row.put("recoilUncPZ/D", pRecoilUnc.z());
            row.put("eleFitPX/D", eleFitP != null ? eleFitP.x() : Double.NaN);
            row.put("eleFitPY/D", eleFitP != null ? eleFitP.y() : Double.NaN);
            row.put("eleFitPZ/D", eleFitP != null ? eleFitP.z() : Double.NaN);
            row.put("eleFitPXErr/D", eleFitPErr != null ? eleFitPErr.x() : Double.NaN);
            row.put("eleFitPYErr/D", eleFitPErr != null ? eleFitPErr.y() : Double.NaN);
            row.put("eleFitPZErr/D", eleFitPErr != null ? eleFitPErr.z() : Double.NaN);
            row.put("posFitPX/D", posFitP != null ? posFitP.x() : Double.NaN);
            row.put("posFitPY/D", posFitP != null ? posFitP.y() : Double.NaN);
            row.put("posFitPZ/D", posFitP != null ? posFitP.z() : Double.NaN);
            row.put("posFitPXErr/D", posFitPErr != null ? posFitPErr.x() : Double.NaN);
            row.put("posFitPYErr/D", posFitPErr != null ? posFitPErr.y() : Double.NaN);
            row.put("posFitPZErr/D", posFitPErr != null ? posFitPErr.z() : Double.NaN);
            row.put("eleUncPX/D", eleUncP.x());
            row.put("eleUncPY/D", eleUncP.y());
            row.put("eleUncPZ/D", eleUncP.z());
            row.put("posUncPX/D", posUncP.x());
            row.put("posUncPY/D", posUncP.y());
            row.put("posUncPZ/D", posUncP.z());
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
            row.put("eleMomXMC/D", eleMomMC != null ? eleMomMC.x() : Double.NaN);
            row.put("eleMomYMC/D", eleMomMC != null ? eleMomMC.y() : Double.NaN);
            row.put("eleMomZMC/D", eleMomMC != null ? eleMomMC.z() : Double.NaN);
            row.put("posMomXMC/D", posMomMC != null ? posMomMC.x() : Double.NaN);
            row.put("posMomYMC/D", posMomMC != null ? posMomMC.y() : Double.NaN);
            row.put("posMomZMC/D", posMomMC != null ? posMomMC.z() : Double.NaN);
            row.put("recoilMomXMC/D", recoilMomMC != null ? recoilMomMC.x() : Double.NaN);
            row.put("recoilMomYMC/D", recoilMomMC != null ? recoilMomMC.y() : Double.NaN);
            row.put("recoilMomZMC/D", recoilMomMC != null ? recoilMomMC.z() : Double.NaN);
            row.put("v0EleTruthMatched/I", eleMatched ? 1.0 : 0.0);
            row.put("v0PosTruthMatched/I", posMatched ? 1.0 : 0.0);
            row.put("v0BothTruthMatchedToAp/I", eleMatched && posMatched ? 1.0 : 0.0);
            row.put("recoilTruthMatched/I", recoilMatched ? 1.0 : 0.0);
            row.put("v0ElePurity/D", trackPurity.get(v0EleDaughter.getTracks().get(0)));
            row.put("v0PosPurity/D", trackPurity.get(v0PosDaughter.getTracks().get(0)));
            row.put("recoilPurity/D", trackPurity.get(recoilElectron.getTracks().get(0)));

            if (bcCandidates != null) {
                ReconstructedParticle bcCascade = bcCandidates.get(candidateIndex);
                BilliorVertex bcVtxCheck = (BilliorVertex) bcCascade.getStartVertex();
                Double bcNdfCheck = bcVtxCheck.getCustomParameters().get("ndf");
                boolean isPlaceholder = bcNdfCheck == null || bcNdfCheck == -9999.0;
                if (!isPlaceholder) {
                    BilliorVertex bcVtx = bcVtxCheck;
                    ReconstructedParticle bcV0Particle = bcCascade.getParticles().get(0);
                    BilliorVertex bcV0Vtx = (BilliorVertex) bcV0Particle.getStartVertex();
                    Hep3Vector bcPos = bcVtx.getPosition();
                    Hep3Vector bcV0P = bcVtx.getFittedMomentum(0);
                    Hep3Vector bcRecoilP = bcVtx.getFittedMomentum(1);
                    Hep3Vector bcV0PErr = bcVtx.getFittedMomentumError(0);
                    Hep3Vector bcRecoilPErr = bcVtx.getFittedMomentumError(1);
                    Hep3Vector bcEleFitP = bcV0Vtx.getFittedMomentum(0);
                    Hep3Vector bcPosFitP = bcV0Vtx.getFittedMomentum(1);
                    Hep3Vector bcEleFitPErr = bcV0Vtx.getFittedMomentumError(0);
                    Hep3Vector bcPosFitPErr = bcV0Vtx.getFittedMomentumError(1);
                    Double bcNdf = bcVtx.getCustomParameters().get("ndf");
                    Hep3Vector bcV0Pos = bcV0Vtx.getPosition();
                    row.put("cascadeBeamMomConstrainedVtxX/D", bcPos.x());
                    row.put("cascadeBeamMomConstrainedVtxY/D", bcPos.y());
                    row.put("cascadeBeamMomConstrainedVtxZ/D", bcPos.z());
                    row.put("cascadeBeamMomConstrainedVtxXErr/D", Math.sqrt(Math.abs(bcVtx.getCovMatrix().e(0, 0))));
                    row.put("cascadeBeamMomConstrainedVtxYErr/D", Math.sqrt(Math.abs(bcVtx.getCovMatrix().e(1, 1))));
                    row.put("cascadeBeamMomConstrainedVtxZErr/D", Math.sqrt(Math.abs(bcVtx.getCovMatrix().e(2, 2))));
                    row.put("cascadeBeamMomConstrainedChi2/D", bcVtx.getChi2());
                    row.put("cascadeBeamMomConstrainedNdf/I", bcNdf != null ? bcNdf : -9999.0);
                    row.put("cascadeBeamMomConstrainedMass/D", bcVtx.getInvMass());
                    row.put("cascadeBeamMomConstrainedV0PX/D", bcV0P.x());
                    row.put("cascadeBeamMomConstrainedV0PY/D", bcV0P.y());
                    row.put("cascadeBeamMomConstrainedV0PZ/D", bcV0P.z());
                    row.put("cascadeBeamMomConstrainedV0PXErr/D", bcV0PErr != null ? bcV0PErr.x() : Double.NaN);
                    row.put("cascadeBeamMomConstrainedV0PYErr/D", bcV0PErr != null ? bcV0PErr.y() : Double.NaN);
                    row.put("cascadeBeamMomConstrainedV0PZErr/D", bcV0PErr != null ? bcV0PErr.z() : Double.NaN);
                    row.put("cascadeBeamMomConstrainedRecoilPX/D", bcRecoilP.x());
                    row.put("cascadeBeamMomConstrainedRecoilPY/D", bcRecoilP.y());
                    row.put("cascadeBeamMomConstrainedRecoilPZ/D", bcRecoilP.z());
                    row.put("cascadeBeamMomConstrainedRecoilPXErr/D", bcRecoilPErr != null ? bcRecoilPErr.x() : Double.NaN);
                    row.put("cascadeBeamMomConstrainedRecoilPYErr/D", bcRecoilPErr != null ? bcRecoilPErr.y() : Double.NaN);
                    row.put("cascadeBeamMomConstrainedRecoilPZErr/D", bcRecoilPErr != null ? bcRecoilPErr.z() : Double.NaN);
                    row.put("cascadeBeamMomConstrainedElePX/D", bcEleFitP != null ? bcEleFitP.x() : Double.NaN);
                    row.put("cascadeBeamMomConstrainedElePY/D", bcEleFitP != null ? bcEleFitP.y() : Double.NaN);
                    row.put("cascadeBeamMomConstrainedElePZ/D", bcEleFitP != null ? bcEleFitP.z() : Double.NaN);
                    row.put("cascadeBeamMomConstrainedElePXErr/D", bcEleFitPErr != null ? bcEleFitPErr.x() : Double.NaN);
                    row.put("cascadeBeamMomConstrainedElePYErr/D", bcEleFitPErr != null ? bcEleFitPErr.y() : Double.NaN);
                    row.put("cascadeBeamMomConstrainedElePZErr/D", bcEleFitPErr != null ? bcEleFitPErr.z() : Double.NaN);
                    row.put("cascadeBeamMomConstrainedPosPX/D", bcPosFitP != null ? bcPosFitP.x() : Double.NaN);
                    row.put("cascadeBeamMomConstrainedPosPY/D", bcPosFitP != null ? bcPosFitP.y() : Double.NaN);
                    row.put("cascadeBeamMomConstrainedPosPZ/D", bcPosFitP != null ? bcPosFitP.z() : Double.NaN);
                    row.put("cascadeBeamMomConstrainedPosPXErr/D", bcPosFitPErr != null ? bcPosFitPErr.x() : Double.NaN);
                    row.put("cascadeBeamMomConstrainedPosPYErr/D", bcPosFitPErr != null ? bcPosFitPErr.y() : Double.NaN);
                    row.put("cascadeBeamMomConstrainedPosPZErr/D", bcPosFitPErr != null ? bcPosFitPErr.z() : Double.NaN);
                    row.put("cascadeBeamMomConstrainedV0VtxX/D", bcV0Pos.x());
                    row.put("cascadeBeamMomConstrainedV0VtxY/D", bcV0Pos.y());
                    row.put("cascadeBeamMomConstrainedV0VtxZ/D", bcV0Pos.z());
                    row.put("cascadeBeamMomConstrainedV0VtxXErr/D", Math.sqrt(Math.abs(bcV0Vtx.getCovMatrix().e(0, 0))));
                    row.put("cascadeBeamMomConstrainedV0VtxYErr/D", Math.sqrt(Math.abs(bcV0Vtx.getCovMatrix().e(1, 1))));
                    row.put("cascadeBeamMomConstrainedV0VtxZErr/D", Math.sqrt(Math.abs(bcV0Vtx.getCovMatrix().e(2, 2))));
                    row.put("cascadeBeamMomConstrainedV0Mass/D", bcV0Vtx.getInvMass());
                    row.put("cascadeBeamMomConstrainedV0Chi2/D", bcV0Vtx.getChi2());
                }
            }

            if (bsCandidates != null) {
                ReconstructedParticle bsCascade = bsCandidates.get(candidateIndex);
                BilliorVertex bsVtxCheck = (BilliorVertex) bsCascade.getStartVertex();
                Double bsNdfCheck = bsVtxCheck.getCustomParameters().get("ndf");
                boolean isPlaceholder = bsNdfCheck == null || bsNdfCheck == -9999.0;
                if (!isPlaceholder) {
                    BilliorVertex bsVtx = bsVtxCheck;
                    ReconstructedParticle bsV0Particle = bsCascade.getParticles().get(0);
                    BilliorVertex bsV0Vtx = (BilliorVertex) bsV0Particle.getStartVertex();
                    Hep3Vector bsPos = bsVtx.getPosition();
                    Hep3Vector bsV0P = bsVtx.getFittedMomentum(0);
                    Hep3Vector bsRecoilP = bsVtx.getFittedMomentum(1);
                    Hep3Vector bsV0PErr = bsVtx.getFittedMomentumError(0);
                    Hep3Vector bsRecoilPErr = bsVtx.getFittedMomentumError(1);
                    Hep3Vector bsEleFitP = bsV0Vtx.getFittedMomentum(0);
                    Hep3Vector bsPosFitP = bsV0Vtx.getFittedMomentum(1);
                    Hep3Vector bsEleFitPErr = bsV0Vtx.getFittedMomentumError(0);
                    Hep3Vector bsPosFitPErr = bsV0Vtx.getFittedMomentumError(1);
                    Double bsNdf = bsVtx.getCustomParameters().get("ndf");
                    Hep3Vector bsV0Pos = bsV0Vtx.getPosition();
                    row.put("cascadeBeamspotConstrainedVtxX/D", bsPos.x());
                    row.put("cascadeBeamspotConstrainedVtxY/D", bsPos.y());
                    row.put("cascadeBeamspotConstrainedVtxZ/D", bsPos.z());
                    row.put("cascadeBeamspotConstrainedVtxXErr/D", Math.sqrt(Math.abs(bsVtx.getCovMatrix().e(0, 0))));
                    row.put("cascadeBeamspotConstrainedVtxYErr/D", Math.sqrt(Math.abs(bsVtx.getCovMatrix().e(1, 1))));
                    row.put("cascadeBeamspotConstrainedVtxZErr/D", Math.sqrt(Math.abs(bsVtx.getCovMatrix().e(2, 2))));
                    row.put("cascadeBeamspotConstrainedChi2/D", bsVtx.getChi2());
                    row.put("cascadeBeamspotConstrainedNdf/I", bsNdf != null ? bsNdf : -9999.0);
                    row.put("cascadeBeamspotConstrainedMass/D", bsVtx.getInvMass());
                    row.put("cascadeBeamspotConstrainedV0PX/D", bsV0P.x());
                    row.put("cascadeBeamspotConstrainedV0PY/D", bsV0P.y());
                    row.put("cascadeBeamspotConstrainedV0PZ/D", bsV0P.z());
                    row.put("cascadeBeamspotConstrainedV0PXErr/D", bsV0PErr != null ? bsV0PErr.x() : Double.NaN);
                    row.put("cascadeBeamspotConstrainedV0PYErr/D", bsV0PErr != null ? bsV0PErr.y() : Double.NaN);
                    row.put("cascadeBeamspotConstrainedV0PZErr/D", bsV0PErr != null ? bsV0PErr.z() : Double.NaN);
                    row.put("cascadeBeamspotConstrainedRecoilPX/D", bsRecoilP.x());
                    row.put("cascadeBeamspotConstrainedRecoilPY/D", bsRecoilP.y());
                    row.put("cascadeBeamspotConstrainedRecoilPZ/D", bsRecoilP.z());
                    row.put("cascadeBeamspotConstrainedRecoilPXErr/D", bsRecoilPErr != null ? bsRecoilPErr.x() : Double.NaN);
                    row.put("cascadeBeamspotConstrainedRecoilPYErr/D", bsRecoilPErr != null ? bsRecoilPErr.y() : Double.NaN);
                    row.put("cascadeBeamspotConstrainedRecoilPZErr/D", bsRecoilPErr != null ? bsRecoilPErr.z() : Double.NaN);
                    row.put("cascadeBeamspotConstrainedElePX/D", bsEleFitP != null ? bsEleFitP.x() : Double.NaN);
                    row.put("cascadeBeamspotConstrainedElePY/D", bsEleFitP != null ? bsEleFitP.y() : Double.NaN);
                    row.put("cascadeBeamspotConstrainedElePZ/D", bsEleFitP != null ? bsEleFitP.z() : Double.NaN);
                    row.put("cascadeBeamspotConstrainedElePXErr/D", bsEleFitPErr != null ? bsEleFitPErr.x() : Double.NaN);
                    row.put("cascadeBeamspotConstrainedElePYErr/D", bsEleFitPErr != null ? bsEleFitPErr.y() : Double.NaN);
                    row.put("cascadeBeamspotConstrainedElePZErr/D", bsEleFitPErr != null ? bsEleFitPErr.z() : Double.NaN);
                    row.put("cascadeBeamspotConstrainedPosPX/D", bsPosFitP != null ? bsPosFitP.x() : Double.NaN);
                    row.put("cascadeBeamspotConstrainedPosPY/D", bsPosFitP != null ? bsPosFitP.y() : Double.NaN);
                    row.put("cascadeBeamspotConstrainedPosPZ/D", bsPosFitP != null ? bsPosFitP.z() : Double.NaN);
                    row.put("cascadeBeamspotConstrainedPosPXErr/D", bsPosFitPErr != null ? bsPosFitPErr.x() : Double.NaN);
                    row.put("cascadeBeamspotConstrainedPosPYErr/D", bsPosFitPErr != null ? bsPosFitPErr.y() : Double.NaN);
                    row.put("cascadeBeamspotConstrainedPosPZErr/D", bsPosFitPErr != null ? bsPosFitPErr.z() : Double.NaN);
                    row.put("cascadeBeamspotConstrainedV0VtxX/D", bsV0Pos.x());
                    row.put("cascadeBeamspotConstrainedV0VtxY/D", bsV0Pos.y());
                    row.put("cascadeBeamspotConstrainedV0VtxZ/D", bsV0Pos.z());
                    row.put("cascadeBeamspotConstrainedV0VtxXErr/D", Math.sqrt(Math.abs(bsV0Vtx.getCovMatrix().e(0, 0))));
                    row.put("cascadeBeamspotConstrainedV0VtxYErr/D", Math.sqrt(Math.abs(bsV0Vtx.getCovMatrix().e(1, 1))));
                    row.put("cascadeBeamspotConstrainedV0VtxZErr/D", Math.sqrt(Math.abs(bsV0Vtx.getCovMatrix().e(2, 2))));
                    row.put("cascadeBeamspotConstrainedV0Mass/D", bsV0Vtx.getInvMass());
                    row.put("cascadeBeamspotConstrainedV0Chi2/D", bsV0Vtx.getChi2());
                }
            }

            if (bothCandidates != null) {
                ReconstructedParticle bothCascade = bothCandidates.get(candidateIndex);
                BilliorVertex bothVtxCheck = (BilliorVertex) bothCascade.getStartVertex();
                Double bothNdfCheck = bothVtxCheck.getCustomParameters().get("ndf");
                boolean isPlaceholder = bothNdfCheck == null || bothNdfCheck == -9999.0;
                if (!isPlaceholder) {
                    BilliorVertex bothVtx = bothVtxCheck;
                    ReconstructedParticle bothV0Particle = bothCascade.getParticles().get(0);
                    BilliorVertex bothV0Vtx = (BilliorVertex) bothV0Particle.getStartVertex();
                    Hep3Vector bothPos = bothVtx.getPosition();
                    Hep3Vector bothV0P = bothVtx.getFittedMomentum(0);
                    Hep3Vector bothRecoilP = bothVtx.getFittedMomentum(1);
                    Hep3Vector bothV0PErr = bothVtx.getFittedMomentumError(0);
                    Hep3Vector bothRecoilPErr = bothVtx.getFittedMomentumError(1);
                    Hep3Vector bothEleFitP = bothV0Vtx.getFittedMomentum(0);
                    Hep3Vector bothPosFitP = bothV0Vtx.getFittedMomentum(1);
                    Hep3Vector bothEleFitPErr = bothV0Vtx.getFittedMomentumError(0);
                    Hep3Vector bothPosFitPErr = bothV0Vtx.getFittedMomentumError(1);
                    Double bothNdf = bothVtx.getCustomParameters().get("ndf");
                    Hep3Vector bothV0Pos = bothV0Vtx.getPosition();
                    row.put("cascadeBothConstrainedVtxX/D", bothPos.x());
                    row.put("cascadeBothConstrainedVtxY/D", bothPos.y());
                    row.put("cascadeBothConstrainedVtxZ/D", bothPos.z());
                    row.put("cascadeBothConstrainedVtxXErr/D", Math.sqrt(Math.abs(bothVtx.getCovMatrix().e(0, 0))));
                    row.put("cascadeBothConstrainedVtxYErr/D", Math.sqrt(Math.abs(bothVtx.getCovMatrix().e(1, 1))));
                    row.put("cascadeBothConstrainedVtxZErr/D", Math.sqrt(Math.abs(bothVtx.getCovMatrix().e(2, 2))));
                    row.put("cascadeBothConstrainedChi2/D", bothVtx.getChi2());
                    row.put("cascadeBothConstrainedNdf/I", bothNdf != null ? bothNdf : -9999.0);
                    row.put("cascadeBothConstrainedMass/D", bothVtx.getInvMass());
                    row.put("cascadeBothConstrainedV0PX/D", bothV0P.x());
                    row.put("cascadeBothConstrainedV0PY/D", bothV0P.y());
                    row.put("cascadeBothConstrainedV0PZ/D", bothV0P.z());
                    row.put("cascadeBothConstrainedV0PXErr/D", bothV0PErr != null ? bothV0PErr.x() : Double.NaN);
                    row.put("cascadeBothConstrainedV0PYErr/D", bothV0PErr != null ? bothV0PErr.y() : Double.NaN);
                    row.put("cascadeBothConstrainedV0PZErr/D", bothV0PErr != null ? bothV0PErr.z() : Double.NaN);
                    row.put("cascadeBothConstrainedRecoilPX/D", bothRecoilP.x());
                    row.put("cascadeBothConstrainedRecoilPY/D", bothRecoilP.y());
                    row.put("cascadeBothConstrainedRecoilPZ/D", bothRecoilP.z());
                    row.put("cascadeBothConstrainedRecoilPXErr/D", bothRecoilPErr != null ? bothRecoilPErr.x() : Double.NaN);
                    row.put("cascadeBothConstrainedRecoilPYErr/D", bothRecoilPErr != null ? bothRecoilPErr.y() : Double.NaN);
                    row.put("cascadeBothConstrainedRecoilPZErr/D", bothRecoilPErr != null ? bothRecoilPErr.z() : Double.NaN);
                    row.put("cascadeBothConstrainedElePX/D", bothEleFitP != null ? bothEleFitP.x() : Double.NaN);
                    row.put("cascadeBothConstrainedElePY/D", bothEleFitP != null ? bothEleFitP.y() : Double.NaN);
                    row.put("cascadeBothConstrainedElePZ/D", bothEleFitP != null ? bothEleFitP.z() : Double.NaN);
                    row.put("cascadeBothConstrainedElePXErr/D", bothEleFitPErr != null ? bothEleFitPErr.x() : Double.NaN);
                    row.put("cascadeBothConstrainedElePYErr/D", bothEleFitPErr != null ? bothEleFitPErr.y() : Double.NaN);
                    row.put("cascadeBothConstrainedElePZErr/D", bothEleFitPErr != null ? bothEleFitPErr.z() : Double.NaN);
                    row.put("cascadeBothConstrainedPosPX/D", bothPosFitP != null ? bothPosFitP.x() : Double.NaN);
                    row.put("cascadeBothConstrainedPosPY/D", bothPosFitP != null ? bothPosFitP.y() : Double.NaN);
                    row.put("cascadeBothConstrainedPosPZ/D", bothPosFitP != null ? bothPosFitP.z() : Double.NaN);
                    row.put("cascadeBothConstrainedPosPXErr/D", bothPosFitPErr != null ? bothPosFitPErr.x() : Double.NaN);
                    row.put("cascadeBothConstrainedPosPYErr/D", bothPosFitPErr != null ? bothPosFitPErr.y() : Double.NaN);
                    row.put("cascadeBothConstrainedPosPZErr/D", bothPosFitPErr != null ? bothPosFitPErr.z() : Double.NaN);
                    row.put("cascadeBothConstrainedV0VtxX/D", bothV0Pos.x());
                    row.put("cascadeBothConstrainedV0VtxY/D", bothV0Pos.y());
                    row.put("cascadeBothConstrainedV0VtxZ/D", bothV0Pos.z());
                    row.put("cascadeBothConstrainedV0VtxXErr/D", Math.sqrt(Math.abs(bothV0Vtx.getCovMatrix().e(0, 0))));
                    row.put("cascadeBothConstrainedV0VtxYErr/D", Math.sqrt(Math.abs(bothV0Vtx.getCovMatrix().e(1, 1))));
                    row.put("cascadeBothConstrainedV0VtxZErr/D", Math.sqrt(Math.abs(bothV0Vtx.getCovMatrix().e(2, 2))));
                    row.put("cascadeBothConstrainedV0Mass/D", bothV0Vtx.getInvMass());
                    row.put("cascadeBothConstrainedV0Chi2/D", bothV0Vtx.getChi2());
                }
            }

            if (ntrackCandidates != null) {
                BilliorVertex ntrackVtx = (BilliorVertex) ntrackCandidates.get(candidateIndex);
                Double ntrackNdfCheck = ntrackVtx.getCustomParameters().get("ndf");
                boolean ntrackIsPlaceholder = ntrackNdfCheck == null || ntrackNdfCheck == -9999.0;
                if (!ntrackIsPlaceholder) {
                    Hep3Vector ntrackPos = ntrackVtx.getPosition();
                    Hep3Vector ntrackEleP = ntrackVtx.getFittedMomentum(0);
                    Hep3Vector ntrackPosP = ntrackVtx.getFittedMomentum(1);
                    Hep3Vector ntrackRecoilP = ntrackVtx.getFittedMomentum(2);
                    Hep3Vector ntrackElePErr = ntrackVtx.getFittedMomentumError(0);
                    Hep3Vector ntrackPosPErr = ntrackVtx.getFittedMomentumError(1);
                    Hep3Vector ntrackRecoilPErr = ntrackVtx.getFittedMomentumError(2);
                    row.put("ntrackVtxX/D", ntrackPos.x());
                    row.put("ntrackVtxY/D", ntrackPos.y());
                    row.put("ntrackVtxZ/D", ntrackPos.z());
                    row.put("ntrackVtxXErr/D", Math.sqrt(Math.abs(ntrackVtx.getCovMatrix().e(0, 0))));
                    row.put("ntrackVtxYErr/D", Math.sqrt(Math.abs(ntrackVtx.getCovMatrix().e(1, 1))));
                    row.put("ntrackVtxZErr/D", Math.sqrt(Math.abs(ntrackVtx.getCovMatrix().e(2, 2))));
                    row.put("ntrackChi2/D", ntrackVtx.getChi2());
                    row.put("ntrackNdf/I", ntrackNdfCheck);
                    row.put("ntrackMass/D", ntrackVtx.getInvMass());
                    row.put("ntrackElePX/D", ntrackEleP.x());
                    row.put("ntrackElePY/D", ntrackEleP.y());
                    row.put("ntrackElePZ/D", ntrackEleP.z());
                    row.put("ntrackElePXErr/D", ntrackElePErr != null ? ntrackElePErr.x() : Double.NaN);
                    row.put("ntrackElePYErr/D", ntrackElePErr != null ? ntrackElePErr.y() : Double.NaN);
                    row.put("ntrackElePZErr/D", ntrackElePErr != null ? ntrackElePErr.z() : Double.NaN);
                    row.put("ntrackPosPX/D", ntrackPosP.x());
                    row.put("ntrackPosPY/D", ntrackPosP.y());
                    row.put("ntrackPosPZ/D", ntrackPosP.z());
                    row.put("ntrackPosPXErr/D", ntrackPosPErr != null ? ntrackPosPErr.x() : Double.NaN);
                    row.put("ntrackPosPYErr/D", ntrackPosPErr != null ? ntrackPosPErr.y() : Double.NaN);
                    row.put("ntrackPosPZErr/D", ntrackPosPErr != null ? ntrackPosPErr.z() : Double.NaN);
                    row.put("ntrackRecoilPX/D", ntrackRecoilP.x());
                    row.put("ntrackRecoilPY/D", ntrackRecoilP.y());
                    row.put("ntrackRecoilPZ/D", ntrackRecoilP.z());
                    row.put("ntrackRecoilPXErr/D", ntrackRecoilPErr != null ? ntrackRecoilPErr.x() : Double.NaN);
                    row.put("ntrackRecoilPYErr/D", ntrackRecoilPErr != null ? ntrackRecoilPErr.y() : Double.NaN);
                    row.put("ntrackRecoilPZErr/D", ntrackRecoilPErr != null ? ntrackRecoilPErr.z() : Double.NaN);
                }
            }

            if (ntrackBcCandidates != null) {
                BilliorVertex ntrackBcVtx = (BilliorVertex) ntrackBcCandidates.get(candidateIndex);
                Double ntrackBcNdfCheck = ntrackBcVtx.getCustomParameters().get("ndf");
                boolean ntrackBcIsPlaceholder = ntrackBcNdfCheck == null || ntrackBcNdfCheck == -9999.0;
                if (!ntrackBcIsPlaceholder) {
                    Hep3Vector ntrackBcPos = ntrackBcVtx.getPosition();
                    Hep3Vector ntrackBcEleP = ntrackBcVtx.getFittedMomentum(0);
                    Hep3Vector ntrackBcPosP = ntrackBcVtx.getFittedMomentum(1);
                    Hep3Vector ntrackBcRecoilP = ntrackBcVtx.getFittedMomentum(2);
                    Hep3Vector ntrackBcElePErr = ntrackBcVtx.getFittedMomentumError(0);
                    Hep3Vector ntrackBcPosPErr = ntrackBcVtx.getFittedMomentumError(1);
                    Hep3Vector ntrackBcRecoilPErr = ntrackBcVtx.getFittedMomentumError(2);
                    row.put("ntrackBeamMomConstrainedVtxX/D", ntrackBcPos.x());
                    row.put("ntrackBeamMomConstrainedVtxY/D", ntrackBcPos.y());
                    row.put("ntrackBeamMomConstrainedVtxZ/D", ntrackBcPos.z());
                    row.put("ntrackBeamMomConstrainedVtxXErr/D", Math.sqrt(Math.abs(ntrackBcVtx.getCovMatrix().e(0, 0))));
                    row.put("ntrackBeamMomConstrainedVtxYErr/D", Math.sqrt(Math.abs(ntrackBcVtx.getCovMatrix().e(1, 1))));
                    row.put("ntrackBeamMomConstrainedVtxZErr/D", Math.sqrt(Math.abs(ntrackBcVtx.getCovMatrix().e(2, 2))));
                    row.put("ntrackBeamMomConstrainedChi2/D", ntrackBcVtx.getChi2());
                    row.put("ntrackBeamMomConstrainedNdf/I", ntrackBcNdfCheck);
                    row.put("ntrackBeamMomConstrainedMass/D", ntrackBcVtx.getInvMass());
                    row.put("ntrackBeamMomConstrainedElePX/D", ntrackBcEleP.x());
                    row.put("ntrackBeamMomConstrainedElePY/D", ntrackBcEleP.y());
                    row.put("ntrackBeamMomConstrainedElePZ/D", ntrackBcEleP.z());
                    row.put("ntrackBeamMomConstrainedElePXErr/D", ntrackBcElePErr != null ? ntrackBcElePErr.x() : Double.NaN);
                    row.put("ntrackBeamMomConstrainedElePYErr/D", ntrackBcElePErr != null ? ntrackBcElePErr.y() : Double.NaN);
                    row.put("ntrackBeamMomConstrainedElePZErr/D", ntrackBcElePErr != null ? ntrackBcElePErr.z() : Double.NaN);
                    row.put("ntrackBeamMomConstrainedPosPX/D", ntrackBcPosP.x());
                    row.put("ntrackBeamMomConstrainedPosPY/D", ntrackBcPosP.y());
                    row.put("ntrackBeamMomConstrainedPosPZ/D", ntrackBcPosP.z());
                    row.put("ntrackBeamMomConstrainedPosPXErr/D", ntrackBcPosPErr != null ? ntrackBcPosPErr.x() : Double.NaN);
                    row.put("ntrackBeamMomConstrainedPosPYErr/D", ntrackBcPosPErr != null ? ntrackBcPosPErr.y() : Double.NaN);
                    row.put("ntrackBeamMomConstrainedPosPZErr/D", ntrackBcPosPErr != null ? ntrackBcPosPErr.z() : Double.NaN);
                    row.put("ntrackBeamMomConstrainedRecoilPX/D", ntrackBcRecoilP.x());
                    row.put("ntrackBeamMomConstrainedRecoilPY/D", ntrackBcRecoilP.y());
                    row.put("ntrackBeamMomConstrainedRecoilPZ/D", ntrackBcRecoilP.z());
                    row.put("ntrackBeamMomConstrainedRecoilPXErr/D", ntrackBcRecoilPErr != null ? ntrackBcRecoilPErr.x() : Double.NaN);
                    row.put("ntrackBeamMomConstrainedRecoilPYErr/D", ntrackBcRecoilPErr != null ? ntrackBcRecoilPErr.y() : Double.NaN);
                    row.put("ntrackBeamMomConstrainedRecoilPZErr/D", ntrackBcRecoilPErr != null ? ntrackBcRecoilPErr.z() : Double.NaN);
                }
            }
            if (ntrackBscCandidates != null) {
                BilliorVertex ntrackBscVtx = (BilliorVertex) ntrackBscCandidates.get(candidateIndex);
                Double ntrackBscNdfCheck = ntrackBscVtx.getCustomParameters().get("ndf");
                if (ntrackBscNdfCheck != null && ntrackBscNdfCheck >= 0) {
                    Hep3Vector ntrackBscPos = ntrackBscVtx.getPosition();
                    Hep3Vector ntrackBscEleP = ntrackBscVtx.getFittedMomentum(0);
                    Hep3Vector ntrackBscPosP = ntrackBscVtx.getFittedMomentum(1);
                    Hep3Vector ntrackBscRecoilP = ntrackBscVtx.getFittedMomentum(2);
                    Hep3Vector ntrackBscElePErr = ntrackBscVtx.getFittedMomentumError(0);
                    Hep3Vector ntrackBscPosPErr = ntrackBscVtx.getFittedMomentumError(1);
                    Hep3Vector ntrackBscRecoilPErr = ntrackBscVtx.getFittedMomentumError(2);
                    row.put("ntrackBeamspotConstrainedVtxX/D", ntrackBscPos.x());
                    row.put("ntrackBeamspotConstrainedVtxY/D", ntrackBscPos.y());
                    row.put("ntrackBeamspotConstrainedVtxZ/D", ntrackBscPos.z());
                    row.put("ntrackBeamspotConstrainedVtxXErr/D", Math.sqrt(Math.abs(ntrackBscVtx.getCovMatrix().e(0, 0))));
                    row.put("ntrackBeamspotConstrainedVtxYErr/D", Math.sqrt(Math.abs(ntrackBscVtx.getCovMatrix().e(1, 1))));
                    row.put("ntrackBeamspotConstrainedVtxZErr/D", Math.sqrt(Math.abs(ntrackBscVtx.getCovMatrix().e(2, 2))));
                    row.put("ntrackBeamspotConstrainedChi2/D", ntrackBscVtx.getChi2());
                    row.put("ntrackBeamspotConstrainedNdf/I", ntrackBscNdfCheck);
                    row.put("ntrackBeamspotConstrainedMass/D", ntrackBscVtx.getInvMass());
                    row.put("ntrackBeamspotConstrainedElePX/D", ntrackBscEleP.x());
                    row.put("ntrackBeamspotConstrainedElePY/D", ntrackBscEleP.y());
                    row.put("ntrackBeamspotConstrainedElePZ/D", ntrackBscEleP.z());
                    row.put("ntrackBeamspotConstrainedElePXErr/D", ntrackBscElePErr != null ? ntrackBscElePErr.x() : Double.NaN);
                    row.put("ntrackBeamspotConstrainedElePYErr/D", ntrackBscElePErr != null ? ntrackBscElePErr.y() : Double.NaN);
                    row.put("ntrackBeamspotConstrainedElePZErr/D", ntrackBscElePErr != null ? ntrackBscElePErr.z() : Double.NaN);
                    row.put("ntrackBeamspotConstrainedPosPX/D", ntrackBscPosP.x());
                    row.put("ntrackBeamspotConstrainedPosPY/D", ntrackBscPosP.y());
                    row.put("ntrackBeamspotConstrainedPosPZ/D", ntrackBscPosP.z());
                    row.put("ntrackBeamspotConstrainedPosPXErr/D", ntrackBscPosPErr != null ? ntrackBscPosPErr.x() : Double.NaN);
                    row.put("ntrackBeamspotConstrainedPosPYErr/D", ntrackBscPosPErr != null ? ntrackBscPosPErr.y() : Double.NaN);
                    row.put("ntrackBeamspotConstrainedPosPZErr/D", ntrackBscPosPErr != null ? ntrackBscPosPErr.z() : Double.NaN);
                    row.put("ntrackBeamspotConstrainedRecoilPX/D", ntrackBscRecoilP.x());
                    row.put("ntrackBeamspotConstrainedRecoilPY/D", ntrackBscRecoilP.y());
                    row.put("ntrackBeamspotConstrainedRecoilPZ/D", ntrackBscRecoilP.z());
                    row.put("ntrackBeamspotConstrainedRecoilPXErr/D", ntrackBscRecoilPErr != null ? ntrackBscRecoilPErr.x() : Double.NaN);
                    row.put("ntrackBeamspotConstrainedRecoilPYErr/D", ntrackBscRecoilPErr != null ? ntrackBscRecoilPErr.y() : Double.NaN);
                    row.put("ntrackBeamspotConstrainedRecoilPZErr/D", ntrackBscRecoilPErr != null ? ntrackBscRecoilPErr.z() : Double.NaN);
                }
            }

            if (ntrackBothCandidates != null) {
                BilliorVertex ntrackBothVtx = (BilliorVertex) ntrackBothCandidates.get(candidateIndex);
                Double ntrackBothNdfCheck = ntrackBothVtx.getCustomParameters().get("ndf");
                if (ntrackBothNdfCheck != null && ntrackBothNdfCheck >= 0) {
                    Hep3Vector ntrackBothPos = ntrackBothVtx.getPosition();
                    Hep3Vector ntrackBothEleP = ntrackBothVtx.getFittedMomentum(0);
                    Hep3Vector ntrackBothPosP = ntrackBothVtx.getFittedMomentum(1);
                    Hep3Vector ntrackBothRecoilP = ntrackBothVtx.getFittedMomentum(2);
                    Hep3Vector ntrackBothElePErr = ntrackBothVtx.getFittedMomentumError(0);
                    Hep3Vector ntrackBothPosPErr = ntrackBothVtx.getFittedMomentumError(1);
                    Hep3Vector ntrackBothRecoilPErr = ntrackBothVtx.getFittedMomentumError(2);
                    row.put("ntrackBothConstrainedVtxX/D", ntrackBothPos.x());
                    row.put("ntrackBothConstrainedVtxY/D", ntrackBothPos.y());
                    row.put("ntrackBothConstrainedVtxZ/D", ntrackBothPos.z());
                    row.put("ntrackBothConstrainedVtxXErr/D", Math.sqrt(Math.abs(ntrackBothVtx.getCovMatrix().e(0, 0))));
                    row.put("ntrackBothConstrainedVtxYErr/D", Math.sqrt(Math.abs(ntrackBothVtx.getCovMatrix().e(1, 1))));
                    row.put("ntrackBothConstrainedVtxZErr/D", Math.sqrt(Math.abs(ntrackBothVtx.getCovMatrix().e(2, 2))));
                    row.put("ntrackBothConstrainedChi2/D", ntrackBothVtx.getChi2());
                    row.put("ntrackBothConstrainedNdf/I", ntrackBothNdfCheck);
                    row.put("ntrackBothConstrainedMass/D", ntrackBothVtx.getInvMass());
                    row.put("ntrackBothConstrainedElePX/D", ntrackBothEleP.x());
                    row.put("ntrackBothConstrainedElePY/D", ntrackBothEleP.y());
                    row.put("ntrackBothConstrainedElePZ/D", ntrackBothEleP.z());
                    row.put("ntrackBothConstrainedElePXErr/D", ntrackBothElePErr != null ? ntrackBothElePErr.x() : Double.NaN);
                    row.put("ntrackBothConstrainedElePYErr/D", ntrackBothElePErr != null ? ntrackBothElePErr.y() : Double.NaN);
                    row.put("ntrackBothConstrainedElePZErr/D", ntrackBothElePErr != null ? ntrackBothElePErr.z() : Double.NaN);
                    row.put("ntrackBothConstrainedPosPX/D", ntrackBothPosP.x());
                    row.put("ntrackBothConstrainedPosPY/D", ntrackBothPosP.y());
                    row.put("ntrackBothConstrainedPosPZ/D", ntrackBothPosP.z());
                    row.put("ntrackBothConstrainedPosPXErr/D", ntrackBothPosPErr != null ? ntrackBothPosPErr.x() : Double.NaN);
                    row.put("ntrackBothConstrainedPosPYErr/D", ntrackBothPosPErr != null ? ntrackBothPosPErr.y() : Double.NaN);
                    row.put("ntrackBothConstrainedPosPZErr/D", ntrackBothPosPErr != null ? ntrackBothPosPErr.z() : Double.NaN);
                    row.put("ntrackBothConstrainedRecoilPX/D", ntrackBothRecoilP.x());
                    row.put("ntrackBothConstrainedRecoilPY/D", ntrackBothRecoilP.y());
                    row.put("ntrackBothConstrainedRecoilPZ/D", ntrackBothRecoilP.z());
                    row.put("ntrackBothConstrainedRecoilPXErr/D", ntrackBothRecoilPErr != null ? ntrackBothRecoilPErr.x() : Double.NaN);
                    row.put("ntrackBothConstrainedRecoilPYErr/D", ntrackBothRecoilPErr != null ? ntrackBothRecoilPErr.y() : Double.NaN);
                    row.put("ntrackBothConstrainedRecoilPZErr/D", ntrackBothRecoilPErr != null ? ntrackBothRecoilPErr.z() : Double.NaN);
                }
            }

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
