package org.hps.recon.ecal.cluster;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import org.hps.recon.ecal.CalorimeterHitUtilities;
import org.lcsim.conditions.ConditionsEvent;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.base.BaseCalorimeterHit;
import org.lcsim.event.base.BaseCluster;
import org.lcsim.geometry.IDDecoder;

/**
 * Creates clusters from CalorimeterHits in the HPSEcal detector.
 *
 * The clustering algorithm is from JLab Hall B 6 GeV DVCS Trigger Design doc.
 *
 * @author Kyle McCarty
 * @author Sho Uemura <meeg@slac.stanford.edu>
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class CTPClusterer extends AbstractClusterer {

    IDDecoder dec;
    
    Set<Long> clusterCenters = null;
    Map<Long, Double> hitSums = null;
    Map<Long, CalorimeterHit> hitMap = null;

    // The time period in which clusters may be formed. A negative value means that all hits
    // will always be used in cluster finding, regardless of the time difference between them.
    double clusterWindow = -1;
    
    // The minimum energy needed for a hit to be considered.
    double addEMin = 0;
    
    CTPClusterer() {
        super(new String[] { "addEMin", "clusterWindow"}, new double[] { 0., -1. });
    }
    
    public void initialize() {
        addEMin = getCuts().getValue("addEMin");
        clusterWindow = getCuts().getValue("clusterWindow");
    }
         
    @Override
    public void conditionsChanged(ConditionsEvent event) {
        super.conditionsChanged(event);
        
        // Get the decoder for the ECal IDs.
        dec = ecal.getIDDecoder();    
                
        // Make set of valid cluster centers.
        // Exclude edge crystals as good cluster centers.
        clusterCenters = new HashSet<Long>();
        for (Long cellID : neighborMap.keySet()) {
            boolean isValidCenter = true;
            Set<Long> neighbors = neighborMap.get(cellID);
            for (Long neighborID : neighbors) {
                Set<Long> neighborneighbors = new HashSet<Long>();
                neighborneighbors.addAll(neighborMap.get(neighborID));
                neighborneighbors.add(neighborID);

                if (neighborneighbors.containsAll(neighbors)) {
                    isValidCenter = false;
                    break;
                }
            }
            if (isValidCenter) {
                clusterCenters.add(cellID);
            }
        }
    }

    @Override
    public List<Cluster> createClusters(EventHeader event, List<CalorimeterHit> hits) {

        // Define a list of clusters to be filled.
        List<Cluster> clusters;

        // If there is a cluster window, run the cluster window code. A cluster window is a time
        // period in nanoseconds during which hits can be applied to the same cluster.
        if (clusterWindow >= 0) {
            // Create priority queues. These are sorted by the time variable associated with each hit.
            PriorityQueue<CalorimeterHit> futureHits = new PriorityQueue<CalorimeterHit>(10, new TimeComparator());
            PriorityQueue<CalorimeterHit> pastHits = new PriorityQueue<CalorimeterHit>(10, new TimeComparator());

            // Initialize the cluster list variable.
            clusters = new ArrayList<Cluster>();

            // Populate the list of unprocessed hits with the calorimeter hits. These will then be sorted
            // by time, from first to last, automatically by the priority queue.
            for (CalorimeterHit hit : hits) {
                if (hit.getRawEnergy() > addEMin) {
                    futureHits.add(hit);
                }
            }

            // We process the unprocessed hits...
            while (!futureHits.isEmpty()) {
                // Move the first occurring hit from the unprocessed list to the processed list.
                CalorimeterHit nextHit = futureHits.poll();
                pastHits.add(nextHit);

                // Add any hits that occurred at the same time as the hit we just added to the processed list.
                while (!futureHits.isEmpty() && futureHits.peek().getTime() == nextHit.getTime()) {
                    pastHits.add(futureHits.poll());
                }

                // Remove hits that happened earlier than the cluster window period.
                while (pastHits.peek().getTime() < nextHit.getTime() - clusterWindow) {
                    pastHits.poll();
                }

                // Calculate the cluster energy for each crystal. This should be the
                // total energy for the 3x3 crystal collection sorrounding the center
                // crystal.
                sumHits(pastHits);

                // Choose which crystal is the appropriate cluster crystal.
                clusters.addAll(createClusters());
            }
            // If there is no cluster window, then all the hits in the event are visible simultaneously.
        } else {
            // Calculate the cluster energy of each crystal.
            sumHits(hits);
            // Generate the clusters.
            clusters = createClusters();
        }
        
        return clusters;
    }

    private void sumHits(Collection<CalorimeterHit> hits) {
        // Store the latest hit on each crystal in a map for later reference in
        // the clustering algorithm.
        hitMap = new HashMap<Long, CalorimeterHit>();
        // Store the cluster energy for each crystal. Cluster energy represents
        // the total energy of the 3x3 crystal set.
        hitSums = new HashMap<Long, Double>();

        // Loop over the active calorimeter hits to compute the cluster energies.
        for (CalorimeterHit hit : hits) {
            // Make a hit map for quick lookup by ID.
            hitMap.put(hit.getCellID(), hit);

            // Get the cell ID for the current crystal's neighbors.
            Set<Long> neighbors = neighborMap.get(hit.getCellID());

            // If there are no neighbors, something is rather wrong.
            if (neighbors == null) {
                throw new RuntimeException("Oops!  Set of neighbors is null!");
            }

            // Store the energy of the current calorimeter hit.
            Double hitSum;

            // We are only interested in this crystal's cluster energy if it is
            // a valid cluster crystal. Edge crystals are not allowed to be clusters,
            // so these are ignored.
            if (clusterCenters.contains(hit.getCellID())) {
                // Check if an energy has been assigned to the crystal.
                hitSum = hitSums.get(hit.getCellID());

                // If not, then the crystal's cluster energy is equal to this hit's energy.
                if (hitSum == null) {
                    hitSums.put(hit.getCellID(), hit.getRawEnergy());
                }
                // Otherwise, add the energy of this hit to the total crystal cluster energy.
                else {
                    hitSums.put(hit.getCellID(), hitSum + hit.getRawEnergy());
                }
            }

            // Loop over neighbors to add the current hit's energy to the neighbor's
            // cluster energy.
            for (Long neighborId : neighbors) {
                // If the crystal is not an edge crystal, ignore its hit energy.
                if (!clusterCenters.contains(neighborId)) {
                    continue;
                }

                // Get the cluster energy of the neighboring crystals.
                hitSum = hitSums.get(neighborId);

                // If the neighbor crystal has no cluster energy, then set the
                // cluster energy to the current hit's energy.
                if (hitSum == null) {
                    hitSums.put(neighborId, hit.getRawEnergy());
                }
                // Otherwise, add the hit's energy to the neighbor's cluster energy.
                else {
                    hitSums.put(neighborId, hitSum + hit.getRawEnergy());
                }
            }
        }
    }

    private List<Cluster> createClusters() {
        // Create a list of clusters to be added to the event,
        List<Cluster> clusters = new ArrayList<Cluster>();

        // We examine each crystal with a non-zero cluster energy.
        for(Long possibleCluster : hitSums.keySet()) {
            // Get the luster energy for the crystal this hit is assocaite with.
            Double thisSum = hitSums.get(possibleCluster);

            // Get neighboring crystals' IDs.
            Set<Long> neighbors = neighborMap.get(possibleCluster);

            // If there are no neighbors, throw an error.
            if (neighbors == null) {
                throw new RuntimeException("Oops!  Set of neighbors is null!");
            }

            // Get the x/y position of the hit's associated crystal.
            dec.setID(possibleCluster);
            int x1 = dec.getValue("ix");
            int y1 = dec.getValue("iy");

            // Store whether it is a valid cluster or not.
            boolean isCluster = true;

            // Check to see if any of the crystal's neighbors preclude the current crystal
            // from being a proper cluster. The cluster crystal should have the highest
            // energy among its neighbors
            for (Long neighborId : neighbors) {
                // Get the x/y position of the neighbor's associated crystal.
                dec.setID(neighborId);
                int x2 = dec.getValue("ix");
                int y2 = dec.getValue("iy");

                // If the neighbor's energy value does not exist, we don't need to perform
                // any additional checks for this neighbor. A crystal with no energy can
                // not be the center of a cluster.
                Double neighborSum = hitSums.get(neighborId);
                if (neighborSum == null) {
                    continue;
                }

                // If the neighbor's energy value is greater than this crystal's value,
                // then this crystal is not the cluster and we may terminate the check.
                if (neighborSum > thisSum) {
                    isCluster = false;
                    break;
                }
                // If the crystals each have the same energy, we choose the crystal that
                // is closest to the electron side of the detector. If both crystals are
                // equally close, we choose the crystal closest to the beam gap. If the
                // neighbor fits these parameters better, this is not a crystal and we
                // may skip any further checks.
                else if (neighborSum.equals(thisSum) && (x1 > x2 || (x1 == x2 && Math.abs(y1) < Math.abs(y2)))) {
                    isCluster = false;
                    break;
                }
            }

            // If the crystal was not invalidated by the any of the neighboring crystals,
            // then it is a cluster crystal and should be processed.
            if (isCluster) {
                // Make a list to store the hits that are part of this cluster.
                List<CalorimeterHit> hits = new ArrayList<CalorimeterHit>();

                // Store the time at which the cluster occurred.
                double clusterTime = Double.NEGATIVE_INFINITY;

                // Get the last hit on this crystal.
                CalorimeterHit hit = hitMap.get(possibleCluster);

                // If the hit exists, add it to the list of associated hits.
                if (hit != null) {
                    hits.add(hit);

                    // If the latest hit's time is later than the current cluster time,
                    // set the cluster time to the latest hit's time.
                    if (hit.getTime() > clusterTime) {
                        clusterTime = hit.getTime();
                    }
                }

                // Add all of the neighboring crystals to the cluster, if they have a
                // hit associated with them. Crystals with no hits are not actually part
                // of a cluster.
                for (Long neighborId : neighbors) {
                    hit = hitMap.get(neighborId);
                    if (hit != null) {
                        hits.add(hit);
                        if (hit.getTime() > clusterTime) {
                            clusterTime = hit.getTime();
                        }
                    }
                }

                // Generate a new cluster seed hit from the above results.
                CalorimeterHit seedHit = (BaseCalorimeterHit)CalorimeterHitUtilities.create(0.0, clusterTime, possibleCluster, hits.get(0).getMetaData());

                // Generate a new cluster from the seed hit.
                BaseCluster cluster = createBasicCluster();
                cluster.addHit(seedHit);
                cluster.setPosition(seedHit.getDetectorElement().getGeometry().getPosition().v());
                cluster.setNeedsPropertyCalculation(false);
                // Populate the cluster with each of the chosen neighbors.
                for (CalorimeterHit clusterHit : hits) {
                    cluster.addHit(clusterHit);
                }
                // Add the cluster to the cluster list.
                clusters.add(cluster);
            }
        }

        // Return the list of clusters.
        return clusters;
    }

    static class TimeComparator implements Comparator<CalorimeterHit> {
        // Compare by time with the earlier coming before the later.
        public int compare(CalorimeterHit o1, CalorimeterHit o2) {
            if (o1.getTime() == o2.getTime()) {
                return 0;
            } else {
                return (o1.getTime() > o2.getTime()) ? 1 : -1;
            }
        }
    }

    @Override
    public ClusterType getClusterType() {
        return ClusterType.CTP;
    }
}
