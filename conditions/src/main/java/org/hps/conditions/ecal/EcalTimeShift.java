package org.hps.conditions.ecal;

import org.hps.conditions.AbstractConditionsObject;
import org.hps.conditions.ConditionsObjectCollection;

/**
 * This class represents a time shift calibration value for an ECAL channel.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public final class EcalTimeShift extends AbstractConditionsObject {
    
    /**
     * A collection of {@link EcalTimeShift} objects.
     */
    public static class EcalTimeShiftCollection extends ConditionsObjectCollection<EcalTimeShift> {
    }
    
    /**
     * Get the channel ID.
     * @return The ECAL channel ID.
     */
    public int getChannelId() {
        return getFieldValue("ecal_channel_id");
    }
    
    /**
     * Get the time shift in nanoseconds.
     * @return The time shift in nanoseconds.
     */
    public double getTimeShift() {
        return getFieldValue("time_shift");
    }
}
