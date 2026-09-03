package org.hps.recon.filtering;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.lcsim.event.EventHeader;
import org.lcsim.event.LCRelation;
import org.lcsim.event.MCParticle;
import org.lcsim.lcio.LCIOWriter;
import org.lcsim.util.Driver;

/**
 * Writes a separate, small skim LCIO file containing only events where all three signal
 * electrons -- the e-/e+ daughters of an A' (MCParticle PDGID 622 with two daughters) and a
 * separate recoil e- (the PDGID-11 daughter of a top-level PDGID-623 "reaction" particle) --
 * are each matched to a reconstructed Track, using the same MC-truth conventions as
 * CascadeVertexTupleDriver. Unlike filtering on CascadeVertexCandidates/ThreeTrackVertexCandidates,
 * this only requires that tracking found all three tracks; it doesn't require that any
 * V0/vertex candidate was successfully built from them, so the skim stays valid regardless of
 * changes to the downstream vertex-fitting code.
 *
 * This driver does not skip events: every event flows through the rest of the driver chain
 * unaffected, and passing events are additionally written to {@link #outputFilePath} here.
 */
public class ThreeElectronTrackingFilter extends Driver {

    private String mcParticlesColName = "MCParticle";
    private String trackToMCParticleRelationsColName = "KalmanFullTracksToMCParticleRelations";
    private String outputFilePath;
    private LCIOWriter writer;
    private int nprocessed = 0;
    private int npassed = 0;

    public void setMcParticlesColName(String mcParticlesColName) {
        this.mcParticlesColName = mcParticlesColName;
    }

    public void setTrackToMCParticleRelationsColName(String trackToMCParticleRelationsColName) {
        this.trackToMCParticleRelationsColName = trackToMCParticleRelationsColName;
    }

    public void setOutputFilePath(String outputFilePath) {
        this.outputFilePath = outputFilePath;
    }

    @Override
    protected void startOfData() {
        if (outputFilePath == null) {
            throw new RuntimeException("outputFilePath must be set");
        }
        try {
            writer = new LCIOWriter(outputFilePath);
            writer.reOpen();
        } catch (IOException x) {
            throw new RuntimeException("Error creating skim LCIO writer", x);
        }
    }

    @Override
    protected void endOfData() {
        try {
            writer.close();
        } catch (IOException x) {
            throw new RuntimeException("Error closing skim LCIO writer", x);
        }
        System.out.println(this.getClass().getSimpleName() + ": processed " + nprocessed + ", passed " + npassed);
    }

    @Override
    protected void process(EventHeader event) {
        nprocessed++;

        if (passesThreeElectronTrackingCut(event)) {
            npassed++;
            try {
                writer.write(event);
            } catch (IOException x) {
                throw new RuntimeException("Error writing skim LCIO file", x);
            }
        }
    }

    private boolean passesThreeElectronTrackingCut(EventHeader event) {
        if (!event.hasCollection(MCParticle.class, mcParticlesColName)
                || !event.hasCollection(LCRelation.class, trackToMCParticleRelationsColName)) {
            return false;
        }

        List<MCParticle> mcParticles = event.get(MCParticle.class, mcParticlesColName);

        MCParticle eleMC = null;
        MCParticle posMC = null;
        MCParticle recoilMC = null;
        for (MCParticle mcp : mcParticles) {
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
        if (eleMC != null || posMC != null) {
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

        if (eleMC == null || posMC == null || recoilMC == null) {
            return false;
        }

        Set<MCParticle> trackedMC = new HashSet<MCParticle>();
        for (LCRelation rel : event.get(LCRelation.class, trackToMCParticleRelationsColName)) {
            trackedMC.add((MCParticle) rel.getTo());
        }

        return trackedMC.contains(eleMC) && trackedMC.contains(posMC) && trackedMC.contains(recoilMC);
    }
}
