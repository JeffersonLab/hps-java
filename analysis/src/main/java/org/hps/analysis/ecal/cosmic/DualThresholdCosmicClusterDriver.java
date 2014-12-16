package org.hps.analysis.ecal.cosmic;

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
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.base.BaseCluster;
import org.lcsim.lcio.LCIOConstants;

public class DualThresholdCosmicClusterDriver extends CosmicClusterDriver {
    
    String inputTightHitsCollection = "TightEcalCosmicReadoutHits";
    String inputLooseHitsCollection = "LooseEcalCosmicCalHits";
         
    List<CalorimeterHit> currentLooseHits;
    List<RawTrackerHit> currentTightHits;    
    Set<Long> currentTightHitCellIDs;
    
    boolean writeOnlyClusterEvents = true;
    
    static boolean debug = false; 
    
    public void writeOnlyClusterEvents(boolean writeOnlyClusterEvents) {
        this.writeOnlyClusterEvents = writeOnlyClusterEvents;
    }
    
    public void setInputTightHitsCollection(String inputTightHitsCollection) {
        this.inputTightHitsCollection = inputTightHitsCollection;
    }
    
    public void setInputLooseHitsCollection(String inputLooseHitsCollection) {
        this.inputLooseHitsCollection = inputLooseHitsCollection;
    }
        
    private Set<Long> createCellIDSet(List<RawTrackerHit> rawHits) {
        Set<Long> set = new HashSet<Long>();
        for (RawTrackerHit hit : rawHits) {
            set.add(hit.getCellID());
        }        
        return set;
    }
    
    public void process(EventHeader event) {                

        // Get loose hits collection of CalorimeterHits.
        if (!event.hasCollection(CalorimeterHit.class, inputLooseHitsCollection)) {
            throw new RuntimeException("The collection " + inputLooseHitsCollection + " does not exist.");
        }
        currentLooseHits = event.get(CalorimeterHit.class, inputLooseHitsCollection);
        if (debug) {
            System.out.println("DEBUG: collection " + inputLooseHitsCollection + " has " + currentLooseHits.size() + " hits");
        }
        
        // Get tight hits collection of RawTrackerHit (raw data).
        if (!event.hasCollection(RawTrackerHit.class, inputTightHitsCollection)) {
            throw new RuntimeException("The collection " + inputTightHitsCollection + " does not exist.");
        }
        currentTightHits = event.get(RawTrackerHit.class, inputTightHitsCollection);
        if (debug) {
            System.out.println("DEBUG: collection " + inputTightHitsCollection + " has " + currentTightHits.size() + " hits");
        }
        
        // Create set of tight hit cell IDs for use by clustering algorithm.
        currentTightHitCellIDs = createCellIDSet(currentTightHits);
        
        // Create the clusters.
        List<Cluster> clusters = createClusters(currentLooseHits);
        if (debug) {
            System.out.println("DEBUG: created " + clusters.size() + " clusters from " + inputLooseHitsCollection);
        }
        
        // Apply cluster cuts.
        List<Cluster> selectedClusters = this.applyClusterCuts(clusters);
        if (debug) {
            System.out.println("DEBUG: " + selectedClusters.size() + " clusters passed cuts");
        }
        
        if (selectedClusters.isEmpty() && this.writeOnlyClusterEvents) {
            if (debug) {
                System.out.println("skipping event without any clusters");
            }
            throw new NextEventException();
        }
        
        // Write clusters to event (may write empty collection).        
        int flags = 1 << LCIOConstants.CLBIT_HITS;                
        event.put(outputClusterCollectionName, selectedClusters, Cluster.class, flags);        
    }
        
    List<Cluster> createClusters(List<CalorimeterHit> hitList) {
        List<Cluster> clusters = new ArrayList<Cluster>();
                
        // Create map of IDs to hits for convenience.
        Map<Long, CalorimeterHit> hitMap = createHitMap(hitList);
        
        // Create list of hits that are clusterable, which is initially all of them.
        Set<CalorimeterHit> clusterable = new HashSet<CalorimeterHit>();
        clusterable.addAll(hitList);
        
        // Loop over all hits in the map.
        for (CalorimeterHit currentHit : hitList) {
                                               
            // Is hit clusterable and in tight significance list?
            if (!clusterable.contains(currentHit) || !currentTightHitCellIDs.contains(currentHit.getCellID())) {
                // Continue to next hit.
                continue;
            }
            
            // Create list for clustering this hit.
            List<CalorimeterHit> clusterHits = new ArrayList<CalorimeterHit>();
            
            // Set of hits whose neighbors have not been checked yet.
            LinkedList<CalorimeterHit> uncheckedHits = new LinkedList<CalorimeterHit>();
            uncheckedHits.add(currentHit);

            // While there are still unchecked hits in the cluster.
            while (uncheckedHits.size() > 0) {
                                
                // Get the first hit and add it to the cluster.
                CalorimeterHit clusterHit = uncheckedHits.removeFirst();
                
                // Add hit to the cluster.
                clusterHits.add(clusterHit);
                                        
                // Remove the hit from the clusterable list.
                clusterable.remove(clusterHit);                                    
                                
                // Loop over the neighbors and add IDs with hits to the unchecked list.
                for (Long neighborHitID : this.findNeighborHitIDs(clusterHit, hitMap)) {
                    CalorimeterHit neighborHit = hitMap.get(neighborHitID);
                    if (clusterable.contains(neighborHit)) {
                        uncheckedHits.add(neighborHit);
                    }
                }                                                
            }
            
            // Are there enough hits in cluster?
            if (clusterHits.size() >= this.minimumClusterSize) {
                // Create cluster and add to the output list.
                clusters.add(createCluster(clusterHits));
            }
        }
             
        return clusters;
    }

    Cluster createCluster(List<CalorimeterHit> clusterHits) {
        BaseCluster cluster = new BaseCluster();
        double totalEnergy = 0;
        for (CalorimeterHit clusterHit : clusterHits) {
            cluster.addHit(clusterHit);
            totalEnergy += clusterHit.getCorrectedEnergy();
        }
        cluster.setEnergy(totalEnergy);        
        return cluster;
    }    
    
    /**
     * This method takes a list of potential cluster hits and applies selection cuts,
     * returning a new list that has the hit lists which did not pass the cuts removed.
     * @param clusteredHitLists The input hit lists. 
     * @return The hit lists that passed the cuts.
     */
    List<Cluster> applyClusterCuts(List<Cluster> inputClusters) {
        List<Cluster> selectedClusters = new ArrayList<Cluster>();
        for (Cluster cluster : inputClusters) {            
            Map<Integer, Set<CalorimeterHit>> rowMap = new HashMap<Integer, Set<CalorimeterHit>>();            
            for (CalorimeterHit hit : cluster.getCalorimeterHits()) {
                int row = helper.getValue(new Identifier(hit.getCellID()), "iy");
                if (rowMap.get(row) == null) {
                    rowMap.put(row, new HashSet<CalorimeterHit>());
                }
                rowMap.get(row).add(hit);
            }
            if (rowMap.size() >= minimumRows) {
                boolean okay = true;
                rowMapLoop: for (Entry<Integer, Set<CalorimeterHit>> entries : rowMap.entrySet()) {
                    if (entries.getValue().size() > maximumHitsPerRow) {
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
    
}
