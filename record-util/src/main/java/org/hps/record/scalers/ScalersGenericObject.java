package org.hps.record.scalers;

import org.lcsim.event.GenericObject;

/**
 * This is the LCIO {@link org.lcsim.event.GenericObject} binding for EVIO scaler data. This should not be used
 * directly. Rather the {@link ScalerData} class should be used for loading data from LCIO events.
 */
final class ScalersGenericObject implements GenericObject {

    /**
     * The scaler data values.
     */
    private final int[] values;

    /**
     * Create a new object with the given scaler values.
     *
     * @param values the array of scaler values
     */
    ScalersGenericObject(final int[] values) {
        this.values = values;
    }

    @Override
    public double getDoubleVal(final int index) {
        return 0;
    }

    @Override
    public float getFloatVal(final int index) {
        return 0;
    }

    /**
     * Get the scaler value at the index.
     *
     * @param index the index in the data array
     */
    @Override
    public int getIntVal(final int index) {
        return this.values[index];
    }

    @Override
    public int getNDouble() {
        return 0;
    }

    @Override
    public int getNFloat() {
        return 0;
    }

    /**
     * Get the number of integer values in the array.
     *
     * @return the number of integer values in the array
     */
    @Override
    public int getNInt() {
        return this.values.length;
    }

    /**
     * Returns <code>false</code> to indicate object is not fixed size.
     *
     * @return <code>false</code> to indicate object is not fixed size
     */
    @Override
    public boolean isFixedSize() {
        return false;
    }
}
