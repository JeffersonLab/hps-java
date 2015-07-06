package org.hps.conditions.ecal;

import java.util.Comparator;

import org.hps.conditions.api.BaseConditionsObject;
import org.hps.conditions.api.BaseConditionsObjectCollection;
import org.hps.conditions.api.ConditionsObjectCollection;
import org.hps.conditions.database.Field;
import org.hps.conditions.database.Table;

/**
 * A conditions class for representing the setup of the LED system in the ECAL for one channel.
 *
 * @author Jeremy McCormick, SLAC
 */
@Table(names = "ecal_leds")
public final class EcalLed extends BaseConditionsObject {

    /**
     * Generic collection class for these objects.
     */
    @SuppressWarnings("serial")
    public static class EcalLedCollection extends BaseConditionsObjectCollection<EcalLed> {

        /**
         * Comparison implementation by channel ID.
         */
        class ChannelIdComparator implements Comparator<EcalLed> {
            /**
             * Compare two objects by channel ID.
             *
             * @param o1 the first object
             * @param o2 the second object
             * @return -1, 0, or 1 if first channel ID is less than, equal to or greater than the first
             */
            @Override
            public int compare(final EcalLed o1, final EcalLed o2) {
                if (o1.getEcalChannelId() < o2.getEcalChannelId()) {
                    return -1;
                } else if (o1.getEcalChannelId() > o2.getEcalChannelId()) {
                    return 1;
                } else {
                    return 0;
                }
            }

        }

        /**
         * Sort and return a copy of this collection.
         *
         * @return the new sorted collection
         */
        public ConditionsObjectCollection<EcalLed> sorted() {
            return this.sorted(new ChannelIdComparator());
        }
    }

    /**
     * Get the amplitude high setting.
     *
     * @return the amplitude high setting
     */
    @Field(names = {"amplitude_high"})
    public Double getAmplitudeHigh() {
        return this.getFieldValue("amplitude_high");
    }

    /**
     * Get the amplitude low setting.
     *
     * @return the amplitude low setting
     */
    @Field(names = {"amplitude_low"})
    public Double getAmplitudeLow() {
        return this.getFieldValue("amplitude_low");
    }

    /**
     * Get the crate number assigned to this crystal.
     *
     * @return the crate number
     */
    @Field(names = {"crate"})
    public Integer getCrateNumber() {
        return this.getFieldValue("crate");
    }

    /**
     * Get the ECAL channel ID.
     *
     * @return the ECAL channel ID
     */
    @Field(names = {"ecal_channel_id"})
    public Integer getEcalChannelId() {
        return this.getFieldValue("ecal_channel_id");
    }

    /**
     * Get the LED number assigned to this crystal.
     *
     * @return the LED number
     */
    @Field(names = {"number"})
    public Integer getLedNumber() {
        return this.getFieldValue("number");
    }

    /**
     * Get the time delay of this channel.
     *
     * @return the time delay
     */
    @Field(names = {"time_delay"})
    public Double getTimeDelay() {
        return this.getFieldValue("time_delay");
    }
}
