package org.hps.record.epics;

import org.lcsim.event.GenericObject;

/**
 * This is an implementation of GenericObject for reading and writing EPICS data. There is no functionality here
 * intended for ends users. Instead, the EPICS data should be accessed using
 * {@link EpicsData#read(org.lcsim.event.EventHeader)} to create the data object from input event data.
 */
final class EpicsGenericObject implements GenericObject {

    /**
     * The header information.
     */
    private int[] headerData;

    /**
     * The names of the EPICS variables.
     */
    private String[] keys;

    /**
     * The values of the EPICS variables.
     */
    private double[] values;

    /**
     * Get a double value of an EPICS variable.
     *
     * @param index the index of the variable
     */
    @Override
    public double getDoubleVal(final int index) {
        return this.values[index];
    }

    /**
     * Dummy implementation.
     *
     * @param index the array index
     * @return always returns 0
     */
    @Override
    public float getFloatVal(final int index) {
        return 0;
    }

    /**
     * Get an int value which is used to store the EPICS header information.
     *
     * @param index the array index
     * @return the int value at <code>index</code>
     */
    @Override
    public int getIntVal(final int index) {
        return headerData[index];
    }

    /**
     * Get a key by index.
     *
     * @param index the index
     * @return the key which is the name of an EPICS variable
     */
    public String getKey(final int index) {
        return this.keys[index];
    }

    /**
     * Get the keys which are the EPICS variable names.
     *
     * @return the keys
     */
    String[] getKeys() {
        return this.keys;
    }

    /**
     * Get the number of doubles which matches the number of EPICS variables.
     *
     * @return the number of double values
     */
    @Override
    public int getNDouble() {
        return this.values.length;
    }

    /**
     * Dummy implementation.
     *
     * @return always returns 0
     */
    @Override
    public int getNFloat() {
        return 0;
    }

    /**
     * Get the number of int values which is the length of the data header.
     *
     * @return the number of int values
     */
    @Override
    public int getNInt() {
        return this.headerData.length;
    }

    /**
     * Returns <code>false</code> to indicate this object does not have a fixed size.
     */
    @Override
    public boolean isFixedSize() {
        return false;
    }

    /**
     * Set the header data.
     *
     * @param data the header data array
     */
    void setHeaderData(final int[] headerData) {
        this.headerData = headerData;
    }

    /**
     * Set a key string by index.
     *
     * @param index the index
     * @param key the key string which is an EPICS variable
     */
    void setKey(final int index, final String key) {
        this.keys[index] = key;
    }

    /**
     * Set the keys.
     *
     * @param keys the keys array
     */
    void setKeys(final String[] keys) {
        this.keys = keys;
    }

    /**
     * Set a value by index.
     *
     * @param index the index
     * @param value the value
     */
    void setValue(final int index, final double value) {
        this.values[index] = value;
    }

    /**
     * Set the values array.
     *
     * @param values the values array
     */
    void setValues(final double[] values) {
        this.values = values;
    }
}
