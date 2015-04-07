package org.hps.conditions.svt;

import org.hps.conditions.api.BaseConditionsObject;
import org.hps.conditions.api.BaseConditionsObjectCollection;
import org.hps.conditions.database.Field;
import org.hps.util.Pair;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;

/**
 * This abstract class provides some of the basic functionality used to access SVT DAQ map variables.
 *
 * @author <a href="mailto:omoreno1@ucsc.edu">Omar Moreno</a>
 */
public abstract class AbstractSvtDaqMapping extends BaseConditionsObject {

    /**
     * Flag value for top half.
     */
    public static final String TOP_HALF = "T";

    /**
     * Flag value for bottom half.
     */
    public static final String BOTTOM_HALF = "B";

    /**
     * Flag value for axial sensor.
     */
    public static final String AXIAL = "A";

    /**
     * Flag value for stereo sensor.
     */
    public static final String STEREO = "S";

    /**
     * The collection implementation for {@link AbstractSvtDaqMapping}.
     *
     * @param <T> the type of the object in the collection which must extend {@link AbstractSvtDaqMapping}
     */
    @SuppressWarnings("serial")
    public static abstract class AbstractSvtDaqMappingCollection<T extends AbstractSvtDaqMapping> extends
            BaseConditionsObjectCollection<T> {

        /**
         * Get a DAQ pair for the given {@link HpsSiSensor}.
         *
         * @param sensor a sensor of type {@link HpsSiSensor}
         * @return the DAQ pair associated with the sensor
         */
        public abstract Pair<Integer, Integer> getDaqPair(HpsSiSensor sensor);

        /**
         * Get the orientation of a sensor.
         *
         * @param daqPair for a given sensor
         * @return if a daqPair is found, return an "A" if the sensor orientation is Axial, an "S" if the orientation is
         *         Stereo or null if the daqPair doesn't exist
         */
        public abstract String getOrientation(Pair<Integer, Integer> daqPair);

    }

    /**
     * Get the SVT half (TOP or BOTTOM) that the sensor belongs to.
     *
     * @return SVT half (TOP or BOTTOM)
     */
    @Field(names = { "svt_half" })
    public final String getSvtHalf() {
        return getFieldValue("svt_half");
    }

    /**
     * Get the SVT sensor layer number (1-10 for test run and 1-12 for engineering run).
     *
     * @return SVT sensor layer number
     */
    @Field(names = { "layer" })
    public final int getLayerNumber() {
        return getFieldValue("layer");
    }

    /**
     * Get the orientation of an SVT sensor (AXIAL or STEREO).
     * @see {@link #AXIAL}
     * @see {@link #STEREO}
     * @return the orientation of the SVT sensor
     */
    @Field(names = { "orientation" })
    public final String getOrientation() {
        return getFieldValue("orientation");
    }

    /**
     * Set the SVT half that the sensor belongs to.
     *
     * @param svtHalf the SVT half (TOP or BOTTOM)
     * @see {@link #TOP_HALF}
     * @see {@link #BOTTOM_HALF}
     */
    public final void setSvtHalf(final String svtHalf) {
        if (!svtHalf.equals(AbstractSvtDaqMapping.TOP_HALF) && !svtHalf.equals(AbstractSvtDaqMapping.BOTTOM_HALF)) {
            throw new RuntimeException("[ " + this.getClass().getSimpleName() + " ]: Invalid value of SVT half.");
        }
        this.setFieldValue("svt_half", svtHalf);

    }

    /**
     * Set the SVT sensor layer number (1-10 for test run and 1-12 for engineering run).
     *
     * @param layer : SVT sensor layer number
     */
    public final void setLayerNumber(final int layer) {
        this.setFieldValue("layer", layer);
    }

    /**
     * Set the orientation of an SVT sensor (AXIAL or STEREO).
     *
     * @param orientation : Orientation of an SVT sensor (AXIAL or STEREO)
     */
    public final void setOrientation(final String orientation) {
        if (!orientation.equals(AbstractSvtDaqMapping.AXIAL) && !orientation.equals(AbstractSvtDaqMapping.STEREO)) {
            throw new RuntimeException("[ " + this.getClass().getSimpleName() + " ]: Invalid orientation of sensor.");
        }
        this.setFieldValue("orientation", orientation);
    }
}
