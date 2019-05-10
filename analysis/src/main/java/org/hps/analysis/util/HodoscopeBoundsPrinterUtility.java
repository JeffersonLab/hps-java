package org.hps.analysis.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.detector.hodoscope.HodoscopeDetectorElement;
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
 * org.hps.analysis.hodoscope.HodoscopeBoundsPrinterUtility -R
 * $RUN_NUMBER -d $HODOSCOPE_DETECTOR_NAME</code>
 * 
 * @author Kyle McCarty <mccarty@jlab.org>
 */
public class HodoscopeBoundsPrinterUtility {
    public static final void main(String args[]) throws ConditionsNotFoundException {
        // Define the command line arguments.
        UtilityArgumentParser argsParser = new UtilityArgumentParser("java -cp $HPS_JAVA org.hps.analysis.hodoscope.HodoscopeBoundsPrinterUtility");
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
        double[] dimensions = hodoscopeDetectorElement.getScintillatorHalfDimensions(id);
        double[] position = hodoscopeDetectorElement.getScintillatorPosition(id);
        double[] holeX = hodoscopeDetectorElement.getScintillatorHolePositions(id);
        return String.format("Bounds for scintillator <%d, %2d, %d> :: x = [ %6.2f, %6.2f ], y = [ %6.2f, %6.2f ], z = [ %7.2f, %7.2f ];   FADC Channels :: %d;   Hole Positions:  x1 = %6.2f, x2 = %s",
                indices[0], indices[1], indices[2], position[0] - dimensions[0], position[0] + dimensions[0],
                position[1] - dimensions[1], position[1] + dimensions[1], position[2] - dimensions[2], position[2] + dimensions[2],
                hodoscopeDetectorElement.getScintillatorChannelCount(id), holeX[0], holeX.length > 1 ? String.format("%6.2f", holeX[1]) : "N/A");
    }
}