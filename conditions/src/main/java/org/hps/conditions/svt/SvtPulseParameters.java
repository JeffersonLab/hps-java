package org.hps.conditions.svt;

import org.hps.conditions.AbstractConditionsObject;
import org.hps.conditions.ConditionsObjectCollection;

/**
 * This class represents the pulse parameters for an SVT channel.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public final class SvtPulseParameters extends AbstractConditionsObject {

    public static class SvtPulseParametersCollection extends ConditionsObjectCollection<SvtPulseParameters> {
    }

    /**
     * Get the SVT channel ID.
     * @return The SVT channel ID.
     */
    int getChannelId() {
        return getFieldValue(Integer.class, "svt_channel_id");
    }

    /**
     * Get the amplitude.
     * @return The amplifude.
     */
    double getAmplitude() {
        return getFieldValue(Double.class, "amplitude");
    }

    /**
     * Get the starting time.
     * @return The starting time.
     */
    double getT0() {
        return getFieldValue(Double.class, "t0");
    }

    /**
     * Get the time shift.
     * @return The time shift.
     */
    double getTimeShift() {
        return getFieldValue(Double.class, "tp");
    }

    /**
     * Get the chisq.
     * @return The chisq.
     */
    double getChisq() {
        return getFieldValue(Double.class, "chisq");
    }

    /**
     * Convert this object to a human readable string.
     * @return This object converted to a string.
     */
    public String toString() {
        return "amp: " + getAmplitude() + ", t0: " + getT0() + ", shift: " + getTimeShift() + ", chisq: " + getChisq();
    }

    /**
     * Convert this object to an array of doubles.
     * @return This object converted to an array of doubles.
     */
    public double[] toArray() {
        double[] values = new double[4];
        values[0] = getAmplitude();
        values[1] = getT0();
        values[2] = getTimeShift();
        values[3] = getChisq();
        return values;
    }
}
