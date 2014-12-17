package org.hps.recon.ecal.cluster;

import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.Cluster;
import org.lcsim.event.base.BaseCluster;
import org.lcsim.geometry.subdetector.HPSEcal3;

/**
 * This is a set of simple clustering utility methods.
 */
public final class ClusterUtilities {
    
    private ClusterUtilities() {        
    }

    /**
     * Create a map of IDs to their hits.
     * @return The hit map.
     */
    public static Map<Long, CalorimeterHit> createHitMap(List<CalorimeterHit> hits) {
        Map<Long, CalorimeterHit> hitMap = new LinkedHashMap<Long, CalorimeterHit>();
        for (CalorimeterHit hit : hits) {
            hitMap.put(hit.getCellID(), hit);
        }
        return hitMap;
    }
    
    /**
     * Given a hit, find the list of neighboring crystal IDs that also have hits.
     * @param hit The input hit.
     * @param hitMap The hit map with all the collection's hits.
     * @return The set of neighboring hit IDs.
     */
    public static Set<Long> findNeighborHitIDs(HPSEcal3 ecal, CalorimeterHit hit, Map<Long, CalorimeterHit> hitMap) {
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
     * Create a basic cluster object from a list of hits.
     * @param clusterHits The list of hits.
     * @return The basic cluster.
     */
    public static Cluster createBasicCluster(List<CalorimeterHit> clusterHits) {
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
     * Compare CalorimeterHit objects by their energy using default double comparison strategy.
     */
    public static class HitEnergyComparator implements Comparator<CalorimeterHit> {
        @Override
        public int compare(CalorimeterHit o1, CalorimeterHit o2) {
            return Double.compare(o1.getCorrectedEnergy(), o2.getCorrectedEnergy());
        }
    }    
}
