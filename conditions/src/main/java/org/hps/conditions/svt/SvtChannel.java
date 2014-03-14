package org.hps.conditions.svt;

import org.hps.conditions.AbstractConditionsObject;

/**
 * This class represents SVT channel setup information, including hybrid, FPGA, and channel numbers.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class SvtChannel extends AbstractConditionsObject {
    
    /**
     * Get the channel ID.
     * @return The channel ID.
     */
    public int getId() {
        return getFieldValue("id");
    }
    
    /**
     * Get the hybrid number.
     * @return The hybrid number.
     */
    public int getHybrid() {
        return getFieldValue("hybrid");
    }
    
    /**
     * Get the FPGA number.
     * @return The FPGA number.
     */
    public int getFpga() {
        return getFieldValue("fpga");
    }
    
    /**
     * Get the channel number.  This is different from the ID.
     * @return The channel number.
     */
    public int getChannel() {
        return getFieldValue("channel");
    }    
    
    /**
     * Convert this object to a human readable string.
     * @return This object as a string.
     */
    public String toString() {
        return "id: " + getId() + ", fpga: " + getFpga() + ", hybrid: " + getHybrid() + ", channel: " + getChannel();
    }
    
    /**
     * Implementation of equals.
     * @return True if the object equals this one; false if not.
     */
    public boolean equals(Object o) {
        if (o == null)
            return false;
        if (!(o instanceof SvtChannel))
            return false;
        if (o == this)
            return true;
        SvtChannel channel = (SvtChannel)o;
        return getId() == channel.getId() 
                && getHybrid() == channel.getHybrid() 
                && getFpga() == channel.getFpga() 
                && getHybrid() == channel.getHybrid();
    }    
}