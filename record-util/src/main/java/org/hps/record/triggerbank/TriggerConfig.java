package org.hps.record.triggerbank;

import java.util.HashMap;

/**
 * Trigger config information in the form of variables with <code>long</code> values.
 * <p>
 * This is not the "standard" interface using in LCSim to access trigger configuration. It is used as a simplistic
 * representation for the run database.
 */
@SuppressWarnings("serial")
public class TriggerConfig extends HashMap<TriggerConfigVariable, Long> {

    /**
     * Get the TI time offset.
     *
     * @return the TI time offset
     */
    public Long getTiTimeOffset() {
        return this.get(TriggerConfigVariable.TI_TIME_OFFSET);
    }
}
