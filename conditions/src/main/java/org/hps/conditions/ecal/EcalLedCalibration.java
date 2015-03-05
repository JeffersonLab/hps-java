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
    
    public EcalLedCalibration(int channelId, double mean, double rms) {
        this.setFieldValue("ecal_channel_id", channelId);
        this.setFieldValue("led_response", mean);
        this.setFieldValue("rms", rms);
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
     * Get the average led response
     * @return The average led response
     */
    @Field(names = {"led_response"})
    public double getLedResponse() {
        return getFieldValue("led_response");
    }

    /**
     * Get the RMS of the LED response
     * @return The RMS of the LED response.
     */
    @Field(names = {"rms"})
    public int getRms() {
        return getFieldValue("rms");
    }
}