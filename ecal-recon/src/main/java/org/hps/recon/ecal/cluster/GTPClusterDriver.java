package org.hps.recon.ecal.cluster;

/**
 * This is a Driver to wrap the GTPClusterer algorithm,
 * allowing the <code>limitClusterRange</code> to be
 * set publicly.
 * 
 * @see GTPClusterer
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class GTPClusterDriver extends ClusterDriver {
    
    public GTPClusterDriver() {
        clusterer = ClustererFactory.create("GTPClusterer");
    }
    
    /**
     * Sets whether hits should be added to a cluster from the entire
     * cluster window or just the "future" hits, plus one clock-cycle
     * of "past" hits as a safety buffer to account for time uncertainty.
     * 
     * @param limitClusterRange - <code>true</code> indicates that
     * the asymmetric clustering window should be used and <code>
     * false</code> that the symmetric window should be used.
     */
    void setLimitClusterRange(boolean limitClusterRange) {
        GTPClusterer gtpClusterer = getClusterer();
        gtpClusterer.setLimitClusterRange(limitClusterRange);
    }        
    
    public void setClusterWindow(int clusterWindow) {
        getClusterer().getCuts().setValue("clusterWindow", clusterWindow);
    }
    
    public void setSeedEnergyThreshold(double seedEnergyThreshold) {
    	getClusterer().getCuts().setValue("seedEnergyThreshold", seedEnergyThreshold);
    }
    
    public void setAsymmetricWindow(boolean asymmetricWindow) {
    	((GTPClusterer) getClusterer()).setLimitClusterRange(asymmetricWindow);
    }
}
