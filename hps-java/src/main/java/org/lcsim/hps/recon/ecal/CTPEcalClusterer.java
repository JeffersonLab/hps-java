package org.lcsim.hps.recon.ecal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import org.lcsim.event.CalorimeterHit;
//import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.IDDecoder;
import org.lcsim.geometry.subdetector.HPSEcal3.NeighborMap;
import org.lcsim.geometry.subdetector.HPSEcal3;
import org.lcsim.hps.recon.ecal.CTPEcalClusterer.TimeComparator;
import org.lcsim.util.Driver;
import org.lcsim.lcio.LCIOConstants;

/**
 * Creates clusters from CalorimeterHits in the HPSEcal detector.
 *
 * The clustering algorithm is from JLab Hall B 6 GeV DVCS Trigger Design doc.
 *
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 * @author Tim Nelson <tknelson@slac.stanford.edu>
 * @author Sho Uemura <meeg@slac.stanford.edu>
 * @version $Id: CTPEcalClusterer.java,v 1.1 2013/02/25 22:39:24 meeg Exp $
 */
public class CTPEcalClusterer extends Driver {

    HPSEcal3 ecal;
    IDDecoder dec;
    String ecalName;
    String ecalCollectionName;
    String clusterCollectionName = "EcalClusters";
    Set<Long> clusterCenters = null;
    Map<Long, Double> hitSums = null;
    Map<Long, CalorimeterHit> hitMap = null;
    // Map of crystals to their neighbors.
    NeighborMap neighborMap = null;
    double clusterWindow = -1;
    double addEMin = 0;

    public CTPEcalClusterer() {
    }

    public void setAddEMin(double addEMin) {
        this.addEMin = addEMin;
    }

    public void setClusterWindow(double clusterWindow) {
        this.clusterWindow = clusterWindow;
    }

    public void setClusterCollectionName(String clusterCollectionName) {
        this.clusterCollectionName = clusterCollectionName;
    }

    public void setEcalCollectionName(String ecalCollectionName) {
        this.ecalCollectionName = ecalCollectionName;
    }

    public void setEcalName(String ecalName) {
        this.ecalName = ecalName;
    }

    @Override
    public void startOfData() {
        if (ecalCollectionName == null) {
            throw new RuntimeException("The parameter ecalCollectionName was not set!");
        }

        if (ecalName == null) {
            throw new RuntimeException("The parameter ecalName was not set!");
        }
    }

    @Override
    public void detectorChanged(Detector detector) {
        // Get the Subdetector.
        ecal = (HPSEcal3) detector.getSubdetector(ecalName);

        // Get the decoder for the ECal IDs.
        dec = ecal.getIDDecoder();

        // Cache ref to neighbor map.
        neighborMap = ecal.getNeighborMap();

        clusterCenters = new HashSet<Long>();
        //Make set of valid cluster centers.
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

        //System.out.println(ecal.getName());
        //System.out.println("  nx="+ecal.nx());
        //System.out.println("  ny="+ecal.ny());
        //System.out.println("  beamgap="+ecal.beamGap());
        //System.out.println("  dface="+ecal.distanceToFace());

        //System.out.println(neighborMap.toString());
    }

    @Override
    public void process(EventHeader event) {
        //System.out.println(this.getClass().getCanonicalName() + " - process");

        if (event.hasCollection(CalorimeterHit.class, ecalCollectionName)) {
            // Get the list of raw ECal hits.
            List<CalorimeterHit> hits = event.get(CalorimeterHit.class, ecalCollectionName);

            List<HPSEcalCluster> clusters;

            if (clusterWindow >= 0) {
                PriorityQueue<CalorimeterHit> futureHits = new PriorityQueue<CalorimeterHit>(10, new TimeComparator());
                PriorityQueue<CalorimeterHit> pastHits = new PriorityQueue<CalorimeterHit>(10, new TimeComparator());
                clusters = new ArrayList<HPSEcalCluster>();

                for (CalorimeterHit hit : hits) {
                    if (hit.getRawEnergy() > addEMin) {
                        futureHits.add(hit);
                    }
                }

                while (!futureHits.isEmpty()) {
                    CalorimeterHit nextHit = futureHits.poll();
                    pastHits.add(nextHit);
                    while (!futureHits.isEmpty() && futureHits.peek().getTime() == nextHit.getTime()) {
                        pastHits.add(futureHits.poll());
                    }
                    while (pastHits.peek().getTime() < nextHit.getTime() - clusterWindow) {
                        pastHits.poll();
                    }
                    sumHits(pastHits);
                    clusters.addAll(createClusters());
                }
            } else {
                sumHits(hits);
                clusters = createClusters();
            }

            // Put Cluster collection into event.
            int flag = 1 << LCIOConstants.CLBIT_HITS;
            event.put(clusterCollectionName, clusters, HPSEcalCluster.class, flag);
        }
    }

