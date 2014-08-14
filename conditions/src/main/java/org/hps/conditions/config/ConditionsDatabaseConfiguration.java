package org.hps.conditions.config;

import org.hps.conditions.DatabaseConditionsManager;

/**
 * Convenience class for setting up access to the conditions.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class ConditionsDatabaseConfiguration {
    
    private String xmlConfig;
    private String connectionProp;
    private DatabaseConditionsManager manager;

    /**
     * Constructor.
     */
    public ConditionsDatabaseConfiguration(
            String xmlConfig, 
            String connectionProp) {
        this.xmlConfig = xmlConfig;
        this.connectionProp = connectionProp;
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
}