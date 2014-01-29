package org.lcsim.hps.users.ngraf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.MCParticle;
import org.lcsim.event.SimCalorimeterHit;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.subdetector.HPSEcal3;
import org.lcsim.hps.recon.ecal.ECalUtils;
import org.lcsim.lcio.LCIOConstants;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 * This is a test
 * @author Norman A Graf
 *
 * @version $Id:
 */
public class NearestNeighborClusterDriver extends Driver
{
    // Histogram manager
    private AIDA aida;
    // Minimum E for cluster seed.
    double seedEMin = .00 * ECalUtils.GeV;
    // Minimum E to add hit to cluster.
    double addEMin = .00 * ECalUtils.GeV;
    // Map of crystals to their neighbors.
    HPSEcal3.NeighborMap _neighborMap = null;

    @Override
    public void processChildren(EventHeader event)
    {
        boolean debug = true;
        // This is example code to run over a simple lcio file output from slic
        // on which no calorimeter digitzation has been done.
        // Therefore need to convert from SimCalorimeterHit to CalorimeterHit
        // This would normally be done as a part of the reconstruction
        if (event.hasCollection(SimCalorimeterHit.class, "EcalHits")) {
            // Get the list of sim ECal hits.
            List<SimCalorimeterHit> hits = event.get(SimCalorimeterHit.class, "EcalHits");

            // create a list of CalorimeterHits
            // Make a hit map for quick lookup by ID.
            Map<Long, CalorimeterHit> hitMap = new HashMap<Long, CalorimeterHit>();
            for (SimCalorimeterHit hit : hits) {
                hitMap.put(hit.getCellID(), hit);
            }

            // Cluster the hits using a nearest neighbor algorithm
            int flag = 1 << LCIOConstants.CLBIT_HITS;
            List<NearestNeighborCluster> clusters = createNearestNeighborClusters(hitMap);
            // quick analysis example
            if(debug) System.out.println("found " + clusters.size() + " clusters");
            double eTop = 0.;
            double eBottom = 0.;
            for (Cluster clus : clusters) {
                if(debug)System.out.println("x: " + clus.getPosition()[0] + " y: " + clus.getPosition()[1] + " iPhi: " + clus.getIPhi() + " iTheta " + clus.getITheta());
                if(clus.getPosition()[1]>0)
                {
                    eTop+=clus.getEnergy();
                }
                else
                {
                    eBottom += clus.getEnergy();
                }
            }
            //fill the histograms
            aida.cloud1D("Top energy sum").fill(eTop);
            aida.cloud1D("Bottom energy sum").fill(eBottom);
            aida.cloud1D("energy diff").fill(eTop-eBottom);
            //add the list of clusters to the event
            event.put("NearestNeighborClusters", clusters, NearestNeighborCluster.class, flag);
        }
    }

    @Override
    protected void detectorChanged(Detector detector)
    {
        // Get the Subdetector.
        HPSEcal3 ecal = (HPSEcal3) detector.getSubdetector("Ecal");
        // Cache the neighbor map for use by the nearest neighbor clustering algorithm
        _neighborMap = ecal.getNeighborMap();
        // set up some plotting infrastructure
        aida = AIDA.defaultInstance();
        aida.tree().cd("/");
    }

    /**
     * Run the clustering algorithm over the list of hit crystals
     * @param map Map of CalorimeterHit keyed on CellID. Note that this map is modified
     * @return The list of found clusters
     */
    public List<NearestNeighborCluster> createNearestNeighborClusters(Map<Long, CalorimeterHit> map)
    {
        // New Cluster list to be added to event.
        List<NearestNeighborCluster> clusters = new ArrayList<NearestNeighborCluster>();

        while (!map.isEmpty()) {
            Long k = map.keySet().iterator().next();
            CalorimeterHit hit = map.get(k);
            NearestNeighborCluster nnclus = new NearestNeighborCluster(map, hit, addEMin, _neighborMap);
            //done with this cluster, let's compute some shape properties
            if (nnclus.getSize() > 1) {
                nnclus.calculateProperties();
                clusters.add(nnclus);
            }
        }
        return clusters;
    }
}