package org.hps.conditions.database;

/**
 * <p>
 * This is a static set of constants defining default table names and lookup key
 * values for conditions data. The actual table names for the conditions data
 * are defined externally in an XML configuration file read by the
 * {@link DatabaseConditionsManager}.
 * <p>
 * It is possible that the key and table names are defined differently in the
 * XML configuration, e.g. if the name of the key is not exactly the same as the
 * table name, but usually they are the same value.
 */
// TODO: This should be removed as these names are all used by default and can be overridden if needed.
public final class TableConstants {

    private TableConstants() {
    }

    // FIXME: Add getEcalConditions() to manager API in order to replace the usage of this constant.
    /** Conditions key for combined ECal conditions. */
    public static final String ECAL_CONDITIONS = "ecal_conditions";   
}