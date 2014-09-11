package org.hps.conditions.svt;

/**
 * This class represents the conditions for a single SVT channel.
 * 
 * @author Omar Moreno <omoreno1@ucsc.edu>
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 *
 */
public final class ChannelConstants {

    private SvtCalibration calibration = null;
    private SvtGain gain = null;
    private SvtShapeFitParameters shapeFitParameters = null;
    private boolean badChannel = false;

    /**
     * Class constructor.
     */
    ChannelConstants() {
    }

    /**
     * Set the pulse parameters.
     * @param pulseParameters The pulse parameters
     */
    void setShapeFitParameters(SvtShapeFitParameters shapeFitParameters) {
        this.shapeFitParameters = shapeFitParameters;
    }

    /**
     * Set the gain.
     * @param gain The gain object.
     */
    void setGain(SvtGain gain) {
        this.gain = gain;
    }

    /**
     * Set the calibration.
     * @param calibration The calibration object.
     */
    void setCalibration(SvtCalibration calibration) {
        this.calibration = calibration;
    }

    /**
     * Set the bad channel flag.
     * @param badChannel The bad channel flag value.
     */
    void setBadChannel(boolean badChannel) {
        this.badChannel = badChannel;
    }

    /**
     * Check if this is a bad channel.
     * @return True if channel is bad; false if not.
     */
    public boolean isBadChannel() {
        return badChannel;
    }

    /**
     * Get the shape fit parameters.
     * @return The shape fit parameters.
     */
    public SvtShapeFitParameters getShapeFitParameters() {
        return shapeFitParameters;
    }

    /**
     * Get the gain.
     * @return The gain.
     */
    public SvtGain getGain() {
        return gain;
    }

    /**
     * Get the calibration.
     * @return The calibration.
     */
    public SvtCalibration getCalibration() {
        return calibration;
    }

    /**
     * Convert this object to a string.
     * @return This object converted to a string.
     */
    public String toString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append(getCalibration());
        buffer.append(", ");
        buffer.append(getGain());
        buffer.append(", ");
        buffer.append(getShapeFitParameters());
        return buffer.toString();
    }
}
