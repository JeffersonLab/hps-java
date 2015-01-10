package org.hps.recon.ecal.cluster;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.lcsim.detector.identifier.Identifier;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;

/**
 * <p>
 * This algorithm clusters an input list of CalorimeterHits using an iterative nearest neighbor algorithm.
 * <p>
 * There is a set of cuts applied on the initial cluster list that includes the following defaults:
 * <ul>
 * <li>must have at least 3 hits total in the cluster</li>
 * <li>must have at least three rows of crystals with hits</li>
 * <li>must have no more than 2 hits in each row of crystals</li>
 * </ul>
 * <p>
 * This algorithm does not cluster across the beam gap.  Separate clusters are made for the top
 * and bottom sets of crystals.
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 * @author Tim "THammer" Nelson <tknelson@slac.stanford.edu>
 */
public class SimpleCosmicClusterer extends AbstractClusterer {

    protected int minClusterSize;
    protected int minRows;
    protected int maxHitsPerRow;
    
    /**
     * Default constructor that sets cut names and default values.
     */
    SimpleCosmicClusterer() {
        super(new String[] { "minClusterSize", "minRows", "maxHitsPerRow" }, new double[] { 3, 3, 2 });
    }
    
    /**
     * Initialize some instance variables from cut settings.
     */
    public void initialize() {
        this.minClusterSize = (int) getCuts().getValue("minClusterSize");
        this.minRows = (int) getCuts().getValue("minRows");
        this.maxHitsPerRow = (int) getCuts().getValue("maxHitsPerRow");
    }       
        
    /**
     * This method implements the cosmic clustering algorithm which uses a iterative 
     * nearest neighbor algorithm.  It uses each hit as a seed and tries to then
     * extend the cluster by adding neighboring hits.  Clustered hits are added to a list 
     * which is checked so that they are not reused.
     * @param hitList The input hit list of hits.
     * @param event The current LCSim event.
     * @return A list of calorimeter hits that can be turned into clusters.
     */
    public List<Cluster> createClusters(EventHeader event, List<CalorimeterHit> hitList) {
        
        // Create empty list of clusters which are just lists of hits.
        //List<List<CalorimeterHit>> clusterList = new ArrayList<List<CalorimeterHit>>();
        List<Cluster> clusterList = new ArrayList<Cluster>();
        
        // Create map of IDs to hits for convenience.
        Map<Long, CalorimeterHit> hitMap = ClusterUtilities.createHitMap(hitList);
        
        // Create list of hits that are clusterable, which is initially all of them.
        Set<CalorimeterHit> clusterable = new HashSet<CalorimeterHit>();
        clusterable.addAll(hitList);
        
        // Loop over all hits in the map.
        for (CalorimeterHit hit : hitList) {
                                               
            // Is hit clusterable?
            if (!clusterable.contains(hit)) {
                // Continue to next hit.
                continue;
            }
            
            // Create list for clustering this hit.
            List<CalorimeterHit> clusterHits = new ArrayList<CalorimeterHit>();
            
            // Set of hits whose neighbors have not been checked yet.
            LinkedList<CalorimeterHit> uncheckedHits = new LinkedList<CalorimeterHit>();
            uncheckedHits.add(hit);

            // While there are still unchecked hits in the cluster.
            while (uncheckedHits.size() > 0) {
                                
                // Get the first hit and add it to the cluster.
                CalorimeterHit clusterHit = uncheckedHits.removeFirst();
                
                // Add hit to the cluster.
                clusterHits.add(clusterHit);
                                        
                // Remove the hit from the clusterable list.
                clusterable.remove(clusterHit);                                    
                                
                // Loop over the neighbors and add IDs with hits to the unchecked list.
                for (Long neighborHitID : ClusterUtilities.findNeighborHitIDs(ecal, clusterHit, hitMap)) {
                    CalorimeterHit neighborHit = hitMap.get(neighborHitID);
                    if (clusterable.contains(neighborHit)) {
                        uncheckedHits.add(neighborHit);
                    }
                }                                                
            }
            
            if (clusterHits.size() >= this.minClusterSize) {
                Cluster cluster = ClusterUtilities.createBasicCluster(clusterHits);
                clusterList.add(cluster);
            }
        }
        
        // Apply cuts on the cluster list that was generated.
        List<Cluster> selectedClusters = applyCuts(clusterList);
        
        return selectedClusters;
    }    

    /**
     * This method takes a list of potential cluster hits and applies selection cuts,
     * returning a new list that has the hit lists which did not pass the cuts removed.
     * @param clusteredHitLists The input hit lists. 
     * @return The hit lists that passed the cuts.
     */
    protected List<Cluster> applyCuts(List<Cluster> clusterList) {
        List<Cluster> selectedClusters = new ArrayList<Cluster>();
        for (Cluster cluster  : clusterList) {            
            Map<Integer, Set<CalorimeterHit>> rowMap = new HashMap<Integer, Set<CalorimeterHit>>();            
            for (CalorimeterHit hit : cluster.getCalorimeterHits()) {
                int row = ecal.getDetectorElement().getIdentifierHelper().getValue(new Identifier(hit.getCellID()), "iy");
                if (rowMap.get(row) == null) {
                    rowMap.put(row, new HashSet<CalorimeterHit>());
                }
                rowMap.get(row).add(hit);
            }
            if (rowMap.size() >= this.minRows) {
                boolean okay = true;
                rowMapLoop: for (Entry<Integer, Set<CalorimeterHit>> entries : rowMap.entrySet()) {
                    if (entries.getValue().size() > this.maxHitsPerRow) {
                        okay = false;
                        break rowMapLoop;
                    }
                }
                if (okay) {
                    selectedClusters.add(cluster);
                }
            }
        }
        return selectedClusters;
    }

    @Override
    public ClusterType getClusterType() {
        return ClusterType.SIMPLE_COSMIC;
    }    
}
