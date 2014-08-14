package org.hps.conditions.config;

import org.hps.conditions.DatabaseConditionsManager;
import org.lcsim.conditions.ConditionsReader;

/**
 * Convenience class for setting up access to the conditions.
 */
public class ConditionsDatabaseConfiguration {
    
    private String xmlConfig;
    private String connectionProp;
    private ConditionsReader reader;
    private DatabaseConditionsManager manager;

    /**
     * Constructor.
     */
    public ConditionsDatabaseConfiguration(
            String xmlConfig, 
            String connectionProp,
            ConditionsReader reader) {
        this.xmlConfig = xmlConfig;
        this.connectionProp = connectionProp;
        this.reader = reader;
    }
    
    /**
     * Setup the XML config and connection properties on the conditions manager.
     */
    public void setup() {
        manager = new DatabaseConditionsManager();
        manager.configure(xmlConfig);
        manager.setConnectionResource(connectionProp);
        if (reader != null)
            manager.setBaseConditionsReader(reader);
        manager.register();
    }    
}