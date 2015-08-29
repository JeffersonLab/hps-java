package org.hps.run.database;

import org.hps.record.epics.EpicsData;

/**
 * Enum for representing different types of EPICS data in the run database, of which there are currently two (1s and
 * 10s).
 *
 * @author Jeremy McCormick, SLAC
 */
public enum EpicsType {

    /**
     * 10S EPICS data.
     */
    EPICS_10S(10),
    /**
     * 1S EPICS data.
     */
    EPICS_1S(1);

    /**
     * Get the type from an int.
     *
     * @param type the type from an int
     * @return the type from an int
     * @throws IllegalArgumentException if <code>type</code> is invalid (not 1 or 10)
     */
    public static EpicsType fromInt(final int type) {
        if (type == EPICS_1S.type) {
            return EPICS_1S;
        } else if (type == EPICS_10S.type) {
            return EPICS_10S;
        } else {
            throw new IllegalArgumentException("The type code is invalid (must be 1 or 10): " + type);
        }
    }

    /**
     * Return the type of the EPICS data (1s or 10s).
     *
     * @return the type of the EPICS data
     */
    public static EpicsType getEpicsType(final EpicsData epicsData) {
        // FIXME: The type argument should be set on creation which would make this key check unnecessary.
        if (epicsData.getKeys().contains("MBSY2C_energy")) {
            return EpicsType.EPICS_1S;
        } else {
            return EpicsType.EPICS_10S;
        }
    }

    /**
     * The type encoding (1 or 10).
     */
    private int type;

    /**
     * Create an EPICS type.
     *
     * @param type the type encoding (1 or 10)
     */
    private EpicsType(final int type) {
        this.type = type;
    }

    /**
     * Get the table name for this type of data.
     *
     * @return the table name
     */
    public String getTableName() {
        return this.name().toLowerCase();
    }

    /**
     * Get the type encoding.
     *
     * @return the type encoding
     */
    public int getTypeCode() {
        return this.type;
    }
}
