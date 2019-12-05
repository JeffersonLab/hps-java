package org.hps.analysis.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.hodoscope.HodoscopeCalibration;
import org.hps.conditions.hodoscope.HodoscopeChannel;
import org.hps.conditions.hodoscope.HodoscopeGain;
import org.hps.conditions.hodoscope.HodoscopeTimeShift;
import org.hps.conditions.hodoscope.HodoscopeCalibration.HodoscopeCalibrationCollection;
import org.hps.conditions.hodoscope.HodoscopeChannel.HodoscopeChannelCollection;
import org.hps.conditions.hodoscope.HodoscopeGain.HodoscopeGainCollection;
import org.hps.conditions.hodoscope.HodoscopeTimeShift.HodoscopeTimeShiftCollection;
import org.hps.util.BashParameter;
import org.lcsim.conditions.ConditionsManager.ConditionsNotFoundException;

/**
 * <code>HodoscopeChannelConditionsUtility</code> is a utility method
 * designed to print out all of the conditions related to the
 * hodoscope subdetector for a given run number and detector name.
 * <br/><br/>
 * It may be run using the command: <code>java -cp $HPS_JAVA
 * org.hps.analysis.util.HodoscopeChannelConditionsUtility -R
 * $RUN_NUMBER -d $HODOSCOPE_DETECTOR_NAME</code>
 * 
 * @author Kyle McCarty <mccarty@jlab.org>
 */
public class HodoscopeChannelConditionsUtility {
    /**
     * Maps hodoscope channels to the gain for that channel.
     */
    private static Map<Long, HodoscopeGain> channelToGainsMap = new HashMap<Long, HodoscopeGain>();
    
    /**
     * Maps hodoscope channels to the time shifts for that channel.
     */
    private static Map<Long, HodoscopeTimeShift> channelToTimeShiftsMap = new HashMap<Long, HodoscopeTimeShift>();
    
    /**
     * Maps hodoscope channels to the noise sigma and pedestals for that channel.
     */
    private static Map<Long, HodoscopeCalibration> channelToCalibrationsMap = new HashMap<Long, HodoscopeCalibration>();
    
    /**
     * Prints the conditions for all channels for the specified run
     * number and detector for the hodoscope subdetector.
     * @param args - The command line arguments.
     * @throws ConditionsNotFoundException Occurs if no conditions
     * exist for the specified run number and detector.
     */
    public static final void main(String args[]) throws ConditionsNotFoundException {
        // Define the command line arguments.
        UtilityArgumentParser argsParser = new UtilityArgumentParser("java -cp $HPS_JAVA org.hps.analysis.util.HodoscopeChannelConditionsUtility");
        argsParser.addSingleValueArgument("-d", "--detector", "Detector name", true);
        argsParser.addSingleValueArgument("-R", "--run", "Run number", true);
        argsParser.addFlagArgument("-h", "--help", "Display usage information", false);
        
        // Parse the command line arguments.
        argsParser.parseArguments(args);
        
        // If the "display usage information" argument is present,
        // then display the usage information and exit.
        if(argsParser.isDefined("-h") || !argsParser.verifyRequirements()) {
            System.out.println(argsParser.getHelpText());
            System.exit(0);
        }
        
        // Get the required arguments.
        String detectorName = ((SingleValueArgument) argsParser.getArgument("-d")).getValue();
        int runNumber = Integer.parseInt(((SingleValueArgument) argsParser.getArgument("-R")).getValue());
        
        // Load the conditions database.
        DatabaseConditionsManager conditions = DatabaseConditionsManager.getInstance();
        conditions.setRun(runNumber);
        conditions.setDetector(detectorName, runNumber);
        mapConditions();
        
        // Iterate over the channels and output the conditions.
        final HodoscopeChannelCollection channels = conditions.getCachedConditions(HodoscopeChannelCollection.class, "hodo_channels").getCachedData();
        List<String> conditionsData = new ArrayList<String>(channels.size());
        for(HodoscopeChannel channel : channels) {
            // Get a string that describes the channel.
            String channelString = toString(channel);
            
            // Get the conditions.
            double gain = channelToGainsMap.get(Long.valueOf(channel.getChannelId().intValue())).getGain().doubleValue();
            double timeShift = channelToTimeShiftsMap.get(Long.valueOf(channel.getChannelId().intValue())).getTimeShift().doubleValue();
            double pedestal = channelToCalibrationsMap.get(Long.valueOf(channel.getChannelId().intValue())).getPedestal().doubleValue();
            double noise = channelToCalibrationsMap.get(Long.valueOf(channel.getChannelId().intValue())).getNoise().doubleValue();
            
            // Create an output string for each conditions.
            String gainString      = String.format("\tGain       :: " + yellow("%f"), gain);
            String noiseString     = String.format("\tNoise      :: " + yellow("%f"), noise);
            String pedestalString  = String.format("\tPedestal   :: " + yellow("%f"), pedestal);
            String timeShiftString = String.format("\tTime Shift :: " + yellow("%f"), timeShift);
            
            // Store the combined channel string.
            String combinedString = String.format("%s%n%s%n%s%n%s%n%s%n", channelString, gainString, noiseString, pedestalString, timeShiftString);
            conditionsData.add(combinedString);
        }
        
        // Sort and print the channel data.
        System.out.println(lblue("Hodoscope Conditions for Detector ") + yellow(detectorName)
                + lblue(" and Run Number ") + yellow(Integer.toString(runNumber)) + lblue(":"));
        Collections.sort(conditionsData);
        for(String conditionsDatum : conditionsData) {
            System.out.println(conditionsDatum);
        }
    }
    
