package org.hps.recon.ecal.cluster;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawTrackerHit;

/**
 * This is a more complicated version of the {@link SimpleCosmicClusterer} 
 * which uses a list of tight hits for seeding.
 * 
 * @see SimpleCosmicClusterer
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class DualThresholdCosmicClusterer extends SimpleCosmicClusterer {
             
    String inputTightHitCollectionName;
    
    void setInputTightHitCollectionName(String inputTightHitCollectionName) {
        this.inputTightHitCollectionName = inputTightHitCollectionName;
    }
             
    /**
     * Create and return clusters.
     */
    public List<Cluster> createClusters(EventHeader event, List<CalorimeterHit> hitList) {
        
        if (inputTightHitCollectionName == null) {
            throw new RuntimeException("The inputTightHitCollectionName was never set.");
        }
        
        // Get tight hits collection of RawTrackerHit (raw data).
        if (!event.hasCollection(RawTrackerHit.class, inputTightHitCollectionName)) {
            throw new RuntimeException("The hit collection " + inputTightHitCollectionName + " does not exist.");
        }
        List<RawTrackerHit> currentTightHits = event.get(RawTrackerHit.class, inputTightHitCollectionName);               
        Set<Long> tightIDs = this.createCellIDSet(currentTightHits);
        
        List<Cluster> clusters = new ArrayList<Cluster>();
                        
        // Create map of IDs to hits for convenience.
        Map<Long, CalorimeterHit> hitMap = ClusterUtilities.createHitMap(hitList);
        
        // Create list of CalorimeterHits that are clusterable, which is initially all of them.
        Set<CalorimeterHit> clusterable = new HashSet<CalorimeterHit>();
        clusterable.addAll(hitList);
        
        // Loop over all hits in the map.
        for (CalorimeterHit currentHit : hitList) {
                                               
            // Is hit not clusterable (e.g. already used) or not in tight hit list?
            if (!clusterable.contains(currentHit) || !tightIDs.contains(currentHit.getCellID())) {
                // Continue to the next hit.
                continue;
            }
            
            // Create list for clustering this hit.
            List<CalorimeterHit> clusterHits = new ArrayList<CalorimeterHit>();
            
            // Set of hits whose neighbors have not been checked yet.
            LinkedList<CalorimeterHit> uncheckedHits = new LinkedList<CalorimeterHit>();
            uncheckedHits.add(currentHit);

            // While there are still unchecked hits.
            while (uncheckedHits.size() > 0) {
                                
                // Get the first hit.
                CalorimeterHit clusterHit = uncheckedHits.removeFirst();
                
                // Add hit to the cluster.
                clusterHits.add(clusterHit);
                                        
                // Remove the hit from the clusterable list as we have used it in a cluster.
                clusterable.remove(clusterHit);                                    
                                
                // Loop over the neighbors and to the unchecked list neighboring hits.
                for (Long neighborHitID : ClusterUtilities.findNeighborHitIDs(ecal, clusterHit, hitMap)) {
                    CalorimeterHit neighborHit = hitMap.get(neighborHitID);
                    if (clusterable.contains(neighborHit)) {
                        uncheckedHits.add(neighborHit);
                    }
                }                                                
            }
            
            // Are there enough hits in the cluster?
            if (clusterHits.size() >= this.minClusterSize) {
                // Create cluster and add it to the output list.
                clusters.add(ClusterUtilities.createBasicCluster(clusterHits));
            }
        }
             
        // Return the cluster list, applying cosmic clustering cuts (from super-class).
        return applyCuts(clusters);
    }
                     
    /**
     * Create a list of cell IDs from a set of RawTrackerHit (raw data) objects.
     * @param rawHits The input hits.
     * @return The ID set.
     */
    Set<Long> createCellIDSet(List<RawTrackerHit> rawHits) {
        Set<Long> set = new HashSet<Long>();
        for (RawTrackerHit hit : rawHits) {
            set.add(hit.getCellID());
        }        
        return set;
    }

    @Override
    public ClusterType getClusterType() {
        return ClusterType.DUAL_THRESHOLD_COSMIC;
    }    
}
