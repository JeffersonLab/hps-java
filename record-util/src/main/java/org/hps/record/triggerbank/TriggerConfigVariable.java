package org.hps.record.triggerbank;

/**
 * Enum for trigger config variables.
 *
 */
public enum TriggerConfigVariable {

    /**
     * TI time offset variable.
     */
    TI_TIME_OFFSET;

    /**
     * Get the column name in the run database for the config variable (convenience method).
     *
     * @return the column name
     */
    public String getColumnName() {
        return this.name().toLowerCase();
    }
}
