package org.hps.conditions.svt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.hps.conditions.database.Converter;
import org.hps.conditions.database.Field;
import org.hps.conditions.database.MultipleCollectionsAction;
import org.hps.conditions.database.Table;
import org.hps.util.Pair;

/**
 * This class represents SVT channel setup information, including FEB ID, FEB Hybrid ID, and channel numbers.
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 * @author Omar Moreno <omoreno1@ucsc.edu>
 */
@Table(names = { "svt_channels" })
@Converter(multipleCollectionsAction = MultipleCollectionsAction.LAST_CREATED)
public final class SvtChannel extends AbstractSvtChannel {

    /**
     * 
     */
    public SvtChannel() { 
    }
    
    /**
     *  Constructor 
     *
     *  @param channelID : The SVT channel ID
     *  @param febID : The Front End Board (FEB) ID (0-9)
     *  @param febHybridID : The hybrid ID (0-3)
     *  @param channel : The channel number (0-639)
     */
    public SvtChannel(int channelID, int febID, int febHybridID, int channel) { 
        if (!this.isValidFeb(febID) 
                || !this.isValidFebHybridID(febHybridID) 
                || !this.isValidPhysicalChannel(channel)) { 
            throw new RuntimeException("Invalid FEB ID, FEB hybrid ID or physical channel number is being used.");
        }
        this.setChannelID(channelID);
        this.setFebID(febID);
        this.setFebHybridID(febHybridID);
        this.setChannel(channel);
    }
    
    public static class SvtChannelCollection extends AbstractSvtChannel.AbstractSvtChannelCollection<SvtChannel> {
        /**
         *  Find channels that match a DAQ pair (FEB ID, FEB Hybrid ID).
         * 
         *  @param pair : The DAQ pair consiting of a FEB ID and FEB Hybrid ID.
         *  @return The channels matching the DAQ pair or null if ?not found.
         */
        @Override
        public Collection<SvtChannel> find(Pair<Integer, Integer> pair) {
            List<SvtChannel> channels = new ArrayList<SvtChannel>();
            int febID = pair.getFirstElement();
            int febHybridID = pair.getSecondElement();
            for (SvtChannel channel : this) {
                if (channel.getFebID() == febID && channel.getFebHybridID() == febHybridID) {
                    channels.add(channel);
                }
            }
            return channels;
        }

        /**
         *  Get the SVT channel ID associated with a given FEB ID/Hybrid ID/physical channel.
         *
         *  @param febID : The FEB ID
         *  @param febHybridID : The FEB hybrid ID
         *  @param channel : The physical channel number
         *  @return The SVT channel ID
         *  @throws {@link RuntimeException} if the channel ID can't be found
         */
        public int findChannelID(int febID, int febHybridID, int channel) {
            for (SvtChannel svtChannel : this) {
                if (svtChannel.getFebID() == febID && svtChannel.getFebHybridID() == febHybridID && svtChannel.getChannel() == channel) {
                    return svtChannel.getChannelID();
                }
            }
            // throw new RuntimeException("Channel ID couldn't be found");
            return -1;
        }
    }

    /**
     *  Get the FEB ID associated with this SVT channel ID.
     * 
     *  @return The FEB ID.
     */
    @Field(names = { "feb_id" })
    public int getFebID() {
        return getFieldValue("feb_id");
    }

    /**
     *  Get the FEB hybrid ID associated with this SVT channel ID.
     * 
     *  @return The FEB hybrid ID.
     */
    @Field(names = { "feb_hybrid_id" })
    public int getFebHybridID() {
        return getFieldValue("feb_hybrid_id");
    }

    /**
     *  Set the FEB ID associated with this SVT channel ID.
     * 
     *  @param febID : The FEB ID
     */
    public void setFebID(int febID) { 
        this.setFieldValue("feb_id", febID);
    }
    
    /**
     *  Set the FEB hybrid ID associated with this SVT channel ID.
     * 
     *  @param febHybridID : The FEB hybrid ID
     */
    public void setFebHybridID(int febHybridID) {
        this.setFieldValue("feb_hybrid_id", febHybridID);
    }
    
    /**
     *  Checks if a FEB ID is valid
     * 
     *  @param febID : The Front End Board (FEB) ID
     *  @return True if the FEB ID lies within the range 0-9, false otherwise
     */
    public boolean isValidFeb(int febID) { 
        return (febID >= 0 && febID <= 9) ? true : false; 
    }
    
    /**
     *  Checks if a Front End Board hybrid ID is valid
     *  
     *  @param febHybridID : The hybrid ID 
     *  @return True if the hybrid ID lies within the range 0-3, false otherwise
     */
    public boolean isValidFebHybridID(int febHybridID) { 
        return (febHybridID >= 0 && febHybridID <= 3) ? true : false; 
    }
    
    /**
     *  Checks if a physical channel number is valid
     *  
     *  @param channel : The physical channel number
     *  @return True if the channel number lies within the range 0-639, false 
     *          otherwise
     */
    public boolean isValidPhysicalChannel(int channel) { 
        return (channel >= 0 && channel <= 639) ? true : false; 
    }
    
    /**
     *  Implementation of equals.
     *  
     *  @return True if the object equals this one; false if not.
     */
    public boolean equals(Object o) {
        if (o == null)
            return false;
        if (!(o instanceof SvtChannel))
            return false;
        if (o == this)
            return true;
        SvtChannel channel = (SvtChannel) o;
        return getChannelID() == channel.getChannelID() && getFebID() == channel.getFebID() && getFebHybridID() == channel.getFebHybridID() && getChannel() == channel.getChannel();
    }
}
