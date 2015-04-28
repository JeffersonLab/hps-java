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
 * A per channel ECAL gain value.
 *
 * @author <a href="mailto:jeremym@slac.stanford.edu">Jeremy McCormick</a>
 */
@Table(names = {"ecal_gains", "test_run_ecal_gains", "ecal_hardware_gains"})
@Converter(multipleCollectionsAction = MultipleCollectionsAction.LAST_CREATED)
public final class EcalGain extends BaseConditionsObject {

    /**
     * The collection implementation for this class.
     */
    @SuppressWarnings("serial")
    public static final class EcalGainCollection extends BaseConditionsObjectCollection<EcalGain> {

        /**
         * Sort and return a copy of the collection.
         * 
         * @return A sorted copy of the collection.
         */
        public ConditionsObjectCollection<EcalGain> sorted() {
            return sorted(new ChannelIdComparator());
        }

        /**
         * Comparison implementation by channel ID.
         */
        class ChannelIdComparator implements Comparator<EcalGain> {
            /**
             * Compare two objects by their channel ID.
             * 
             * @param o1 The first object.
             * @param o2 The second object.
             * @return -1, 0 or 1 if first channel ID is less than, equal to, or greater than second.
             */
            @Override
            public int compare(final EcalGain o1, final EcalGain o2) {
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
     * Get the gain value in units of MeV/ADC count.
     *
     * @return the gain value
     */
    @Field(names = {"gain"})
    public Double getGain() {
        return getFieldValue("gain");
    }

    /**
     * Get the ECal channel ID.
     *
     * @return the ECal channel ID
     */
    @Field(names = {"ecal_channel_id"})
    public Integer getChannelId() {
        return getFieldValue("ecal_channel_id");
    }
}
