package org.hps.conditions.ecal;

import java.util.Comparator;

import org.hps.conditions.api.BaseConditionsObject;
import org.hps.conditions.api.BaseConditionsObjectCollection;
import org.hps.conditions.api.ConditionsObjectCollection;
import org.hps.conditions.database.Field;
import org.hps.conditions.database.Table;

/**
 * A per channel ECAL gain value.
 *
 * @author Jeremy McCormick, SLAC
 */
@Table(names = {"ecal_gains", "test_run_ecal_gains"})
public final class EcalGain extends BaseConditionsObject {

    /**
     * The collection implementation for this class.
     */
    @SuppressWarnings("serial")
    public static final class EcalGainCollection extends BaseConditionsObjectCollection<EcalGain> {

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

        /**
         * Sort and return a copy of the collection.
         *
         * @return A sorted copy of the collection.
         */
        public ConditionsObjectCollection<EcalGain> sorted() {
            return this.sorted(new ChannelIdComparator());
        }
    }

    /**
     * Get the ECal channel ID.
     *
     * @return the ECal channel ID
     */
    @Field(names = {"ecal_channel_id"})
    public Integer getChannelId() {
        return this.getFieldValue("ecal_channel_id");
    }

    /**
     * Get the gain value in units of MeV/ADC count.
     *
     * @return the gain value
     */
    @Field(names = {"gain"})
    public Double getGain() {
        return this.getFieldValue("gain");
    }
}
