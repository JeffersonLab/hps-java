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
        return computeEnergySum(cluster.getCalorimeterHits());
    }
    
    /**
     * Compute the energy sum for a set of CalorimeterHits by adding all their corrected energies.
     * @return The energy sum.
     */
    public static double computeEnergySum(List<CalorimeterHit> hits) {
        double uncorrectedEnergy = 0;
        for (CalorimeterHit hit : hits) {
            uncorrectedEnergy += hit.getCorrectedEnergy();
        }
        return uncorrectedEnergy;
    }
    
    /**
     * Find the hit with the highest energy value.
     * This method doesn't handle hits with the same energy properly.
     * It will simply return the first of them.
     * Use {@link #sortReconClusterHits(List)} if this level of disambiguation
     * is needed (e.g. in order to find an exact seed hit).
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
     * Find hits in a cluster that are shared with other Clusters.
     * @param cluster The input cluster.
     * @param clusters The list of clusters.
     * @return The list of shared hits.
     */
    public static List<CalorimeterHit> findSharedHits(Cluster cluster, List<Cluster> clusters) {
        Set<CalorimeterHit> allHits = new HashSet<CalorimeterHit>();
        List<CalorimeterHit> sharedHits = new ArrayList<CalorimeterHit>();
        for (Cluster otherCluster : clusters) {
            if (otherCluster != cluster) {
                allHits.addAll(otherCluster.getCalorimeterHits());
            }
        }
        for (CalorimeterHit clusterHit : cluster.getCalorimeterHits()) {
            if (allHits.contains(clusterHit)) {
                sharedHits.add(clusterHit);
            }
        }
        return sharedHits;
    }
    
    
    /**
     * Find the seed hit of a Cluster.
     * @param cluster The input Cluster.
     * @return The seed hit.
     */
    public static CalorimeterHit findSeedHit(Cluster cluster) {        
        if (cluster.getSize() == 0) {
            // There are no hits!
            return null;
        } else if (cluster.getSize() > 1) {        
            // Make sure the hit ordering is correct.
            List<Cluster> clusterList = new ArrayList<Cluster>();
            clusterList.add(cluster);
            
            // Sort hits on energy, with position used for disambiguation of equal energies.
            sortReconClusterHits(clusterList);
        }        
        // Get the first hit, which should now be the seed.
        return cluster.getCalorimeterHits().get(0);
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
     
    /**
     * Sort in place the CalorimeterHit list of a set of clusters, 
     * disambiguating when the energies are exactly the same.
     * @param clusters The list of clusters with hits to sort.
     */
    public static void sortReconClusterHits(List<Cluster> clusters) {
        Comparator<CalorimeterHit> comparator = Collections.reverseOrder(new UniqueEnergyComparator());
        for (Cluster cluster : clusters) {
            Collections.sort(cluster.getCalorimeterHits(), comparator);
        }
    }
    
    /**
     * Compare CalorimeterHit energies and disambiguate equal values using the crystal position.         
     */
    static final class UniqueEnergyComparator implements Comparator<CalorimeterHit> {
        /**
         * Compares the first hit with respect to the second. This method will compare hits first by
         * energy, and then spatially. In the case of equal energy hits, the hit closest to the beam
         * gap and closest to the positron side of the detector will be selected. If all of these
         * conditions are true, the hit with the positive y-index will be selected. Hits with all
         * four conditions matching are the same hit.
         * @param hit1 The hit to compare.
         * @param hit2 The hit with respect to which the first should be compared.
         */
        public int compare(CalorimeterHit hit1, CalorimeterHit hit2) {
            // Hits are sorted on a hierarchy by three conditions. First,
            // the hits with the highest energy come first. Next, they
            // are ranked by vertical proximity to the beam gap, and
            // lastly, they are sorted by horizontal proximity to the
            // positron side of the detector.

            // Get the hit energies.
            double[] e = { hit1.getCorrectedEnergy(), hit2.getCorrectedEnergy() };

            // Perform the energy comparison. The higher energy hit
            // will be ordered first.
            if (e[0] < e[1]) {
                return -1;
            } else if (e[0] > e[1]) {
                return 1;
            }

            // If the hits are the same energy, we must perform the
            // spatial comparisons.
            else {
                // Get the position with respect to the beam gap.
                int[] iy = { Math.abs(hit1.getIdentifierFieldValue("iy")), Math.abs(hit2.getIdentifierFieldValue("iy")) };

                // The closest hit is first.
                if (iy[0] > iy[1]) {
                    return -1;
                } else if (iy[0] < iy[1]) {
                    return 1;
                }

                // Hits that are identical in vertical distance from
                // beam gap and energy are differentiated with distance
                // horizontally from the positron side of the detector.
                else {
                    // Get the position from the positron side.
                    int[] ix = { hit1.getIdentifierFieldValue("ix"), hit2.getIdentifierFieldValue("ix") };

                    // The closest hit is first.
                    if (ix[0] > ix[1]) {
                        return 1;
                    } else if (ix[0] < ix[1]) {
                        return -1;
                    }

                    // If all of these checks are the same, compare
                    // the raw value for iy. If these are identical,
                    // then the two hits are the same. Otherwise, sort
                    // the numerical value of iy. (This removes the
                    // issue where hits (x, y) and (x, -y) can have
                    // the same energy and be otherwise seen as the
                    // same hit from the above checks.
                    else {
                        return Integer.compare(hit1.getIdentifierFieldValue("iy"), hit2.getIdentifierFieldValue("iy"));
                    }
                }
            }
        }
    }
    
    /**
     * Apply HPS-specific energy and position corrections to a list of clusters in place.
     * 
     * @see ReconClusterPropertyCalculator
     * @see ReconClusterPositionCorrection
     * @see ReconClusterEnergyCorrection
     */
    public static void applyCorrections(List<Cluster> clusters) {
        
        // Use the HPS specific property calculator.
        ReconClusterPropertyCalculator calc = new ReconClusterPropertyCalculator();
        
        // Loop over the clusters.
        for (Cluster cluster : clusters) {
            
            if (cluster instanceof BaseCluster) {
            
                BaseCluster baseCluster = (BaseCluster)cluster;            
            
                // First calculate the cluster properties, if needed.
                if (baseCluster.needsPropertyCalculation()) {                
                    // Calculate the properties of the cluster.
                    baseCluster.setPropertyCalculator(calc);
                    baseCluster.calculateProperties();
                }
            
                // Apply position correction, which should happen before final energy correction.
                ReconClusterPositionCorrection.setCorrectedPosition(baseCluster);
            
                // Apply energy correction.
                ReconClusterEnergyCorrection.setCorrectedEnergy(baseCluster);
            }
        }
    }
    
    /**
     * Call {@link org.lcsim.event.base.BaseCluster#calculateProperties()}
     * on all clusters in the list.
     * @param clusters The list of clusters.
     */
    public static void calculateProperties(List<Cluster> clusters) {
        ReconClusterPropertyCalculator calc = new ReconClusterPropertyCalculator();
        for (Cluster cluster : clusters) {
            if (cluster instanceof BaseCluster) {
                BaseCluster baseCluster = (BaseCluster)cluster;
                //if (baseCluster.needsPropertyCalculation()) {
                baseCluster.setPropertyCalculator(calc);
                baseCluster.calculateProperties();
                //}
            }
        }
    }    
}