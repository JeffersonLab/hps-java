package org.hps.conditions.ecal;

import java.util.HashMap;
import java.util.Map;

/**
 * This class provides access to all ECal conditions from the database,
 * including gain, pedestal and bad channel settings, per crystal. 
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class EcalConditions {
    
    /** Channel map. */
    EcalChannelMap channelMap = new EcalChannelMap();
    
    /** Map between channels and conditions data. */
    Map<EcalChannel,EcalChannelConstants> channelData = new HashMap<EcalChannel,EcalChannelConstants>();
             
    /**
     * Class constructor, which is package protected.
     */
    EcalConditions() {        
    }
    
    /**
     * Set the channel map.
     * @param channels The channel map.
     */
    void setChannelMap(EcalChannelMap channelMap) {
        this.channelMap = channelMap;
    }
        
    /**
     * Get the map between database IDs and <code>EcalChannel</code> objects.
     * @return The channel map.
     */
    public EcalChannelMap getChannelMap() {
        return channelMap;
    }
       
    /**
     * Get the conditions constants for a specific channel.  These will be
     * created if they do not exist for the given channel, BUT only channels
     * in the current channel map are allowed as an argument.
     * @param channel The ECAL channel.
     * @return The conditions constants for the channel.
     * @throws IllegalArgumentException if channel does not exist in the channel map.
     */
    public EcalChannelConstants getChannelConstants(EcalChannel channel) {
        // This channel must come from the map.
        if (!channelMap.contains(channel)) {
            System.err.println("Channel not found in map => " + channel);
            throw new IllegalArgumentException("Channel was not found in map.");
        }
        // If channel has no data yet, then add it.
        if (!channelData.containsKey(channel))
            channelData.put(channel, new EcalChannelConstants());
        return channelData.get(channel);
    }         
            
    /**
     * Convert this object to a string.
     * @return A string representation of this object.
     */
    public String toString() {
        StringBuffer buff = new StringBuffer();
        
        buff.append('\n');
        buff.append("Printing ECAL conditions ...");
        buff.append('\n');
        buff.append('\n');
        
        // Table header:
        buff.append("id");
        buff.append("    ");
        buff.append("crate");
        buff.append("  ");
        buff.append("slot");        
        buff.append("   ");
        buff.append("channel");        
        buff.append("  ");
        buff.append("x");
        buff.append("      ");
        buff.append("y");        
        buff.append("     ");
        buff.append("gain");
        buff.append("       ");
        buff.append("pedestal");
        buff.append("   ");
        buff.append("noise");
        buff.append("      ");
        buff.append("bad");
        buff.append('\n');
        for (int i=0; i<80; i++) {
            buff.append("-");
        }        
        buff.append('\n');
        
        // Loop over all channels.
        for (EcalChannel channel : channelMap.getObjects()) {
            
            EcalChannelConstants constants = getChannelConstants(channel);
            
            double gain = constants.getGain().getGain();
            double pedestal = constants.getCalibration().getPedestal();
            double noise = constants.getCalibration().getNoise();
            boolean bad = constants.isBadChannel();
            
            // Channel data.
            buff.append(String.format("%-5d %-6d %-6d %-8d %-6d %-6d", 
                    channel.getId(), channel.getCrate(), channel.getSlot(), channel.getChannel(), 
                    channel.getX(), channel.getY()));
            
            // Constants.
            buff.append(String.format("%-10.4f %-10.4f %-10.4f ", gain, pedestal, noise));
            
            // Bad channel.
            buff.append(bad);
            
            buff.append('\n');
        }
        return buff.toString();
    }
}