package org.hps.conditions.ecal;

/**
 * This class represents the conditions of a single ECAL channel such as a calibration and gain.
 *
 * @author Jeremy McCormick, SLAC
 * @see EcalGain
 * @see EcalCalibration
 * @see EcalTimeShift
 * @see EcalBadChannel
 */
public final class EcalChannelConstants {

    /**
     * True if channel is bad and should not be used for reconstruction.
     */
    private boolean badChannel = false;

    /**
     * The channel {@link EcalCalibration} conditions.
     */
    private EcalCalibration calibration = null;

    /**
     * The channel {@link EcalGain} conditions.
     */
    private EcalGain gain = null;

    /**
     * The channel {@link EcalTimeShift} conditions.
     */
    private EcalTimeShift timeShift = null;

    /**
     * Class constructor, which is package protected.
     */
    EcalChannelConstants() {
    }

    /**
     * Get the calibration.
     *
     * @return The calibration.
     */
    public EcalCalibration getCalibration() {
        return this.calibration;
    }

    /**
     * Get the gain.
     *
     * @return The gain.
     */
    public EcalGain getGain() {
        return this.gain;
    }

    /**
     * Get the time shift.
     *
     * @return The time shift.
     */
    public EcalTimeShift getTimeShift() {
        return this.timeShift;
    }

    /**
     * True if this is a bad channel.
     *
     * @return True if channel is bad.
     */
    public boolean isBadChannel() {
        return this.badChannel;
    }

    /**
     * Set whether this is a bad channel.
     *
     * @param badChannel set to true to flag channel as bad
     */
    void setBadChannel(final boolean badChannel) {
        this.badChannel = badChannel;
    }

    /**
     * Set the calibration.
     *
     * @param calibration the new calibration object
     */
    void setCalibration(final EcalCalibration calibration) {
        this.calibration = calibration;
    }

    /**
     * Set the gain.
     *
     * @param gain the new gain object
     */
    void setGain(final EcalGain gain) {
        this.gain = gain;
    }

    /**
     * Set the time shift.
     *
     * @param timeShift the new time shift
     */
    void setTimeShift(final EcalTimeShift timeShift) {
        this.timeShift = timeShift;
    }
}
