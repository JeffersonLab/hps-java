package org.hps.conditions.svt;

import org.hps.conditions.AbstractConditionsObject;
import org.hps.conditions.ConditionsObjectCollection;

/**
 * This class represents a noise and pedestal measurement for an SVT channel.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public final class SvtCalibration extends AbstractConditionsObject {

    public static class SvtCalibrationCollection extends ConditionsObjectCollection<SvtCalibration> {
    }
    
    /**
     * Get the channel ID.
     * @return The channel ID.
     */
    public int getChannelId() {
        return getFieldValue(Integer.class, "svt_channel_id");
    }
    
    /**
     * Get the noise value.
     * @return The noise value.
     */
    public double getNoise() {
        return getFieldValue(Double.class, "noise");
    }
    
    /**
     * Get the pedestal value.
     * @return The pedestal value.
     */
    public double getPedestal() {
        return getFieldValue(Double.class, "pedestal");
    }
    
    /**
     * Convert this object to a human readable string.
     * @return This object converted to a string.
     */
    public String toString() {
        return "noise: " + getNoise() + ", pedestal: " + getPedestal();
    }
}
