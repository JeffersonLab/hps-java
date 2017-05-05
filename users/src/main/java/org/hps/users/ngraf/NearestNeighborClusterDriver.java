package org.hps.users.ngraf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hps.recon.ecal.EcalUtils;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.EventHeader;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.subdetector.HPSEcal3;
import org.lcsim.lcio.LCIOConstants;
import org.lcsim.util.Driver;

/**
 * This is a test
 */
public class NearestNeighborClusterDriver extends Driver
{
    // Minimum E for cluster seed.
    double seedEMin = .00 * EcalUtils.GeV;
    // Minimum E to add hit to cluster.
    double addEMin = .00 * EcalUtils.GeV;
    // Map of crystals to their neighbors.
    HPSEcal3.NeighborMap _neighborMap = null;
    String ecalCollectionName;
    String ecalName = "Ecal";
    String clusterCollectionName = "NearestNeighborEcalClusters";

    public void setEcalName(String ecalName)
    {
        this.ecalName = ecalName;
    }

    public void setEcalCollectionName(String ecalCollectionName)
    {
        this.ecalCollectionName = ecalCollectionName;
    }

    public void setClusterCollectionName(String clusterCollectionName)
    {
        this.clusterCollectionName = clusterCollectionName;
    }

    public void setSeedEMin(double seedEMin)
    {
        this.seedEMin = seedEMin;
    }

    public void setAddEMin(double addEMin)
    {
        this.addEMin = addEMin;
        if (seedEMin < addEMin) {
            seedEMin = addEMin;
        }
    }

    @Override
    protected void process(EventHeader event)
    {
        // Fetch the list of CalorimeterHits
        if (event.hasCollection(CalorimeterHit.class, ecalCollectionName)) {
            
            List<CalorimeterHit> hits = event.get(CalorimeterHit.class, ecalCollectionName);

            // Make a hit map for quick lookup by ID.
            Map<Long, CalorimeterHit> hitMap = new HashMap<Long, CalorimeterHit>();
            for (CalorimeterHit hit : hits) {
                hitMap.put(hit.getCellID(), hit);
            }

            // Cluster the hits using a nearest neighbor algorithm
            List<NearestNeighborCluster> clusters = createNearestNeighborClusters(hitMap);
            
            //add the list of clusters to the event
            int flag = 1 << LCIOConstants.CLBIT_HITS;
            event.put(clusterCollectionName, clusters, NearestNeighborCluster.class, flag);
        }
    }

    @Override
    protected void detectorChanged(Detector detector)
    {
        // Get the Subdetector.
        HPSEcal3 ecal = (HPSEcal3) detector.getSubdetector(ecalName);
        // Cache the neighbor map for use by the nearest neighbor clustering algorithm
        _neighborMap = ecal.getNeighborMap();
    }

    /**
     * Run the clustering algorithm over the list of hit crystals
     *
     * @param map Map of CalorimeterHit keyed on CellID. Note that this map is
     * modified
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
