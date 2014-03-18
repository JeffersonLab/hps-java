package org.hps.conditions.ecal;

import org.hps.conditions.AbstractConditionsObject;

public class EcalBadChannel extends AbstractConditionsObject {
    
    int getChannelId() {
        return getFieldValue("ecal_channel_id");
    }
    
}
