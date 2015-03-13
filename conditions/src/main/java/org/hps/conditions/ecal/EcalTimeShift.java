package org.hps.conditions.ecal;

import java.util.Comparator;

import org.hps.conditions.api.AbstractConditionsObject;
import org.hps.conditions.api.AbstractConditionsObjectCollection;
import org.hps.conditions.database.Converter;
import org.hps.conditions.database.Field;
import org.hps.conditions.database.MultipleCollectionsAction;
import org.hps.conditions.database.Table;
import org.hps.conditions.ecal.EcalCalibration.EcalCalibrationCollection.ChannelIdComparator;

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
        public AbstractConditionsObjectCollection<EcalTimeShift> sorted() {
            return sorted(new ChannelIdComparator());
        }
                
        class ChannelIdComparator implements Comparator<EcalTimeShift> {
            public int compare(EcalTimeShift o1, EcalTimeShift o2) {
                if (o1.getChannelId() < o2.getChannelId()) {
                    return -1;
                } else if (o1.getChannelId() > o2.getChannelId()) {
                    return 1;
                } else {
                    return 0;
                }
            }
        }
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
