package org.lcsim.hps.conditions.svt;

/**
 * This class represents the conditions for a single SVT channel.
 * 
 * @author Omar Moreno <omoreno1@ucsc.edu>
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 * @version $Id: ChannelConstants.java,v 1.5 2013/10/04 01:43:48 jeremy Exp $
 */
public class ChannelConstants {

    private SvtCalibration calibration = null;
    private SvtGain gain = null;
    private PulseParameters pulseParameters = null;
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
    void setPulseParameters(PulseParameters pulseParameters) {
        this.pulseParameters = pulseParameters;
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
     * Get the pulse parameters.
     * @return The pulse parameters.
     */
    public PulseParameters getPulseParameters() {
        return pulseParameters;
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
        buffer.append(getPulseParameters());
        return buffer.toString();
    }
}

