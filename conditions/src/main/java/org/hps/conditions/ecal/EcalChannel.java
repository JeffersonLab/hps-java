package org.hps.conditions.ecal;

import org.hps.conditions.AbstractConditionsObject;
import org.hps.conditions.ConditionsObjectCollection;

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
    
    public static final class DaqId {
        public int crate;
        public int slot;
        public int channel;
    }
    
    public static final class GeometryId {
        public int x;
        public int y;
    }
    
    public static final class ChannelId {
        public int id;
    }
    
    public static class EcalChannelCollection extends ConditionsObjectCollection<EcalChannel> {
                    
        /**
         * Find a channel by using DAQ information.
         * @param crate The crate number.
         * @param slot The slot number.
         * @param channelNumber The channel number.
         * @return The matching channel or null if does not exist.
         */
        public EcalChannel findChannel(DaqId daqId) {
            for (EcalChannel channel : getObjects()) {
                if (channel.getCrate() == daqId.crate 
                        && channel.getSlot() == daqId.slot 
                        && channel.getChannel() == daqId.channel) {
                    return channel;
                }
            }
            return null;
        }
        
        /**
         * Find a channel by using its physical ID information.
         * @param x The x value.
         * @param y The y value.
         * @return The matching channel or null if does not exist.
         */
        public EcalChannel findChannel(GeometryId geometryId) {
            for (EcalChannel channel : getObjects()) {
                if (channel.getX() == geometryId.x && channel.getY() == geometryId.y) {
                    return channel;
                }
            }
            return null;
        }
        
        public EcalChannel findChannel(ChannelId channelId) {
            for (EcalChannel channel : getObjects()) {
                if (channel.getChannelId() == channelId.id) {
                    return channel;
                }
            }
            return null;
        }
    }
    
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