package org.hps.conditions.ecal;

import java.util.Comparator;

import org.hps.conditions.api.AbstractConditionsObject;
import org.hps.conditions.api.AbstractConditionsObjectCollection;
import org.hps.conditions.database.Converter;
import org.hps.conditions.database.Field;
import org.hps.conditions.database.MultipleCollectionsAction;
import org.hps.conditions.database.Table;

/**
 * A simplistic representation of gain values from the ECal conditions database.
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
@Table(names = {"ecal_gains", "test_run_ecal_gains", "ecal_hardware_gains"})
@Converter(multipleCollectionsAction = MultipleCollectionsAction.LAST_CREATED)
public final class EcalGain extends AbstractConditionsObject {

    public static class EcalGainCollection extends AbstractConditionsObjectCollection<EcalGain> {
        
        public AbstractConditionsObjectCollection<EcalGain> sorted() {
            return sorted(new ChannelIdComparator());
        }
                
        class ChannelIdComparator implements Comparator<EcalGain> {
            public int compare(EcalGain o1, EcalGain o2) {
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
     * Get the gain value in units of MeV/ADC count.
     * @return The gain value.
     */
    @Field(names = {"gain"})
    public double getGain() {
        return getFieldValue("gain");
    }

    /**
     * Get the ECal channel ID.
     * @return The ECal channel ID.
     */
    @Field(names = {"ecal_channel_id"})
    public int getChannelId() {
        return getFieldValue("ecal_channel_id");
    }
}