package org.hps.users.baltzell;

import org.lcsim.event.GenericObject;

/*
 * class to store RF times after extracting from waveform.
 */
public class RfHit implements GenericObject {
    private double[] times;
    public RfHit(double[] times) { this.times=times; }
    public int getNInt()    { return 0; }
    public int getNFloat()  { return 0; }
    public int getNDouble() { return times.length; }
    public double getDoubleVal(int ii) { return times[ii]; }
    public float  getFloatVal (int ii) { return 0; }
    public int    getIntVal   (int ii) { return 0; }
    public boolean isFixedSize() { return false; }
}
