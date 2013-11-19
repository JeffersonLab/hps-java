package org.lcsim.hps.conditions.ecal;

import java.util.HashMap;

/**
 * This class maps ID values from the database to detailed ECal channel information.
 * There should really only be one of these data structures per job, as the EcalChannel 
 * objects are used as unique identifiers in the {@link EcalConditions} class.
 */
public class EcalChannelMap extends HashMap<Integer, EcalChannel> {
    
    /**
     * Class constructor.
     */
    EcalChannelMap() {        
    }
    
    /**
     * Find a channel by using DAQ information.
     * @param crate The crate number.
     * @param slot The slot number.
     * @param channelNumber The channel number.
     * @return The matching channel or null if does not exist.
     */
    public EcalChannel find(int crate, int slot, int channelNumber) {
        for (EcalChannel channel : values()) {
            if (channel.getCrate() == crate 
                    && channel.getSlot() == slot 
                    && channel.getChannel() == channelNumber) {
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
     * 
     * FIXME: Improve performance of this method from O(N).
     */
    public EcalChannel find(int x, int y) {
        for (EcalChannel channel : values()) {
            if (channel.getX() == x && channel.getY()== y) {
                return channel;
            }
        }
        return null;
    }
}