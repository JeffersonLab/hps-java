package org.hps.conditions.svt;

import org.hps.conditions.api.BaseConditionsObject;
import org.hps.conditions.api.BaseConditionsObjectCollection;
import org.hps.conditions.database.Converter;
import org.hps.conditions.database.Field;
import org.hps.conditions.database.MultipleCollectionsAction;
import org.hps.conditions.database.Table;

/**
 * Represents a channel that has been flagged as bad, which should not be used for physics reconstructions. This might
 * be done if the channel is extremely noisy, etc.
 *
 * @author <a href="mailto:jeremym@slac.stanford.edu">Jeremy McCormick</a>
 */
@Table(names = {"svt_bad_channels", "test_run_svt_bad_channels"})
@Converter(multipleCollectionsAction = MultipleCollectionsAction.LAST_CREATED)
public final class SvtBadChannel extends BaseConditionsObject {

    /**
     * The collection implementation for {@link SvtBadChannel}.
     */
    @SuppressWarnings("serial")
    public static class SvtBadChannelCollection extends BaseConditionsObjectCollection<SvtBadChannel> {
    }

    /**
     * Get the channel ID.
     * 
     * @return the channel ID
     */
    @Field(names = {"svt_channel_id"})
    public Integer getChannelId() {
        return getFieldValue("svt_channel_id");
    }

    /**
     * Get a note about the bad channel.
     * 
     * @return a note about the bad channel
     */
    @Field(names = {"notes"})
    public Integer getNote() {
        return getFieldValue("notes");
    }
}
