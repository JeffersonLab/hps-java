package org.hps.conditions.svt;

import java.util.HashMap;
import java.util.Map;

import org.hps.conditions.svt.AbstractSvtChannel.AbstractSvtChannelCollection;
import org.hps.conditions.svt.AbstractSvtDaqMapping.AbstractSvtDaqMappingCollection;
import org.hps.conditions.svt.AbstractSvtT0Shift.AbstractSvtT0ShiftCollection;

/**
 * Abstract class providing some of the common functionality used to define an SVT conditions object.
 *
 * @author <a href="mailto:omoreno1@ucsc.edu">Omar Moreno</a>
 */
public abstract class AbstractSvtConditions {

    /**
     * A map between the channel object and its conditions constants.
     */
    private Map<AbstractSvtChannel, ChannelConstants> channelData = new HashMap<AbstractSvtChannel, ChannelConstants>();

    /**
     * The time shift collection.
     */
    // FIXME: Should be private with accessor methods.
    protected AbstractSvtT0Shift.AbstractSvtT0ShiftCollection<? extends AbstractSvtT0Shift> t0Shifts = null;

    /**
     * The channel map.
     */
    // FIXME: Should be private with accessor methods.
    protected AbstractSvtChannel.AbstractSvtChannelCollection<? extends AbstractSvtChannel> channelMap = null;

    /**
     * The SVT DAQ mapping.
     */
    // FIXME: Should be private with accessor methods.
    protected AbstractSvtDaqMapping.AbstractSvtDaqMappingCollection<? extends AbstractSvtDaqMapping> daqMap = null;

    /**
     * Get the DAQ map associated with these conditions.
     *
     * @return The SVT DAQ map.
     */
    public abstract AbstractSvtDaqMappingCollection<? extends AbstractSvtDaqMapping> getDaqMap();

    /**
     * Get the conditions constants for a specific channel. These will be created if they do not exist for the given
     * channel, BUT only channels in the current channel map are allowed as an argument.
     *
     * @param channel the SVT channel of interest
     * @return the conditions constants for the given channel
     * @throws IllegalArgumentException if channel was not found in map
     */
    public final ChannelConstants getChannelConstants(final AbstractSvtChannel channel) {
        // This channel must come from the map.
        if (!channelMap.contains(channel)) {
            System.err.println("[ " + this.getClass().getSimpleName() + " ]: Channel not found in map => " + channel);
            throw new IllegalArgumentException("Channel was not found in map.");
        }
        // If channel has no data yet, then add it.
        if (!channelData.containsKey(channel)) {
            channelData.put(channel, new ChannelConstants());
        }
        return channelData.get(channel);
    }

    /**
     * Get the channel map for this set of conditions.
     *
     * @return the SVT channel map
     */
    public abstract AbstractSvtChannelCollection<? extends AbstractSvtChannel> getChannelMap();

    /**
     * Get the t0 shifts for this conditions set.
     *
     * @return the t0 shifts by sensor
     */
    public abstract AbstractSvtT0ShiftCollection<? extends AbstractSvtT0Shift> getT0Shifts();

    /**
     * Set the DAQ map for this conditions set.
     *
     * @param daqMap DAQ map for this conditions set
     */
    public final void setDaqMap(final AbstractSvtDaqMappingCollection<? extends AbstractSvtDaqMapping> daqMap) {
        this.daqMap = daqMap;
    }

    /**
     * Set the SVT channel map for this conditions set.
     *
     * @param channelMap the SVT channel map for this conditions set
     */
    public final void setChannelMap(final AbstractSvtChannelCollection<? extends AbstractSvtChannel> channelMap) {
        this.channelMap = channelMap;
    }

    /**
     * Set the sensor t0 shifts for this conditions set.
     *
     * @param t0Shifts for this conditions set
     */
    public final void setT0Shifts(final AbstractSvtT0ShiftCollection<? extends AbstractSvtT0Shift> t0Shifts) {
        this.t0Shifts = t0Shifts;
    }
}
