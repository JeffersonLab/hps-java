package org.hps.record.scalars;

import org.lcsim.event.GenericObject;

/**
 * This is the LCIO {@link org.lcsim.event.GenericObject} binding
 * for EVIO scalar data.  This should not be used directly.  Rather
 * the {@link ScalarData} class should be used for loading data
 * from LCIO events. 
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
final class ScalarsGenericObject implements GenericObject {

    int[] values;

    @Override
    public int getNInt() {
        return values.length;
    }

    @Override
    public int getNFloat() {
        return 0;
    }

    @Override
    public int getNDouble() {
        return 0;
    }

    @Override
    public int getIntVal(int index) {
        return values[index];
    }

    @Override
    public float getFloatVal(int index) {
        return 0;
    }

    @Override
    public double getDoubleVal(int index) {
        return 0;
    }

    @Override
    public boolean isFixedSize() {
        return false;
    }
}
