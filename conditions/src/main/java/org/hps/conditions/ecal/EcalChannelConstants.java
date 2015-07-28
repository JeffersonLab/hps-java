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
     * <code>true</code> if channel is bad and should not be used for reconstruction.
     */
    private boolean badChannel = false;

    /**
     * The channel's {@link EcalCalibration} conditions (pedestal and noise).
     */
    private EcalCalibration calibration = null;

    /**
     * The channel {@link EcalGain} conditions (per channel gain value).
     */
    private EcalGain gain = null;

    /**
     * The channel {@link EcalTimeShift} conditions.
     */
    private EcalTimeShift timeShift = null;
    
    /**
     * The channel's {@link EcalPulseWidth} conditions.
     */
    private EcalPulseWidth pulseWidth = null;

    /**
     * Class constructor, which is package protected.
     */
    EcalChannelConstants() {
    }

    /**
     * Get the channel calibration.
     *
     * @return the channel calibration
     */
    public EcalCalibration getCalibration() {
        return this.calibration;
    }

    /**
     * Get the channel gain.
     *
     * @return the channel gain
     */
    public EcalGain getGain() {
        return this.gain;
    }
    
    /**
     * Get the pulse width or <code>null</code> if it does not exist.
     * 
     * @return the pulse width
     */
    public EcalPulseWidth getPulseWidth() {
        return this.pulseWidth;
    }

    /**
     * Get the time shift.
     *
     * @return the time shift
     */
    public EcalTimeShift getTimeShift() {
        return this.timeShift;
    }

    /**
     * <code>true</code> if this is a bad channel.
     *
     * @return <code>true</code> if channel is bad
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
     * @param gain the channel gain
     */
    void setGain(final EcalGain gain) {
        this.gain = gain;
    }

    /**
     * Set the time shift.
     *
     * @param timeShift the time shift
     */
    void setTimeShift(final EcalTimeShift timeShift) {
        this.timeShift = timeShift;
    }
    
    /**
     * Set the pulse width.
     * 
     * @param pulseWidth the pulse width
     */
    void setPulseWidth(final EcalPulseWidth pulseWidth) {
        this.pulseWidth = pulseWidth;
    }
}
