package org.hps.conditions.trigger;

import org.hps.conditions.api.BaseConditionsObject;
import org.hps.conditions.api.BaseConditionsObjectCollection;
import org.hps.conditions.database.Field;
import org.hps.conditions.database.Table;

@Table(names = {"trigger_time_windows"})
public class TriggerTimeWindow extends BaseConditionsObject {
    
    @SuppressWarnings("serial")
    public static final class TriggerTimeWindowCollection extends BaseConditionsObjectCollection<TriggerTimeWindow> {
    }
    
    @Field(names = {"trigger_offset"})
    public Double getOffset() {
        return this.getFieldValue("trigger_offset");
    }    
    
    @Field(names = {"trigger_offset_min"})
    public Double getOffsetMin() {
        return this.getFieldValue("trigger_offset_min");
    }
    
    @Field(names = {"trigger_offset_max"})
    public Double getOffsetMax() {
        return this.getFieldValue("trigger_offset_max");
    }
}
