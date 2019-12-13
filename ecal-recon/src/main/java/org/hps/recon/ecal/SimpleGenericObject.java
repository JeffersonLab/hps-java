/*
 * Here comes the text of your license
 * Each line should be prefixed with  * 
 */
package org.hps.recon.ecal;

import org.lcsim.event.GenericObject;

/**
 *
 * @author rafopar
 */
final public class SimpleGenericObject implements GenericObject{
    
    private int[] int_values = new int[]{};
    private double[] double_values = new double[]{};

    @Override
    public double getDoubleVal(final int index) {
        return this.double_values[index];
    }

    @Override
    public int getIntVal(final int index) {
        return this.int_values[index];
    }

    @Override
    public float getFloatVal(final int index) {
        return 0;
    }

    @Override
    public boolean isFixedSize() {
        return false;
    }

    @Override
    public int getNDouble() {
        return this.double_values.length;
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
        return this.int_values.length;
    }

    void setValue(final int index, final int value) {
        this.int_values[index] = value;
    }

    /**
     * Set the values array.
     *
     * @param values the values array
     */
    void setIntValues(final int[] values) {
        this.int_values = values;
    }

    /**
     * Set the values array.
     *
     * @param values the values array
     */
    void setDoubleValues(final double[] values) {
        this.double_values = values;
    }
    
    
}
