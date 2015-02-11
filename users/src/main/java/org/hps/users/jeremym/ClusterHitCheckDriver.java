package org.hps.users.jeremym;

import java.util.List;

import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.util.Driver;

/**
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 *
 */
public class ClusterHitCheckDriver extends Driver {
    
    static String clusterCollectionName = "EcalClusters";
    static String hitCollectionName = "EcalCalHits"; 
    
    public void process(EventHeader event) {
        //System.out.println("ClusterHitCheckDriver - event #" + event.getEventNumber());
        
        if (event.hasCollection(CalorimeterHit.class, hitCollectionName)) {
            List<CalorimeterHit> hits = event.get(CalorimeterHit.class, hitCollectionName);
            for (int i = 0; i < hits.size(); i++) {
                if (hits.get(i) == null) {
                    throw new RuntimeException("CalorimeterHit at " + hitCollectionName + "[" + i + "] is null");
                }
            }
        }
        
        if (event.hasCollection(Cluster.class, clusterCollectionName)) {
            List<Cluster> clusters = event.get(Cluster.class, clusterCollectionName);
            for (Cluster cluster : clusters) { 
                //System.out.println("checking Cluster " + cluster + " of type " + cluster.getClass().getCanonicalName());
                //System.out.println("  iterating hit list...");
                for (CalorimeterHit hit : cluster.getCalorimeterHits()) {
                    if (hit == null) {
                        throw new RuntimeException("CalorimeterHit is null in cluster hit list!");
                        //System.out.println("  WARNING: CalorimeterHit is null in cluster hit list!");
                    }
                }
                List<CalorimeterHit> hits = cluster.getCalorimeterHits();
                //System.out.println(hits.size() + " hits");
                for (int i = 0; i < hits.size(); i++) {
                    CalorimeterHit hit = hits.get(i);
                    if (hit == null) {
                        throw new RuntimeException("CalorimeterHit " + i + " is null in cluster hit list!");
                        //System.out.println("  WARNING: CalorimeterHit " + i + " is null in cluster hit list!");
                        //System.out.println("    pos in hit list = " + event.get(CalorimeterHit.class, hitCollectionName).indexOf(hit));
                    }
                }
            }
        }
    }
}
