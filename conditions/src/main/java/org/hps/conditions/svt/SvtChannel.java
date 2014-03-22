package org.hps.conditions.svt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hps.conditions.AbstractConditionsObject;
import org.hps.conditions.ConditionsObjectCollection;
import org.hps.conditions.ConditionsObjectException;
import org.lcsim.hps.util.Pair;

/**
 * This class represents SVT channel setup information, including hybrid, FPGA, and channel numbers.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class SvtChannel extends AbstractConditionsObject {
    
    public static class SvtChannelCollection extends ConditionsObjectCollection<SvtChannel> {
        
        Map<Integer, SvtChannel> channelMap = new HashMap<Integer, SvtChannel>();
            
        public void add(SvtChannel channel) {
            // Add to map.
            if (channelMap.containsKey(channel.getChannelId())) {
                throw new IllegalArgumentException("Channel ID already exists: " + channel.getChannelId());
            }
            channelMap.put(channel.getChannelId(), channel);
            
            // Add to collection.
            try {
                super.add(channel);
            } catch (ConditionsObjectException e) {
                throw new RuntimeException(e);
            }
        }
        
        public SvtChannel findChannel(int channelId) {
            return channelMap.get(channelId);
        }
        
        /**
         * Find channels that match a DAQ pair (FPGA, hybrid).
         * @param pair The DAQ pair.
         * @return The channels matching the DAQ pair or null if not found.
         */
        public Collection<SvtChannel> find(Pair<Integer,Integer> pair) {
            List<SvtChannel> channels = new ArrayList<SvtChannel>(); 
            int fpga = pair.getFirstElement();
            int hybrid = pair.getSecondElement();
            for (SvtChannel channel : this.getObjects()) {
                if (channel.getFpga() == fpga && channel.getHybrid() == hybrid) {
                    channels.add(channel);
                }
            }
            return channels;
        }
                   
        /**
         * Convert this object to a human readable string.
         * @return This object converted to a string.
         */
        public String toString() {        
            StringBuffer buff = new StringBuffer();
            for (SvtChannel channel : this.getObjects()) {
                buff.append(channel.toString() + '\n');
            }
            return buff.toString();
        }
    }
    
    /**
     * Get the channel ID.
     * @return The channel ID.
     */
    public int getChannelId() {
        return getFieldValue("channel_id");
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
        return "channel_id: " + getChannelId() + ", fpga: " + getFpga() + ", hybrid: " + getHybrid() + ", channel: " + getChannel();
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
        return getChannelId() == channel.getChannelId() 
                && getHybrid() == channel.getHybrid() 
                && getFpga() == channel.getFpga() 
                && getHybrid() == channel.getHybrid();
    }    
}