package org.hps.conditions.svt;

import org.hps.conditions.api.BaseConditionsObject;
import org.hps.conditions.api.BaseConditionsObjectCollection;
import org.hps.conditions.database.Converter;
import org.hps.conditions.database.Field;
import org.hps.conditions.database.MultipleCollectionsAction;
import org.hps.conditions.database.Table;

/**
 * This class represents gain measurements for a single SVT channel.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
@Table(names = {"svt_gains", "test_run_svt_gains"})
@Converter(multipleCollectionsAction = MultipleCollectionsAction.LAST_CREATED)
public final class SvtGain extends BaseConditionsObject {

    public static class SvtGainCollection extends BaseConditionsObjectCollection<SvtGain> {
    }

    /**
     * Get the channel ID.
     * @return The channel ID.
     */
    @Field(names = {"svt_channel_id"})
    public int getChannelID() {
        return getFieldValue(Integer.class, "svt_channel_id");
    }

    /**
     * Get the gain.
     * @return The gain value.
     */
    @Field(names = {"gain"})
    public double getGain() {
        return getFieldValue(Double.class, "gain");
    }

    /**
     * Get the offset.
     * @return The offset value.
     */
    @Field(names = {"offset"})
    public double getOffset() {
        return getFieldValue(Double.class, "offset");
    }
}
