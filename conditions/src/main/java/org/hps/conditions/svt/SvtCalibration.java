package org.hps.conditions.svt;

import org.hps.conditions.AbstractConditionsObject;
import org.hps.conditions.ConditionsObjectCollection;

import static org.hps.conditions.svt.SvtChannel.MAX_NUMBER_OF_SAMPLES;

/**
 * This class encapsulates noise and pedestal measurement for an SVT channel.
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 * @author Omar Moreno <omoreno1@ucsc.edu>
 */
public final class SvtCalibration extends AbstractConditionsObject {

    public static class SvtCalibrationCollection extends ConditionsObjectCollection<SvtCalibration> {
    }

    /**
     * Get the channel ID.
     * @return The channel ID.
     */
    public int getChannelID() {
        return getFieldValue("svt_channel_id");
    }

    /**
     * Get the noise value.
     * @return The noise value.
     */
    public double getNoise(int sample) {
        if (sample < 0 || sample > MAX_NUMBER_OF_SAMPLES) {
            throw new RuntimeException("Sample number is not within range.");
        }
        return getFieldValue(Double.class, "noise_" + Integer.toString(sample));
    }

    /**
     * Get the pedestal value.
     * @return The pedestal value.
     */
    public double getPedestal(int sample) {
        if (sample < 0 || sample > MAX_NUMBER_OF_SAMPLES) {
            throw new RuntimeException("Sample number is not within range.");
        }
        return getFieldValue(Double.class, "pedestal_" + Integer.toString(sample));
    }

    /**
     * Convert this object to a human readable string.
     * @return This object converted to a string.
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
