package org.hps.evio.epics;

import org.lcsim.event.GenericObject;

/**
 * This is an implementation of GenericObject for reading and writing EPICS data.
 * There is no functionality here.  Users that need this data in their <code>Driver</code>
 * classes should instead use {@link EpicsScalarData#read(org.lcsim.event.EventHeader)}
 * to create the class with the actual API.
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
final class EpicsGenericObject implements GenericObject {

    String[] keys;
    double[] values;

    @Override
    public int getNInt() {
        return 0;
    }

    @Override
    public int getNFloat() {
        return 0;
    }

    @Override
    public int getNDouble() {
        return values.length;
    }

    @Override
    public int getIntVal(int index) {
        return 0;
    }

    @Override
    public float getFloatVal(int index) {
        return 0;
    }

    @Override
    public double getDoubleVal(int index) {
        return values[index];
    }

    @Override
    public boolean isFixedSize() {
        return false;
    }

    public String getKey(int index) {
        return keys[index];
    }
}
