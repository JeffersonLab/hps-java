package org.hps.conditions.ecal;

import java.util.Comparator;

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
@Table(names = {"ecal_bad_channels", "test_run_ecal_bad_channels"})
@Converter(multipleCollectionsAction = MultipleCollectionsAction.LAST_UPDATED)
public final class EcalBadChannel extends AbstractConditionsObject {

    public static class EcalBadChannelCollection extends AbstractConditionsObjectCollection<EcalBadChannel> {
        
        public AbstractConditionsObjectCollection<EcalBadChannel> sorted() {
            return sorted(new ChannelIdComparator());
        }
                
        class ChannelIdComparator implements Comparator<EcalBadChannel> {
            public int compare(EcalBadChannel o1, EcalBadChannel o2) {
                if (o1.getChannelId() < o2.getChannelId()) {
                    return -1;
                } else if (o1.getChannelId() > o2.getChannelId()) {
                    return 1;
                } else {
                    return 0;
                }
            }
        }        
    }
    
    /**
     * Get the ECAL channel ID.
     * @return The ECAL channel ID.
     */
    @Field(names = {"ecal_channel_id"})
    public int getChannelId() {
        return getFieldValue("ecal_channel_id");    
    }
}