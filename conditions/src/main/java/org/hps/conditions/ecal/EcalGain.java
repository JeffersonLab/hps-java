package org.hps.conditions.ecal;

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
@Table(names = {"ecal_gains", "test_run_ecal_gains"})
@Converter(multipleCollectionsAction = MultipleCollectionsAction.LAST_CREATED)
public final class EcalGain extends AbstractConditionsObject {

    public static class EcalGainCollection extends AbstractConditionsObjectCollection<EcalGain> {
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