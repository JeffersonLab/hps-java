package org.hps.conditions.ecal;

import org.hps.conditions.AbstractConditionsObject;
import org.hps.conditions.ConditionsObjectCollection;

/**
 * This class is a simplistic representation of ECal pedestal and noise
 * values from the conditions database.
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class EcalCalibration extends AbstractConditionsObject {
    
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
     * Get the pedestal value.
     * @return The gain value.
     */
    public double getPedestal() {
        return getFieldValue("pedestal");
    }       
    
    /**
     * Get the noise value.
     * @return The noise value.
     */
    public double getNoise() {
        return getFieldValue("noise");
    }
}