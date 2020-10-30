package org.hps.analysis.ecal;

import java.text.DecimalFormat;
import java.util.List;
import org.hps.recon.ecal.cluster.ClusterUtilities;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.MCParticle;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 *
 * @author Norman A. Graf
 */
public class EcalSamplingFractionAnalysisDriver extends Driver {

    private AIDA aida = AIDA.defaultInstance();
    DecimalFormat df = new DecimalFormat("###.##");

    public void process(EventHeader event) {
        if (event.hasCollection(MCParticle.class, "MCParticle")) {
            List<MCParticle> particles = event.get(MCParticle.class, "MCParticle");
            MCParticle primary = null;
            for (MCParticle particle : particles) {
                if (particle.getGeneratorStatus() == MCParticle.FINAL_STATE && !particle.getSimulatorStatus().isDecayedInTracker()) {
                    primary = particle;
                }
            }
            if (primary != null) {
                double mcEnergy = primary.getEnergy();
                int pdgId = primary.getPDGID();
                String type;
                switch (pdgId) {
                    case 11:
                        type = "electron";
                        break;
                    case 22:
                        type = "photon";
                        break;
                    case -11:
                        type = "positron";
                        break;
                    default:
                        type = "unknown";
                        break;
                }
                String e = df.format(mcEnergy);
//                aida.tree().mkdirs(type + "/" + e);
//                aida.tree().cd(type + "/" + e);
                String[] collectionNames = {"EcalClustersCorr"};//{"EcalClusters", "EcalClustersCorr"};
                for (String collectionName : collectionNames) {
                    List<Cluster> ecalClusters = event.get(Cluster.class, collectionName);
                    // let's start by requiring two and only two clusters, in opposite hemispheres, whose energies sum to the beam energy
//                    aida.tree().mkdirs(collectionName + " cluster pair analysis");
//                    aida.tree().cd(collectionName + " cluster pair analysis");
//                    aida.histogram1D("number of clusters", 10, 0., 10.).fill(ecalClusters.size());
                    int nclusters = ecalClusters.size();
                    if (nclusters == 1) {
                        Cluster cl = ecalClusters.get(0);
                        double eclus = cl.getEnergy();
                        String fid = isFiducial(ClusterUtilities.findSeedHit(cl)) ? "fiducial" : "edge";
                        aida.histogram1D(type + " " + e + " GeV " + fid + " " + collectionName, 200, 0., 5.0).fill(eclus);
                    }
//                    aida.tree().cd("..");
                }
//                aida.tree().cd("../..");
            }
        }
    }

    public boolean isFiducial(CalorimeterHit hit) {
        int ix = hit.getIdentifierFieldValue("ix");
        int iy = hit.getIdentifierFieldValue("iy");
        // Get the x and y indices for the cluster.
        int absx = Math.abs(ix);
        int absy = Math.abs(iy);

        // Check if the cluster is on the top or the bottom of the
        // calorimeter, as defined by |y| == 5. This is an edge cluster
        // and is not in the fiducial region.
        if (absy == 5) {
            return false;
        }

        // Check if the cluster is on the extreme left or right side
        // of the calorimeter, as defined by |x| == 23. This is also
        // an edge cluster and is not in the fiducial region.
        if (absx == 23) {
            return false;
        }

        // Check if the cluster is along the beam gap, as defined by
        // |y| == 1. This is an internal edge cluster and is not in the
        // fiducial region.
        if (absy == 1) {
            return false;
        }

        // Lastly, check if the cluster falls along the beam hole, as
        // defined by clusters with -11 <= x <= -1 and |y| == 2. This
        // is not the fiducial region.
        if (absy == 2 && ix <= -1 && ix >= -11) {
            return false;
        }

        // If all checks fail, the cluster is in the fiducial region.
        return true;
    }
}
