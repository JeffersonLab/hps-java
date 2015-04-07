package org.hps.conditions.ecal;

/**
 * This class represents the conditions of a single ECAL channel such as a calibration and gain.
 * 
 * @author <a href="mailto:jeremym@slac.stanford.edu">Jeremy McCormick</a>
 * @see EcalGain
 * @see EcalCalibration
 * @see EcalTimeShift
 * @see EcalBadChannel
 */
public final class EcalChannelConstants {

    /**
     * The channel {@link EcalGain} conditions.
     */
    private EcalGain gain = null;

    /**
     * The channel {@link EcalCalibration} conditions.
     */
    private EcalCalibration calibration = null;

    /**
     * The channel {@link EcalTimeShift} conditions.
     */
    private EcalTimeShift timeShift = null;

    /**
     * True if channel is bad and should not be used for reconstruction.
     */
    private boolean badChannel = false;

    /**
     * Class constructor, which is package protected.
     */
    EcalChannelConstants() {
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
     * Set the calibration.
     * 
     * @param calibration the new calibration object
     */
    void setCalibration(final EcalCalibration calibration) {
        this.calibration = calibration;
    }

    /**
     * Set the time shift.
     * 
     * @param timeShift the new time shift
     */
    void setTimeShift(final EcalTimeShift timeShift) {
        this.timeShift = timeShift;
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
     * Get the gain.
     * 
     * @return The gain.
     */
    public EcalGain getGain() {
        return gain;
    }

    /**
     * Get the calibration.
     * 
     * @return The calibration.
     */
    public EcalCalibration getCalibration() {
        return calibration;
    }

    /**
     * Get the time shift.
     * 
     * @return The time shift.
     */
    public EcalTimeShift getTimeShift() {
        return timeShift;
    }

    /**
     * True if this is a bad channel.
     * 
     * @return True if channel is bad.
     */
    public boolean isBadChannel() {
        return badChannel;
    }
}