    private final static void mapConditions() {
        // Load the conditions database and get the hodoscope channel
        // collection data.
        final DatabaseConditionsManager conditions = DatabaseConditionsManager.getInstance();
        final HodoscopeGainCollection gains = conditions.getCachedConditions(HodoscopeGainCollection.class, "hodo_gains").getCachedData();
        final HodoscopeTimeShiftCollection timeShifts = conditions.getCachedConditions(HodoscopeTimeShiftCollection.class, "hodo_time_shifts").getCachedData();
        final HodoscopeCalibrationCollection calibrations = conditions.getCachedConditions(HodoscopeCalibrationCollection.class, "hodo_calibrations").getCachedData();
        
        // Map the gains to channel IDs.
        for(HodoscopeGain gain : gains) {
            channelToGainsMap.put(Long.valueOf(gain.getChannelId().intValue()), gain);
        }
        
        // Map the pedestals and noise to channel IDs.
        for(HodoscopeCalibration calibration : calibrations) {
            channelToCalibrationsMap.put(Long.valueOf(calibration.getChannelId().intValue()), calibration);
        }
        
        // Map time shifts to channel IDs.
        for(HodoscopeTimeShift timeShift :  timeShifts) {
            channelToTimeShiftsMap.put(Long.valueOf(timeShift.getChannelId().intValue()), timeShift);
        }
    }
    
    /**
     * Creates a human-readable description of a hodoscope channel.
     * @param channel - The channel.
     * @return Returns the description of the channel as a {@link
     * java.util.String String} object.
     */
    private static final String toString(HodoscopeChannel channel) {
        int id = channel.getChannelId();
        int ix = channel.getIX();
        boolean isTop = channel.isTop();
        int layer = channel.isLayer1() ? 1 : 2;
        String hole = null;
        if(channel.getHole() == HodoscopeChannel.HOLE_LOW_X) { hole = "1"; }
        else if(channel.getHole() == HodoscopeChannel.HOLE_HIGH_X) { hole = "2"; }
        else if(channel.getHole() == HodoscopeChannel.HOLE_NULL) { hole = "N/A"; }
        
        return String.format("Channel " + yellow("%2d") + ": Scintillator " + yellow("%1d") + ", " + yellow("%-6s") + ", Layer "
                + yellow("%1d") + ", Hole " + yellow("%-3s"), id, ix, isTop ? "Top" : "Bottom", layer, hole);
    }
    
    /**
     * Applies BASH formatting to the argument text such that it will
     * display using light blue text.
     * @param text - The text to format.
     * @return Returns the argument text with BASH formatting.
     */
    private static final String lblue(String text) { return BashParameter.format(text, BashParameter.TEXT_LIGHT_BLUE); }
    
    /**
     * Applies BASH formatting to the argument text such that it will
     * display using yellow text.
     * @param text - The text to format.
     * @return Returns the argument text with BASH formatting.
     */
    private static final String yellow(String text) { return BashParameter.format(text, BashParameter.TEXT_YELLOW); }
}