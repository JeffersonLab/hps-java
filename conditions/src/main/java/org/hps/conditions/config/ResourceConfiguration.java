package org.hps.conditions.config;

import org.hps.conditions.DatabaseConditionsManager;

/**
 * Convenience class for setting up access to the conditions.
 */
public class ResourceConfiguration extends AbstractConfiguration {
    
    protected String config;
    protected String prop; 

    /**
     * Constructor with XML config and connection properties.
     */
    public ResourceConfiguration(
            String config, 
            String prop) {
        this.config = config;
        this.prop = prop;
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