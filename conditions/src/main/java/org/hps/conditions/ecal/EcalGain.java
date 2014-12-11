package org.hps.conditions.ecal;

import org.hps.conditions.api.AbstractConditionsObject;
import org.hps.conditions.api.AbstractConditionsObjectCollection;

/**
 * A simplistic representation of gain values from the ECal conditions database.
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public final class EcalGain extends AbstractConditionsObject {

    public static class EcalGainCollection extends AbstractConditionsObjectCollection<EcalGain> {
    }

    /**
     * Get the gain value in units of MeV/ADC count.
     * @return The gain value.
     */
    public double getGain() {
        return getFieldValue("gain");
    }

    /**
     * Get the ECal channel ID.
     * @return The ECal channel ID.
     */
    public int getChannelId() {
        return getFieldValue("ecal_channel_id");
    }
}