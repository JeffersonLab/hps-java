package org.hps.readout.ecal.daqconfig;


public class DAQConfig extends IDAQConfig {
	// Store the configuration objects.
	private SSPConfig sspConfig = new SSPConfig();
	private FADCConfig fadcConfig = new FADCConfig();
	
	@Override
	void loadConfig(TriggerConfig parser) {
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
