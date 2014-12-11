package org.hps.conditions.ecal;

import org.hps.conditions.api.AbstractConditionsObject;
import org.hps.conditions.api.AbstractConditionsObjectCollection;

/**
 * A conditions class for representing the setup of the LED system in the ECAL
 * for one channel.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
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
    public int getEcalChannelId() {
        return getFieldValue("ecal_channel_id");
    }

    /**
     * Get the crate number assigned to this crystal.
     * @return The crate number.
     */
    public int getCrateNumber() {
        return getFieldValue("crate");
    }

    /**
     * Get the LED number assigned to this crystal.
     * @return The LED number.
     */
    public int getLedNumber() {
        return getFieldValue("number");
    }

    /**
     * Get the time delay of this channel.
     * @return The time delay.
     */
    public double getTimeDelay() {
        return getFieldValue("time_delay");
    }

    /**
     * Get the amplitude high setting.
     * @return The amplitude high setting.
     */
    public double getAmplitudeHigh() {
        return getFieldValue("amplitude_high");
    }

    /**
     * Get the amplitude low setting.
     * @return The amplitude low setting.
     */
    public double getAmplitudeLow() {
        return getFieldValue("amplitude_low");
    }
}
