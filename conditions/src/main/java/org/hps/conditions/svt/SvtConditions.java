package org.hps.conditions.svt;

import java.util.HashMap;
import java.util.Map;

import org.hps.conditions.svt.SvtChannel.SvtChannelCollection;
import org.hps.conditions.svt.SvtDaqMapping.SvtDaqMappingCollection;
import org.hps.conditions.svt.SvtT0Shift.SvtT0ShiftCollection;

import static org.hps.conditions.svt.SvtChannel.MAX_NUMBER_OF_SAMPLES;

/**
 * This class contains all SVT conditions data by readout channel. {@link SvtChannel}
 * objects from the {@linkSvtChannelMap} should be used to lookup the conditions using the
 * {@link #getChannelConstants(SvtChannel)} method.
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 * @author Omar Moreno <omoreno1@ucsc.edu>
 */
public final class SvtConditions {

    /** SVT conditions data. */
    private Map<SvtChannel, ChannelConstants> channelData = new HashMap<SvtChannel, ChannelConstants>();
    private SvtChannelCollection channelMap = null;
    private SvtDaqMappingCollection daqMap = null;
    private SvtT0ShiftCollection t0Shifts = null;

    /**
     * Class constructor, which takes a channel map.
     * 
     * @param channelMap The SVT channel map.
     */
    SvtConditions(SvtChannelCollection channelMap) {
        this.channelMap = channelMap;
    }

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
     * Get the {@link SvtDaqMappingCollection} associated with these conditions.
     * 
     * @return The SVT DAQ map.
     */
    public SvtDaqMappingCollection getDaqMap() {
        return daqMap;
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
     * Set the {@link SvtDaqMappingCollection} associated with these conditions.
     * 
     * @param daqMap The SVT DAQ map.
     */
    void setDaqMap(SvtDaqMappingCollection daqMap) {
        this.daqMap = daqMap;
    }

    /**
     * Set the sensor t0 shifts.
     * 
     * @param t0Shifts The sensor time shifts collection.
     */
    void setTimeShifts(SvtT0ShiftCollection t0Shifts) {
        this.t0Shifts = t0Shifts;
    }

    /**
     * Convert this object to a human readable string. This method prints a formatted
     * table of channel data independently of how its member objects implement their
     * string conversion method. For now, it does not print the time shifts by sensor as
     * all other information is by channel.
     * 
     * @return This object converted to a string, without the DAQ map.
     * TODO: Make this look more human readable.  At the moment, reading this
     * 		 requires a huge terminal window.
     */
    public String toString() {
        StringBuffer buff = new StringBuffer();

        buff.append('\n');
        buff.append("Printing SVTConditions ...");
        buff.append('\n');
        buff.append('\n');

        // Table header:
        buff.append("Channel ID");
        buff.append("     ");
        buff.append("FEB ID");
        buff.append("  ");
        buff.append("FEB Hybrid ID");
        buff.append("   ");
        buff.append("Channel");
        buff.append("  ");
        buff.append("Pedestal sample 0");
        buff.append("     ");
        buff.append("Pedestal sample 1");
        buff.append("     ");
        buff.append("Pedestal sample 2");
        buff.append("     ");
        buff.append("Pedestal sample 3");
        buff.append("     ");
        buff.append("Pedestal sample 4");
        buff.append("     ");
        buff.append("Pedestal sample 5");
        buff.append("     ");
        buff.append("Noise sample 0");
        buff.append("     ");
        buff.append("Noise sample 1");
        buff.append("     ");
        buff.append("Noise sample 2");
        buff.append("     ");
        buff.append("Noise sample 3");
        buff.append("     ");
        buff.append("Noise sample 4");
        buff.append("     ");
        buff.append("Noise sample 5");
        buff.append("     ");
        buff.append("Gain");
        buff.append("   ");
        buff.append("Offset");
        buff.append("    ");
        buff.append("Amplitude");
        buff.append("  ");
        buff.append("t0");
        buff.append("       ");
        buff.append("tp");
        buff.append("    ");
        buff.append("Bad Channels");
        buff.append('\n');
        for (int i = 0; i < 115; i++) {
            buff.append("-");
        }
        buff.append('\n');
        // Loop over channels.
        for (SvtChannel channel : channelMap.getObjects()) {

            // Get the conditions for the channel.
            ChannelConstants constants = getChannelConstants(channel);
            SvtGain gain = constants.getGain();
            SvtShapeFitParameters shapeFit = constants.getShapeFitParameters();
            SvtCalibration calibration = constants.getCalibration();

            // Channel data.
            buff.append(String.format("%-6d %-5d %-8d %-8d ", channel.getChannelID(), channel.getFebID(), channel.getFebHybridID(), channel.getChannel()));

            // Calibration.
            for(int sample = 0; sample < MAX_NUMBER_OF_SAMPLES; sample++){
            	buff.append(calibration.getPedestal(sample));
            	buff.append("      ");
            }
            for(int sample = 0; sample < MAX_NUMBER_OF_SAMPLES; sample++){
            	buff.append(calibration.getNoise(sample));
            	buff.append("      ");
            }

            // Gain.
            buff.append(String.format("%-6.4f %-9.4f ", gain.getGain(), gain.getOffset()));

            // Pulse shape.
            buff.append(String.format("%-10.4f %-8.4f %-8.4f", shapeFit.getAmplitude(), shapeFit.getT0(), shapeFit.getTp()));

            // Bad channel.
            buff.append(constants.isBadChannel());

            buff.append('\n');
        }

        buff.append('\n');
        buff.append("Printing SVT DAQ Map...");
        buff.append('\n');
        buff.append('\n');
        buff.append(getDaqMap());

        return buff.toString();
    }
}
