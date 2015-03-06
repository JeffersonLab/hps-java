package org.hps.conditions.svt;

import org.hps.conditions.api.AbstractConditionsObject;
import org.hps.conditions.api.AbstractConditionsObjectCollection;
import org.hps.conditions.database.Converter;
import org.hps.conditions.database.Field;
import org.hps.conditions.database.MultipleCollectionsAction;
import org.hps.conditions.database.Table;

@Table(names = {/*"svt_bad_channels",*/ "test_run_svt_bad_channels"})
@Converter(multipleCollectionsAction = MultipleCollectionsAction.LAST_CREATED)
public final class SvtBadChannel extends AbstractConditionsObject {

    public static class SvtBadChannelCollection extends AbstractConditionsObjectCollection<SvtBadChannel> {
    }

    @Field(names = {"svt_channel_id"})
    public int getChannelId() {
        return getFieldValue("svt_channel_id");
    }

    @Field(names = {"notes"})
    public int getNote() {
        return getFieldValue("notes");
    }
}
