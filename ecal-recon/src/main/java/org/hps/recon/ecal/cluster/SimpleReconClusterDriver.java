package org.hps.recon.ecal.cluster;


/**
 * This is an implementation of {@link ClusterDriver} specialized for the
 * {@link SimpleReconClusterer}.  
 *  
 * @see SimpleReconClusterer
 */
public class SimpleReconClusterDriver extends ClusterDriver {
                
    public SimpleReconClusterDriver() {
        // Setup the Clusterer with the correct type.
        clusterer = ClustererFactory.create("SimpleReconClusterer");
    }
    
    public void setUseTimeCut(boolean useTimeCut) {
        SimpleReconClusterer clusterer = getClusterer();
        clusterer.setUseTimeCut(useTimeCut);
    }    
}
