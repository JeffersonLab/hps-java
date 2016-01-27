package org.hps.record.daqconfig;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hps.record.evio.EvioBankTag;
import org.hps.record.evio.EvioEventProcessor;
import org.hps.record.evio.EvioEventUtilities;
import org.hps.record.triggerbank.TriggerConfigData;
import org.hps.record.triggerbank.TriggerConfigData.Crate;
import org.jlab.coda.jevio.BaseStructure;
import org.jlab.coda.jevio.EvioEvent;

/**
 * Copied and modified from code in {@link org.hps.evio.TriggerConfigEvioReader} to extract DAQ config without
 * needing an output LCSim event.
 * <p>
 * Only the last valid DAQ config object is available once the job is finished.
 * 
 * @author Jeremy McCormick, SLAC
 */
public class TriggerConfigEvioProcessor extends EvioEventProcessor {

    private Logger LOGGER = Logger.getLogger(TriggerConfigEvioProcessor.class.getPackage().getName());
            
    private TriggerConfigData triggerConfig = null;    
    private Integer run = null;
    private int timestamp = 0;

    /**
     * Process EVIO events to extract DAQ config data.
     */
    @Override
    public void process(EvioEvent evioEvent) {       
        try {            
            // Initialize the run number if necessary.
            if (run == null) {
                try {
                    run = EvioEventUtilities.getRunNumber(evioEvent);
                    LOGGER.info("run " + run);
                } catch (NullPointerException e) {
                }
            }
                        
            // Can only start parsing DAQ banks once the run is set.
            if (run != null) {
                
                // Set current timestamp from head bank.
                BaseStructure headBank = EvioEventUtilities.getHeadBank(evioEvent);
                if (headBank != null) {
                    if (headBank.getIntData()[3] != 0) {
                        timestamp = headBank.getIntData()[3];
                        LOGGER.finest("set timestamp " + timestamp + " from head bank");
                    }
                }
                
                // Parse config data from the EVIO banks.
                parseEvioData(evioEvent);                                                          
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error parsing DAQ config from EVIO.", e);
        }
    }
    
    /**
     * Parse DAQ config from an EVIO event.
     * 
     * @param evioEvent the EVIO event
     * @return a parser object if the event has valid config data; otherwise <code>null</code>
     */
    private void parseEvioData(EvioEvent evioEvent) {
        Map<Crate, String> stringData = null;
        for (BaseStructure bank : evioEvent.getChildrenList()) {
            if (bank.getChildCount() <= 0) {
                continue;
            }
            int crate = bank.getHeader().getTag();
            for (BaseStructure subBank : bank.getChildrenList()) {
                if (EvioBankTag.TRIGGER_CONFIG.equals(subBank)) {
                    if (subBank.getStringData() == null) {
                        LOGGER.warning("Trigger config bank is missing string data.");
                    } else {
                        try { 
                            if (stringData == null) {
                                stringData = new HashMap<Crate, String>();
                            }
                            //LOGGER.fine("got raw trigger config string data ..." + '\n' + subBank.getStringData()[0]);
                            stringData.put(TriggerConfigData.Crate.fromCrateNumber(crate), subBank.getStringData()[0]);
                        } catch (Exception e) {
                            LOGGER.log(Level.WARNING, "Failed to parse crate " + crate + " config.", e);
                        }
                    }
                }
            }
        }
        if (stringData != null) {
            TriggerConfigData currentConfig = new TriggerConfigData(stringData, timestamp);
            if (currentConfig.isValid()) {
                triggerConfig = currentConfig;
                LOGGER.warning("Found valid config in event num " + evioEvent.getEventNumber());
            } else {
                LOGGER.warning("Skipping invalid config from event num "  + evioEvent.getEventNumber());
            }
        }
    }
   
    /**
     * Get a map of bank number to string data for the current config.
     * 
     * @return a map of bank to trigger config data
     */
    public TriggerConfigData getTriggerConfigData() {
        return this.triggerConfig;
    }
}