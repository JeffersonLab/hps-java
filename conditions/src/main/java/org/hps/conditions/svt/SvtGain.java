package org.hps.conditions.svt;

import org.hps.conditions.AbstractConditionsObject;
import org.hps.conditions.ConditionsObjectCollection;

/**
 * This class represents gain measurements for a single SVT channel.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public final class SvtGain extends AbstractConditionsObject {

    public static class SvtGainCollection extends ConditionsObjectCollection<SvtGain> {
    }

    /**
     * Get the channel ID.
     * @return The channel ID.
     */
    int getChannelID() {
        return getFieldValue(Integer.class, "svt_channel_id");
    }

    /**
     * Get the gain.
     * @return The gain value.
     */
    double getGain() {
        return getFieldValue(Double.class, "gain");
    }

    /**
     * Get the offset.
     * @return The offset value.
     */
    double getOffset() {
        return getFieldValue(Double.class, "offset");
    }
}
