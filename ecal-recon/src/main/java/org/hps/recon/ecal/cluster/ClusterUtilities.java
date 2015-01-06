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
import org.lcsim.event.MCParticle;
import org.lcsim.event.SimCalorimeterHit;
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
        for (CalorimeterHit clusterHit : clusterHits) {
            cluster.addHit(clusterHit);
        }
        return cluster;
    }
     
    /**
     * Compute the raw energy of a cluster which is just the sum of all its hit energies.
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
    public static CalorimeterHit findHighestEnergyHit(Cluster cluster) {
        if (cluster.getCalorimeterHits().size() == 1) {
            return cluster.getCalorimeterHits().get(0);
        }
        List<CalorimeterHit> hits = new ArrayList<CalorimeterHit>();
        hits.addAll(cluster.getCalorimeterHits());
        Collections.sort(hits, new CalorimeterHit.CorrectedEnergyComparator());
        Collections.reverse(hits);
        return hits.get(0);
    }
    
    /**
     * Sort the hits in the cluster using a <code>Comparator</code>.
     * This method will not change the hits in place.  It returns a new list.
     * The algorithm does not disambiguate between hits with equal energies.
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
    
    /**
     * Find hits in a Cluster that are not shared with other Clusters.
     * @param cluster The input cluster.
     * @param clusters The list of clusters.
     * @return The list of unshared hits.
     */
    public static List<CalorimeterHit> findUnsharedHits(Cluster cluster, List<Cluster> clusters) {
        Set<CalorimeterHit> allHits = new HashSet<CalorimeterHit>();
        List<CalorimeterHit> unsharedHits = new ArrayList<CalorimeterHit>();
        for (Cluster otherCluster : clusters) {
            if (otherCluster != cluster) {
                allHits.addAll(otherCluster.getCalorimeterHits());
            }
        }
        for (CalorimeterHit clusterHit : cluster.getCalorimeterHits()) {
            if (!allHits.contains(clusterHit)) {
                unsharedHits.add(clusterHit);
            }
        }
        return unsharedHits;
    }
    
    /**
     * Find the seed hit of a Cluster, without any disambiguation when
     * energy is equal.     
     * @param cluster The input Cluster.
     * @return The seed hit.
     */
    public CalorimeterHit findSeedHit(Cluster cluster) {
        if (cluster.getSize() == 0) {
            // There are no hits!
            return null;
        } else if (cluster.getSize() == 1) {
            // There is a single hit.
            return cluster.getCalorimeterHits().get(0);
        } else {
            // Sort hits and return one with highest energy.
            return findHighestEnergyHit(cluster);
        }
    }
    
    /**
     * Find the unique set of MCParticles that are referenced by the 
     * hits of the Cluster.
     * @param clusters The input Cluster.
     * @return The set of unique MCParticles.
     */
    public static Set<MCParticle> findMCParticles(List<Cluster> clusters) {  
        Set<MCParticle> particles = new HashSet<MCParticle>();
        for (Cluster cluster : clusters) {
            for (CalorimeterHit hit : cluster.getCalorimeterHits()) {
                if (hit instanceof SimCalorimeterHit) {
                    SimCalorimeterHit simHit = (SimCalorimeterHit)hit;
                    for (int i = 0; i < simHit.getMCParticleCount(); i++) {
                        particles.add(simHit.getMCParticle(i));
                    }
                }
            }
        }
        return particles;
    }
   
    /**
     * Find CalorimeterHits that are not present in a collection of Clusters.
     * @param clusters The input Clusters.
     * @param hits The input Calorimeter hits.
     * @return The list of CalorimeterHits that were not clustered.
     */
    public static List<CalorimeterHit> findRejectedHits(List<Cluster> clusters, List<CalorimeterHit> hits) {
        List<CalorimeterHit> rejectedHits = new ArrayList<CalorimeterHit>();
        Set<CalorimeterHit> clusterHits = new HashSet<CalorimeterHit>();
        for (Cluster cluster : clusters) {
            clusterHits.addAll(cluster.getCalorimeterHits());
        }
        for (CalorimeterHit hit : hits) {
            if (!clusterHits.contains(hit)) {
                rejectedHits.add(hit);
            }
        }
        return rejectedHits;
    }   
}
