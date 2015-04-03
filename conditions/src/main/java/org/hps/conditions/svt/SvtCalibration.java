package org.hps.conditions.svt;

import org.hps.conditions.api.BaseConditionsObject;
import org.hps.conditions.api.BaseConditionsObjectCollection;
import org.hps.conditions.database.Converter;
import org.hps.conditions.database.Field;
import org.hps.conditions.database.MultipleCollectionsAction;
import org.hps.conditions.database.Table;

import static org.hps.conditions.svt.SvtChannel.MAX_NUMBER_OF_SAMPLES;

/**
 * This class encapsulates noise and pedestal measurement for an SVT channel.
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 * @author Omar Moreno <omoreno1@ucsc.edu>
 */
@Table(names = {"svt_calibrations", "test_run_svt_calibrations"})
@Converter(multipleCollectionsAction = MultipleCollectionsAction.LAST_UPDATED)
public final class SvtCalibration extends BaseConditionsObject {

    public static class SvtCalibrationCollection extends BaseConditionsObjectCollection<SvtCalibration> {
    }

    /**
     *  Default Constructor
     */
    public SvtCalibration() { 
    }
    
    /**
     *  Constructor
     *  
     *  @param channelID : The SVT channel ID
     */
    public SvtCalibration(int channelID) { 
       this.setChannelID(channelID); 
    }
    
    /**
     * Get the channel ID.
     * 
     * @return The channel ID.
     */
    @Field(names = {"svt_channel_id"})
    public int getChannelID() {
        return getFieldValue("svt_channel_id");
    }

    /**
     *  Get the noise value.
     * 
     *  @return The noise value.
     */
    @Field(names = {"noise_0", "noise_1", "noise_2", "noise_3", "noise_4", "noise_5"})
    public double getNoise(int sample) {
        if (sample < 0 || sample > MAX_NUMBER_OF_SAMPLES) {
            throw new IllegalArgumentException("Sample number is not within range.");
        }
        return getFieldValue(Double.class, "noise_" + Integer.toString(sample));
    }

    /**
     *  Get the pedestal value.
     * 
     *  @return The pedestal value.
     */
    @Field(names = {"pedestal_0", "pedestal_1", "pedestal_2", "pedestal_3", "pedestal_4", "pedestal_5"})
    public double getPedestal(int sample) {
        if (sample < 0 || sample > MAX_NUMBER_OF_SAMPLES) {
            throw new IllegalArgumentException("Sample number is not within range.");
        }
        return getFieldValue(Double.class, "pedestal_" + Integer.toString(sample));
    }

    /**
     *  Set the channel ID.
     * 
     *  @param channelID
     */
    public void setChannelID(int channelID) { 
        this.setFieldValue("svt_channel_id", channelID);
    }
   
    /**
     *  Set the noise value for the given sample.
     *  
     *  @param sample
     *  @param noise
     */
    public void setNoise(int sample, double noise) { 
        String noiseField = "noise_" + Integer.toString(sample);
        this.setFieldValue(noiseField, noise);
    }
   
    /**
     * Set the pedestal value for the given sample 
     * 
     * @param sample
     * @param pedestal
     */
    public void setPedestal(int sample, double pedestal) { 
        String pedestalField = "pedestal_" + Integer.toString(sample);
        this.setFieldValue(pedestalField, pedestal);
    }
    
    /**
     *  Convert this object to a human readable string.
     * 
     *  @return This object converted to a string.
     */
    public String toString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append("Channel ID: " + this.getChannelID());
        for (int i = 0; i < 115; i++) {
            buffer.append("-");
        }
        buffer.append("Pedestal sample 0:");
        buffer.append("      ");
        buffer.append("Pedestal sample 1:");
        buffer.append("      ");
        buffer.append("Pedestal sample 2:");
        buffer.append("      ");
        buffer.append("Pedestal sample 3:");
        buffer.append("      ");
        buffer.append("Pedesdtal sample 4:");
        buffer.append("      ");
        buffer.append("Pedestal sample 5:");
        buffer.append("\n");
        for (int i = 0; i < 115; i++) {
            buffer.append("-");
        }
        buffer.append("\n");
        for (int sample = 0; sample < MAX_NUMBER_OF_SAMPLES; sample++) {
            buffer.append(this.getPedestal(sample));
            buffer.append("      ");
        }
        buffer.append("Noise sample 0:");
        buffer.append("      ");
        buffer.append("Noise sample 1:");
        buffer.append("      ");
        buffer.append("Noise sample 2:");
        buffer.append("      ");
        buffer.append("Noise sample 3:");
        buffer.append("      ");
        buffer.append("Noise sample 4:");
        buffer.append("      ");
        buffer.append("Noise sample 5:");
        buffer.append("\n");
        for (int i = 0; i < 115; i++) {
            buffer.append("-");
        }
        buffer.append("\n");
        for (int sample = 0; sample < MAX_NUMBER_OF_SAMPLES; sample++) {
            buffer.append(this.getNoise(sample));
            buffer.append("      ");
        }
        return buffer.toString();
    }
}
