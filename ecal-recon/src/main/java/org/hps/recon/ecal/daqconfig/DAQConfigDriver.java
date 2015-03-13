package org.hps.recon.ecal.daqconfig;

import java.util.List;

import org.lcsim.event.EventHeader;
import org.lcsim.util.Driver;

public class DAQConfigDriver extends Driver {
	
	@Override
	public void process(EventHeader event) {
		// Check if a trigger configuration bank exists.
		if(event.hasCollection(EvioDAQParser.class, "TriggerConfig")) {
			// Get the trigger configuration bank. There should only be
			// one in the list.
			List<EvioDAQParser> configList = event.get(EvioDAQParser.class, "TriggerConfig");
			EvioDAQParser daqConfig = configList.get(0);
			
			// Get the DAQ configuration and update it with the new
			// configuration object.
			ConfigurationManager.updateConfiguration(daqConfig);
		}
	}
	
}