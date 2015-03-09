package org.hps.conditions.svt;

import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.hps.conditions.database.Converter;
import org.hps.conditions.database.Field;
import org.hps.conditions.database.MultipleCollectionsAction;
import org.hps.conditions.database.Table;
import org.hps.util.Pair;

/**
 * This class encapsulates the SVT DAQ map.
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 * @author Omar Moreno <omoreno1@ucsc.edu>
 */
@Table(names = {"svt_daq_map"})
@Converter(multipleCollectionsAction = MultipleCollectionsAction.ERROR)
public class SvtDaqMapping extends AbstractSvtDaqMapping {
   
    /**
     *  Constants describing the side of a sensor
     */
    public static final String ELECTRON = "ELECTRON";
    public static final String POSITRON = "POSITRON";

    public static class SvtDaqMappingCollection extends AbstractSvtDaqMappingCollection<SvtDaqMapping> {

        /**
         * Get a DAQ pair (FEB ID, FEB Hybrid ID) for the given
         * {@link HpsSiSensor}
         * 
         * @param sensor A sensor of type {@link HpsSiSensor}
         * @return The DAQ pair associated with the sensor
         */
        public Pair<Integer, Integer> getDaqPair(HpsSiSensor sensor) {

            String svtHalf = sensor.isTopLayer() ? TOP_HALF : BOTTOM_HALF;
            for (SvtDaqMapping object : this) {

                if (svtHalf.equals(object.getSvtHalf()) && object.getLayerNumber() == sensor.getLayerNumber() && object.getSide().equals(sensor.getSide())) {

                    return new Pair<Integer, Integer>(object.getFebID(), object.getFebHybridID());
                }
            }
            return null;
        }

        /**
         * Get the orientation of a sensor using the FEB ID and FEB Hybrid ID.
         * If the FEB ID and FEB Hybrid ID combination is not found, return
         * null.
         * 
         * @param daqPair (Pair<FEB ID, FEB Hybrid ID>) for a given sensor
         * @return If a daqPair is found, return an "A" if the sensor
         *         orientation is Axial, an "S" if the orientation is Stereo or
         *         null if the daqPair doesn't exist.
         */
        public String getOrientation(Pair<Integer, Integer> daqPair) {

            for (SvtDaqMapping daqMapping : this) {

                if (daqPair.getFirstElement() == daqMapping.getFebID() && daqPair.getSecondElement() == daqMapping.getFebHybridID()) {
                    return daqMapping.getOrientation();
                }
            }
            return null;
        }

        /**
         * Convert this object to a string.
         * @return This object converted to a string.
         */
        public String toString() {
            StringBuffer buff = new StringBuffer();
            buff.append("FEB ID: ");
            buff.append(" ");
            buff.append("FEB Hybrid ID: ");
            buff.append(" ");
            buff.append("Hybrid ID: ");
            buff.append(" ");
            buff.append("SVT half: ");
            buff.append(" ");
            buff.append("Layer");
            buff.append(" ");
            buff.append("Orientation: ");
            buff.append(" ");
            buff.append('\n');
            buff.append("----------------------");
            buff.append('\n');
            for (SvtDaqMapping object : this) {
                buff.append(object.getFebID());
                buff.append("    ");
                buff.append(object.getFebHybridID());
                buff.append("    ");
                buff.append(object.getSvtHalf());
                buff.append("    ");
                buff.append(String.format("%-2d", object.getLayerNumber()));
                buff.append("    ");
                buff.append(object.getSide());
                buff.append("    ");
                buff.append(object.getOrientation());
                buff.append("    ");
                buff.append('\n');
            }
            return buff.toString();
        }
    }

    /**
     *  Get the Front End Board (FEB) ID.
     *
     *  @return The FEB ID
     */
    @Field(names = {"feb_id"})
    public int getFebID() {
        return getFieldValue("feb_id");
    }

    /**
     *  Get the Front End Board (FEB) hybrid ID.
     *  
     *  @param The FEB hybrid ID
     */
    @Field(names = {"feb_hybrid_id"})
    public int getFebHybridID() {
        return getFieldValue("feb_hybrid_id");
    }

    /**
     *  Get the side of the sensor (ELECTRON or POSITRON).
     *  
     *  @param sensor side (ELECTRON or POSITRON)
     */
    @Field(names = {"side"})
    public String getSide() {
        return getFieldValue("side");
    }
    
    /**
     *  Set the Front End Board (FEB) ID.
     *  
     *  @param febID : FEB ID
     */
    public void setFebID(int febID) { 
        this.setFieldValue("feb_id", febID);
    }
    
    /**
     *  Set the Front End Board (FEB) hybrid ID.
     *  
     *  @param febHybridID : FEB hybrid ID
     */
    public void setFebHybridID(int febHybridID) { 
        this.setFieldValue("feb_hybrid_id", febHybridID);
    }
    
    /**
     *  Set the side of the sensor (ELECTRON or POSITRON).
     *  
     *  @param side : sensor side (ELECTRON or POSITRON)
     */
    public void setSide(String side) {
        if (side != SvtDaqMapping.ELECTRON || side != SvtDaqMapping.POSITRON) 
            throw new RuntimeException("[ " + this.getClass().getSimpleName() + " ]: Invalid value for sensor side.");
        this.setFieldValue("side", side);
    }
}