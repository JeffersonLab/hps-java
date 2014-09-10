package org.hps.conditions.config;

/**
 * Convenience class for setting up read only access to the conditions dev database,
 * e.g. for test cases.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public final class DevReadOnlyConfiguration extends ResourceConfiguration {
    
    /**
     * Constructor.
     */
    public DevReadOnlyConfiguration() {       
        super("/org/hps/conditions/config/conditions_dev.xml", 
                "/org/hps/conditions/config/conditions_dev.properties");
    }       
}