package org.hps.conditions.hodoscope;

import org.hps.conditions.api.BaseConditionsObject;
import org.hps.conditions.api.BaseConditionsObjectCollection;
import org.hps.conditions.database.Field;
import org.hps.conditions.database.Table;

@Table(names = {"hodo_gains"})
public final class HodoscopeGain extends BaseConditionsObject {

    /**
     * The collection implementation for this class.
     */
    @SuppressWarnings("serial")
    public static final class HodoscopeGainCollection extends BaseConditionsObjectCollection<HodoscopeGain> {
    }

    @Field(names = {"hodo_channel_id"})
    public Integer getChannelId() {
        return this.getFieldValue("hodo_channel_id");
    }

    /**
     * Get the gain value in units of MeV/ADC count.
     *
     * @return the gain value
     */
    @Field(names = {"gain"})
    public Double getGain() {
        return this.getFieldValue("gain");
    }
}
