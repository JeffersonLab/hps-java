package org.hps.users.jeremym;

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
 * Cluster input hit list of raw ECAL data for cosmic events.
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 * @author Tim "THammer" Nelson <tknelson@slac.stanford.edu>
 */
// TODO: Add output collection of RawTrackerHits.
public class EcalCosmicClusterDriver extends Driver {

    String inputHitCollectionName = "EcalCosmicCalHits";
    String outputClusterCollectionName = "EcalCosmicClusters";
    String ecalName = "Ecal";
    HPSEcal3 ecal = null;
    IIdentifierHelper helper = null;
    int minimumClusterSize = 3;
    int minimumRows = 3;
    int maximumHitsPerRow = 2;

    public void setEcalName(String ecalName) {
        this.ecalName = ecalName;
    }

    public void setInputHitCollectionName(String inputHitCollectionName) {
        this.inputHitCollectionName = inputHitCollectionName;
    }

    public void setOutputClusterCollectionName(String outputClusterCollectionName) {
        this.outputClusterCollectionName = outputClusterCollectionName;
    }

    public void setMinimumClusterSize(int minimumClusterSize) {
        this.minimumClusterSize = minimumClusterSize;
    }
    
    public void setMinimumRows(int minimumRows) {
        this.minimumRows = minimumRows;
    }
    
    public void setMaximumHitsPerRow(int maximumHitsPerRow) {
        this.maximumHitsPerRow = maximumHitsPerRow;
    }

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
            if (clusterCollection.size() > 0) {
                int flags = 1 << LCIOConstants.CLBIT_HITS;                
                event.put(outputClusterCollectionName, clusterCollection, Cluster.class, flags);
                //System.out.println("added " + clusterCollection.size() + " clusters to " + outputClusterCollectionName);
            } else {
                throw new NextEventException();
            }
        }
    }

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

    private Map<Long, CalorimeterHit> createHitMap(List<CalorimeterHit> hitList) {
        Map<Long, CalorimeterHit> hitMap = new HashMap<Long, CalorimeterHit>();
        for (CalorimeterHit hit : hitList) {
            hitMap.put(hit.getCellID(), hit);
        }
        return hitMap;
    }

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
                //System.out.println("adding cosmic cluster of size " + clusterHits.size());
                clusterList.add(clusterHits);
            }
        }
        return clusterList;
    }
    
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
