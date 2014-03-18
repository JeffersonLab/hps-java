package org.hps.conditions.svt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hps.conditions.ConditionsObjectCollection;
import org.hps.conditions.ConditionsTableMetaData;
import org.lcsim.hps.util.Pair;

/**
 * This class represents a map between SVT channels and their IDs from the channels table
 * in the conditions database.  It can be used to lookup information stored in the {@link SvtConditions} object.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class SvtChannelCollection extends ConditionsObjectCollection<SvtChannel> {
    
    Map<Integer, SvtChannel> channelMap = new HashMap<Integer, SvtChannel>();
    
    SvtChannelCollection(ConditionsTableMetaData tableMetaData, int collectionId, boolean isReadOnly) {
        super(tableMetaData, collectionId, isReadOnly);
    }
    
    public void add(SvtChannel channel) {
        // Add to map.
        if (channelMap.containsKey(channel.getChannelId())) {
            throw new IllegalArgumentException("Channel ID already exists: " + channel.getChannelId());
        }
        channelMap.put(channel.getChannelId(), channel);
        
        // Add to collection.
        super.add(channel);
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
