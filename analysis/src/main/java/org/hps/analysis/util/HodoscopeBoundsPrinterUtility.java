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

public class HodoscopeBoundsPrinterUtility {
    public static final void main(String args[]) throws ConditionsNotFoundException {
        for(int i = 0; i < args.length; i++) {
            if(args[i].compareTo("-h") == 0 || args[i].compareTo("--help") == 0) {
                System.out.println(BashParameter.format("java -cp $HPS_JAVA org.hps.analysis.hodoscope.HodoscopeBoundsPrinterUtility", BashParameter.TEXT_LIGHT_BLUE));
                System.out.println(getFormattedArgumentText('d', "Detector name             ", true));
                System.out.println(getFormattedArgumentText('R', "Run number                ", true));
                System.out.println(getFormattedArgumentText('h', "Display help text         ", false));
                System.exit(0);
            }
        }
        
        // Parse the command line arguments.
        String detectorName = null;
        int runNumber = Integer.MIN_VALUE;
        for(int i = 0; i < args.length; i++) {
            if(args[i].compareTo("-d") == 0) {
                i++;
                if(args.length > i) { detectorName = args[i]; } else { throwArgumentError(); }
            } else if(args[i].compareTo("-R") == 0) {
                i++;
                if(args.length > i) { runNumber = Integer.parseInt(args[i]); } else { throwArgumentError(); }
            }
        }
        
        // Validate the mandatory options are defined.
        if(detectorName == null || runNumber == Integer.MIN_VALUE) {
            throw new RuntimeException("Mandatory options not set. See help text.");
        }
        
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
        
        List<String> boundsData = new ArrayList<String>(hodoscopeBoundsMap.size());
        boundsData.addAll(hodoscopeBoundsMap.values());
        Collections.sort(boundsData);
        for(String entry : boundsData) {
            System.out.println(entry);
        }
    }
    
    private static final void throwArgumentError() {
        throw new RuntimeException("Error: invalid commandline arguments.");
    }
    
    private static final String getFormattedArgumentText(char argument, String description, boolean mandatory) {
        StringBuffer outputBuffer = new StringBuffer("\t\t");
        outputBuffer.append(BashParameter.format("-" + Character.toString(argument), BashParameter.TEXT_YELLOW, BashParameter.PROPERTY_BOLD));
        outputBuffer.append(' ');
        outputBuffer.append(description);
        if(mandatory) {
            outputBuffer.append(BashParameter.format("[ MANDATORY ]", BashParameter.TEXT_RED, BashParameter.PROPERTY_BOLD));
        } else {
            outputBuffer.append(BashParameter.format("[ OPTIONAL  ]", BashParameter.TEXT_LIGHT_GREY, BashParameter.PROPERTY_DIM));
        }
        return outputBuffer.toString();
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