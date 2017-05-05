package org.hps.users.jeremym;

import java.util.List;
import java.util.logging.Level;

import org.hps.detector.ecal.EcalCrystal;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.util.Driver;

/**
 * Simple Driver to check validity of CalorimeterHit objects in clusters.
 */
public class ClusterHitCheckDriver extends Driver {
    
    static String clusterCollectionName = "EcalClusters";
    static String hitCollectionName = "EcalCalHits"; 
    
    public ClusterHitCheckDriver() {
        getLogger().setLevel(Level.ALL);
    }
    
    public void process(EventHeader event) {
        getLogger().info("ClusterHitCheckDriver - event #" + event.getEventNumber());
        
        if (event.hasCollection(CalorimeterHit.class, hitCollectionName)) {
            List<CalorimeterHit> hits = event.get(CalorimeterHit.class, hitCollectionName);
            for (int i = 0; i < hits.size(); i++) {
                if (hits.get(i) == null) {
                    throw new RuntimeException("CalorimeterHit at " + hitCollectionName + "[" + i + "] is null");
                }
            }
        }
        
        if (event.hasCollection(Cluster.class, clusterCollectionName)) {
            getLogger().info("checking clus collection " + clusterCollectionName);
            List<Cluster> clusters = event.get(Cluster.class, clusterCollectionName);
            for (Cluster cluster : clusters) { 
                List<CalorimeterHit> hits = cluster.getCalorimeterHits();
                for (int i = 0; i < hits.size(); i++) {
                    getLogger().info("checking hit @ index " + i);
                    CalorimeterHit hit = hits.get(i);
                    if (hit == null) {
                        throw new RuntimeException("CalorimeterHit @ index " + i + " is null in cluster hit list.");
                    }
                    
                    EcalCrystal crystal = (EcalCrystal) hit.getDetectorElement();
                    if (crystal == null) {
                        throw new RuntimeException("Hit does not correctly point to crystal geometry.");
                    }
                    
                    double[] hitPosition = hit.getPosition();
                    getLogger().info("hit @ position " + hitPosition[0] + ", " + hitPosition[1] + ", " + hitPosition[2]);
                    if (hitPosition[0] == 0) {
                        throw new RuntimeException("Hit X position is zero for hit " + hit.getIdentifier().toHexString());
                    }
                    if (hitPosition[1] == 0) {
                        throw new RuntimeException("Hit Y position is zero for hit " + hit.getIdentifier().toHexString());
                    }
                    if (hitPosition[2] == 0) {
                        throw new RuntimeException("Hit Z position is zero for hit " + hit.getIdentifier().toHexString());
                    }
                }
            }
        }
    }
}
