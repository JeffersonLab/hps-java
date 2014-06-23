package org.hps.conditions.config;

import org.hps.conditions.DatabaseConditionsManager;
import org.lcsim.conditions.ConditionsManager.ConditionsNotFoundException;

/**
 * Convenience class for setting up access to the conditions dev database.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class DevDatabaseReadOnlyConfig {
    
    private static String xmlConfig = "/org/hps/conditions/config/conditions_dev.xml";
    private static String connectionProp = "/org/hps/conditions/config/conditions_dev.properties";
    private DatabaseConditionsManager manager;

    /**
     * Constructor.
     */
    public DevDatabaseReadOnlyConfig() {       
    }
    
    /**
     * Setup the XML config and connection properties on the conditions manager.
     */
    public void setup() {
        manager = new DatabaseConditionsManager();
        manager.configure(xmlConfig);
        manager.setConnectionResource(connectionProp);
        manager.register();
    }
    
    /**
     * Load a specific detector and run number to cache matching conditions.
     * @param detectorName The name of the detector.
     * @param runNumber The run number.
     */
    public void load(String detectorName, int runNumber) {    
        try {
            manager.setDetector(detectorName, runNumber);
        } catch (ConditionsNotFoundException e) {
            throw new RuntimeException(e);
        }   
    }
}