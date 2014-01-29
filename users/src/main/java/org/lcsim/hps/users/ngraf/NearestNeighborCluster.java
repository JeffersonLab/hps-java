package org.lcsim.hps.users.ngraf;

import java.util.Map;
import java.util.Set;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.base.BaseCluster;
import org.lcsim.geometry.subdetector.HPSEcal3;

/**
 * This is a cluster created using a nearest-neighbor clustering algorithm. It
 * currently extends BaseCluster but should extend a base HPS Cluster at some
 * time
 *
 * @author Norman A Graf
 *
 * @version $Id:
 */
public class NearestNeighborCluster extends BaseCluster
{

    /**
     *
     * @param map A Map of CalorimeterHit keyed on CellID, Hits added to this
     * cluster are removed from the map
     * @param hit A CalorimeterHit representing a hit crystal
     * @param threshold The energy threshold below which a hit crystal should
     * NOT be added to the cluster
     * @param neighborMap The map of HPS crystal neighbors.
     */
    public NearestNeighborCluster(Map<Long, CalorimeterHit> map, CalorimeterHit hit, double threshold, HPSEcal3.NeighborMap neighborMap)
    {
        // start by adding this hit to the cluster
        addHit(hit);
        // remove this hit from the map so it can't be used again
        map.remove(hit.getCellID());
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
                    if (neighborHit.getRawEnergy() >= threshold) {
                        addHit(neighborHit);
                    }
                    //remove this hit from the map so it can't be used again
                    map.remove(neighborHit.getCellID());
                }
            }
        }
    }
}
