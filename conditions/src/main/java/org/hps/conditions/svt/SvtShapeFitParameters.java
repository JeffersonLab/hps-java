package org.hps.conditions.svt;

import org.hps.conditions.api.BaseConditionsObject;
import org.hps.conditions.api.BaseConditionsObjectCollection;
import org.hps.conditions.database.Field;
import org.hps.conditions.database.Table;

/**
 * This class encapsulates the shape fit parameters for an SVT channel.
 *
 * @author Jeremy McCormick, SLAC
 * @author Omar Moreno, UCSC
 */
@Table(names = {"svt_shape_fit_parameters", "test_run_svt_shape_fit_parameters"})
public final class SvtShapeFitParameters extends BaseConditionsObject {

    /**
     * Collection implementation for {@link SvtShapeFitParameters} objects.
     */
    @SuppressWarnings("serial")
    public static class SvtShapeFitParametersCollection extends BaseConditionsObjectCollection<SvtShapeFitParameters> {
    }

    /**
     * Size of array when retrieving all parameters together using {@link #toArray()}.
     */
    private static final int ARRAY_SIZE = 4;

    /**
     * Get the amplitude.
     *
     * @return The amplitude.
     */
    @Field(names = {"amplitude"})
    public Double getAmplitude() {
        return this.getFieldValue(Double.class, "amplitude");
    }

    /**
     * Get the SVT channel ID.
     *
     * @return The SVT channel ID.
     */
    @Field(names = {"svt_channel_id"})
    public Integer getChannelID() {
        return this.getFieldValue(Integer.class, "svt_channel_id");
    }

    /**
     * Get t0.
     *
     * @return t0
     */
    @Field(names = {"t0"})
    public Double getT0() {
        return this.getFieldValue(Double.class, "t0");
    }

    /**
     * Get shaping time parameter.
     *
     * @return the shaping time parameter
     */
    @Field(names = {"tp"})
    public Double getTp() {
        return this.getFieldValue(Double.class, "tp");
    }

    /**
     * Get the second shaping time parameter.
     *
     * @return the second shaping time parameter
     */
    @Field(names = {"tp2"})
    public Double getTp2() {
        return this.getFieldValue(Double.class, "tp2");
    }

    /**
     * Convert this object to an array of doubles.
     *
     * @return This object converted to an array of doubles.
     */
    public double[] toArray() {
        final double[] values = new double[ARRAY_SIZE];
        values[0] = this.getAmplitude();
        values[1] = this.getT0();
        values[2] = this.getTp();
        values[3] = this.getTp2();
        return values;
    }
}
