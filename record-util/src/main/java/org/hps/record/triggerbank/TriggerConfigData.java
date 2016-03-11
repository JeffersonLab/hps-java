package org.hps.record.triggerbank;

import java.util.Map;
import java.util.Map.Entry;

import org.hps.record.daqconfig.ConfigurationManager;
import org.hps.record.daqconfig.DAQConfig;
import org.hps.record.daqconfig.EvioDAQParser;

/**
 * Raw trigger config string data with an associated timestamp.
 * 
 * @author Jeremy McCormick, SLAC
 */
public final class TriggerConfigData {
        
    public enum Crate {
        CONFIG1(37),
        CONFIG2(39),
        CONFIG3(46),
        CONFIG4(58);
        
        private int crate;
        
        private Crate(int crate) {
            this.crate = crate;
        }
        
        public int getCrateNumber() {
            return crate;
        }
        
        public static Crate fromCrateNumber(int crateNumber) {
            for (Crate crate : Crate.values()) {
                if (crate.getCrateNumber() == crateNumber) {
                    return crate;
                }
            }
            return null;
        }              
    }
                  
    private int timestamp;
    private Map<Crate, String> data;
    
    public TriggerConfigData(Map<Crate, String> data, int timestamp) {
        if (data == null) {
            throw new RuntimeException("The data map is null.");
        }
        this.data = data;
        this.timestamp = timestamp;
    }
    
    /**
     * Get the config's timestamp.
     * 
     * @return the config's timestamp
     */
    public int getTimestamp() {
        return timestamp;
    }
    
    /**
     * Get the config data as a map from crates to strings.
     * 
     * @return the config data
     */
    public Map<Crate, String> getData() {
        return data;
    }
    
    /**
     * Return <code>true</code> if the config is valid which means it has four string banks with the correct crate 
     * numbers and non-null data strings.
     *  
     * @return <code>true</code> if config is valid
     */
    public boolean isValid() {
        if (data.size() != Crate.values().length) {
            return false;
        }
        for (Crate crate : Crate.values()) {
            if (!data.containsKey(crate)) {
                return false;
            }
            if (data.get(crate) == null) {
                return false;
            }
        }
        return true;
    }
         
    /**
     * Load DAQ config object from trigger config string data.
     * 
     * @param run the run number (needed by configuration manager)
     * @return the DAQ config object
     */
    public DAQConfig loadDAQConfig(int run) {
        EvioDAQParser parser = new EvioDAQParser();
        for (Entry<Crate, String> entry : data.entrySet()) {
            parser.parse(entry.getKey().getCrateNumber(), run, new String[] {entry.getValue()});
        }
        ConfigurationManager.updateConfiguration(parser);
        return ConfigurationManager.getInstance();
    }    
}
