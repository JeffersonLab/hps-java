package org.hps.conditions.config;

import java.io.File;

import org.hps.conditions.DatabaseConditionsManager;

/**
 * Convenience class for setting up access to the conditions.
 */
public class FileConfiguration extends AbstractConfiguration {
    
    private File config;
    private File prop;
    private DatabaseConditionsManager manager;

    /**
     * Constructor with XML config, connection properties and ConditionsReader.
     */
    public FileConfiguration(
            File config, 
            File prop) {
        this.config = config;
        this.prop = prop;
    }
              
    /**
     * Setup the configuration on the conditions manager.
     */
    public AbstractConfiguration setup() {
        manager = new DatabaseConditionsManager();       
        manager.configure(config);
        manager.setConnectionProperties(prop);
        manager.register();       
        return this;
    }    
}