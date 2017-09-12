package org.hps.recon.ecal.cluster;

import org.lcsim.event.GenericObject;

/**
 * class to store time difference between the rf and the seed hit of the highest energy cluster in the trigger window.
 **/
public class TriggerTime implements GenericObject {

    private double time;
    private int ix;
    private int iy;

    public TriggerTime(double time, int ix, int iy) {
        this.time = time;
        this.ix = ix;
        this.iy = iy;
    }

    public double getDoubleVal() {
        return time;
    }

    public int getNInt() {
        return 2;
    }

    public int getNFloat() {
        return 0;
    }

    public int getNDouble() {
        return 1;
    }

    public float getFloatVal() {
        return 0;
    }

    public int getIntVal(int ii) {
        if (ii == 0) {
            return ix;
        } else {
            return iy;
        }
    }

    public boolean isFixedSize() {
        return false;
    }

    public double getDoubleVal(int ii) {
        return time;
    }

    public float getFloatVal(int ii) {
        return 0;
    }
}
