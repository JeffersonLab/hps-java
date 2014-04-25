package org.hps.conditions.ecal;

/**
 * This class represents ECAL conditions per channel.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public final class EcalChannelConstants {

    EcalGain gain = null;
    EcalCalibration calibration = null;
    EcalTimeShift timeShift = null;
    boolean badChannel = false;

    /**
     * Class constructor, which is package protected.
     */
    EcalChannelConstants() {
    }

    /**
     * Set the gain.
     * @param gain The gain object.
     */
    void setGain(EcalGain gain) {
        this.gain = gain;
    }

    /**
     * Set the calibration.
     * @param calibration The calibration object.
     */
    void setCalibration(EcalCalibration calibration) {
        this.calibration = calibration;
    }

    /**
     * Set the time shift.
     * @param timeShift the time shift
     */
    void setTimeShift(EcalTimeShift timeShift) {
        this.timeShift = timeShift;
    }

    /**
     * Set whether this is a bad channel.
     * @param badChannel set to true to flag channel as bad
     */
    void setBadChannel(boolean badChannel) {
        this.badChannel = badChannel;
    }

    /**
     * Get the gain.
     * @return The gain.
     */
    public EcalGain getGain() {
        return gain;
    }

    /**
     * Get the calibration.
     * @return The calibration.
     */
    public EcalCalibration getCalibration() {
        return calibration;
    }

    /**
     * Get the time shift.
     * @return the time shift
     */
    public EcalTimeShift getTimeShift() {
        return timeShift;
    }

    /**
     * Get whether this channel is bad or not.
     * @return True if channel is bad; false if not.
     */
    public boolean isBadChannel() {
        return badChannel;
    }

}
