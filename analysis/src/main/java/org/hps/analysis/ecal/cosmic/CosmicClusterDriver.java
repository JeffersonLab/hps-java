package org.hps.analysis.ecal.cosmic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.lcsim.detector.identifier.IIdentifierHelper;
import org.lcsim.detector.identifier.Identifier;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.base.BaseCluster;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.subdetector.HPSEcal3;
import org.lcsim.lcio.LCIOConstants;
import org.lcsim.util.Driver;

/**
 * <p>
 * This Driver clusters an input list of CalorimeterHits into a cosmic cluster
 * using an iterative nearest neighbors algorithm.
 * <p>
 * There is a set of cuts applied on the initial cluster list that includes the following:
 * <ul>
 * <li>must have at least 3 hits total in the cluster</li>
 * <li>must have at least 3 contiguous hits in the cluster</li>
 * <li>must have at least three rows of crystals with hits</li>
 * <li>must have no more than 2 hits in each row of crystals</li>
 * </ul>
 * <p>
 * The new Cluster collection is written to the LCIO event with the default
 * collection name of "EcalCosmicClusters".  This collection name can be changed
 * using the {@link #setOutputClusterCollectionName(String)} method.
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 * @author Tim "THammer" Nelson <tknelson@slac.stanford.edu>
 */
public class CosmicClusterDriver extends Driver {

    String inputHitCollectionName = "EcalCosmicCalHits";
    String outputClusterCollectionName = "EcalCosmicClusters";
    String ecalName = "Ecal";
    HPSEcal3 ecal;
    IIdentifierHelper helper;
    int minimumClusterSize = 3;
    int minimumRows = 3;
    int maximumHitsPerRow = 2;

    /**
     * Set the name of the ECAL subdetector.
     * @param ecalName The name of the ECAL subdetector.
     */
    public void setEcalName(String ecalName) {
        this.ecalName = ecalName;
    }

    /**
     * Set the name of the input RawTrackerHit collection name.  
     * By default this is initialized to "EcalCosmicCalHits".
     * @param inputHitCollectionName The name of the input hit collection.
     */
    public void setInputHitCollectionName(String inputHitCollectionName) {
        this.inputHitCollectionName = inputHitCollectionName;
    }

    /**
     * Set the name of the output cluster collection.
     * By default this is initialized to "EcalCosmicClusters".
     * @param outputClusterCollectionName The name of the output cluster collection.
     */
    public void setOutputClusterCollectionName(String outputClusterCollectionName) {
        this.outputClusterCollectionName = outputClusterCollectionName;
    }

    /**
     * Set the minimum number of hits in the cluster for it to pass selection.
     * @param minimumClusterSize The minimum number of hits in the cluster.
     */
    public void setMinimumClusterSize(int minimumClusterSize) {
        this.minimumClusterSize = minimumClusterSize;
    }
    
    /**
     * Set the minimum number of rows that must have at least one hit.
     * @param minimumRows The minimum number of rows that must have at least one hit.
     */
    public void setMinimumRows(int minimumRows) {
        this.minimumRows = minimumRows;
    }
    
    /**
     * Set the maximum number of hits per row.
     * @param maximumHitsPerRow The maximum number of hits per row.
     */
    public void setMaximumHitsPerRow(int maximumHitsPerRow) {
        this.maximumHitsPerRow = maximumHitsPerRow;
    }

    /**
     * Initialize conditions dependent class variables.
     * @param detector The current Detector object.     
     */
    public void detectorChanged(Detector detector) {
        ecal = (HPSEcal3) detector.getSubdetector(ecalName);
        if (ecal == null) {
            throw new RuntimeException("There is no HPSEcal3 subdetector called " + ecalName + " in this detector.");
        }
        helper = ecal.getDetectorElement().getIdentifierHelper();
        if (helper == null) {
            throw new RuntimeException("Panic!  Could not find IIdentifierHelper for " + ecalName + " subdetector.");
        }
    }

