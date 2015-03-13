package org.hps.conditions.ecal;

import java.util.Comparator;

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
@Table(names = {"ecal_calibrations", "test_run_ecal_calibrations", "ecal_hardware_calibrations"})
@Converter(multipleCollectionsAction = MultipleCollectionsAction.LAST_CREATED)
public final class EcalCalibration extends AbstractConditionsObject {

    public static class EcalCalibrationCollection extends AbstractConditionsObjectCollection<EcalCalibration> {
        
        public AbstractConditionsObjectCollection<EcalCalibration> sorted() {
            return sorted(new ChannelIdComparator());
        }
                
        class ChannelIdComparator implements Comparator<EcalCalibration> {
            public int compare(EcalCalibration o1, EcalCalibration o2) {
                if (o1.getChannelId() < o2.getChannelId()) {
                    return -1;
                } else if (o1.getChannelId() > o2.getChannelId()) {
                    return 1;
                } else {
                    return 0;
                }
            }
        }
    }
               
    public EcalCalibration() {
    }
    
    public EcalCalibration(int channelId, double pedestal, double noise) {
        this.setFieldValue("ecal_channel_id", channelId);
        this.setFieldValue("pedestal", pedestal);
        this.setFieldValue("noise", noise);
    }

    /**
     * Get the ECAL channel ID.
     * @return The ECAL channel ID.
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