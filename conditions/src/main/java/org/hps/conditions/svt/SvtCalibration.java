package org.hps.conditions.svt;

import static org.hps.conditions.svt.AbstractSvtChannel.MAX_NUMBER_OF_SAMPLES;

import org.hps.conditions.api.BaseConditionsObject;
import org.hps.conditions.api.BaseConditionsObjectCollection;
import org.hps.conditions.database.Converter;
import org.hps.conditions.database.Field;
import org.hps.conditions.database.MultipleCollectionsAction;
import org.hps.conditions.database.Table;

/**
 * This class encapsulates noise and pedestal measurement for an SVT channel.
 */
@Table(names = {"svt_calibrations", "test_run_svt_calibrations"})
@Converter(multipleCollectionsAction = MultipleCollectionsAction.LAST_UPDATED)
public final class SvtCalibration extends BaseConditionsObject {

    /**
     * The collection implementation for {@link SvtCalibration}.
     */
    @SuppressWarnings("serial")
    public static class SvtCalibrationCollection extends BaseConditionsObjectCollection<SvtCalibration> {
    }

    /**
     * Default constructor.
     */
    public SvtCalibration() {
    }

    /**
     * Constructor with channel ID.
     *
     * @param channelID the SVT channel ID
     */
    public SvtCalibration(final int channelID) {
        this.setChannelID(channelID);
    }

    /**
     * Get the channel ID.
     *
     * @return The channel ID
     */
    @Field(names = {"svt_channel_id"})
    public Integer getChannelID() {
        return this.getFieldValue("svt_channel_id");
    }

    /**
     * Get the noise value.
     *
     * @param sample the sample number (0-5)
     * @return the noise value
     */
    @Field(names = {"noise_0", "noise_1", "noise_2", "noise_3", "noise_4", "noise_5"})
    public Double getNoise(final int sample) {
        if (sample < 0 || sample > MAX_NUMBER_OF_SAMPLES) {
            throw new IllegalArgumentException("Sample number is not within range.");
        }
        return this.getFieldValue(Double.class, "noise_" + Integer.toString(sample));
    }

    /**
     * Get the pedestal value.
     *
     * @param sample the sample number (0-5)
     * @return The pedestal value.
     */
    @Field(names = {"pedestal_0", "pedestal_1", "pedestal_2", "pedestal_3", "pedestal_4", "pedestal_5"})
    public Double getPedestal(final int sample) {
        if (sample < 0 || sample > MAX_NUMBER_OF_SAMPLES) {
            throw new IllegalArgumentException("Sample number is not within range.");
        }
        return this.getFieldValue(Double.class, "pedestal_" + Integer.toString(sample));
    }

    /**
     * Set the channel ID.
     *
     * @param channelID the channel ID
     */
    public void setChannelID(final int channelID) {
        this.setFieldValue("svt_channel_id", channelID);
    }

    /**
     * Set the noise value for the given sample.
     *
     * @param sample the sample number
     * @param noise the noise
     */
    public void setNoise(final int sample, final double noise) {
        final String noiseField = "noise_" + Integer.toString(sample);
        this.setFieldValue(noiseField, noise);
    }

    /**
     * Set the pedestal value for the given sample.
     *
     * @param sample the sample number
     * @param pedestal the pedestal value
     */
    public void setPedestal(final int sample, final double pedestal) {
        final String pedestalField = "pedestal_" + Integer.toString(sample);
        this.setFieldValue(pedestalField, pedestal);
    }

    /**
     * Convert this object to a human readable string.
     *
     * @return This object converted to a string.
     */
    // FIXME: This is a mess when it prints to console.
    @Override
    public String toString() {
        final StringBuffer buffer = new StringBuffer();
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
