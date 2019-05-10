package org.hps.analysis.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.detector.hodoscope.HodoscopeDetectorElement;
import org.hps.util.BashParameter;
import org.lcsim.conditions.ConditionsManager.ConditionsNotFoundException;
import org.lcsim.detector.identifier.IIdentifier;
import org.lcsim.geometry.Detector;

/**
 * <code>HodoscopeBoundsPrinterUtility</code> is a utility method
 * that prints the positions, bounds, and miscellaneous parameters
 * for a given hodoscope detector to the terminal. It is intended to
 * allow for easy checking of the positioning of hodoscope elements
 * after detector updates to ensure that the programmatic detector
 * matches what is expected<br/><br/>
 * It may be run using the command: <code>java -cp $HPS_JAVA
 * org.hps.analysis.util.HodoscopeBoundsPrinterUtility -R
 * $RUN_NUMBER -d $HODOSCOPE_DETECTOR_NAME</code>
 * 
 * @author Kyle McCarty <mccarty@jlab.org>
 */
public class HodoscopeBoundsPrinterUtility {
    public static final void main(String args[]) throws ConditionsNotFoundException {
        // Define the command line arguments.
        UtilityArgumentParser argsParser = new UtilityArgumentParser("java -cp $HPS_JAVA org.hps.analysis.util.HodoscopeBoundsPrinterUtility");
        argsParser.addSingleValueArgument("-d", "--detector", "Detector name", true);
        argsParser.addSingleValueArgument("-R", "--run", "Run number", true);
        argsParser.addFlagArgument("-h", "--help", "Display usage information", false);
        
        // Parse the command line arguments.
        argsParser.parseArguments(args);
        
        // If the "display usage information" argument is present,
        // then display the usage information and exit. Also do this
        // if a required argument is not included.
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
        
        // Get the detector.
        Detector detector = conditions.getDetectorObject();
        HodoscopeDetectorElement hodoscopeDetectorElement = (HodoscopeDetectorElement) detector.getSubdetector("Hodoscope").getDetectorElement();
        
        // Get the list of all hodoscope scintillator identifiers
        // from the detector.
        List<IIdentifier> scintillators = hodoscopeDetectorElement.getScintillatorIdentifiers();
        
        // Iterate over the scintillators and store their bounds.
        Map<Integer, String> hodoscopeBoundsMap = new HashMap<Integer, String>();
        for(IIdentifier id : scintillators) {
            Integer posvar = Integer.valueOf(hodoscopeDetectorElement.getScintillatorUniqueKey(id));
            hodoscopeBoundsMap.put(posvar, getHodoscopeBounds(id, hodoscopeDetectorElement));
        }
        
        // Sort the bounds data and print it to the terminal.
        System.out.println(lblue("Hodoscope Geometry Bounds for Detector ") + yellow(detectorName)
                + lblue(" and Run Number ") + yellow(Integer.toString(runNumber)) + lblue(":"));
        List<String> boundsData = new ArrayList<String>(hodoscopeBoundsMap.size());
        boundsData.addAll(hodoscopeBoundsMap.values());
        Collections.sort(boundsData);
        for(String entry : boundsData) {
            System.out.println(entry);
        }
    }
    
    /**
     * Creates a concise {@link java.lang.String String} object that
     * contains the geometric details for the scintillator on which
     * the argument hit occurred.
     * @param hit - The hit.
     * @return Returns a <code>String</code> describing the geometric
     * details of the scintillator on which the hit occurred.
     */
    private static String getHodoscopeBounds(IIdentifier id, HodoscopeDetectorElement hodoscopeDetectorElement) {
        int[] indices = hodoscopeDetectorElement.getHodoscopeIndices(id);
        int holes = hodoscopeDetectorElement.getScintillatorChannelCount(id);
        double[] dimensions = hodoscopeDetectorElement.getScintillatorHalfDimensions(id);
        double[] position = hodoscopeDetectorElement.getScintillatorPosition(id);
        double[] holeX = hodoscopeDetectorElement.getScintillatorHolePositions(id);
        
        String scintillatorBoundString = String.format(
                "Scintillator " + yellow("%1d") + ", " + yellow("%-6s") + ", Layer " + yellow("%1d") + "%n"
                + "\tx-Bounds = [ " + yellow("%7.2f") + " mm, " + yellow("%7.2f") + " mm ]%n"
                + "\ty-Bounds = [ " + yellow("%7.2f") + " mm, " + yellow("%7.2f") + " mm ]%n"
                + "\tz-Bounds = [ " + yellow("%7.2f") + " mm, " + yellow("%7.2f") + " mm ]%n"
                + "\tFADC Channels: " + yellow("%d") + "%n",
                (indices[0] + 1), indices[1] == 1 ? "Top" : "Bottom", (indices[2] + 1),
                position[0] - dimensions[0], position[0] + dimensions[0],
                position[1] - dimensions[1], position[1] + dimensions[1],
                position[2] - dimensions[2], position[2] + dimensions[2],
                holes);
        
        String holePositions = null;
        if(holes == 1) {
            holePositions = String.format("\t\tHole Position = " + yellow("%6.2f") + "%n", holeX[0]);
        } else {
            holePositions = String.format(
                    "\t\tHole 1 Position = " + yellow("%6.2f") + " mm%n"
                    + "\t\tHole 2 Position = " + yellow("%6.2f") + " mm%n",
                    holeX[0], holeX[1]);
        }
        
        return scintillatorBoundString + holePositions;
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