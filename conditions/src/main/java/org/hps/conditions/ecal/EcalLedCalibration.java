package org.hps.conditions.ecal;

import org.hps.conditions.api.BaseConditionsObject;
import org.hps.conditions.api.BaseConditionsObjectCollection;
import org.hps.conditions.database.Converter;
import org.hps.conditions.database.Field;
import org.hps.conditions.database.MultipleCollectionsAction;
import org.hps.conditions.database.Table;

/**
 * ECAL LED calibration information per channel.
 * 
 * @author <a href="mailto:jeremym@slac.stanford.edu">Jeremy McCormick</a>
 */
@Table(names = "ecal_led_calibrations")
@Converter(multipleCollectionsAction = MultipleCollectionsAction.LAST_CREATED)
public final class EcalLedCalibration extends BaseConditionsObject {

    /**
     * Generic collection class for these objects.
     */
    @SuppressWarnings("serial")
    public static class EcalLedCalibrationCollection extends BaseConditionsObjectCollection<EcalLedCalibration> {
    }

    /**
     * Class constructor.
     */
    public EcalLedCalibration() {
    }

    /**
     * Fully qualified constructor.
     * @param channelId The ECAL channel ID (not the LED channel ID).
     * @param ledResponse The mean of the LED response.
     * @param rms The RMS of the LED response.
     */
    public EcalLedCalibration(int channelId, double ledResponse, double rms) {
        this.setFieldValue("ecal_channel_id", channelId);
        this.setFieldValue("led_response", ledResponse);
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
     * Get the RMS of the LED response.
     * @return The RMS of the LED response.
     */
    @Field(names = {"rms"})
    public int getRms() {
        return getFieldValue("rms");
    }
}
