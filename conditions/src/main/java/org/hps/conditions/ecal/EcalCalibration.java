package org.hps.conditions.ecal;

import org.hps.conditions.api.AbstractConditionsObject;
import org.hps.conditions.api.AbstractConditionsObjectCollection;
import org.hps.conditions.database.Converter;
import org.hps.conditions.database.Field;
import org.hps.conditions.database.MultipleCollectionsAction;
import org.hps.conditions.database.Table;

/**
 * This class is a simplistic representation of ECal pedestal and noise values
 * from the conditions database.
 * 
 * The pedestal and noise are in units of ADC counts. They are the mean and the
 * standard deviation of the digitized pre-amp output.
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
@Table(names = {"ecal_calibrations"})
@Converter(multipleCollectionsAction = MultipleCollectionsAction.LAST_CREATED)
public final class EcalCalibration extends AbstractConditionsObject {

    public static class EcalCalibrationCollection extends AbstractConditionsObjectCollection<EcalCalibration> {
    }
    
    public EcalCalibration() {
    }
    
    public EcalCalibration(int channelId, double pedestal, double noise) {
        this.setFieldValue("ecal_channel_id", channelId);
        this.setFieldValue("pedestal", pedestal);
        this.setFieldValue("noise", noise);
    }

    /**
     * Get the channel ID.
     * @return The channel ID.
     */
    @Field(names = {"ecal_channel_id"})
    public int getChannelId() {
        return getFieldValue("ecal_channel_id");
    }

    /**
     * Get the pedestal value in units of ADC counts, which is the mean of the
     * digitized pre-amp output.
     * @return The gain value.
     */
    @Field(names = {"pedestal"})
    public double getPedestal() {
        return getFieldValue("pedestal");
    }

    /**
     * Get the noise value in units of ADC counts, which is the standard
     * deviation of the digitized pre-amp output.
     * @return The noise value.
     */
    @Field(names = {"noise"})
    public double getNoise() {
        return getFieldValue("noise");
    }
}