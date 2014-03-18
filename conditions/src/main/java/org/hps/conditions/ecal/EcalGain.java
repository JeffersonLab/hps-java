package org.hps.conditions.ecal;

import org.hps.conditions.AbstractConditionsObject;

/**
 * This class is a simplistic representation of gain values from the ECal
 * conditions database.     
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class EcalGain extends AbstractConditionsObject {
               
    /**
     * Get the gain value.
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