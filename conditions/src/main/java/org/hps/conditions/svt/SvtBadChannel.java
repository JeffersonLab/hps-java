package org.hps.conditions.svt;

import org.hps.conditions.api.AbstractConditionsObject;
import org.hps.conditions.api.AbstractConditionsObjectCollection;

public final class SvtBadChannel extends AbstractConditionsObject {

    public static class SvtBadChannelCollection extends AbstractConditionsObjectCollection<SvtBadChannel> {
    }

    public int getChannelId() {
        return getFieldValue("svt_channel_id");
    }

    public int getNote() {
        return getFieldValue("notes");
    }

}
