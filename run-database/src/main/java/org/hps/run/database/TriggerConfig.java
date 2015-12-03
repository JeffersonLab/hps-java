package org.hps.run.database;

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
final class TriggerConfig {
    
    /**
     * Expected number of string banks in trigger config.
     */
    static final int DATA_LENGTH = 4;
    
    /*
     * Mapping of trigger config database fields to their crate numbers.
     */
    static final int CONFIG1 = 37;
    static final int CONFIG2 = 39;
    static final int CONFIG3 = 46;
    static final int CONFIG4 = 58;
        
    private int timestamp;
    private Map<Integer, String> data;
    
    TriggerConfig(Map<Integer, String> data, int timestamp) {
        if (data == null) {
            throw new RuntimeException("The data is null.");
        }
        this.data = data;
        this.timestamp = timestamp;
    }
    
    /**
     * Get the config's timestamp.
     * 
     * @return the config's timestamp
     */
    int getTimestamp() {
        return timestamp;
    }
    
    /**
     * Get the config data as a map from bank numbers to strings.
     * 
     * @return the config data
     */
    Map<Integer, String> getData() {
        return data;
    }
    
    /**
     * Return <code>true</code> if the config is valid which means it has 
     * four, non-null string data banks.
     *  
     * @return <code>true</code> if config is valid
     */
    boolean isValid() {
        return data.size() == DATA_LENGTH && data.get(CONFIG1) != null && data.get(CONFIG2) != null
                && data.get(CONFIG3) != null && data.get(CONFIG4) != null;
    }
         
    /**
     * Load DAQ config object from trigger config string data.
     * 
     * @param the run number (needed by configuration manager)
     * @return the DAQ config object
     */
    DAQConfig loadDAQConfig(int run) {
        EvioDAQParser parser = new EvioDAQParser();
        for (Entry<Integer, String> entry : data.entrySet()) {
            parser.parse(entry.getKey(), run, new String[] {entry.getValue()});
        }
        ConfigurationManager.updateConfiguration(parser);
        return ConfigurationManager.getInstance();
    }    
}
