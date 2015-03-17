package org.hps.recon.ecal.cluster;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import org.hps.recon.ecal.daqconfig.ConfigurationManager;
import org.hps.recon.ecal.daqconfig.GTPConfig;
import org.lcsim.event.EventHeader;

/**
 * Class <code>GTPOnlineClusterDriver</code> allows parameters for the
 * readout variant of the GTP algorithm to be set.
 * 
 * @author Kyle McCarty <mccarty@jlab.org>
 */
public class GTPOnlineClusterDriver extends ClusterDriver {
    private final GTPOnlineClusterer gtp;
    private boolean useDAQConfig = false;
    
    /**
     * Instantiates a new clustering algorithm using the readout
     * variant of the GTP clustering algorithm.
     */
    public GTPOnlineClusterDriver() {
    	// Instantiate the clusterer.
        clusterer = ClustererFactory.create("GTPOnlineClusterer");
        gtp = (GTPOnlineClusterer) clusterer;
        
        // Track the DAQ configuration status.
        ConfigurationManager.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// If DAQ configuration settings should be used, then
				// update the clusterer.
				if(useDAQConfig) {
					// Get the GTP settings.
					GTPConfig config = ConfigurationManager.getInstance().getGTPConfig();
					
					// Send the DAQ configuration settings to the clusterer.
					gtp.setSeedLowThreshold(config.getSeedEnergyCutConfig().getLowerBound());
					gtp.setWindowAfter(config.getTimeWindowAfter());
					gtp.setWindowBefore(config.getTimeWindowBefore());
					
					// Print the updated settings.
					logSettings();
				}
			}
        });
    }
    
    @Override
    public void process(EventHeader event) {
    	// Only process an event if either the DAQ configuration is not
    	// in use or if it has been initialized.
    	if((useDAQConfig && ConfigurationManager.isInitialized()) || !useDAQConfig) {
    		super.process(event);
    	} else {
    		System.out.println("GTP Clusterer :: Skipping event; DAQ configuration is not initialized.");
    	}
    }
    
    /**
     * Outputs the clusterer settings.
     */
    @Override
    public void startOfData() {
    	// VERBOSE :: Output the driver settings.
    	if(gtp.isVerbose()) { logSettings(); }
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
    
    /**
     * Sets whether GTP settings should be drawn from the EvIO data
     * DAQ configuration or read from the steering file.
     * @param state - <code>true</code> means that DAQ configuration
     * will be used and <code>false</code> that it will not.
     */
    public void setUseDAQConfig(boolean state) {
    	useDAQConfig = state;
    }
    
    private void logSettings() {
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