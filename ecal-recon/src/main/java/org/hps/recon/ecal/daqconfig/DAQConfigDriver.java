package org.hps.recon.ecal.daqconfig;

import java.util.List;

import org.lcsim.event.EventHeader;
import org.lcsim.util.Driver;

/**
 * Class <code>DAQConfigDriver</code> is responsible for checking events
 * for DAQ configuration settings, and then passing them to the associated
 * class <code>ConfigurationManager</code> so that they can be accessed
 * by other classes.<br/>
 * <br/>
 * This driver must be included in the driver chain if any other drivers
 * in the chain rely on <code>ConfigurationManager</code>, as it can
 * not be initialized otherwise.
 * 
 * @author Kyle McCarty
 * @see ConfigurationManager
 */
public class DAQConfigDriver extends Driver {
    /**
     * Checks an event for the DAQ configuration banks and passes them
     * to the <code>ConfigurationManager</code>.
     * @param - The event to check.
     */
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