    /**
     * Process the event by making a list of output clusters that pass the basic
     * selection cuts. 
     */
    public void process(EventHeader event) {
        if (event.hasCollection(CalorimeterHit.class, inputHitCollectionName)) {
            List<CalorimeterHit> calHits = event.get(CalorimeterHit.class, inputHitCollectionName);
            List<List<CalorimeterHit>> clusteredHitList = createClusteredHits(calHits);
            List<List<CalorimeterHit>> selectedClusteredHitList = applyCuts(clusteredHitList);
            List<Cluster> clusterCollection = new ArrayList<Cluster>();
            for (List<CalorimeterHit> hits : selectedClusteredHitList) {
                BaseCluster calCluster = new BaseCluster();
                double totalEnergy = 0;
                for (CalorimeterHit hit : hits) {
                    calCluster.addHit(hit);
                    totalEnergy += hit.getCorrectedEnergy();
                }
                calCluster.setEnergy(totalEnergy);
                clusterCollection.add(calCluster);
            }            
            int flags = 1 << LCIOConstants.CLBIT_HITS;                
            event.put(outputClusterCollectionName, clusterCollection, Cluster.class, flags);
        }
    }

    /**
     * Given a hit, find its list of neighboring crystals that have hits and return their IDs.
     * @param hit The input hit.
     * @param hitMap The hit map with all the collection's hits.
     * @return The set of neighboring hit IDs.
     */
    private Set<Long> findNeighborHitIDs(CalorimeterHit hit, Map<Long, CalorimeterHit> hitMap) {
        Set<Long> neigbhors = ecal.getNeighborMap().get(hit.getCellID());
        Set<Long> neighborHitIDs = new HashSet<Long>();
        for (long neighborID : neigbhors) {
            if (hitMap.containsKey(neighborID)) {
                neighborHitIDs.add(neighborID);
            }
        }
        return neighborHitIDs;
    }

    /**
     * Create a map of ID to hit from a list of hits.
     * @param hitList The input hit list.
     * @return The hit map.
     */
    private Map<Long, CalorimeterHit> createHitMap(List<CalorimeterHit> hitList) {
        Map<Long, CalorimeterHit> hitMap = new HashMap<Long, CalorimeterHit>();
        for (CalorimeterHit hit : hitList) {
            hitMap.put(hit.getCellID(), hit);
        }
        return hitMap;
    }

    /**
     * This is the primary clustering algorithm which uses a topological, iterative
     * nearest neighbor algorithm.  It uses each hit as a seed and tries to then
     * extend the cluster by adding neighboring hits.  Clustered hits are added
     * to a list which is checked so that they are not reused.
     * @param hitList The input hit list from the event.
     * @return A list of calorimeter hits that can be turned into clusters.
     */
    private List<List<CalorimeterHit>> createClusteredHits(List<CalorimeterHit> hitList) {
        
        // Create empty list of clusters which are just lists of hits.
        List<List<CalorimeterHit>> clusterList = new ArrayList<List<CalorimeterHit>>();
        
        // Create map of IDs to hits for convenience.
        Map<Long, CalorimeterHit> hitMap = createHitMap(hitList);
        
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
                for (Long neighborHitID : this.findNeighborHitIDs(clusterHit, hitMap)) {
                    CalorimeterHit neighborHit = hitMap.get(neighborHitID);
                    if (clusterable.contains(neighborHit)) {
                        uncheckedHits.add(neighborHit);
                    }
                }                                                
            }
            
            if (clusterHits.size() >= this.minimumClusterSize) {
                clusterList.add(clusterHits);
            }
        }
        return clusterList;
    }
    
    /**
     * This method takes a list of potential cluster hits and applies selection cuts,
     * returning a new list that has the hit lists which did not pass the cuts removed.
     * @param clusteredHitLists The input hit lists. 
     * @return The hit lists that passed the cuts.
     */
    List<List<CalorimeterHit>> applyCuts(List<List<CalorimeterHit>> clusteredHitLists) {
        List<List<CalorimeterHit>> selectedHitLists = new ArrayList<List<CalorimeterHit>>();
        for (List<CalorimeterHit> hitList : clusteredHitLists) {            
            Map<Integer, Set<CalorimeterHit>> rowMap = new HashMap<Integer, Set<CalorimeterHit>>();            
            for (CalorimeterHit hit : hitList) {
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
                    selectedHitLists.add(hitList);
                }
            }
        }
        return selectedHitLists;
    }
}
