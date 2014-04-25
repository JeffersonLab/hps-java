package org.hps.conditions.ecal;

import org.hps.conditions.AbstractConditionsObject;
import org.hps.conditions.ConditionsObjectCollection;

/**
 * This class is a simplistic representation of ECal pedestal and noise values from the
 * conditions database.
 * 
 * The pedestal and noise are in units of ADC counts. They are the mean and the standard
 * deviation of the digitized pre-amp output.
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public final class EcalCalibration extends AbstractConditionsObject {

    public static class EcalCalibrationCollection extends ConditionsObjectCollection<EcalCalibration> {
    }

    /**
     * Get the channel ID.
     * @return The channel ID.
     */
    public int getChannelId() {
        return getFieldValue("ecal_channel_id");
    }

    /**
     * Get the pedestal value in units of ADC counts, which is the mean of the digitized
     * pre-amp output.
     * @return The gain value.
     */
    public double getPedestal() {
        return getFieldValue("pedestal");
    }

    /**
     * Get the noise value in units of ADC counts, which is the standard deviation of the
     * digitized pre-amp output.
     * @return The noise value.
     */
    public double getNoise() {
        return getFieldValue("noise");
    }
}