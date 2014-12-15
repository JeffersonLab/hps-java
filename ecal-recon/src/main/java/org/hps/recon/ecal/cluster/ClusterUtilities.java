package org.hps.recon.ecal.cluster;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.lcsim.event.CalorimeterHit;
import org.lcsim.geometry.subdetector.HPSEcal3;

public final class ClusterUtilities {
    
    private ClusterUtilities() {        
    }

    public static Map<Long, CalorimeterHit> createHitMap(List<CalorimeterHit> hits) {
        Map<Long, CalorimeterHit> hitMap = new LinkedHashMap<Long, CalorimeterHit>();
        for (CalorimeterHit hit : hits) {
            hitMap.put(hit.getCellID(), hit);
        }
        return hitMap;
    }
    
    /**
     * Given a hit, find its list of neighboring crystals that have hits and return their IDs.
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
}
