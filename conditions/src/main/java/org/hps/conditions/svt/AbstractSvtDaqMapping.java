package org.hps.conditions.svt;

import org.hps.conditions.api.AbstractConditionsObject;
import org.hps.conditions.api.AbstractConditionsObjectCollection;
import org.hps.conditions.database.Field;
import org.hps.util.Pair;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;

/**
 * This abstract class provides some of the basic functionality used to access
 * SVT DAQ map variables.
 * 
 * @author Omar Moreno <omoreno1@ucsc.edu>
 */
public abstract class AbstractSvtDaqMapping extends AbstractConditionsObject {

        /**
         * Flag values for top or bottom half.
         */
        public static final String TOP_HALF = "T";
        public static final String BOTTOM_HALF = "B";

        /**
         * Flag values for axial or stereo sensors
         */
        public static final String AXIAL = "A";
        public static final String STEREO = "S";

        public static abstract class AbstractSvtDaqMappingCollection<T extends AbstractSvtDaqMapping> extends AbstractConditionsObjectCollection<T> {


        /**
         * Get a DAQ pair for the given {@link HpsSiSensor}
         * 
         * @param sensor A sensor of type {@link HpsSiSensor}
         * @return The DAQ pair associated with the sensor
         */
        public abstract Pair<Integer, Integer> getDaqPair(HpsSiSensor sensor);

        /**
         * Get the orientation of a sensor.
         * 
         * @param daqPair for a given sensor
         * @return If a daqPair is found, return an "A" if the sensor
         *         orientation is Axial, an "S" if the orientation is Stereo or
         *         null if the daqPair doesn't exist.
         */
        public abstract String getOrientation(Pair<Integer, Integer> daqPair);

    }

    /**
     *  Get the SVT half (TOP or BOTTOM) that the sensor belongs to.
     *  
     *  @return SVT half (TOP or BOTTOM)
     */
    @Field(names = {"svt_half"})
    public String getSvtHalf() {
        return getFieldValue("svt_half");
    }

    /**
     *  Get the SVT sensor layer number (1-10 for test run and 1-12 for
     *  engineering run).
     *  
     *  @return SVT sensor layer number
     */
    @Field(names = {"layer"})
    public int getLayerNumber() {
        return getFieldValue("layer");
    }

    
    /**
     *  Get the orientation of an SVT sensor (AXIAL or STEREO).
     * 
     *  @param orientation : Orientation of an SVT sensor (AXIAL or STEREO) 
     */
    @Field(names = {"orientation"})
    public String getOrientation() {
        return getFieldValue("orientation");
    }
    
    /**
     *  Set the SVT half (TOP or BOTTOM) that the sensor belongs to.
     *  
     *   @param svtHalf : SVT half (TOP or BOTTOM)
     */
    public void setSvtHalf(String svtHalf) { 
        if (!svtHalf.equals(AbstractSvtDaqMapping.TOP_HALF) && !svtHalf.equals(AbstractSvtDaqMapping.BOTTOM_HALF)) 
            throw new RuntimeException("[ " + this.getClass().getSimpleName() + " ]: Invalid value of SVT half.");
        this.setFieldValue("svt_half", svtHalf);
        
    }
    
    /**
     *  Set the SVT sensor layer number (1-10 for test run and 1-12 for
     *  engineering run).
     *  
     *  @param layer : SVT sensor layer number
     */
    public void setLayerNumber(int layer) { 
        this.setFieldValue("layer", layer);
    }
    
    /**
     *  Set the orientation of an SVT sensor (AXIAL or STEREO).
     * 
     *  @param orientation : Orientation of an SVT sensor (AXIAL or STEREO) 
     */
    public void setOrientation(String orientation) { 
        if (!orientation.equals(AbstractSvtDaqMapping.AXIAL) && !orientation.equals(AbstractSvtDaqMapping.STEREO))
            throw new RuntimeException("[ " + this.getClass().getSimpleName() + " ]: Invalid orientation of sensor.");
        this.setFieldValue("orientation", orientation);
    }
}
