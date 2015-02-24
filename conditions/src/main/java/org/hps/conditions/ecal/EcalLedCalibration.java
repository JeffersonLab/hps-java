package org.hps.conditions.ecal;

import org.hps.conditions.api.AbstractConditionsObject;
import org.hps.conditions.api.AbstractConditionsObjectCollection;
import org.hps.conditions.database.Converter;
import org.hps.conditions.database.Field;
import org.hps.conditions.database.MultipleCollectionsAction;
import org.hps.conditions.database.Table;

@Table(names = "ecal_led_calibrations")
@Converter(multipleCollectionsAction = MultipleCollectionsAction.LAST_CREATED)
public class EcalLedCalibration extends AbstractConditionsObject {
    
    /**
     * Generic collection class for these objects.
     */
    public static class EcalLedCalibrationCollection extends AbstractConditionsObjectCollection<EcalLedCalibration> {
    }
    
    /**
     * Get the ECAL channel ID.
     * @return The ECAL channel ID.
     */
    @Field(names = {"ecal_channel_id"})
    public int getEcalChannelId() {
        return getFieldValue("ecal_channel_id");
    }

    /**
     * Get the crate number assigned to this crystal.
     * @return The crate number.
     */
    @Field(names = {"led_response"})
    public double getLedResponse() {
        return getFieldValue("led_response");
    }

    /**
     * Get the LED number assigned to this crystal.
     * @return The LED number.
     */
    @Field(names = {"rms"})
    public int getRms() {
        return getFieldValue("rms");
    }
}