package org.hps.conditions.ecal;

import org.hps.conditions.api.AbstractConditionsObject;
import org.hps.conditions.api.AbstractConditionsObjectCollection;

/**
 * This class represents an ECAL channel that is considered "bad" which means it
 * should not be used in reconstruction.
 */
public final class EcalBadChannel extends AbstractConditionsObject {

    public static class EcalBadChannelCollection extends AbstractConditionsObjectCollection<EcalBadChannel> {
    }

    int getChannelId() {
        return getFieldValue("ecal_channel_id");
    }

}
