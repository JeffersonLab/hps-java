package org.hps.recon.ecal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.EventHeader;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.subdetector.HPSEcal3;
import org.lcsim.geometry.subdetector.HPSEcal3.NeighborMap;
import org.lcsim.lcio.LCIOConstants;
import org.lcsim.util.Driver;

/**
 * This Driver creates clusters from the CalorimeterHits of an
 * {@link org.lcsim.geometry.subdetector.HPSEcal3} detector.
 *
 * Uses basic IC clustering algorithm as given in CLAS note 2004-040: no common
 * hits (hits are assigned to cluster with largest seed hit energy).
 *
 * Hit time information is not used, and multiple hits in the same crystal are
 * not handled correctly (a warning is printed); optional time cut is applied at
 * the beginning to discard hits too far from t0.
 *
 *
 * @author Holly Szumila-Vance <hszumila@jlab.org>
 * @author Sho Uemura <meeg@slac.stanford.edu>
 *
 */
public class EcalClusterICBasic extends Driver {

    HPSEcal3 ecal;
    String ecalCollectionName;
    String ecalName = "Ecal";
    String clusterCollectionName = "EcalClusters";
    // Map of crystals to their neighbors.
    NeighborMap neighborMap = null;
    //Minimum energy that counts as hit
    double Emin = 0.001;
    boolean timeCut = false;
    double minTime = 0.0;
    double timeWindow = 20.0;

    public void setClusterCollectionName(String clusterCollectionName) {
        this.clusterCollectionName = clusterCollectionName;
    }

    public void setEcalCollectionName(String ecalCollectionName) {
        this.ecalCollectionName = ecalCollectionName;
    }

    public void setEcalName(String ecalName) {
        this.ecalName = ecalName;
    }

    /**
     * Minimum energy for a hit to be used in a cluster. Default of 0.001 GeV..
     *
     * @param Emin
     */
    public void setEmin(double Emin) {
        this.Emin = Emin;
    }

    /**
     * Apply time cuts to hits. Defaults to false.
     *
     * @param timeCut
     */
    public void setTimeCut(boolean timeCut) {
        this.timeCut = timeCut;
    }

    /**
     * Minimum hit time, if timeCut is true. Default of 0 ns.
     *
     * @param minTime
     */
    public void setMinTime(double minTime) {
        this.minTime = minTime;
    }

    /**
     * Width of time window, if timeCut is true. Default of 20 ns.
     *
     * @param timeWindow
     */
    public void setTimeWindow(double timeWindow) {
        this.timeWindow = timeWindow;
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

        // Cache ref to neighbor map.
        neighborMap = ecal.getNeighborMap();
    }

    @Override
    public void process(EventHeader event) {
        if (event.hasCollection(CalorimeterHit.class, ecalCollectionName)) {
            // Get the list of raw ECal hits.
            List<CalorimeterHit> hits = event.get(CalorimeterHit.class, ecalCollectionName);

            //System.out.println("Number of ECal hits: "+hitMap.size());

            // Put Cluster collection into event.
            int flag = 1 << LCIOConstants.CLBIT_HITS; //store list of hits
            try {
                event.put(clusterCollectionName, createClusters(hits), HPSEcalCluster.class, flag);
            } catch (IOException e) {
            }
        }
    }

    private List<HPSEcalCluster> createClusters(List<CalorimeterHit> allHits) throws IOException {

        // New Cluster list to be added to event.
        List<HPSEcalCluster> clusters = new ArrayList<HPSEcalCluster>();

        //Create a Calorimeter hit list in each event, then sort with highest energy first
        ArrayList<CalorimeterHit> sortedHitList = new ArrayList<CalorimeterHit>(allHits.size());
        for (CalorimeterHit h : allHits) {
            //reject hits below the energy cut
            if (h.getCorrectedEnergy() < Emin) {
                continue;
            }
            //if time cut is being used, reject hits outside the time window
            if (timeCut && (h.getTime() < minTime || h.getTime() > minTime + timeWindow)) {
                continue;
            }
            sortedHitList.add(h);
        }
        
        //sort the list, highest energy first
        Collections.sort(sortedHitList, Collections.reverseOrder(new EnergyComparator()));

        //map from seed hit to cluster
        Map<CalorimeterHit, HPSEcalCluster> seedToCluster = new HashMap<CalorimeterHit, HPSEcalCluster>();

        //Quick Map to access hits from cell IDs
        Map<Long, CalorimeterHit> idToHit = new HashMap<Long, CalorimeterHit>();

        //map from each hit to its cluster seed
        Map<CalorimeterHit, CalorimeterHit> hitToSeed = new HashMap<CalorimeterHit, CalorimeterHit>();

        //Fill Map with cell ID and hit
        for (CalorimeterHit hit : sortedHitList) {
            if (idToHit.containsKey(hit.getCellID())) {
                System.out.println(this.getName() + ": multiple CalorimeterHits in same crystal");
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
            if (biggestSeed == null) { //if no neighbors had more energy than this hit, this hit is a seed
                hitToSeed.put(hit, hit);
                HPSEcalCluster cluster = new HPSEcalCluster(hit.getCellID());
                clusters.add(cluster);
                seedToCluster.put(hit, cluster);
            } else {
                hitToSeed.put(hit, biggestSeed);
            }
        }

        //add all hits to clusters
        for (CalorimeterHit hit : sortedHitList) {
            CalorimeterHit seed = hitToSeed.get(hit);
            HPSEcalCluster cluster = seedToCluster.get(seed);
            cluster.addHit(hit);
        }

        return clusters;
    }

    private class EnergyComparator implements Comparator<CalorimeterHit> {

        @Override
        public int compare(CalorimeterHit o1, CalorimeterHit o2) {
            return Double.compare(o1.getCorrectedEnergy(), o2.getCorrectedEnergy());
        }
    }
}
