package org.hps.conditions.svt;

import org.hps.conditions.api.AbstractConditionsObject;
import org.hps.conditions.api.AbstractConditionsObjectCollection;

/**
 * This class encapsulates the shape fit parameters for an SVT channel.
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 * @author Omar Moreno <omoreno1@ucsc.edu>
 */
public final class SvtShapeFitParameters extends AbstractConditionsObject {

    public static class SvtShapeFitParametersCollection extends AbstractConditionsObjectCollection<SvtShapeFitParameters> {
    }

    /**
     * Get the SVT channel ID.
     * @return The SVT channel ID.
     */
    int getChannelID() {
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
     * Get t0.
     * @return t0.
     */
    double getT0() {
        return getFieldValue(Double.class, "t0");
    }

    /**
     * Get tp.
     * @return tp.
     */
    double getTp() {
        return getFieldValue(Double.class, "tp");
    }

    /**
     * Convert this object to an array of doubles.
     * @return This object converted to an array of doubles.
     */
    public double[] toArray() {
        double[] values = new double[3];
        values[0] = getAmplitude();
        values[1] = getT0();
        values[2] = getTp();
        return values;
    }
}
