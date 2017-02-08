package org.hps.recon.ecal.cluster;

/**
 * This is a Driver to wrap the GTPClusterer algorithm,
 * allowing the <code>limitClusterRange</code> to be
 * set publicly.
 * 
 * @see GTPClusterer
 */
public class CTPClusterDriver extends ClusterDriver {
    
    public CTPClusterDriver() {
        clusterer = ClustererFactory.create("CTPClusterer");
        setWriteClusterCollection(false);
    }
    
    public void setClusterWindow(int clusterWindow) {
        getClusterer().getCuts().setValue("clusterWindow", clusterWindow);
    }
}
