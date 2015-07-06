package org.hps.conditions.ecal;

import java.util.Comparator;

import org.hps.conditions.api.BaseConditionsObject;
import org.hps.conditions.api.BaseConditionsObjectCollection;
import org.hps.conditions.api.ConditionsObjectCollection;
import org.hps.conditions.database.Field;
import org.hps.conditions.database.Table;

/**
 * This class represents ECal pedestal and noise values from the conditions database. The pedestal and noise are in
 * units of ADC counts. They are the mean and the standard deviation of the digitized pre-amp output.
 *
 * @author Jeremy McCormick, SLAC
 */
@Table(names = {"ecal_calibrations", "test_run_ecal_calibrations"})
public final class EcalCalibration extends BaseConditionsObject {

    /**
     * The collection implementation for the object class.
     */
    public static class EcalCalibrationCollection extends BaseConditionsObjectCollection<EcalCalibration> {

        /**
         * Comparison using channel ID.
         */
        class ChannelIdComparator implements Comparator<EcalCalibration> {
            /**
             * Compare two ECAL calibration objects.
             *
             * @param o1 the first object
             * @param o2 the second object
             * @return -1, 0, 1 if first channel ID is less than, equal to, or greater than the second
             */
            @Override
            public int compare(final EcalCalibration o1, final EcalCalibration o2) {
                if (o1.getChannelId() < o2.getChannelId()) {
                    return -1;
                } else if (o1.getChannelId() > o2.getChannelId()) {
                    return 1;
                } else {
                    return 0;
                }
            }
        }

        /**
         * Sort and return the collection but do no modify in place.
         *
         * @return the sorted collection
         */
        public ConditionsObjectCollection<EcalCalibration> sorted() {
            return this.sorted(new ChannelIdComparator());
        }
    }

    /**
     * No argument constructor.
     */
    public EcalCalibration() {
    }

    /**
     * Full qualified constructor.
     *
     * @param channelId the channel ID
     * @param pedestal the pedestal measurement (ADC counts)
     * @param noise the noise measured as RMS
     */
    public EcalCalibration(final int channelId, final double pedestal, final double noise) {
        this.setFieldValue("ecal_channel_id", channelId);
        this.setFieldValue("pedestal", pedestal);
        this.setFieldValue("noise", noise);
    }

    /**
     * Get the ECAL channel ID.
     *
     * @return the ECAL channel ID
     */
    @Field(names = {"ecal_channel_id"})
    public Integer getChannelId() {
        return this.getFieldValue("ecal_channel_id");
    }

    /**
     * Get the noise value in units of ADC counts, which is the standard deviation of the digitized preamplifier output.
     *
     * @return the noise value
     */
    @Field(names = {"noise"})
    public Double getNoise() {
        return this.getFieldValue("noise");
    }

    /**
     * Get the pedestal value in units of ADC counts, which is the mean of the digitized preamplifier output.
     *
     * @return the gain value
     */
    @Field(names = {"pedestal"})
    public Double getPedestal() {
        return this.getFieldValue("pedestal");
    }
}