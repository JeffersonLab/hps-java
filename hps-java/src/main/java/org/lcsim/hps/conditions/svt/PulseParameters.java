package org.lcsim.hps.conditions.svt;

/**
 * This class represents the pulse parameters for an SVT channel.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class PulseParameters {
    
    double amplitude = Double.NaN;
    double t0 = Double.NaN;
    double tp = Double.NaN;
    double chisq = Double.NaN;

    /**
     * Full qualified class constructor.
     * @param amplitude The amplitude.
     * @param t0 The start time.
     * @param tp The shaping time.
     * @param chisq The chisq of the measurement.
     */
    PulseParameters(double amplitude, double t0, double tp, double chisq) {
        this.amplitude = amplitude;
        this.t0 = t0;
        this.tp = tp;
        this.chisq = chisq;
    }
    
    /**
     * Get the amplitude.
     * @return The amplifude.
     */
    double getAmplitude() {
        return amplitude;
    }
    
    /**
     * Get the starting time.
     * @return The starting time.
     */
    double getT0() {
        return t0;
    }
    
    /**
     * Get the time shift.
     * @return The time shift.
     */
    double getTimeShift() {
        return tp;
    }
    
    /**
     * Get the chisq.
     * @return The chisq.
     */
    double getChisq() {
        return chisq;
    }
    
    /**
     * Convert this object to a human readable string.
     * @return This object converted to a string.
     */
    public String toString() {
        return "amp: " + amplitude + ", t0: " + t0 + ", shift: " + tp + ", chisq: " + chisq;
    }
    
    /**
     * Convert this object to an array of doubles.
     * @return This object converted to an array of doubles.
     */
    public double[] toArray() {
        double[] values = new double[4];
        values[0] = amplitude;
        values[1] = t0;
        values[2] = tp;
        values[3] = chisq;
        return values;
    }
}
