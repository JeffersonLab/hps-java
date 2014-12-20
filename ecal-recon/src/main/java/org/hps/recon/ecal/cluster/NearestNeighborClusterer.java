package org.hps.recon.ecal.cluster;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.base.BaseCluster;

/**
 * This is a simple (example) nearest-neighbor clustering algorithm.
 *
 * @author Norman A. Graf
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class NearestNeighborClusterer extends AbstractClusterer {

    double minHitEnergy = 0;
    double minSize = 2;
    
    public NearestNeighborClusterer() {
        super(new String[] {"minHitEnergy", "minSize" }, new double[] { 0.0, 2.0 });
    }
    
    public void initialize() {
        minHitEnergy = getCuts().getValue("minHitEnergy");
        minSize = getCuts().getValue("minSize");
    }
    
    @Override
    public List<Cluster> createClusters(EventHeader event, List<CalorimeterHit> hits) {
        Map<Long, CalorimeterHit> hitMap = ClusterUtilities.createHitMap(hits);
        // New Cluster list to be added to event.
        List<Cluster> clusters = new ArrayList<Cluster>();

        while (!hitMap.isEmpty()) {
            Long k = hitMap.keySet().iterator().next();
            CalorimeterHit hit = hitMap.get(k);
            BaseCluster cluster = this.createCluster(hitMap, hit);
            //done with this cluster, let's compute some shape properties
            if (cluster.getSize() >= minSize) {
                cluster.calculateProperties();
                clusters.add(cluster);
            }
        }
        return clusters;
    }
       
    /**
     *
     * @param map A Map of CalorimeterHit keyed on CellID, Hits added to this
     * cluster are removed from the map
     * @param hit A CalorimeterHit representing a hit crystal
     * @param threshold The energy threshold below which a hit crystal should
     * NOT be added to the cluster
     * @param neighborMap The map of HPS crystal neighbors.
     */    
    BaseCluster createCluster(Map<Long, CalorimeterHit> map, CalorimeterHit hit) {
        BaseCluster cluster = new BaseCluster();
        // start by adding this hit to the cluster
        cluster.addHit(hit);
        // remove this hit from the map so it can't be used again
        map.remove(hit.getCellID());
        List<CalorimeterHit> hits = cluster.getCalorimeterHits();
        //  loop over the hits in the cluster and add all its neighbors
        // note that hits.size() grows as we add cells, so we recursively find neighbors of neighbors
        for (int i = 0; i < hits.size(); ++i) {
            CalorimeterHit c = hits.get(i);
            // Get neighbor crystal IDs.
            Set<Long> neighbors = neighborMap.get(c.getCellID());
            // loop over all neighboring cell Ids
            for (Long neighborId : neighbors) {
                // Find the neighbor hit in the event if it exists.
                CalorimeterHit neighborHit = map.get(neighborId);
                // Was this neighbor cell hit?
                if (neighborHit != null) {
                    // if so, does it meet or exceed threshold?
                    if (neighborHit.getRawEnergy() >= minHitEnergy) {
                        cluster.addHit(neighborHit);
                    }
                    //remove this hit from the map so it can't be used again
                    map.remove(neighborHit.getCellID());
                }
            }
        }
        return cluster;
    }
}
