package org.hps.conditions.hodoscope;

import org.hps.conditions.api.BaseConditionsObject;
import org.hps.conditions.api.BaseConditionsObjectCollection;
import org.hps.conditions.database.Field;
import org.hps.conditions.database.Table;

@Table(names = {"hodo_calibrations"})
public final class HodoscopeCalibration extends BaseConditionsObject {

    public static class HodoscopeCalibrationCollection extends BaseConditionsObjectCollection<HodoscopeCalibration> {
    }

    @Field(names = {"hodo_channel_id"})
    public Integer getChannelId() {
        return this.getFieldValue("hodo_channel_id");
    }

    @Field(names = {"noise"})
    public Double getNoise() {
        return this.getFieldValue("noise");
    }

    @Field(names = {"pedestal"})
    public Double getPedestal() {
        return this.getFieldValue("pedestal");
    }
}