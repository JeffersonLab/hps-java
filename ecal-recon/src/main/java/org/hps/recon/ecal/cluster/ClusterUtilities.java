package org.hps.recon.ecal.cluster;

import java.util.ArrayList;
import java.util.Collections;
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
 * This is a set of simple utility methods for clustering algorithms.
 * 
 * @see org.lcsim.event.Cluster
 * @see org.lcsim.event.base.BaseCluster
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
     * Compute the raw energy of a cluster which is just the 
     * sum of all its hit energies.
     * @param cluster The input cluster.
     * @return The total raw energy.
     */
    public static double computeRawEnergy(Cluster cluster) {
        double uncorrectedEnergy = 0;
        for (CalorimeterHit hit : cluster.getCalorimeterHits()) {
            uncorrectedEnergy += hit.getCorrectedEnergy();
        }
        return uncorrectedEnergy;
    }
    
    /**
     * Find the hit with the highest energy value.
     * @param cluster The input cluster.
     * @return The hit with the highest energy value.
     */
    public static CalorimeterHit getHighestEnergyHit(Cluster cluster) {
        double maxEnergy = Double.MIN_VALUE;
        CalorimeterHit highestEnergyHit = null;
        for (CalorimeterHit hit : cluster.getCalorimeterHits()) {
            if (hit.getCorrectedEnergy() > maxEnergy) {
                highestEnergyHit = hit;
            }
        }
        return highestEnergyHit;
    }
    
    /**
     * Sort the hits in the cluster using a <code>Comparator</code>.
     * This method will not change the hits in place.  It returns a new list.
     * @param cluster The input cluster.
     * @param comparator The Comparator to use for sorting.
     * @param reverseOrder True to use reverse rather than default ordering.
     * @return The sorted list of hits.     
     */
    public static List<CalorimeterHit> sortedHits(Cluster cluster, Comparator<CalorimeterHit> comparator, boolean reverseOrder) {
        List<CalorimeterHit> sortedHits = new ArrayList<CalorimeterHit>(cluster.getCalorimeterHits());
        Comparator<CalorimeterHit>sortComparator = comparator;
        if (reverseOrder) {
            sortComparator = Collections.reverseOrder(comparator);
        }
        Collections.sort(sortedHits, sortComparator);
        return sortedHits;
    }
}
