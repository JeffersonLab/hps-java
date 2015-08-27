package org.hps.record.triggerbank;

import java.util.HashMap;

/**
 * Trigger config information in the form of string keys and long values.
 * <p>
 * This is not the "standard" interface using in LCSim to access trigger configuration.  
 * It is used as a simplistic representation for the run database.
 * 
 * @author Jeremy McCormick, SLAC
 */
@SuppressWarnings("serial")
public class TriggerConfigInt extends HashMap<String, Long> {
    
    /**
     * Get a particular trigger config variable's value.
     * 
     * @param variable the variable enum
     * @return the variable's value
     */
    Long getValue(TriggerConfigVariable variable) {
        return this.get(variable.name());
    }
}
