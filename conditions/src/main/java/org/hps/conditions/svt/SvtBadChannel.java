package org.hps.conditions.svt;

import org.hps.conditions.AbstractConditionsObject;

public class SvtBadChannel extends AbstractConditionsObject {
    
    public int getChannelId() {
        return getFieldValue("svt_channel_id");
    }

}