    public void sumHits(Collection<CalorimeterHit> hits) {
        // Hit map.
        hitMap = new HashMap<Long, CalorimeterHit>();

        hitSums = new HashMap<Long, Double>();
        // Loop over ECal hits to compute energy sums
        for (CalorimeterHit hit : hits) {
//                System.out.format("hit: %f\n",hit.getRawEnergy());
            // Make a hit map for quick lookup by ID.
            hitMap.put(hit.getCellID(), hit);

            // Get neighbor crystal IDs. 
            Set<Long> neighbors = neighborMap.get(hit.getCellID());

            if (neighbors == null) {
                throw new RuntimeException("Oops!  Set of neighbors is null!");
            }

            Double hitSum;

            if (clusterCenters.contains(hit.getCellID())) {
                hitSum = hitSums.get(hit.getCellID());
                if (hitSum == null) {
                    hitSums.put(hit.getCellID(), hit.getRawEnergy());
                } else {
                    hitSums.put(hit.getCellID(), hitSum + hit.getRawEnergy());
                }
            }

            // Loop over neighbors to make hit list for cluster.
            for (Long neighborId : neighbors) {
                if (!clusterCenters.contains(neighborId)) {
                    continue;
                }
                hitSum = hitSums.get(neighborId);
                if (hitSum == null) {
                    hitSums.put(neighborId, hit.getRawEnergy());
                } else {
                    hitSums.put(neighborId, hitSum + hit.getRawEnergy());
                }
            }
        }
    }

    public List<HPSEcalCluster> createClusters() {
//		boolean printClusters;
        // New Cluster list to be added to event.
        List<HPSEcalCluster> clusters = new ArrayList<HPSEcalCluster>();
        //System.out.println("New event");
        //for each crystal with a nonzero hit count, test for cluster
        for (Long possibleCluster : hitSums.keySet()) {
            Double thisSum = hitSums.get(possibleCluster);

            // Get neighbor crystal IDs.
            Set<Long> neighbors = neighborMap.get(possibleCluster);

            if (neighbors == null) {
                throw new RuntimeException("Oops!  Set of neighbors is null!");
            }

            //Apply peak detector scheme.
            // Set the ID.
            dec.setID(possibleCluster);
            int x1 = dec.getValue("ix");
            int y1 = dec.getValue("iy");
//			System.out.printf("\nThis cluster: E= %f, ID=%d, x=%d, y=%d, neighbors=%d\n", thisSum, possibleCluster, x1, y1, neighbors.size());
            boolean isCluster = true;
            for (Long neighborId : neighbors) {
                // Set the ID.
                dec.setID(neighborId);
                int x2 = dec.getValue("ix");
                int y2 = dec.getValue("iy");

                Double neighborSum = hitSums.get(neighborId);
                if (neighborSum == null) {
                    continue;
                }

//				System.out.printf("Neighbor cluster: E= %f, ID=%d, x=%d, y=%d, neighbors=%d\n", neighborSum, neighborId, x2, y2, neighborMap.get(neighborId).size());

                if (neighborSum > thisSum) {
//					System.out.println("Reject cluster: sum cut");
                    isCluster = false;
                    break;
                } //								else if (false) { //ctp
                //								else if (neighborSum.equals(thisSum) && neighborId > possibleCluster) { //id
                //								else if (neighborSum.equals(thisSum) && (x1<x2 || (x1==x2 && Math.abs(y1)>Math.abs(y2)))) { //right_in
                //								else if (neighborSum.equals(thisSum) && (x1<x2 || (x1==x2 && Math.abs(y1)<Math.abs(y2)))) { //right_out
                //								else if (neighborSum.equals(thisSum) && (x1>x2 || (x1==x2 && Math.abs(y1)>Math.abs(y2)))) { //left_in
                else if (neighborSum.equals(thisSum) && (x1 > x2 || (x1 == x2 && Math.abs(y1) < Math.abs(y2)))) { //left_out
//								else if (neighborSum.equals(thisSum) && (x1<x2 || (x1==x2 && y1<y2))) { //right_up
//								else if (neighborSum.equals(thisSum) && (x1>x2 || (x1==x2 && y1<y2))) { //left_up
//					System.out.println("Reject cluster: tie-breaker cut");
                    isCluster = false;
                    break;
                }
            }

            if (isCluster) {
                List<CalorimeterHit> hits = new ArrayList<CalorimeterHit>();
                double clusterTime = Double.NEGATIVE_INFINITY;
                CalorimeterHit hit = hitMap.get(possibleCluster);
                if (hit != null) {
                    hits.add(hit);
                    if (hit.getTime() > clusterTime) {
                        clusterTime = hit.getTime();
                    }
                }
                for (Long neighborId : neighbors) {
                    hit = hitMap.get(neighborId);
                    if (hit != null) {
                        hits.add(hit);
                        if (hit.getTime() > clusterTime) {
                            clusterTime = hit.getTime();
                        }
                    }
                }
                CalorimeterHit seedHit = new HPSCalorimeterHit(0.0, clusterTime, possibleCluster, hits.get(0).getType());
                seedHit.setMetaData(hits.get(0).getMetaData());
                HPSEcalCluster cluster = new HPSEcalCluster(seedHit);
                for (CalorimeterHit clusterHit : hits) {
                    cluster.addHit(clusterHit);
                }
                clusters.add(cluster);
//                System.out.println(cluster.getEnergy());
            }
        }
        return clusters;
    }

    static class TimeComparator implements Comparator<CalorimeterHit> {

        @Override
        public int compare(CalorimeterHit o1, CalorimeterHit o2) {
            if (o1.getTime() == o2.getTime()) {
                return 0;
            } else {
                return (o1.getTime() > o2.getTime()) ? 1 : -1;
            }
        }
    }
}
