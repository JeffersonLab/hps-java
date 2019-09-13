/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hps.evio;

import org.lcsim.event.GenericObject;

/**
 * This was created very similarly to the EPICGenericObject
 * This is designed only to keep only words from the VTP banks,
 * which are only 32 bit integers.
 * 
 * @author rafopar
 */
final class VTPGenericObject implements GenericObject {

    private int[] values;

    @Override
    public double getDoubleVal(final int index) {
        return 0;
    }

    @Override
    public int getIntVal(final int index) {
        return this.values[index];
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
        return 0;
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
        return this.values.length;
    }

    void setValue(final int index, final int value) {
        this.values[index] = value;
    }

    /**
     * Set the values array.
     *
     * @param values the values array
     */
    void setValues(final int[] values) {
        this.values = values;
    }

}
