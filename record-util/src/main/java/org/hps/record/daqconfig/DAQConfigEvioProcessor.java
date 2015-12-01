package org.hps.record.daqconfig;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hps.record.evio.EvioBankTag;
import org.hps.record.evio.EvioEventProcessor;
import org.hps.record.evio.EvioEventUtilities;
import org.jlab.coda.jevio.BaseStructure;
import org.jlab.coda.jevio.EvioEvent;

/**
 * Copied and modified from code in {@link org.hps.evio.TriggerConfigEvioReader} to extract DAQ config without
 * needing an output LCSim event.
 * <p>
 * Only the last valid DAQ config object will be saved.
 * 
 * @author Jeremy McCormick, SLAC
 */
public class DAQConfigEvioProcessor extends EvioEventProcessor {

    private Logger LOGGER = Logger.getLogger(DAQConfigEvioProcessor.class.getPackage().getName());
        
    private DAQConfig daqConfig = null;
    
    private Map<Integer, String> stringData = new HashMap<Integer, String>();
    
    private Integer run = null;

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
                
                // Parse config data from the EVIO banks.
                EvioDAQParser evioParser = parseEvioData(evioEvent);
            
                // Was there a valid config created from the EVIO event?
                if (evioParser != null) {            
                    // Set the current DAQ config object.
                    ConfigurationManager.updateConfiguration(evioParser);
                    daqConfig = ConfigurationManager.getInstance();
                }
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
    private EvioDAQParser parseEvioData(EvioEvent evioEvent) {
        EvioDAQParser parser = null;
        int configBanks = 0;
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
                            if (parser == null) {
                                parser = new EvioDAQParser();
                                stringData.clear();
                            }
                            LOGGER.fine("raw string data" + subBank.getStringData()[0]);
                            stringData.put(crate, subBank.getStringData()[0]);
                            LOGGER.info("Parsing DAQ config from crate " + crate + ".");
                            parser.parse(crate, run, subBank.getStringData());
                            ++configBanks;
                        } catch (Exception e) {
                            LOGGER.log(Level.WARNING, "Failed to parse DAQ config.", e);
                        }
                    }
                }
            }
        }
        if (configBanks >= 4 || parser == null) {
            if (parser != null) {
                LOGGER.info("DAQ config was created from event " + evioEvent.getEventNumber() + " with " + configBanks + " banks.");
            }
            return parser;
        } else {
            LOGGER.warning("Not enough banks were found to build DAQ config.");
            return null;
        }
    }

    /**
     * Get the DAQ config.
     * 
     * @return the DAQ config
     */
    public DAQConfig getDAQConfig() {
        return this.daqConfig;
    }
    
    /**
     * Get a map of bank number to its string data for the current config.
     * 
     * @return a map of bank to trigger config data
     */
    public Map<Integer, String> getTriggerConfigData() {
        return this.stringData;
    }
}