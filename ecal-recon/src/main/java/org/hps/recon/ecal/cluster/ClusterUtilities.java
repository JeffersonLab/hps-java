package org.hps.recon.ecal.cluster;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
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
import org.lcsim.event.base.ClusterPropertyCalculator;
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
     * Find the unique set of MCParticles that are referenced by the hits of the Cluster.
     * @param cluster The input Cluster.
     * @return The set of unique MCParticles.
     */
    public static Set<MCParticle> findMCParticles(Cluster cluster) {  
        Set<MCParticle> particles = new HashSet<MCParticle>();
        for (CalorimeterHit hit : cluster.getCalorimeterHits()) {
            if (hit instanceof SimCalorimeterHit) {
                SimCalorimeterHit simHit = (SimCalorimeterHit)hit;
                for (int i = 0; i < simHit.getMCParticleCount(); i++) {
                    particles.add(simHit.getMCParticle(i));
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
     * Sort in place a list of hits by their corrected energy.
     * If energy is equal then position is used to disambiguate.
     * @param hits The list of hits.
     */
    public static void sortHitsUniqueEnergy(List<CalorimeterHit> hits) {
        Comparator<CalorimeterHit> comparator = Collections.reverseOrder(new UniqueEnergyComparator());
        Collections.sort(hits, comparator);
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
     * @param clusters The list of clusters.
     */
    public static void applyCorrections(HPSEcal3 ecal, List<Cluster> clusters, boolean isMC) {
                
        // Loop over the clusters.
        for (Cluster cluster : clusters) {
            
            if (cluster instanceof BaseCluster) {
            
                BaseCluster baseCluster = (BaseCluster)cluster;            
                        
                // Apply PID based position correction, which should happen before final energy correction.
                ClusterPositionCorrection.setCorrectedPosition(baseCluster);
            
                // Apply PID based energy correction.
                ClusterEnergyCorrection.setCorrectedEnergy(ecal, baseCluster, isMC);
            }
        }
    }
      
    /**
     * Apply HPS-specific energy and position corrections to a cluster without track information.
     * @param cluster The input cluster.
     */
    public static void applyCorrections(HPSEcal3 ecal, Cluster cluster, boolean isMC) {
                            
        if (cluster instanceof BaseCluster) {
            
            BaseCluster baseCluster = (BaseCluster)cluster;            
                        
            // Apply PID based position correction, which should happen before final energy correction.
            ClusterPositionCorrection.setCorrectedPosition(baseCluster);
            
            // Apply PID based energy correction.
            ClusterEnergyCorrection.setCorrectedEnergy(ecal, baseCluster, isMC);
        }        
    }   
    
    /**
     * Apply HPS-specific energy and position corrections to a cluster with track information.
     * @param cluster The input cluster.
     */
    public static void applyCorrections(HPSEcal3 ecal, Cluster cluster, double ypos, boolean isMC) {
        
        if (cluster instanceof BaseCluster) {
            
            BaseCluster baseCluster = (BaseCluster)cluster;            
                        
            // Apply PID based position correction, which should happen before final energy correction.
            ClusterPositionCorrection.setCorrectedPosition(baseCluster);
            
            // Apply PID based energy correction.
            ClusterEnergyCorrection.setCorrectedEnergy(ecal, baseCluster, ypos, isMC);
        }        
    }    
        
    /**
     * Call {@link org.lcsim.event.base.BaseCluster#calculateProperties()}
     * on all clusters in the list using the given calculator.
     * @param clusters The list of clusters.
     * @param calc The property calculator.
     */
    public static void calculateProperties(List<Cluster> clusters, ClusterPropertyCalculator calc) {
        for (Cluster cluster : clusters) {
            if (cluster instanceof BaseCluster) {
                BaseCluster baseCluster = (BaseCluster)cluster;
                baseCluster.setPropertyCalculator(calc);
                baseCluster.calculateProperties();
            }
        }
    }    
    
    /**
     * Create a map between a particle and its list of hits from a Cluster.
     * @param cluster The input cluster.
     * @return A map between a particle and its hits.
     */
    public static Map<MCParticle, List<SimCalorimeterHit>> createParticleHitMap(Cluster cluster) {
        Map<MCParticle, List<SimCalorimeterHit>> particleHitMap = new HashMap<MCParticle, List<SimCalorimeterHit>>();
        for (CalorimeterHit hit : cluster.getCalorimeterHits()) {
            if (hit instanceof SimCalorimeterHit) {
                SimCalorimeterHit simHit = (SimCalorimeterHit) hit;
                for (int i = 0; i < simHit.getMCParticleCount(); i++) {
                    MCParticle particle = simHit.getMCParticle(i);
                    if (particleHitMap.get(particle) == null) {
                        particleHitMap.put(particle, new ArrayList<SimCalorimeterHit>());
                    }
                    particleHitMap.get(particle).add(simHit);
                }
            }
        }
        return particleHitMap;
    }
    
    /**
     * Get the set of hits from a list of clusters.
     * @param clusters The input cluster list.
     * @return The list of hits from all the clusters.
     */
    public static Set<CalorimeterHit> getHits(List<Cluster> clusters) {
        Set<CalorimeterHit> hits = new HashSet<CalorimeterHit>();
        for (Cluster cluster : clusters) {
            hits.addAll(cluster.getCalorimeterHits());
        }
        return hits;
    }
    
    /**
     * This method will determine which hits are likely shared with
     * other clusters, by comparing a hit's energy to its contribution.
     * If the energy is greater than the contribution, then its energy
     * will assumed to be shared between clusters.  This convention
     * might not be true depending on the clustering algorithm, but
     * it is probably the best that can be done without the complete 
     * list of clusters in the event.
     * @param cluster The input cluster.
     */
    public static List<CalorimeterHit> findSharedHits(Cluster cluster) {
        List<CalorimeterHit> sharedHits = new ArrayList<CalorimeterHit>();
        for (int i = 0; i < cluster.getCalorimeterHits().size(); i++) {
            if (cluster.getCalorimeterHits().get(i).getCorrectedEnergy() > cluster.getHitContributions()[i]) {
                sharedHits.add(cluster.getCalorimeterHits().get(i));
            }
        }
        return sharedHits;
    }

    /**
     * This method will determine which hits are likely not shared
     * other clusters, by comparing a hit's energy to its contribution.
     * If the energy is equal to the contribution, then its energy
     * will assumed to not be shared between clusters.  This convention
     * might not be true depending on the clustering algorithm, but
     * it is probably the best that can be done without the complete list
     * of clusters in the event.
     * @param cluster The input cluster.
     */
    public static List<CalorimeterHit> findUnsharedHits(Cluster cluster) {
        List<CalorimeterHit> uniqueHits = new ArrayList<CalorimeterHit>();
        for (int i = 0; i < cluster.getCalorimeterHits().size(); i++) {
            if (cluster.getCalorimeterHits().get(i).getCorrectedEnergy() == cluster.getHitContributions()[i]) {
                uniqueHits.add(cluster.getCalorimeterHits().get(i));
            }
        }
        return uniqueHits;
    }

    /**
     * True if cluster has a type code that indicates it was generated by a hardware clustering algorithm.
     * These are not actually hardware clusters but are generated by Driver code that simulates these algorithms.
     * @param cluster The Cluster.
     * @return True if cluster is generated from hardware clustering algorithm.
     */
    public static boolean isHardwareCluster(Cluster cluster) {
        return cluster.getType() == ClusterType.CTP.getType() || 
                cluster.getType() == ClusterType.GTP.getType() || 
                cluster.getType() == ClusterType.GTP_ONLINE.getType();
    }
    
    /**
     * Comparator of cluster energies.
     */
    static final class ClusterEnergyComparator implements Comparator<Cluster> {

        /**
         * Compare cluster energies.
         * 
         * @return -1, 0, or 1 if first cluster's energy is less than, equal to, or greater than the first's
         */
        @Override
        public int compare(Cluster o1, Cluster o2) {
            if (o1.getEnergy() < o2.getEnergy()) {
                return -1;
            } else if (o1.getEnergy() > o2.getEnergy()) {
                return 1;
            } else {
                return 0;
            }
        }       
    }
    
    /**
     * Comparator of cluster energies.
     */
    static final class ClusterSeedComparator implements Comparator<Cluster> {

        /**
         * Compare cluster seed energies.
         * 
         * @return -1, 0, or 1 if first cluster's seed energy is less than, equal to, or greater than the first's
         */
        @Override
        public int compare(Cluster o1, Cluster o2) {
            if (findSeedHit(o1).getCorrectedEnergy() < findSeedHit(o2).getCorrectedEnergy()){
                return -1;
            } else if (findSeedHit(o1).getCorrectedEnergy() > findSeedHit(o2).getCorrectedEnergy()) {
                return 1;
            } else {
                return 0;
            }
        }       
    }
    
    
    
    
    /**
     * Sort a list of clusters.
     * 
     * @param clusters the list of clusters
     * @param comparator the comparator to use for sorting
     * @param inPlace <code>true</code> to sort the list in-place and not make a new list
     * @param reverse <code>true</code> to use reverse comparator (results in list ordered highest to lowest energy)
     * @return the sorted list of clusters
     */
    public static List<Cluster> sort(final List<Cluster> clusters, final Comparator<Cluster> comparator, boolean inPlace, boolean reverse) {
        List<Cluster> sortedList = null;
        if (inPlace) {
            sortedList = clusters;
        } else {
            sortedList = new ArrayList<Cluster>(clusters);
        }
        if (reverse) {
            Collections.sort(clusters, Collections.reverseOrder(comparator));
        } else {
            Collections.sort(clusters, comparator);
        }
        return sortedList;
    }
        
    /**
     * Find the highest energy cluster from the list.
     * 
     * @param clusters the list of clusters
     * @return the highest energy cluster
     */
    public static Cluster findHighestEnergyCluster(final List<Cluster> clusters) {
        if (clusters.isEmpty()) {
            throw new IllegalArgumentException("The cluster list is empty.");
        }
        return sort(clusters, new ClusterEnergyComparator(), true, true).get(0);
    }
    
    /**
     * Return the time of the seed hit.
     * @param cluster
     * @return the seed hit time
     */
    public static double getSeedHitTime(Cluster cluster) {
        return findSeedHit(cluster).getTime();
    }
}