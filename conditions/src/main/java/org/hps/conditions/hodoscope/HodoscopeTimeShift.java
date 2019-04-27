package org.hps.conditions.hodoscope;

import org.hps.conditions.api.BaseConditionsObject;
import org.hps.conditions.api.BaseConditionsObjectCollection;
import org.hps.conditions.database.Field;
import org.hps.conditions.database.Table;

@Table(names = {"hodo_time_shifts"})
public final class HodoscopeTimeShift extends BaseConditionsObject {

    public static final class HodoscopeTimeShiftCollection extends BaseConditionsObjectCollection<HodoscopeTimeShift> {
    }

    @Field(names = {"hodo_channel_id"})
    public Integer getChannelId() {
        return this.getFieldValue("hodo_channel_id");
    }

    @Field(names = {"time_shift"})
    public Double getTimeShift() {
        return this.getFieldValue("time_shift");
    }
}
