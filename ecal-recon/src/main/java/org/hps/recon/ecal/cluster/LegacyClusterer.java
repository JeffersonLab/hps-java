package org.hps.recon.ecal.cluster;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hps.recon.ecal.ECalUtils;
import org.hps.recon.ecal.HPSEcalCluster;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.Cluster;

/**
 * This Driver creates clusters from the CalorimeterHits of an
 * {@link org.lcsim.geometry.subdetectur.HPSEcal3} detector.
 *
 * The clustering algorithm is from pages 83 and 84 of the HPS Proposal.
 *
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 * @author Tim Nelson <tknelson@slac.stanford.edu>
 */
public class LegacyClusterer extends AbstractClusterer {
        
    // Minimum E for cluster seed.
    double minimumClusterSeedEnergy = 0.05 * ECalUtils.GeV;

    // Minimum E to add hit to cluster.
    double minimumHitEnergy = 0.03 * ECalUtils.GeV;
     
    void setMinimumClusterSeedEnergy(double minimumClusterSeedEnergy) {
        this.minimumClusterSeedEnergy = minimumClusterSeedEnergy;
    }

    void setMinimumHitEnergy(double minimumHitEnergy) {
        this.minimumHitEnergy = minimumHitEnergy;
        if (minimumClusterSeedEnergy < minimumHitEnergy) {
            minimumClusterSeedEnergy = minimumHitEnergy;
        }
    }
    
    public List<Cluster> createClusters(List<CalorimeterHit> hits) {

        Map<Long, CalorimeterHit> hitMap = ClusterUtilities.createHitMap(hits);
        
        // New Cluster list to be added to event.
        List<Cluster> clusters = new ArrayList<Cluster>();

        // Loop over ECal hits to find cluster seeds.
        for (CalorimeterHit hit : hitMap.values()) {
            // Cut on min seed E.
            if (hit.getRawEnergy() < minimumClusterSeedEnergy) {
                continue;
            }

            // Get neighbor crystal IDs.
            Set<Long> neighbors = neighborMap.get(hit.getCellID());

            // List for neighboring hits.
            List<CalorimeterHit> neighborHits = new ArrayList<CalorimeterHit>();

            // Loop over neighbors to make hit list for cluster.
            boolean isSeed = true;
            for (Long neighborId : neighbors) {
                // Find the neighbor hit in the event if it exists.
                CalorimeterHit neighborHit = hitMap.get(neighborId);

                // Was this neighbor cell hit?
                if (neighborHit != null) {
                    // Check if neighbor cell has more energy.
                    if (neighborHit.getRawEnergy() > hit.getRawEnergy()) {
                        // Neighbor has more energy, so cell is not a seed.
                        isSeed = false;
                        break;
                    }

                    // Add to cluster if above min E.
                    if (neighborHit.getRawEnergy() >= minimumHitEnergy) {
                        neighborHits.add(neighborHit);
                    }
                }
            }

            // Did we find a seed?
            if (isSeed) {
                // Make a cluster from the hit list.
                HPSEcalCluster cluster = new HPSEcalCluster();
                cluster.setSeedHit(hit);
                cluster.addHit(hit);
                for (CalorimeterHit clusHit : neighborHits) {
                    cluster.addHit(clusHit);
                }
                clusters.add(cluster);
            }
        }
        return clusters;
    }
}