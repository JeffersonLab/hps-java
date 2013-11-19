package org.lcsim.hps.conditions.ecal;

/**
 * This class encapsulates all the setup information about a single ECal channel, e.g. one crystal.
 * This includes the channel ID from the conditions database; the crate, slot, and channel numbers
 * from the DAQ hardware; and the physical x and y values of the geometric crystal volumes. 
 * Each of these three pieces of data specifies a unique channel, so the information is in 
 * some sense redundant.  This class allows all these values to be associated by channel 
 * in the same place.  The object references are used as keys into a {@link EcalChannelMap}
 * in the {@link EcalConditions} object for getting channel data.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class EcalChannel {
    
    /** Channel info. */
    int id, crate, slot, channel, x, y;
    
    /**
     * Identifying information for an ECal channel.  This is over-specified in 
     * that crate-slot-channel, x-y, and id all uniquely identify the 
     * channel by themselves.  But we add them all here to have the information 
     * in one place.
     * @param id The database ID of the channel.
     * @param crate The crate number (1 or 2).
     * @param slot The slot number.
     * @param channel The channel number.
     * @param x The x value of the channel.
     * @param y The y value of the channel.
     */
    EcalChannel(int id, int crate, int slot, int channel, int x, int y) {
        this.id = id;
        this.crate = crate;
        this.slot = slot;
        this.channel = channel;
        this.x = x;
        this.y = y;
    }

    /**
     * Get the crate number.
     * @return The crate number.
     */
    public int getCrate() {
        return crate;
    }
    
    /**
     * Get the slot number.
     * @return The slot number.
     */
    public int getSlot() {
        return slot;
    }
    
    /**
     * Get the channel number.
     * @return The channel number.
     */
    public int getChannel() {
        return channel;
    }
    
    /**
     * Get the x value.
     * @return The x value.
     */
    public int getX() {
        return x;
    }
    
    /**
     * Get the y value.
     * @return The y value.
     */
    public int getY() {
        return y;
    }

    /**
     * Get the ID.
     * @return The ID of the channel.
     */
    public int getId() {
        return id;
    }
    
    /**
     * Implementation of equals.
     * @return True if objects are equal; false if not.
     */
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        if (!(o instanceof EcalChannel)) {
            return false;
        }
        if (o == this) {
            return true;
        }
        EcalChannel c = (EcalChannel)o;
        return c.getId() == id 
                && c.getCrate() == crate 
                && c.getSlot() == slot 
                && c.getChannel() == channel
                && c.getX() == x
                && c.getY() == y;
    }
    
    /**
     * Implement of string conversion.
     * @return The string representation of this channel data.
     */
    public String toString() {
        return "id: " + id 
                + ", crate: " + crate 
                + ", slot: " + slot 
                + ", channel: " + channel 
                + ", x: " + x 
                + ", y: " + y;
    }
}