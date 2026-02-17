package org.hps.analysis.examples;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.lcsim.event.EventHeader;
import org.lcsim.event.MCParticle;
import org.lcsim.event.SimTrackerHit;
import org.lcsim.geometry.Detector;
import org.lcsim.lcio.LCIOWriter;
import org.lcsim.util.Driver;

/**
 * Driver that skims events with 2 electrons and 1 positron from MCParticles,
 * where each particle has SimTrackerHits in at least a minimum number of sensor layers,
 * and all three particles have a common parent with PDG ID 623 (A').
 * Writes passing events to an LCIO file.
 */
public class ThreeProngMCSkimDriver extends Driver {

    private String mcParticleCollectionName = "MCParticle";
    private String simTrackerHitCollectionName = "TrackerHits";
    private String outputFileName = "threeprong_mc_skim.slcio";
    private int minLayersPerParticle = 9;
    private int requiredParentPDG = 623;  // A' PDG ID

    private LCIOWriter writer;
    private int nProcessed = 0;
    private int nPassed = 0;

    public void setMcParticleCollectionName(String name) {
        this.mcParticleCollectionName = name;
    }

    public void setSimTrackerHitCollectionName(String name) {
        this.simTrackerHitCollectionName = name;
    }

    public void setOutputFileName(String name) {
        this.outputFileName = name;
    }

    public void setMinLayersPerParticle(int n) {
        this.minLayersPerParticle = n;
    }

    public void setRequiredParentPDG(int pdg) {
        this.requiredParentPDG = pdg;
    }

    @Override
    protected void detectorChanged(Detector detector) {
        // Setup LCIO writer
        try {
            File outputFile = new File(outputFileName);
            writer = new LCIOWriter(outputFile);
            writer.reOpen();
        } catch (IOException e) {
            throw new RuntimeException("Error creating LCIO writer for " + outputFileName, e);
        }
    }

    @Override
    public void process(EventHeader event) {
        nProcessed++;

        // Get MCParticles
        if (!event.hasCollection(MCParticle.class, mcParticleCollectionName)) {
            return;
        }
        List<MCParticle> mcParticles = event.get(MCParticle.class, mcParticleCollectionName);

        // Get SimTrackerHits
        if (!event.hasCollection(SimTrackerHit.class, simTrackerHitCollectionName)) {
            return;
        }
        List<SimTrackerHit> simHits = event.get(SimTrackerHit.class, simTrackerHitCollectionName);

        // Build map from MCParticle to set of layers with hits
        Map<MCParticle, Set<Integer>> particleLayerMap = new HashMap<>();
        for (SimTrackerHit hit : simHits) {
            MCParticle mcp = hit.getMCParticle();
            if (mcp == null) continue;

            int layer = hit.getLayer();
            if (!particleLayerMap.containsKey(mcp)) {
                particleLayerMap.put(mcp, new HashSet<>());
            }
            particleLayerMap.get(mcp).add(layer);
        }

        // Find electrons and positrons with sufficient layer coverage
        // that have a parent with the required PDG ID (623 = A')
        // PDG: electron = 11, positron = -11
        int nElectrons = 0;
        int nPositrons = 0;

        for (MCParticle mcp : mcParticles) {
            // Only consider final state particles (status == 1)
            if (mcp.getGeneratorStatus() != 1) continue;

            int pdgId = mcp.getPDGID();
            if (pdgId != 11 && pdgId != -11) continue;

            // Check if parent has required PDG ID
            List<MCParticle> parents = mcp.getParents();
            boolean hasRequiredParent = false;
            for (MCParticle parent : parents) {
                if (parent.getPDGID() == requiredParentPDG) {
                    hasRequiredParent = true;
                    break;
                }
            }
            if (!hasRequiredParent) continue;

            // Check if this particle has hits in enough layers
            Set<Integer> layers = particleLayerMap.get(mcp);
            if (layers == null || layers.size() < minLayersPerParticle) continue;

            if (pdgId == 11) {
                nElectrons++;
            } else if (pdgId == -11) {
                nPositrons++;
            }
        }

        // Check if we have 2 electrons and 1 positron (all from A' parent)
        if (nElectrons >= 2 && nPositrons >= 1) {
            nPassed++;
            try {
                writer.write(event);
            } catch (IOException e) {
                throw new RuntimeException("Error writing event to LCIO file", e);
            }
        }
    }

    @Override
    protected void endOfData() {
        System.out.println("ThreeProngMCSkimDriver Summary:");
        System.out.println("  Events processed: " + nProcessed);
        System.out.println("  Events passing:   " + nPassed);
        System.out.printf("  Efficiency:       %.2f%%%n", 100.0 * nPassed / nProcessed);

        if (writer != null) {
            try {
                writer.flush();
                writer.close();
            } catch (IOException e) {
                System.err.println("Error closing LCIO writer: " + e.getMessage());
            }
        }
    }
}
