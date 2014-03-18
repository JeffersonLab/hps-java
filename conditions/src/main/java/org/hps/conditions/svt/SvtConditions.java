package org.hps.conditions.svt;

import java.util.HashMap;
import java.util.Map;

/**
 * This class contains all SVT conditions data by readout channel.
 * {@link SvtChannel} objects from the {@linkSvtChannelMap} should be 
 * used to lookup the conditions using the {@link #getChannelConstants(SvtChannel)} method.
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 * @version $Id: SvtConditions.java,v 1.11 2013/10/15 23:45:56 jeremy Exp $
 */
public class SvtConditions {
    
    /** SVT conditions data. */
    private Map<SvtChannel, ChannelConstants> channelData = new HashMap<SvtChannel, ChannelConstants>();
    private SvtChannelCollection channelMap = null;
    private SvtDaqMap daqMap = null;
    private SvtTimeShiftCollection timeShifts = null;
    
    /**
     * Class constructor, which takes a channel map.
     * @param channelMap The SVT channel map.
     */
    SvtConditions(SvtChannelCollection channelMap) {
        this.channelMap = channelMap;
    }     
        
    /**
     * Get the conditions constants for a specific channel.  These will be
     * created if they do not exist for the given channel, BUT only channels
     * in the current channel map are allowed as an argument.
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
     * @return The SVT channel map.
     */
    public SvtChannelCollection getChannelMap() {
        return channelMap;
    }
    
    /**
     * Get the {@link SvtDaqMap} associated with these conditions.
     * @return The SVT DAQ map.
     */
    public SvtDaqMap getDaqMap() {
        return daqMap;
    }
    
    /**
     * Get the {@link SvtTimeShiftCollection}.
     * @return The time shifts by sensor.
     */
    public SvtTimeShiftCollection getTimeShifts() {
        return timeShifts;
    }
    
    /**
     * Set the {@link SvtDaqMap} associated with these conditions.
     * @param daqMap The SVT DAQ map.
     */
    void setDaqMap(SvtDaqMap daqMap) {
        this.daqMap = daqMap;
    }
    
    /**
     * Set the sensor time shifts.
     * @param timeShifts The sensor time shifts collection.
     */
    void setTimeShifts(SvtTimeShiftCollection timeShifts) {
        this.timeShifts = timeShifts;
    }
            
    /**
     * Convert this object to a human readable string.  This method prints a formatted table 
     * of channel data independently of how its member objects implement their string conversion method. 
     * For now, it does not print the time shifts by sensor as all other information is by channel.
     * @return This object converted to a string, without the DAQ map.
     */
    public String toString() {
        StringBuffer buff = new StringBuffer();
        
        buff.append('\n');
        buff.append("Printing SVTConditions ...");
        buff.append('\n');
        buff.append('\n');
        
        // Table header:
        buff.append("id");
        buff.append("     ");
        buff.append("fpga");
        buff.append("  ");
        buff.append("hybrid");
        buff.append("   ");
        buff.append("channel");
        buff.append("  ");
        buff.append("noise");
        buff.append("     ");
        buff.append("pedestal");
        buff.append("    ");
        buff.append("gain");
        buff.append("   ");
        buff.append("offset");
        buff.append("    ");
        buff.append("amplitude");
        buff.append("  ");
        buff.append("t0");
        buff.append("       ");
        buff.append("shift");
        buff.append("    ");
        buff.append("chisq");
        buff.append("      ");
        buff.append("bad");
        buff.append('\n'); 
        for (int i=0; i<115; i++) {
            buff.append("-");
        }        
        buff.append('\n');
        // Loop over channels.
        for (SvtChannel channel : channelMap.getObjects()) {
            
            // Get the conditions for the channel.
            ChannelConstants constants = getChannelConstants(channel);
            SvtGain gain = constants.getGain();
            SvtPulseParameters pulse = constants.getPulseParameters();
            SvtCalibration calibration = constants.getCalibration();
            
            // Channel data.
            buff.append(String.format("%-6d %-5d %-8d %-8d ", channel.getChannelId(), channel.getFpga(), channel.getHybrid(), channel.getChannel()));

            // Calibration.
            buff.append(String.format("%-9.4f %-11.4f ", calibration.getNoise(), calibration.getPedestal()));
            
            // Gain.
            buff.append(String.format("%-6.4f %-9.4f ", gain.getGain(), gain.getOffset()));
            
            // Pulse shape.
            buff.append(String.format("%-10.4f %-8.4f %-8.4f %-10.4f ", pulse.getAmplitude(), pulse.getT0(), pulse.getTimeShift(), pulse.getChisq()));
            
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

