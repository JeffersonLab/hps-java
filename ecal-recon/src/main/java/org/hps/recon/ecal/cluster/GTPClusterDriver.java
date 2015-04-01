package org.hps.recon.ecal.cluster;

/**
 * Class <code>GTPClusterDriver</code> instantiates an instance of
 * the clustering algorithm framework for the Monte Carlo version
 * of the GTP algorithm. This version assumes that events are equal
 * to 2 ns beam bunches. The class also allows the seed energy threshold
 * and cluster window to be set as well as whether or not the algorithm
 * should employ an asymmetric time window and write out verbose debug
 * text.
 * 
 * @author Kyle McCarty <mccarty@jlab.org>
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 * @see GTPClusterer
 */
public class GTPClusterDriver extends ClusterDriver {
    // The GTP clustering algorithm.
    private final GTPClusterer gtp;
    
    /**
     * Instantiates a new <code>GTPClusterer</code>.
     */
    public GTPClusterDriver() {
        clusterer = ClustererFactory.create("GTPClusterer");
        gtp = (GTPClusterer) clusterer;
        setWriteClusterCollection(false);
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
    @Deprecated
    void setLimitClusterRange(boolean limitClusterRange) {
        gtp.setLimitClusterRange(limitClusterRange);
    }
    
    /**
     * Sets the number of clock-cycles (4 ns) before and after a hit
     * in which the hit must be the maximum energy hit in its 3 x 3
     * window in order to be considered a seed hit and form a cluster.
     * @param clusterWindow - The number of clock-cycles around the
     * hit in one direction; i.e. a value of 1 indicates that the full
     * window will include the current clock-cycle, plus one cycle both
     * before and after the current cycle. This gives a total number
     * of cycles equal to (2 * clusterWindow) + 1.
     */
    public void setClusterWindow(int clusterWindow) {
        gtp.getCuts().setValue("clusterWindow", clusterWindow);
    }
    
    /**
     * Sets the minimum energy needed for a seed hit to form a cluster.
     * @param seedEnergyThreshold - The minimum seed energy in GeV.
     */
    public void setSeedEnergyThreshold(double seedEnergyThreshold) {
        gtp.getCuts().setValue("seedEnergyThreshold", seedEnergyThreshold);
    }
    
    /**
     * Sets whether the clustering algorithm should use an asymmetric
     * clustering window. The asymmetric window will include hits in
     * a cluster that are present within the full time window ahead of
     * the seed hit, but only one clock-cycle behind it. This is to
     * allow for variation in hit timing with respect to the seed due
     * to jitter in the hardware.
     * @param asymmetricWindow - <code>true</code> indicates that the
     * asymmetric window should be used and <code>false</code> that it
     * should not.
     */
    public void setAsymmetricWindow(boolean asymmetricWindow) {
        gtp.setLimitClusterRange(asymmetricWindow);
    }
    
    /**
     * Sets whether the clustering algorithm should output diagnostic
     * text or not.
     * @param verbose <code>true</code> indicates that the driver should
     * output diagnostic text and <code>false</code> that it should not.
     */
    public void setVerbose(boolean verbose) {
        gtp.setVerbose(verbose);
    }
}