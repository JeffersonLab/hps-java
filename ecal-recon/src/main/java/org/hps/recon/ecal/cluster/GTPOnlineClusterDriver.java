package org.hps.recon.ecal.cluster;

/**
 * Class <code>GTPOnlineClusterDriver</code> allows parameters for the
 * readout variant of the GTP algorithm to be set.
 * 
 * @author Kyle McCarty <mccarty@jlab.org>
 */
public class GTPOnlineClusterDriver extends ClusterDriver {
    // Store the clustering algorithm.
    private final GTPOnlineClusterer gtp;
    
    /**
     * Instantiates a new clustering algorithm using the readout
     * variant of the GTP clustering algorithm.
     */
    public GTPOnlineClusterDriver() {
        clusterer = ClustererFactory.create("GTPOnlineClusterer");
        gtp = (GTPOnlineClusterer) clusterer;
    }
    
    /**
     * Outputs the clusterer settings.
     */
    @Override
    public void startOfData() {
    	// VERBOSE :: Output the driver settings.
    	if(gtp.isVerbose()) {
			// Print the cluster driver header.
			System.out.println();
			System.out.println();
			System.out.println("======================================================================");
			System.out.println("=== GTP Readout Clusterer Settings ===================================");
			System.out.println("======================================================================");
			
			// Output the driver settings.
			System.out.printf("Seed Energy Threshold :: %.3f GeV%n", gtp.getSeedLowThreshold());
			System.out.printf("Time Window (Before)  :: %.0f ns%n", gtp.getWindowBefore());
			System.out.printf("Time Window (After)   :: %.0f ns%n", gtp.getWindowAfter());
    	}
    }
    
    /**
     * Sets the minimum seed energy needed for a hit to be considered
     * for forming a cluster.
     * @param seedEnergyThreshold - The minimum cluster seed energy in
     * GeV.
     */
    public void setSeedEnergyThreshold(double seedEnergyThreshold) {
        getClusterer().getCuts().setValue("seedThreshold", seedEnergyThreshold);
        gtp.setSeedLowThreshold(seedEnergyThreshold);
    }
    
    /**
     * Sets the number of clock-cycles to include in the clustering
     * window before the seed hit. One clock-cycle is four nanoseconds.
     * @param cyclesBefore - The length of the clustering window before
     * the seed hit in clock cycles.
     */
    public void setWindowBefore(int cyclesBefore) {
        gtp.setWindowBefore(cyclesBefore);
    }
    
    /**
     * Sets the number of clock-cycles to include in the clustering
     * window after the seed hit. One clock-cycle is four nanoseconds.
     * @param cyclesAfter - The length of the clustering window after
     * the seed hit in clock cycles.
     */
    public void setWindowAfter(int cyclesAfter) {
        gtp.setWindowAfter(cyclesAfter);
    }
    
    /**
     * Sets whether the clusterer should output diagnostic text or not.
     * @param verbose - <code>true</code> indicates that the clusterer
     * should output diagnostic text and <code>false</code> that it
     * should not.
     */
    public void setVerbose(boolean verbose) {
        gtp.setVerbose(verbose);
    }
}