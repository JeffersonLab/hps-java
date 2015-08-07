package org.hps.recon.ecal.cluster;

/**
 * Class <code>GTPClusterDriver</code> is an implementation of the
 * <code>ClusterDriver</code> class that defines employs the readout
 * variant of the GTP hardware clustering algorithm. Specifics on the
 * behavior of this algorithm can be found in its documentation.<br/>
 * <br/>
 * <code>GTPClusterDriver</code> allows for all of the variable settings
 * used by the GTP algorithm to be defined. It also can be set to
 * "verbose" mode, where it will output detailed information on each
 * event and the cluster forming process. This is disabled by default,
 * but can be enabled for debugging purposes.<br/>
 * <br/>
 * <code>GTPClusterDriver</code> is designed to read from Monte Carlo
 * data organized into 2-ns beam bunches. It can not be used for hardware
 * readout data, or Monte Carlo formatted in this style. For this data,
 * the <code>GTPOnlineClusterer</code> should be employed instead.
 * 
 * @author Kyle McCarty <mccarty@jlab.org>
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 * @see GTPClusterer
 */
public class GTPClusterDriver extends ClusterDriver {
	/** An instance of the clustering algorithm object for producing
	 * cluster objects. */
    private final GTPClusterer gtp;
    
    /**
     * Instantiates a new <code>GTPClusterer</code>, which will produce
     * clusters using the GTP algorithm in the 2-ns beam bunch scheme.
     * It will, by default, use a 50 MeV seed energy cut with +/- 2 a
     * clock-cycle verification and inclusion window.
     */
    public GTPClusterDriver() {
        clusterer = ClustererFactory.create("GTPClusterer");
        gtp = (GTPClusterer) clusterer;
        setWriteClusterCollection(true);
    }
    
    /**
     * Sets whether the behavior of the hit inclusion window with respect
     * to the hit verification window. If set to <code>false</code>,
     * both windows will be identical in size. Otherwise, the inclusion
     * window will be equal in size after the seed hit, but encompass
     * only one clock-cycle before the seed hit. This should be replaced
     * by the method <code>setAsymmetricWindow</code>.
     * @param limitClusterRange - <code>true</code> indicates that the
     * asymmetric window should be used and <code>false</code> that it
     * should not.
     */
    @Deprecated
    public void setLimitClusterRange(boolean limitClusterRange) {
        gtp.setAsymmetricWindow(limitClusterRange);
    }
    
    /**
     * Sets the size of the hit verification temporal window. Note
     * that this defines the size of the window in one direction, so
     * the full time window will be <code>(2 * clusterWindow) + 1</code>
     * clock-cycles in length. (i.e., it will be a length of
     * <code>clusterWindow</code> before the seed hit, a length of
     * <code>clusterWindow</code> after the seed hit, plus the cycle
     * that includes the seed hit.) Time length is in clock-cycles.
     * @param clusterWindow - The number of clock-cycles around the
     * hit in one direction.
     */
    public void setClusterWindow(int clusterWindow) {
        gtp.getCuts().setValue("clusterWindow", clusterWindow);
    }
    
    /**
     * Sets the minimum seed energy needed for a hit to be considered
     * for forming a cluster. This is the seed energy lower bound trigger
     * cut and is in units of GeV.
     * @param seedEnergyThreshold - The minimum cluster seed energy in
     * GeV.
     */
    public void setSeedEnergyThreshold(double seedEnergyThreshold) {
        gtp.getCuts().setValue("seedEnergyThreshold", seedEnergyThreshold);
    }
    
    /**
     * Sets whether the behavior of the hit inclusion window with respect
     * to the hit verification window. If set to <code>false</code>,
     * both windows will be identical in size. Otherwise, the inclusion
     * window will be equal in size after the seed hit, but encompass
     * only one clock-cycle before the seed hit.
     * @param asymmetricWindow - <code>true</code> indicates that the
     * asymmetric window should be used and <code>false</code> that it
     * should not.
     */
    public void setAsymmetricWindow(boolean asymmetricWindow) {
        gtp.setAsymmetricWindow(asymmetricWindow);
    }
    
    /**
     * Sets whether the clustering algorithm should output diagnostic
     * text or not.
     * @param verbose - <code>true</code> indicates that the driver should
     * output diagnostic text and <code>false</code> that it should not.
     */
    public void setVerbose(boolean verbose) {
        gtp.setVerbose(verbose);
    }
    
    /**
     * Defines whether the output of this clusterer should be persisted
     * to LCIO or not. By default, this 
     * @param state - <code>true</code> indicates that clusters will
     * be persisted, and <code>false</code> that they will not.
     */
    @Override
    public void setWriteClusterCollection(boolean state) {
    	// Set the flag as appropriate with the superclass.
    	super.setWriteClusterCollection(state);
    	
    	// Also tell the clusterer whether it should persist its hit
    	// collection or not.
    	gtp.setWriteHitCollection(state);
    }
}