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
 * Extracts DAQ config strings from an EVIO event stream, saving a reference to the most recent
 * {@link org.hps.record.triggerbank.TriggerConfigData} object.
 * <p>
 * When event processing is completed, the <code>triggerConfig</code> variable should reference
 * the last valid DAQ config and can be accessed using the {@link #getTriggerConfigData()} method.
 * 
 * @author Jeremy McCormick, SLAC
 */
public class TriggerConfigEvioProcessor extends EvioEventProcessor {

    private Logger LOGGER = Logger.getLogger(TriggerConfigEvioProcessor.class.getPackage().getName());
            
    private TriggerConfigData triggerConfig = null;
    private int timestamp = 0;

    /**
     * Process EVIO events to extract DAQ config data.
     */
    @Override
    public void process(EvioEvent evioEvent) {       
        try {            
           
            // Set current timestamp from head bank.
            BaseStructure headBank = EvioEventUtilities.getHeadBank(evioEvent);
            if (headBank != null) {
                if (headBank.getIntData()[3] != 0) {
                    timestamp = headBank.getIntData()[3];
                    LOGGER.finest("Set timestamp " + timestamp + " from head bank.");
                }
            }
                
            // Parse config data from the EVIO banks.
            parseEvioData(evioEvent);                                                          
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error parsing DAQ config from EVIO.", e);
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
        // Loop over top banks.
        for (BaseStructure bank : evioEvent.getChildrenList()) {
            if (bank.getChildCount() <= 0) {
                continue;
            }
            int crateNumber = bank.getHeader().getTag();
            // Loop over sub-banks.
            for (BaseStructure subBank : bank.getChildrenList()) {
                // In trigger config bank?
                if (EvioBankTag.TRIGGER_CONFIG.equals(subBank)) {
                    // Has a valid string array?
                    if (subBank.getStringData() != null) {                    
                        try { 
                            
                            // Make sure string data map is initialized for this event.
                            if (stringData == null) {
                                stringData = new HashMap<Crate, String>();
                            }                                                       
                            
                            // Get the Crate enum from crate number (if this returns null then the crate is ignored).
                            Crate crate = Crate.fromCrateNumber(crateNumber);
                            
                            // Is crate number valid?
                            if (crate != null) {
                                
                                // Is there valid string data in the array?
                                if (subBank.getStringData().length > 0) {
                                    // Add string data to map.
                                    stringData.put(crate, subBank.getStringData()[0]);
                                    LOGGER.info("Added crate " + crate.getCrateNumber() + " data ..." + '\n' + subBank.getStringData()[0]);
                                } /*else { 
                                    LOGGER.warning("The string bank has no data.");
                                }*/
                            } 
                        } catch (Exception e) {
                            LOGGER.log(Level.SEVERE, "Error parsing DAQ config from crate " + crateNumber, e);
                            e.printStackTrace();
                        }
                    }
                } /*else {
                    LOGGER.warning("Trigger config bank is missing string data.");
                }*/
            }
        }
        if (stringData != null) {
            LOGGER.info("Found " + stringData.size() + " config data strings in event " + evioEvent.getEventNumber());
            TriggerConfigData currentConfig = new TriggerConfigData(stringData, timestamp);
            if (currentConfig.isValid()) {
                triggerConfig = currentConfig;
                LOGGER.info("Found valid DAQ config data in event num " + evioEvent.getEventNumber());
            } else {
                LOGGER.warning("Skipping invalid DAQ config data in event num "  + evioEvent.getEventNumber());
            }
        }
    }
   
    /**
     * Get the last valid set of config data that was found in the event stream.
     * 
     * @return a map of bank number to the corresponding trigger config string data
     */
    public TriggerConfigData getTriggerConfigData() {
        return this.triggerConfig;
    }
}
