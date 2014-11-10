package org.hps.conditions.ecal;

import org.hps.conditions.AbstractConditionsObject;
import org.hps.conditions.ConditionsObjectCollection;

/**
 * A conditions class for representing the setup of the LED system in the ECAL
 * for one channel.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class EcalLed extends AbstractConditionsObject {

    /**
     * Generic collection class for these objects.
     */
    public static class EcalLedCollection extends ConditionsObjectCollection<EcalLed> {
    }

    /**
     * Get the ECAL channel ID.
     * @return The ECAL channel ID.
     */
    int getEcalChannelId() {
        return getFieldValue("ecal_channel_id");
    }

    /**
     * Get the crate number assigned to this crystal.
     * @return The crate number.
     */
    int getCrateNumber() {
        return getFieldValue("crate");
    }

    /**
     * Get the LED number assigned to this crystal.
     * @return The LED number.
     */
    int getLedNumber() {
        return getFieldValue("number");
    }

    /**
     * Get the time delay of this channel.
     * @return The time delay.
     */
    double getTimeDelay() {
        return getFieldValue("time_delay");
    }

    /**
     * Get the amplitude high setting.
     * @return The amplitude high setting.
     */
    double getAmplitudeHigh() {
        return getFieldValue("amplitude_high");
    }

    /**
     * Get the amplitude low setting.
     * @return The amplitude low setting.
     */
    double getAmplitudeLow() {
        return getFieldValue("amplitude_low");
    }
}
