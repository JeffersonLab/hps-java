package org.hps.conditions.ecal;

import org.hps.conditions.AbstractConditionsObject;
import org.hps.conditions.ConditionsObjectCollection;

/**
 * This class encapsulates all the setup information about a single ECal channel, e.g. one crystal.
 *
 * Any one of the three ID types specifies a unique channel.  This class allows all these values to be 
 * associated together by channel in the same place for ease of lookup.
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class EcalChannel extends AbstractConditionsObject {
    
    /**
     * The <code>DaqId</code> is the combination of crate, slot and channel that specify the channel's 
     * DAQ configuration.
     */
    public static final class DaqId {
        public int crate;
        public int slot;
        public int channel;
    }
    
    /**
     * The <code>GeometryId</code> contains the x and y indices of the crystal in the LCSIM-based geometry 
     * representation.
     */
    public static final class GeometryId {
        public int x;
        public int y;
    }
    
    /**
     * The <code>channelId</code> is a unique number identifying the channel within its conditions collection.
     * The channels in the database are given sequential channel IDs from 1 to N in semi-arbitrary order.
     * The channel ID is generally the number used to connect other conditions objects such as {@link EcalGain}
     * or {@link EcalCalibration} to the appropriate crystal in the calorimeter.
     */
    public static final class ChannelId {
        public int id;
    }
    
    /**
     * A collection of {@link EcalChannel} objects that can be queried.         
     */
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
        
        /**
         * Find a channel by its channel ID.
         * @param channelId The channel ID to find.
         * @return The matching channel or null if does not exist.
         */
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
     * Get the crate number of the channel.
     * @return The crate number.
     */
    public int getCrate() {
        return getFieldValue("crate");
    }
    
    /**
     * Get the slot number of the channel.
     * @return The slot number.
     */
    public int getSlot() {
        return getFieldValue("slot");
    }
    
    /**
     * Get the channel number of the channel.
     * @return The channel number.
     */
    public int getChannel() {
        return getFieldValue("channel");
    }
    
    /**
     * Get the x value of the channel.
     * @return The x value.
     */
    public int getX() {
        return getFieldValue("x");
    }
    
    /**
     * Get the y value of the channel.
     * @return The y value.
     */
    public int getY() {
        return getFieldValue("y");
    }

    /**
     * Get the ID of the channel.
     * @return The ID of the channel.
     */
    public int getChannelId() {
        return getFieldValue("channel_id");
    }
    
    /**
     * Implementation of equals.
     * @return True if objects are equal.
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