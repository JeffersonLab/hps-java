package org.hps.conditions.ecal;

import java.util.Comparator;

import org.hps.conditions.api.BaseConditionsObject;
import org.hps.conditions.api.BaseConditionsObjectCollection;
import org.hps.conditions.database.Converter;
import org.hps.conditions.database.Field;
import org.hps.conditions.database.MultipleCollectionsAction;
import org.hps.conditions.database.Table;

/**
 * This class represents an ECAL channel that is considered "bad" which means it
 * should not be used in reconstruction.
 */
@Table(names = {"ecal_bad_channels", "test_run_ecal_bad_channels"})
@Converter(multipleCollectionsAction = MultipleCollectionsAction.LAST_UPDATED)
public final class EcalBadChannel extends BaseConditionsObject {

    /**
     * The collection class for this object.
     */
    @SuppressWarnings("serial")
    public static class EcalBadChannelCollection extends BaseConditionsObjectCollection<EcalBadChannel> {
        
        /**
         * Sort and return the collection without modifying in place.
         * @return The sorted collection.
         */
        public BaseConditionsObjectCollection<EcalBadChannel> sorted() {
            return sorted(new ChannelIdComparator());
        }
                
        /**
         * Comparison class for ECAL bad channels, which uses channel ID.
         */
        class ChannelIdComparator implements Comparator<EcalBadChannel> {
            /**
             * Compare two ECAL bad channel objects.
             * @param o1 The first object.
             * @param o2 The second object.
             * @return -1, 0, 1 if first channel ID is less than, equal to, or greater than the second.
             */
            public int compare(EcalBadChannel o1, EcalBadChannel o2) {
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
     * Get the ECAL channel ID.
     * @return The ECAL channel ID.
     */
    @Field(names = {"ecal_channel_id"})
    public int getChannelId() {
        return getFieldValue("ecal_channel_id");
    }
}