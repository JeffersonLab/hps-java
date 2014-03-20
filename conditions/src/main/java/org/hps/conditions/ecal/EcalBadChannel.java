package org.hps.conditions.ecal;

import org.hps.conditions.AbstractConditionsObject;
import org.hps.conditions.ConditionsObjectCollection;

public class EcalBadChannel extends AbstractConditionsObject {
    
    public static class EcalBadChannelCollection extends ConditionsObjectCollection<EcalBadChannel> {    
    }
    
    int getChannelId() {
        return getFieldValue("ecal_channel_id");
    }
    
}
