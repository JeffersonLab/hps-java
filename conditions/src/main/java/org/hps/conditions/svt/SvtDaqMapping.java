package org.hps.conditions.svt;

import org.hps.conditions.database.Field;
import org.hps.conditions.database.Table;
import org.hps.util.Pair;

/**
 * This class encapsulates the SVT DAQ map.
 *
 * @author Jeremy McCormick, SLAC
 * @author Omar Moreno, UCSC
 */
@Table(names = {"svt_daq_map"})
public class SvtDaqMapping extends AbstractSvtDaqMapping {

    /**
     * Collection implementation for {@link SvtDaqMapping} objects.
     */
    @SuppressWarnings("serial")
    public static class SvtDaqMappingCollection extends AbstractSvtDaqMappingCollection<SvtDaqMapping> {
       
        /**
         * Get the orientation of a sensor using the FEB ID and FEB Hybrid ID. If the FEB ID and FEB Hybrid ID
         * combination is not found, return null.
         *
         * @param daqPair the DAQ pair for a given sensor
         * @return "A" if sensor orientation is Axial; "S" if Stereo; null if daqPair doesn't exist.
         */
        @Override
        public String getOrientation(final Pair<Integer, Integer> daqPair) {
            for (final SvtDaqMapping daqMapping : this) {
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
        @Override
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
            for (final SvtDaqMapping object : this) {
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
     * Get the Front End Board (FEB) hybrid ID.
     *
     * @return the FEB Hybrid ID
     */
    @Field(names = {"feb_hybrid_id"})
    public final Integer getFebHybridID() {
        return this.getFieldValue("feb_hybrid_id");
    }

    /**
     * Get the Front End Board (FEB) ID.
     *
     * @return the FEB ID
     */
    @Field(names = {"feb_id"})
    public final Integer getFebID() {
        return this.getFieldValue("feb_id");
    }

    /**
     * Get the side of the sensor (ELECTRON or POSITRON).
     *
     * @see ELECTRON
     * @see POSITRON
     * @return sensor side (ELECTRON or POSITRON)
     */
    @Field(names = {"side"})
    public final String getSide() {
        return this.getFieldValue("side");
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
     * Set the Front End Board (FEB) ID.
     *
     * @param febID the FEB ID
     */
    public final void setFebID(final int febID) {
        this.setFieldValue("feb_id", febID);
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
