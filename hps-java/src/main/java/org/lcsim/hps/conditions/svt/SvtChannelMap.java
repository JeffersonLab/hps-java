package org.lcsim.hps.conditions.svt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import org.lcsim.hps.util.Pair;

/**
 * This class represents a map between SVT channels and their IDs from the channels table
 * in the conditions database.  It can be used to lookup information stored in the {@link SvtConditions} object.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class SvtChannelMap extends HashMap<Integer,SvtChannel> {
    
    /**
     * Constructor, which is package protected.  Users should not 
     * create this class directly but retrieve it from the {@link SvtConditions}
     * object instead.
     */
    SvtChannelMap() {
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
        for (SvtChannel channel : values()) {
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
        for (SvtChannel channel : values()) {
            buff.append(channel.toString() + '\n');
        }
        return buff.toString();
    }
}
