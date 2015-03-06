package org.hps.conditions.ecal;

import org.hps.conditions.api.AbstractConditionsObject;
import org.hps.conditions.api.AbstractConditionsObjectCollection;
import org.hps.conditions.database.Converter;
import org.hps.conditions.database.Field;
import org.hps.conditions.database.MultipleCollectionsAction;
import org.hps.conditions.database.Table;

/**
 * A conditions class for representing the setup of the LED system in the ECAL
 * for one channel.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
@Table(names = "ecal_leds")
@Converter(multipleCollectionsAction = MultipleCollectionsAction.LAST_CREATED)
public class EcalLed extends AbstractConditionsObject {

    /**
     * Generic collection class for these objects.
     */
    public static class EcalLedCollection extends AbstractConditionsObjectCollection<EcalLed> {
    }

    /**
     * Get the ECAL channel ID.
     * @return The ECAL channel ID.
     */
    @Field(names = {"ecal_channel_id"})
    public int getEcalChannelId() {
        return getFieldValue("ecal_channel_id");
    }

    /**
     * Get the crate number assigned to this crystal.
     * @return The crate number.
     */
    @Field(names = {"crate"})
    public int getCrateNumber() {
        return getFieldValue("crate");
    }

    /**
     * Get the LED number assigned to this crystal.
     * @return The LED number.
     */
    @Field(names = {"number"})
    public int getLedNumber() {
        return getFieldValue("number");
    }

    /**
     * Get the time delay of this channel.
     * @return The time delay.
     */
    @Field(names = {"time_delay"})
    public double getTimeDelay() {
        return getFieldValue("time_delay");
    }

    /**
     * Get the amplitude high setting.
     * @return The amplitude high setting.
     */
    @Field(names = {"amplitude_high"})
    public double getAmplitudeHigh() {
        return getFieldValue("amplitude_high");
    }

    /**
     * Get the amplitude low setting.
     * @return The amplitude low setting.
     */
    @Field(names = {"amplitude_low"})
    public double getAmplitudeLow() {
        return getFieldValue("amplitude_low");
    }
}
