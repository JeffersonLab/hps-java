package org.hps.conditions.ecal;

import org.hps.conditions.AbstractConditionsObject;
import org.hps.conditions.ConditionsObjectCollection;

/**
 * This class represents an ECAL channel that is considered "bad" which means
 * it should not be used in reconstruction.
 */
public final class EcalBadChannel extends AbstractConditionsObject {
    
    public static class EcalBadChannelCollection extends ConditionsObjectCollection<EcalBadChannel> {    
    }
    
    int getChannelId() {
        return getFieldValue("ecal_channel_id");
    }
    
}
