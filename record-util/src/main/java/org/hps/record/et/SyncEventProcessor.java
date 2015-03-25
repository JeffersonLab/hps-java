package org.hps.record.et;

import java.io.IOException;
import java.util.List;

import org.hps.evio.TriggerConfigEvioReader;
import org.hps.recon.ecal.daqconfig.ConfigurationManager;
import org.hps.recon.ecal.daqconfig.EvioDAQParser;
import org.hps.record.evio.EvioEventUtilities;
import org.jlab.coda.et.EtEvent;
import org.jlab.coda.jevio.EvioEvent;
import org.jlab.coda.jevio.EvioException;
import org.jlab.coda.jevio.EvioReader;
import org.lcsim.event.base.BaseLCSimEvent;

/**
 * This is an ET event processor that will load DAQ configuration into the global manager 
 * from EVIO physics SYNC events, which have an event type in which bits 6 and 7 are set to 1.
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 * 
 * @see org.hps.recon.ecal.daqconfig.ConfigurationManager
 * @see org.hps.recon.ecal.daqconfig.EvioDAQParser
 */
public class SyncEventProcessor extends EtEventProcessor {

    private static final String TRIGGER_CONFIG = "TriggerConfig";
    TriggerConfigEvioReader configReader = new TriggerConfigEvioReader();

    public void process(EtEvent event) {
        EvioEvent evioEvent = null;
        try {
            evioEvent = new EvioReader(event.getDataBuffer()).parseNextEvent();
        } catch (IOException | EvioException e) {
            throw new RuntimeException(e);
        }
        try {
            // Create a dummy LCIO event to satisfy the configuration reader's interface.
            BaseLCSimEvent dummyLcsimEvent = 
                    new BaseLCSimEvent(EvioEventUtilities.getRunNumber(evioEvent), evioEvent.getEventNumber(), "DUMMY", 0, false);
            
            // Create the DAQ configuration object in the LCIO event.
            configReader.getDAQConfig(evioEvent, dummyLcsimEvent);
            
            // Update the global configuration if a configuration was created.
            if (dummyLcsimEvent.hasCollection(EvioDAQParser.class, TRIGGER_CONFIG)) {
                List<EvioDAQParser> configList = dummyLcsimEvent.get(EvioDAQParser.class, TRIGGER_CONFIG);
                if (!configList.isEmpty()) {
                    ConfigurationManager.updateConfiguration(configList.get(0));
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to load DAQ config from sync event ...");
            e.printStackTrace();
        }
    }
}
