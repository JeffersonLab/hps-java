package org.hps.monitoring.ecal;

import org.lcsim.event.EventHeader;
import org.lcsim.util.Driver;

public class EcalMonitorDriver extends Driver {
    private Viewer viewer;
    private String ecalCollectionName = "EcalHits";
    private String clusterCollectionName = "EcalClusters";
    
    public void setEcalCollectionName(String ecalCollectionName) {
        this.ecalCollectionName = ecalCollectionName;
    }
    
    public void setClusterCollectionName(String clusterCollectionName) {
        this.clusterCollectionName = clusterCollectionName;
    }
    
    public void startOfData() {
        viewer = new Viewer();
        viewer.setVisible(true);
    }
    
    public void process(EventHeader event) {
        viewer.displayLCIOEvent(event, ecalCollectionName, clusterCollectionName);
    }
    
    public void endOfData() {
        viewer.setVisible(false);
        viewer = null;
    }
}
