package org.hps.conditions.ecal;

/**
 * This class represents ECAL conditions per channel.  Individual channel
 * settings can be retrieved using the {@link EcalConditions} object
 * and its {@link EcalChannelCollection}.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class EcalChannelConstants {
    
    EcalGain gain = null;
    EcalCalibration calibration = null;
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
     * Set the bad channel setting.
     * @param badChannel The bad channel setting.
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
     * Get whether this channel is bad or not.
     * @return True if channel is bad; false if not.
     */
    public boolean isBadChannel() {
        return badChannel;
    }

}
