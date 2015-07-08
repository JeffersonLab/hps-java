package org.hps.conditions.svt;

import org.hps.conditions.api.BaseConditionsObject;
import org.hps.conditions.api.BaseConditionsObjectCollection;
import org.hps.conditions.database.Field;
import org.hps.conditions.database.Table;

/**
 * This class represents the signal gain measurement for a single SVT channel.
 *
 * @author Jeremy McCormick, SLAC
 */
@Table(names = {"svt_gains", "test_run_svt_gains"})
public final class SvtGain extends BaseConditionsObject {

    /**
     * Collection implementation for {@link SvtGain} objects.
     */
    @SuppressWarnings("serial")
    public static class SvtGainCollection extends BaseConditionsObjectCollection<SvtGain> {
    }

    /**
     * Get the channel ID.
     *
     * @return The channel ID.
     */
    @Field(names = {"svt_channel_id"})
    public Integer getChannelID() {
        return this.getFieldValue(Integer.class, "svt_channel_id");
    }

    /**
     * Get the gain.
     *
     * @return The gain value.
     */
    @Field(names = {"gain"})
    public Double getGain() {
        return this.getFieldValue(Double.class, "gain");
    }

    /**
     * Get the offset.
     *
     * @return The offset value.
     */
    @Field(names = {"offset"})
    public Double getOffset() {
        return this.getFieldValue(Double.class, "offset");
    }
}
