package org.hps.conditions.svt;

/**
 * This class represents the combined conditions for a single SVT channel.
 *
 * @author Omar Moreno, UCSC
 * @author Jeremy McCormick, SLAC
 */
public final class ChannelConstants {

    /**
     * Flag to indicate the channel is bad and should not be used for reconstruction.
     */
    private boolean badChannel = false;

    /**
     * The pedestal and noise for the channel.
     */
    private SvtCalibration calibration = null;

    /**
     * The calculated gain for the channel.
     */
    private SvtGain gain = null;

    /**
     * The shape fit parameters for the channel.
     */
    private SvtShapeFitParameters shapeFitParameters = null;

    /**
     * Class constructor.
     */
    ChannelConstants() {
    }

    /**
     * Get the calibration.
     *
     * @return the calibration object
     */
    public SvtCalibration getCalibration() {
        return this.calibration;
    }

    /**
     * Get the gain.
     *
     * @return the gain object
     */
    public SvtGain getGain() {
        return this.gain;
    }

    /**
     * Get the shape fit parameters.
     *
     * @return the shape fit parameters
     */
    public SvtShapeFitParameters getShapeFitParameters() {
        return this.shapeFitParameters;
    }

    /**
     * Check if this is a bad channel.
     *
     * @return <code>true</code> if channel is bad
     */
    public boolean isBadChannel() {
        return this.badChannel;
    }

    /**
     * Set the bad channel flag.
     *
     * @param badChannel the bad channel flag value
     */
    void setBadChannel(final boolean badChannel) {
        this.badChannel = badChannel;
    }

    /**
     * Set the calibration.
     *
     * @param calibration the calibration object
     */
    void setCalibration(final SvtCalibration calibration) {
        this.calibration = calibration;
    }

    /**
     * Set the gain.
     *
     * @param gain the gain object
     */
    void setGain(final SvtGain gain) {
        this.gain = gain;
    }

    /**
     * Set the pulse parameters.
     *
     * @param shapeFitParameters the pulse parameters
     */
    void setShapeFitParameters(final SvtShapeFitParameters shapeFitParameters) {
        this.shapeFitParameters = shapeFitParameters;
    }

    /**
     * Convert this object to a string.
     *
     * @return This object converted to a string.
     */
    @Override
    public String toString() {
        final StringBuffer buffer = new StringBuffer();
        buffer.append(this.getCalibration());
        buffer.append(", ");
        buffer.append(this.getGain());
        buffer.append(", ");
        buffer.append(this.getShapeFitParameters());
        return buffer.toString();
    }
}
