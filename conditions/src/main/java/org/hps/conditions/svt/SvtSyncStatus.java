package org.hps.conditions.svt;

import org.hps.conditions.api.BaseConditionsObject;
import org.hps.conditions.api.BaseConditionsObjectCollection;
import org.hps.conditions.database.Field;
import org.hps.conditions.database.Table;

@Table(names = {"svt_sync_statuses"})
public final class SvtSyncStatus extends BaseConditionsObject {

    public static class SvtSyncStatusCollection extends BaseConditionsObjectCollection<SvtSyncStatus> {
    }

    @Field(names = {"good"})
    public Boolean isGood() {
        return (this.getFieldValue(Integer.class, "good") > 0);
    }
}


