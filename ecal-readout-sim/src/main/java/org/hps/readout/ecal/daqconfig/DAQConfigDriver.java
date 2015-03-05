package org.hps.readout.ecal.daqconfig;

import java.util.List;

import org.lcsim.event.EventHeader;
import org.lcsim.util.Driver;

public class DAQConfigDriver extends Driver {
	
	@Override
	public void process(EventHeader event) {
		// Check if a trigger configuration bank exists.
		if(event.hasCollection(TriggerConfig.class, "TriggerConfig")) {
			// Get the trigger configuration bank. There should only be
			// one in the list.
			List<TriggerConfig> configList = event.get(TriggerConfig.class, "TriggerConfig");
			TriggerConfig daqConfig = configList.get(0);
			
			// Perform a debug print of the configuration to test that
			// it is being read properly.
			daqConfig.printVars();
			
			// Get the DAQ configuration and update it with the new
			// configuration object.
			ConfigurationManager.updateConfiguration(daqConfig);
		}
	}
	
}
