package org.hps.conditions.ecal;

import java.util.Comparator;

import org.hps.conditions.api.BaseConditionsObject;
import org.hps.conditions.api.BaseConditionsObjectCollection;
import org.hps.conditions.api.ConditionsObjectCollection;
import org.hps.conditions.database.Converter;
import org.hps.conditions.database.Field;
import org.hps.conditions.database.MultipleCollectionsAction;
import org.hps.conditions.database.Table;

/**
 * This class represents a time shift calibration value for an ECAL channel.
 *
 * @author <a href="mailto:jeremym@slac.stanford.edu">Jeremy McCormick</a>
 */
@Table(names = {"ecal_time_shifts", "test_run_ecal_time_shifts"})
@Converter(multipleCollectionsAction = MultipleCollectionsAction.LAST_CREATED)
public final class EcalTimeShift extends BaseConditionsObject {

    /**
     * A collection of {@link EcalTimeShift} objects.
     */
    @SuppressWarnings("serial")
    public static final class EcalTimeShiftCollection extends BaseConditionsObjectCollection<EcalTimeShift> {

        /**
         * Sort and return a copy of the collection.
         * 
         * @return The sorted copy of the collection.
         */
        public ConditionsObjectCollection<EcalTimeShift> sorted() {
            return sorted(new ChannelIdComparator());
        }

        /**
         * Compare two objects by their channel ID.
         */
        class ChannelIdComparator implements Comparator<EcalTimeShift> {
            /**
             * Compare two objects by channel ID.
             * 
             * @param o1 the first object
             * @param o2 the second object
             * @return -1, 0 or 1 if first channel ID is less than, equal to, or greater than second
             */
            @Override
            public int compare(final EcalTimeShift o1, final EcalTimeShift o2) {
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
     * 
     * @return the ECAL channel ID
     */
    @Field(names = {"ecal_channel_id"})
    public Integer getChannelId() {
        return getFieldValue("ecal_channel_id");
    }

    /**
     * Get the time shift in nanoseconds
     * 
     * @return the time shift in nanoseconds
     */
    @Field(names = {"time_shift"})
    public Double getTimeShift() {
        return getFieldValue("time_shift");
    }
}
