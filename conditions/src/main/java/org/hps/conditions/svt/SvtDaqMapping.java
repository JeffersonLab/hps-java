package org.hps.conditions.svt;

import org.hps.conditions.database.Converter;
import org.hps.conditions.database.Field;
import org.hps.conditions.database.MultipleCollectionsAction;
import org.hps.conditions.database.Table;
import org.hps.util.Pair;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;

/**
 * This class encapsulates the SVT DAQ map.
 *
 * @author <a href="mailto:jeremym@slac.stanford.edu">Jeremy McCormick</a>
 * @author <a href="mailto:omoreno1@ucsc.edu">Omar Moreno</a>
 */
@Table(names = { "svt_daq_map" })
@Converter(multipleCollectionsAction = MultipleCollectionsAction.LAST_CREATED)
public class SvtDaqMapping extends AbstractSvtDaqMapping {

    /**
     * Electron side of a sensor.
     */
    public static final String ELECTRON = "ELECTRON";

    /**
     * Positron side of a sensor.
     */
    public static final String POSITRON = "POSITRON";

    /**
     * Default Constructor.
     */
    public SvtDaqMapping() {
    }

    /**
     * Constructor that takes FEB ID and Hybrid ID.
     *
     * @param febID the Front End Board (FEB) ID (0-9)
     * @param febHybridID the FEB hybrid ID (0-3)
     */
    public SvtDaqMapping(final int febID, final int febHybridID) {
        this.setFebID(febID);
        this.setFebHybridID(febHybridID);
    }

    /**
     * Collection implementation for {@link SvtDaqMapping} objects.
     */
    @SuppressWarnings("serial")
    public static class SvtDaqMappingCollection extends AbstractSvtDaqMappingCollection<SvtDaqMapping> {

        /**
         * Get a DAQ pair (FEB ID, FEB Hybrid ID) for the given {@link HpsSiSensor}.
         *
         * @param sensor a sensor of type {@link HpsSiSensor}
         * @return the DAQ pair associated with the sensor
         */
        @Override
        public Pair<Integer, Integer> getDaqPair(final HpsSiSensor sensor) {

            final String svtHalf = sensor.isTopLayer() ? TOP_HALF : BOTTOM_HALF;
            for (SvtDaqMapping object : this) {

                if (svtHalf.equals(object.getSvtHalf()) && object.getLayerNumber() == sensor.getLayerNumber()
                        && object.getSide().equals(sensor.getSide())) {

                    return new Pair<Integer, Integer>(object.getFebID(), object.getFebHybridID());
                }
            }
            return null;
        }

        /**
         * Get the orientation of a sensor using the FEB ID and FEB Hybrid ID. If the FEB ID and FEB Hybrid ID
         * combination is not found, return null.
         *
         * @param daqPair the DAQ pair for a given sensor
         * @return "A" if sensor orientation is Axial; "S" if Stereo; null if daqPair doesn't exist.
         */
        @Override
        public String getOrientation(final Pair<Integer, Integer> daqPair) {
            for (SvtDaqMapping daqMapping : this) {
                if (daqPair.getFirstElement() == daqMapping.getFebID()
                        && daqPair.getSecondElement() == daqMapping.getFebHybridID()) {
                    return daqMapping.getOrientation();
                }
            }
            return null;
        }

        /**
         * Convert this object to a string.
         *
         * @return this object converted to a string
         */
        public String toString() {
            final StringBuffer buff = new StringBuffer();
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
     * Get the Front End Board (FEB) ID.
     *
     * @return the FEB ID
     */
    @Field(names = { "feb_id" })
    public final int getFebID() {
        return getFieldValue("feb_id");
    }

    /**
     * Get the Front End Board (FEB) hybrid ID.
     *
     * @return the FEB Hybrid ID
     */
    @Field(names = { "feb_hybrid_id" })
    public final int getFebHybridID() {
        return getFieldValue("feb_hybrid_id");
    }

    /**
     * Get the side of the sensor (ELECTRON or POSITRON).
     *
     * @see {@link #ELECTRON}
     * @see {@link #POSITRON}
     * @return sensor side (ELECTRON or POSITRON)
     */
    @Field(names = { "side" })
    public final String getSide() {
        return getFieldValue("side");
    }

    /**
     * Set the Front End Board (FEB) ID.
     *
     * @param febID the FEB ID
     */
    public final void setFebID(final int febID) {
        this.setFieldValue("feb_id", febID);
    }

    /**
     * Set the Front End Board (FEB) hybrid ID.
     *
     * @param febHybridID the FEB hybrid ID
     */
    public final void setFebHybridID(final int febHybridID) {
        this.setFieldValue("feb_hybrid_id", febHybridID);
    }

    /**
     * Set the side of the sensor (ELECTRON or POSITRON).
     *
     * @param side the sensor side (ELECTRON or POSITRON)
     * @see {@link #ELECTRON}
     * @see {@link #POSITRON}
     */
    public final void setSide(final String side) {
        if (!side.equals(SvtDaqMapping.ELECTRON) && !side.equals(SvtDaqMapping.POSITRON)) {
            throw new RuntimeException("[ " + this.getClass().getSimpleName() + " ]: Invalid value for sensor side.");
        }
        this.setFieldValue("side", side);
    }
}
