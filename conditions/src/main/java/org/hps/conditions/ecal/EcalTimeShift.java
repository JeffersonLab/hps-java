package org.hps.conditions.ecal;

import org.hps.conditions.api.AbstractConditionsObject;
import org.hps.conditions.api.AbstractConditionsObjectCollection;
import org.hps.conditions.database.Converter;
import org.hps.conditions.database.Field;
import org.hps.conditions.database.MultipleCollectionsAction;
import org.hps.conditions.database.Table;

/**
 * This class represents a time shift calibration value for an ECAL channel.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
@Table(names = {"ecal_time_shifts", "test_run_ecal_time_shifts"})
@Converter(multipleCollectionsAction = MultipleCollectionsAction.LAST_CREATED)
public final class EcalTimeShift extends AbstractConditionsObject {

    /**
     * A collection of {@link EcalTimeShift} objects.
     */
    public static class EcalTimeShiftCollection extends AbstractConditionsObjectCollection<EcalTimeShift> {
    }

    /**
     * Get the channel ID.
     * @return The ECAL channel ID.
     */
    @Field(names = {"ecal_channel_id"})
    public int getChannelId() {
        return getFieldValue("ecal_channel_id");
    }

    /**
     * Get the time shift in nanoseconds.
     * @return The time shift in nanoseconds.
     */
    @Field(names = {"time_shift"})
    public double getTimeShift() {
        return getFieldValue("time_shift");
    }
}
