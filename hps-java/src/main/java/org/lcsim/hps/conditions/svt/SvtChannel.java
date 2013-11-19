package org.lcsim.hps.conditions.svt;

/**
 * This class represents SVT channel setup information, including hybrid, FPGA, and channel numbers.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class SvtChannel {

    /** Channel data. */
    private int id, hybrid, fpga, channel;
    
    /**
     * Fully qualified constructor.
     * @param id The database record ID from the channel table.
     * @param fpga The FPGA number (0 to 6).
     * @param hybrid The hybrid number (0 to 2).
     * @param channel The channel number (0 to 639).
     */
    SvtChannel(int id, int fpga, int hybrid, int channel) {
        this.id = id;
        this.fpga = fpga;
        this.hybrid = hybrid;        
        this.channel = channel;
    }
    
    /**
     * Get the channel ID.
     * @return The channel ID.
     */
    public int getId() {
        return id;
    }
    
    /**
     * Get the hybrid number.
     * @return The hybrid number.
     */
    public int getHybrid() {
        return hybrid;
    }
    
    /**
     * Get the FPGA number.
     * @return The FPGA number.
     */
    public int getFpga() {
        return fpga;
    }
    
    /**
     * Get the channel number.  This is different from the ID.
     * @return The channel number.
     */
    public int getChannel() {
        return channel;
    }    
    
    /**
     * Convert this object to a human readable string.
     * @return This object as a string.
     */
    public String toString() {
        return "id: " + id + ", fpga: " + fpga + ", hybrid: " + hybrid + ", channel: " + channel;
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
        return id == channel.getId() 
                && hybrid == channel.getHybrid() 
                && fpga == channel.getFpga() 
                && hybrid == channel.getHybrid();
    }    
}