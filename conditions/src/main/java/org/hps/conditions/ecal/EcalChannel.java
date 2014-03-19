package org.hps.conditions.ecal;

import org.hps.conditions.AbstractConditionsObject;

/**
 * This class encapsulates all the setup information about a single ECal channel, e.g. one crystal.
 * This includes the channel ID from the conditions database; the crate, slot, and channel numbers
 * from the DAQ hardware, and the physical x and y values of the geometric crystal volumes. 
 * Each of these three pieces of data specifies a unique channel, so the information is in 
 * some sense redundant.  This class allows all these values to be associated by channel 
 * in the same place.  The object references are used as keys into a {@link EcalChannelCollection}
 * in the {@link EcalConditions} object for getting channel data.
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class EcalChannel extends AbstractConditionsObject {
    
    /**
     * Get the crate number.
     * @return The crate number.
     */
    public int getCrate() {
        return getFieldValue("crate");
    }
    
    /**
     * Get the slot number.
     * @return The slot number.
     */
    public int getSlot() {
        return getFieldValue("slot");
    }
    
    /**
     * Get the channel number.
     * @return The channel number.
     */
    public int getChannel() {
        return getFieldValue("channel");
    }
    
    /**
     * Get the x value.
     * @return The x value.
     */
    public int getX() {
        return getFieldValue("x");
    }
    
    /**
     * Get the y value.
     * @return The y value.
     */
    public int getY() {
        return getFieldValue("y");
    }

    /**
     * Get the ID.
     * @return The ID of the channel.
     */
    public int getChannelId() {
        return getFieldValue("channel_id");
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
        return c.getChannelId() == getChannelId() 
                && c.getCrate() == getCrate()
                && c.getSlot() == getSlot()
                && c.getChannel() == getChannel()
                && c.getX() == getX()
                && c.getY() == getY();
    }
    
    /**
     * Implementation of string conversion.
     * @return The string representation of this channel data.
     */
    public String toString() {
        return "id: " + getChannelId() 
                + ", crate: " + getCrate() 
                + ", slot: " + getSlot()
                + ", channel: " + getChannel() 
                + ", x: " + getX()
                + ", y: " + getY();
    }
}