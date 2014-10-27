package org.hps.conditions.svt;

import java.util.HashMap;
import java.util.Map;

import org.hps.conditions.svt.AbstractSvtChannel.AbstractSvtChannelCollection;
import org.hps.conditions.svt.AbstractSvtDaqMapping.AbstractSvtDaqMappingCollection;
import org.hps.conditions.svt.SvtChannel.SvtChannelCollection;
import org.hps.conditions.svt.SvtT0Shift.SvtT0ShiftCollection;

/**
 * Abstract class providing some of the common functionality used to define an 
 * SVT conditions object
 * 
 * @author Omar Moreno <omoreno1@ucsc.edu>
 *
 */
public abstract class AbstractSvtConditions {

    protected Map<AbstractSvtChannel, ChannelConstants> channelData 
        = new HashMap<AbstractSvtChannel, ChannelConstants>(); 
    protected SvtT0ShiftCollection t0Shifts = null;
    protected AbstractSvtChannel.AbstractSvtChannelCollection<? extends AbstractSvtChannel> channelMap = null;

    
    /**
     * Get the DAQ map associated with these conditions.
     * 
     * @return The SVT DAQ map.
     */
    public abstract AbstractSvtDaqMappingCollection<? extends AbstractSvtDaqMapping> getDaqMap(); 
    
    /**
     * Get the conditions constants for a specific channel. These will be created if they
     * do not exist for the given channel, BUT only channels in the current channel map
     * are allowed as an argument.
     * 
     * @param channel The SVT channel of interest
     * @return The conditions constants for the given channel
     * @throws IllegalArgumentException if .
     */
    public ChannelConstants getChannelConstants(AbstractSvtChannel channel) {
        // This channel must come from the map.
        if (!channelMap.getObjects().contains(channel)) {
            System.err.println("[ " + this.getClass().getSimpleName() + " ]: Channel not found in map => " + channel);
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
    public abstract AbstractSvtChannelCollection<? extends AbstractSvtChannel> getChannelMap();
    
    /**
     * Get the {@link SvtT0ShiftCollection}.
     * 
     * @return The t0 shifts by sensor.
     */
    public SvtT0ShiftCollection getT0Shifts() {
        return t0Shifts;
    
    }
   
    /**
     * Set the sensor t0 shifts of type {@link SvtT0ShiftCollection}
     * 
     * @param t0Shifts The sensor time shifts collection.
     */
    public void setT0Shifts(SvtT0ShiftCollection t0Shifts) {
        this.t0Shifts = t0Shifts;
    }
}
