package org.hps.recon.ecal.daqconfig;

/**
 * Class <code>DAQConfig</code> holds all of the supported parameters
 * from the DAQ configuration that exists in EVIO files. These values
 * are stored in various subclasses appropriate to the parameter that
 * are accessed through this primary interface.
 * 
 * @author Kyle McCarty <mccarty@jlab.org>
 */
public class DAQConfig extends IDAQConfig {
	// Store the configuration objects.
	private SSPConfig sspConfig = new SSPConfig();
	private FADCConfig fadcConfig = new FADCConfig();
	
	/**
	 * Gets the configuration parameters for the FADC.
	 * @return Returns the FADC configuration.
	 */
	public FADCConfig getFADCConfig() {
		return fadcConfig;
	}
	
	/**
	 * Gets the configuration parameters for the SSP.
	 * @return Returns the SSP configuration.
	 */
	public SSPConfig getSSPConfig() {
		return sspConfig;
	}
	
	@Override
	void loadConfig(EvioDAQParser parser) {
		// Pass the configuration parser to the system-specific objects.
		sspConfig.loadConfig(parser);
		fadcConfig.loadConfig(parser);
	}

	@Override
	public void printConfig() {
		// Print the system-specific objects.
		fadcConfig.printConfig();
		System.out.println();
		sspConfig.printConfig();
	}
	
}
