package org.hps.recon.ecal.cluster;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import org.hps.recon.ecal.daqconfig.ConfigurationManager;
import org.hps.recon.ecal.daqconfig.GTPConfig;
import org.lcsim.event.EventHeader;

/**
 * Class <code>GTPOnlineClusterDriver</code> is an implementation of
 * the <code>ClusterDriver</code> class that defines employs the readout
 * variant of the GTP hardware clustering algorithm. Specifics on the
 * behavior of this algorithm can be found in its documentation.<br/>
 * <br/>
 * <code>GTPOnlineClusterDriver</code> allows for all of the variable
 * settings used by the GTP algorithm to be defined. It also can be
 * set to "verbose" mode, where it will output detailed information on
 * each event and the cluster forming process. This is disabled by
 * default, but can be enabled for debugging purposes.<br/>
 * <br/>
 * Lastly, <code>GTPOnlineClusterDriver</code> can be set to draw its
 * settings from the <code>ConfigurationManager</code> static class,
 * which reads and stores settings extracted directly from EvIO data.
 * This option is disabled by default, and can be activated with the
 * method <code>setUseDAQConfig</code>. When enabled, no clusters will
 * be generated until <code>ConfigurationManager</code> reads a config
 * event. This requires that the driver <code>DAQConfigDriver</code>
 * be included in the driver chain.<br/>
 * <br/>
 * <code>GTPOnlineClusterDriver</code> is designed for use on hardware
 * readout data or Monte Carlo formatted in this style. It can not be
 * used for 2-ns beam bunch formatted data. <code>GTPClusterDriver</code>
 * should be used for this data instead.
 * 
 * @author Kyle McCarty <mccarty@jlab.org>
 * @see GTPOnlineClusterer
 * @see ConfigurationManager
 * @see org.hps.recon.ecal.daqconfig.DAQConfigDriver
 */
public class GTPOnlineClusterDriver extends ClusterDriver {
	/** An instance of the clustering algorithm object for producing
	 * cluster objects. */
    private final GTPOnlineClusterer gtp;
    /** Indicates whether the <code>ConfigurationManager</code> object
     * should be used for clustering settings or not. */
    private boolean useDAQConfig = false;
    
    /**
     * Initializes a clustering driver. This implements the readout
     * variant of the hardware GTP algorithm, as defined in the class
     * <code>GTPClusterer</code>.
     * @see GTPOnlineClusterer
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
    
    /**
     * Processes an an <code>EventHeader</code> object to generate
     * clusters. Events will not be processed if <code>UseDAQConfig</code>
     * is <code>true</code> unless the <code>ConfigurationManager</code>
     * static class has received a DAQ configuration from the event
     * stream and is initialized. Driver <code>DAQConfigDriver</code>
     * must be in the driver chain for this to occur.
     * @see org.hps.recon.ecal.daqconfig.DAQConfigDriver
     */
    @Override
    public void process(EventHeader event) {
    	// Only process an event if either the DAQ configuration is not
    	// in use or if it has been initialized.
    	if((useDAQConfig && ConfigurationManager.isInitialized()) || !useDAQConfig) {
    		super.process(event);
    	}
    }
    
    /**
     * Outputs the clusterer settings at driver initialization, assuming
     * <code>setVerbose</code> is set to <code>true</code>.
     */
    @Override
    public void startOfData() {
    	// VERBOSE :: Output the driver settings.
    	if(gtp.isVerbose()) { logSettings(); }
    }
    
    /**
     * Sets the minimum seed energy needed for a hit to be considered
     * for forming a cluster. This is the seed energy lower bound trigger
     * cut and is in units of GeV.
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
     * This defines the first half of the temporal hit inclusion window.
     * The temporal hit verification window is defined as
     * <code>max(windowAfter, windowBefore) * 2) + 1</code>.
     * @param cyclesBefore - The length of the clustering window before
     * the seed hit in clock cycles.
     */
    public void setWindowBefore(int cyclesBefore) {
        gtp.setWindowBefore(cyclesBefore);
    }
    
    /**
     * Sets the number of clock-cycles to include in the clustering
     * window after the seed hit. One clock-cycle is four nanoseconds.
     * This defines the latter half of the temporal hit inclusion window.
     * The temporal hit verification window is defined as
     * <code>max(windowAfter, windowBefore) * 2) + 1</code>.
     * @param cyclesAfter - The length of the clustering window after
     * the seed hit in clock cycles.
     * @see GTPOnlineClusterer
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
     * DAQ configuration or read from the steering file. If this is
     * set to <code>true</code>, no clusters will be generated until
     * the static class <code>ConfigurationManager</code> has received
     * a DAQ settings event and initialized. If this class is not part
     * of the driver chain, then no clusters will ever be created.
     * @param state - <code>true</code> means that DAQ configuration
     * will be used and <code>false</code> that it will not.
     * @see org.hps.recon.ecal.daqconfig.DAQConfigDriver
     */
    public void setUseDAQConfig(boolean state) {
    	useDAQConfig = state;
    }
    
    /**
     * Outputs the current GTP settings to the terminal.
     */
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