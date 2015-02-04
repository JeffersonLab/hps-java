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
    
    public void process(EventHeader event) {
        System.out.println("ClusterHitCheckDriver - event #" + event.getEventNumber());
        if (event.hasCollection(Cluster.class, clusterCollectionName)) {
            List<Cluster> clusters = event.get(Cluster.class, clusterCollectionName);
            for (Cluster cluster : clusters) { 
                System.out.println("checking Cluster " + cluster + " of type " + cluster.getClass().getCanonicalName());
                System.out.println(cluster.getCalorimeterHits().size() + " hits");
                for (CalorimeterHit hit : cluster.getCalorimeterHits()) {
                    if (hit == null) {
                        System.err.println("WARNING: CalorimeterHit is null in cluster hit list!");
                    }
                }
            }
        }
    }
}
