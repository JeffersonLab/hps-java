package org.hps.analysis.ecal;

import java.util.List;
import org.hps.record.triggerbank.TriggerModule;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.MCParticle;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 * Basic analysis Driver to make crystal-by-crystal plots of single-crystal
 * cluster energy. To be run over single muon MC.
 *
 * @author Norman A> Graf
 */
public class EcalMuonMCGainCalibrationDriver extends Driver {

    private AIDA aida = AIDA.defaultInstance();

    protected void process(EventHeader event) {

        List<MCParticle> mcparticles = event.get(MCParticle.class, "MCParticle");
        // should I insist on one and only one MCParticle?
        if (mcparticles.isEmpty()) {
            return;
        }
        int pdgId = mcparticles.get(0).getPDGID();
        String type = "";
        if (pdgId == 13) {
            type = "mu-";
        }
        if (pdgId == -13) {
            type = "mu+";
        }
        if (event.hasCollection(Cluster.class, "EcalClusters")) {
            List<Cluster> clusters = event.get(Cluster.class, "EcalClusters");
            aida.histogram1D("Number of Clusters in Event", 10, 0., 10.).fill(clusters.size());
            aida.tree().mkdirs("mc");
            aida.tree().cd("mc");
            for (Cluster cluster : clusters) {
                CalorimeterHit seed = cluster.getCalorimeterHits().get(0);
                int ix = seed.getIdentifierFieldValue("ix");
                int iy = seed.getIdentifierFieldValue("iy");
                aida.histogram1D("cluster size", 20, 0., 20.).fill(cluster.getCalorimeterHits().size());
                aida.histogram2D("Event Cluster ix vs iy", 47, -23.5, 23.5, 11, -5.5, 5.5).fill(ix, iy);
                String half = cluster.getPosition()[1] > 0. ? "Top " : "Bottom ";
                aida.histogram1D(half + "Event Cluster Energy", 100, 0., 6.).fill(cluster.getEnergy());
                if (cluster.getCalorimeterHits().size() == 1) {
                    analyzeCluster(cluster, type);
                }
            }
            aida.tree().cd("..");
        }
    }

    void analyzeCluster(Cluster cluster, String type) {
        if (cluster.getCalorimeterHits().size() == 1) {
            aida.tree().mkdirs("clusterAnalysis");
            aida.tree().cd("clusterAnalysis");
            CalorimeterHit seed = cluster.getCalorimeterHits().get(0);
            int ix = seed.getIdentifierFieldValue("ix");
            int iy = seed.getIdentifierFieldValue("iy");
            String fid = TriggerModule.inFiducialRegion(cluster) ? "fiducial " : "edge ";
            aida.histogram1D(fid + type + " single-crystal cluster energy", 50, 0.1, 0.3).fill(cluster.getEnergy());
            aida.histogram2D(fid + "cluster ix vs iy", 47, -23.5, 23.5, 11, -5.5, 5.5).fill(ix, iy);
            aida.histogram1D(type + " single-crystal cluster energy", 50, 0.1, 0.3).fill(cluster.getEnergy());
            aida.histogram2D("cluster ix vs iy", 47, -23.5, 23.5, 11, -5.5, 5.5).fill(ix, iy);
            aida.histogram1D(ix + " " + iy + " " + type + " crystal energy", 50, 0.1, 0.3).fill(cluster.getEnergy());
            aida.tree().cd("..");
        }
    }
}
