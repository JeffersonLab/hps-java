package org.hps.conditions.ecal;

import org.hps.conditions.api.BaseConditionsObject;
import org.hps.conditions.api.BaseConditionsObjectCollection;
import org.hps.conditions.database.Field;
import org.hps.conditions.database.Table;

/**
 * Conditions object for representing the ECAL signal pulse width of a single channel.
 * <p>
 * The convention for the units is defined in the ECal reconstruction code (nominally nanoseconds).
 * 
 * @author Jeremy McCormick, SLAC
 */
@Table(names = {"ecal_pulse_widths"})
public final class EcalPulseWidth extends BaseConditionsObject {
    
    /**
     * Collection implementation for {@link EcalPulseWidth}.
     */
    public static final class EcalPulseWidthCollection extends BaseConditionsObjectCollection<EcalPulseWidth> {
    }
    
    /**
     * Get the ECAL channel ID.
     * @return the ECAL channel ID
     */
    @Field(names = {"ecal_channel_id"})
    public Integer getChannelId() {
        return getFieldValue("ecal_channel_id");
    }
    
    /**
     * Get the signal pulse width.
     * @return the signal pulse width
     */
    @Field(names = {"pulse_width"})
    public Double getPulseWidth() {
        return this.getFieldValue("pulse_width");
    }
}
