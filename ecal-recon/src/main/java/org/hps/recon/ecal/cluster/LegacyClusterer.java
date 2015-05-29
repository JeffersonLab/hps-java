package org.hps.recon.ecal.cluster;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.base.BaseCluster;

/**
 * <p>
 * This Driver creates clusters from a CalorimeterHit input collection.
 * <p>
 * The clustering algorithm is implemented according to the description on pages 83 and 84 of the 
 * <a href="https://confluence.slac.stanford.edu/download/attachments/86676777/HPSProposal-FINAL_Rev2.pdf">HPS Proposal document</a>.
 *
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 * @author Tim Nelson <tknelson@slac.stanford.edu>
 */
public class LegacyClusterer extends AbstractClusterer {
    
    double minClusterSeedEnergy;
    double minHitEnergy;
    
    LegacyClusterer() {
        super(new String[] { "minClusterSeedEnergy", "minHitEnergy" }, new double[] { 0.05, 0.03 });
    }
    
    public void initialize() {
        minClusterSeedEnergy = getCuts().getValue("minClusterSeedEnergy");
        minHitEnergy = getCuts().getValue("minHitEnergy");
    }
                 
    public List<Cluster> createClusters(EventHeader event, List<CalorimeterHit> hits) {

        Map<Long, CalorimeterHit> hitMap = ClusterUtilities.createHitMap(hits);
        
        // New Cluster list to be added to event.
        List<Cluster> clusters = new ArrayList<Cluster>();

        // Loop over ECal hits to find cluster seeds.
        for (CalorimeterHit hit : hitMap.values()) {
            // Cut on min seed E.
            if (hit.getRawEnergy() < minClusterSeedEnergy) {
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
                    if (neighborHit.getRawEnergy() >= minHitEnergy) {
                        neighborHits.add(neighborHit);
                    }
                }
            }

            // Did we find a seed?
            if (isSeed) {
                // Make a cluster from the hit list.
                BaseCluster cluster = createBasicCluster();
                cluster.addHit(hit);
                for (CalorimeterHit clusHit : neighborHits) {
                    cluster.addHit(clusHit);
                }
                clusters.add(cluster);
            }
        }
        return clusters;
    }

    @Override
    public ClusterType getClusterType() {
        return ClusterType.LEGACY;
    }
}