package org.hps.run.database;

import org.hps.record.epics.EpicsData;

/**
 * Enum for representing different types of EPICS data in the run database, of which there are currently two (2s and
 * 20s).
 *
 * @author Jeremy McCormick, SLAC
 */
public enum EpicsType {

    /**
     * 20S EPICS data.
     */
    EPICS_20s(20),
    /**
     * 2S EPICS data.
     */
    EPICS_2s(2);

    /**
     * Get the type from an int.
     *
     * @param type the type from an int
     * @return the type from an int
     * @throws IllegalArgumentException if <code>type</code> is invalid (not 2 or 20)
     */
    public static EpicsType fromInt(final int type) {
        if (type == EPICS_2s.type) {
            return EPICS_2s;
        } else if (type == EPICS_20s.type) {
            return EPICS_20s;
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
            return EpicsType.EPICS_2s;
        } else {
            return EpicsType.EPICS_20s;
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
