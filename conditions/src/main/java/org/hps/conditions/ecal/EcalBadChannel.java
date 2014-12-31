package org.hps.conditions.ecal;

import org.hps.conditions.api.AbstractConditionsObject;
import org.hps.conditions.api.AbstractConditionsObjectCollection;
import org.hps.conditions.database.Converter;
import org.hps.conditions.database.Field;
import org.hps.conditions.database.MultipleCollectionsAction;
import org.hps.conditions.database.Table;

/**
 * This class represents an ECAL channel that is considered "bad" which means it
 * should not be used in reconstruction.
 */
@Table(names = {"ecal_bad_channels"})
@Converter(multipleCollectionsAction = MultipleCollectionsAction.COMBINE)
public final class EcalBadChannel extends AbstractConditionsObject {

    public static class EcalBadChannelCollection extends AbstractConditionsObjectCollection<EcalBadChannel> {
    }

    /**
     * Get the channel ID of the bad channel.
     * @return The channel ID of the bad channel.
     */
    @Field(names = {"ecal_channel_id"})
    public int getChannelId() {
        return getFieldValue("ecal_channel_id");
    }
}
