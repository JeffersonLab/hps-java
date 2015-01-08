package org.hps.recon.ecal.cluster;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hps.recon.ecal.HPSEcalCluster;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.base.BaseCluster;

/**
 * <p>
 * This clustering algorithm creates clusters from an input CalorimeterHit collection.
 * <p>
 * It uses the basic Inner Calorimeter (IC) clustering algorithm as described in 
 * <a href="https://misportal.jlab.org/ul/Physics/Hall-B/clas/viewFile.cfm/2005-001.pdf?documentId=6">CLAS Note 2004-040</a>.
 * <p> 
 * Hits are assigned to a cluster with the largest seed hit energy.  Time information is not used, and multiple hits in the same 
 * crystal are not handled correctly so an exception is throw if this occurs.  An optional cut can be applied to discard hits
 * with a time that is too far from t0.
 *
 * @author Holly Szumila-Vance <hszumila@jlab.org>
 * @author Sho Uemura <meeg@slac.stanford.edu>
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class SimpleReconClusterer extends AbstractClusterer {
    
    double minEnergy;
    double minTime;
    double timeWindow;
    boolean timeCut;

    /**
     * Initialize the algorithm with default cuts.
     */
    SimpleReconClusterer() {
        super(new String[] { "minEnergy", "minTime", "timeWindow", "timeCut" }, new double[] { 0.001, 0.0, 20.0, 0. });
    }

    public void initialize() {
        // Setup class variables from cuts.
        timeCut = (getCuts().getValue("timeCut") == 1.0);
        minEnergy = getCuts().getValue("minEnergy");
        minTime = getCuts().getValue("minTime");
        timeWindow = getCuts().getValue("timeWindow");
    }

    public List<Cluster> createClusters(EventHeader event, List<CalorimeterHit> hitCollection) {

        // New Cluster list to be added to event.
        List<Cluster> clusters = new ArrayList<Cluster>();

        // Create a Calorimeter hit list in each event, then sort with highest energy first
        ArrayList<CalorimeterHit> sortedHitList = new ArrayList<CalorimeterHit>(hitCollection.size());
        for (CalorimeterHit h : hitCollection) {
            // reject hits below the energy cut
            if (h.getCorrectedEnergy() < this.minEnergy) {
                continue;
            }
            // if time cut is being used, reject hits outside the time window
            if (timeCut && (h.getTime() < minTime || h.getTime() > minTime + timeWindow)) {
                continue;
            }
            sortedHitList.add(h);
        }

        // sort the list, highest energy first
        Collections.sort(sortedHitList, Collections.reverseOrder(new CalorimeterHit.CorrectedEnergyComparator()));

        // map from seed hit to cluster
        Map<CalorimeterHit, BaseCluster> seedToCluster = new HashMap<CalorimeterHit, BaseCluster>();

        // Quick Map to access hits from cell IDs
        Map<Long, CalorimeterHit> idToHit = new HashMap<Long, CalorimeterHit>();

        // map from each hit to its cluster seed
        Map<CalorimeterHit, CalorimeterHit> hitToSeed = new HashMap<CalorimeterHit, CalorimeterHit>();

        // Fill Map with cell ID and hit
        for (CalorimeterHit hit : sortedHitList) {
            if (idToHit.containsKey(hit.getCellID())) {
                //System.out.println(this.getName() + ": multiple CalorimeterHits in same crystal");
                // Make this an error for now.
                throw new RuntimeException("Multiple CalorimeterHits found in same crystal.");
            }
            idToHit.put(hit.getCellID(), hit);
        }

        for (CalorimeterHit hit : sortedHitList) {
            CalorimeterHit biggestSeed = null;

            for (Long neighbor : neighborMap.get(hit.getCellID())) {
                CalorimeterHit neighborHit = idToHit.get(neighbor);
                if (neighborHit == null) {
                    continue;
                }

                if (neighborHit.getCorrectedEnergy() > hit.getCorrectedEnergy()) {
                    CalorimeterHit neighborSeed = hitToSeed.get(neighborHit);
                    if (biggestSeed == null || neighborHit.getCorrectedEnergy() > biggestSeed.getCorrectedEnergy()) {
                        biggestSeed = neighborSeed;
                    }
                }
            }
            if (biggestSeed == null) { // if no neighbors had more energy than this hit, this hit is a seed
                hitToSeed.put(hit, hit);
                BaseCluster cluster = new HPSEcalCluster(); // FIXME: Replace with BaseCluster.
                clusters.add(cluster);
                seedToCluster.put(hit, cluster);
            } else {
                hitToSeed.put(hit, biggestSeed);
            }
        }

        // add all hits to clusters
        for (CalorimeterHit hit : sortedHitList) {
            CalorimeterHit seed = hitToSeed.get(hit);
            BaseCluster cluster = seedToCluster.get(seed);
            cluster.addHit(hit);
        }
        
        return clusters;
    }
}
