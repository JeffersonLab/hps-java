package org.hps.conditions.ecal;

import org.hps.conditions.ConditionsObjectCollection;

/**
 * This class maps ID values from the database to detailed ECal channel information.
 * There should really only be one of these data structures per job, as the EcalChannel 
 * objects are used as unique identifiers in the {@link EcalConditions} class.
 */
public class EcalChannelCollection extends ConditionsObjectCollection<EcalChannel> {
    
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
    
    /**
     * Class constructor.
     */
    EcalChannelCollection() {        
    }
    
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