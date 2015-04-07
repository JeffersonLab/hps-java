package org.hps.conditions.svt;

/**
 * This class represents the combined conditions for a single SVT channel.
 *
 * @author <a href="mailto:omoreno1@ucsc.edu">Omar Moreno</a>
 * @author <a href="mailto:jeremym@slac.stanford.edu">Jeremy McCormick</a>
 */
public final class ChannelConstants {

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
     * Flag to indicate the channel is bad and should not be used for reconstruction.
     */
    private boolean badChannel = false;

    /**
     * Class constructor.
     */
    ChannelConstants() {
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
     * Set the gain.
     *
     * @param gain the gain object
     */
    void setGain(final SvtGain gain) {
        this.gain = gain;
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
     * Set the bad channel flag.
     *
     * @param badChannel the bad channel flag value
     */
    void setBadChannel(final boolean badChannel) {
        this.badChannel = badChannel;
    }

    /**
     * Check if this is a bad channel.
     *
     * @return <code>true</code> if channel is bad
     */
    public boolean isBadChannel() {
        return badChannel;
    }

    /**
     * Get the shape fit parameters.
     *
     * @return the shape fit parameters
     */
    public SvtShapeFitParameters getShapeFitParameters() {
        return shapeFitParameters;
    }

    /**
     * Get the gain.
     *
     * @return the gain object
     */
    public SvtGain getGain() {
        return gain;
    }

    /**
     * Get the calibration.
     *
     * @return the calibration object
     */
    public SvtCalibration getCalibration() {
        return calibration;
    }

    /**
     * Convert this object to a string.
     *
     * @return This object converted to a string.
     */
    public String toString() {
        final StringBuffer buffer = new StringBuffer();
        buffer.append(getCalibration());
        buffer.append(", ");
        buffer.append(getGain());
        buffer.append(", ");
        buffer.append(getShapeFitParameters());
        return buffer.toString();
    }
}
