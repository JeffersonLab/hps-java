package org.hps.conditions.config;

import org.hps.conditions.DatabaseConditionsManager;
import org.lcsim.conditions.ConditionsReader;

/**
 * Convenience class for setting up access to the conditions.
 */
public class ResourceConfiguration extends AbstractConfiguration {
    
    protected String config;
    protected String prop; 

    /**
     * Constructor with XML config, connection properties and ConditionsReader.
     */
    public ResourceConfiguration(
            String config, 
            String prop,
            ConditionsReader reader) {
        this.config = config;
        this.prop = prop;
    }
    
    /**
     * Constructor with XML config and connection properties.
     */
    public ResourceConfiguration(
            String config, 
            String connection) {
        this.config = config;
        this.prop = connection;
    }    
          
    /**
     * Setup the configuration on the conditions manager.
     */
    public AbstractConfiguration setup() {
        manager = new DatabaseConditionsManager();
        manager.configure(config);
        manager.setConnectionResource(prop);
        manager.register();
        return this;
    }           
}