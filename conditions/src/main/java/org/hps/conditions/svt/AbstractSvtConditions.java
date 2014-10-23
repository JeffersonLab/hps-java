package org.hps.conditions.svt;

import java.util.HashMap;
import java.util.Map;

import org.hps.conditions.svt.SvtChannel.SvtChannelCollection;
import org.hps.conditions.svt.SvtT0Shift.SvtT0ShiftCollection;

/**
 * Abstract class providing some of the common functionality used to define an 
 * SVT conditions object
 * 
 * @author Omar Moreno <omoreno1@ucsc.edu>
 *
 * @param <T>  SVT conditions object type
 */
public class AbstractSvtConditions {

    protected Map<SvtChannel, ChannelConstants> channelData = new HashMap<SvtChannel, ChannelConstants>();
    protected SvtChannelCollection channelMap = null;
    protected SvtT0ShiftCollection t0Shifts = null;

    public AbstractSvtConditions() {}
    
    /**
     * Get the conditions constants for a specific channel. These will be created if they
     * do not exist for the given channel, BUT only channels in the current channel map
     * are allowed as an argument.
     * 
     * @param channel The SVT channel.
     * @return The conditions constants for the channel.
     * @throws IllegalArgumentException if .
     */
    public ChannelConstants getChannelConstants(SvtChannel channel) {
        // This channel must come from the map.
        if (!channelMap.getObjects().contains(channel)) {
            System.err.println("Channel not found in map => " + channel);
            throw new IllegalArgumentException("Channel was not found in map.");
        }
        // If channel has no data yet, then add it.
        if (!channelData.containsKey(channel))
            channelData.put(channel, new ChannelConstants());
        return channelData.get(channel);
    }

    /**
     * Get the {@link SvtChannelCollection} for this set of conditions.
     * 
     * @return The SVT channel map.
     */
    public SvtChannelCollection getChannelMap() {
        return channelMap;
    }
    
    /**
     * Get the {@link SvtT0ShiftCollection}.
     * 
     * @return The t0 shifts by sensor.
     */
    public SvtT0ShiftCollection getT0Shifts() {
        return t0Shifts;
    }

    /**
     * Set the channel map of type {@link SvtChannelCollection}.
     * 
     *  @param channelMap The SVT channel map.
     */
    public void setChannelMap(SvtChannelCollection channelMap){
    	this.channelMap = channelMap;
    }

    /**
     * Set the sensor t0 shifts.
     * 
     * @param t0Shifts The sensor time shifts collection.
     */
    public void setTimeShifts(SvtT0ShiftCollection t0Shifts) {
        this.t0Shifts = t0Shifts;
    }
}
