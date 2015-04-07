package org.hps.conditions.svt;

import org.hps.conditions.api.BaseConditionsObject;
import org.hps.conditions.api.BaseConditionsObjectCollection;
import org.hps.conditions.database.Converter;
import org.hps.conditions.database.Field;
import org.hps.conditions.database.MultipleCollectionsAction;
import org.hps.conditions.database.Table;

/**
 * This class encapsulates the shape fit parameters for an SVT channel.
 *
 * @author <a href="mailto:jeremym@slac.stanford.edu">Jeremy McCormick</a>
 * @author <a href="mailto:omoreno1@ucsc.edu">Omar Moreno</a>
 */
@Table(names = { "svt_shape_fit_parameters", "test_run_svt_shape_fit_parameters" })
@Converter(multipleCollectionsAction = MultipleCollectionsAction.LAST_CREATED)
// TODO: This class needs better documentation as to what these parameters actually mean.
public final class SvtShapeFitParameters extends BaseConditionsObject {

    /**
     * Collection implementation for {@link SvtShapeFitParameters} objects.
     */
    @SuppressWarnings("serial")
    public static class SvtShapeFitParametersCollection extends BaseConditionsObjectCollection<SvtShapeFitParameters> {
    }

    /**
     * Get the SVT channel ID.
     *
     * @return The SVT channel ID.
     */
    @Field(names = { "svt_channel_id" })
    public int getChannelID() {
        return getFieldValue(Integer.class, "svt_channel_id");
    }

    /**
     * Get the amplitude.
     *
     * @return The amplifude.
     */
    @Field(names = { "amplitude" })
    public double getAmplitude() {
        return getFieldValue(Double.class, "amplitude");
    }

    /**
     * Get t0.
     *
     * @return t0
     */
    @Field(names = { "t0" })
    public double getT0() {
        return getFieldValue(Double.class, "t0");
    }

    /**
     * Get tp.
     *
     * @return tp
     */
    @Field(names = { "tp" })
    public double getTp() {
        return getFieldValue(Double.class, "tp");
    }

    /**
     * Convert this object to an array of doubles.
     *
     * @return This object converted to an array of doubles.
     */
    public double[] toArray() {
        final double[] values = new double[3];
        values[0] = getAmplitude();
        values[1] = getT0();
        values[2] = getTp();
        return values;
    }
}
