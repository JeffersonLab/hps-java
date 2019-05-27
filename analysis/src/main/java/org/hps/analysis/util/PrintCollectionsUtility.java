package org.hps.analysis.util;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.util.BashParameter;
import org.hps.util.Pair;
import org.lcsim.conditions.ConditionsManager.ConditionsNotFoundException;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.EventHeader;
import org.lcsim.event.EventHeader.LCMetaData;
import org.lcsim.event.GenericObject;
import org.lcsim.event.MCParticle;
import org.lcsim.event.SimCalorimeterHit;
import org.lcsim.lcio.LCIOReader;

/**
 * <code>PrintCollectionsUtility</code> is a utility designed to
 * print all of the collections in an argument LCIO file from the
 * command line. Users can specify how many events to search for new
 * unique collection names. The utility will print both the names of
 * all collections observed and the object types.
 * <br/><br/>
 * Additionally, users may specify that the utility should print the
 * full collection data for specific collections. This will only
 * occur if there is a print method for that object type implemented.
 * 
 * @author Kyle McCarty <mccarty@jlab.org>
 */
public class PrintCollectionsUtility {
    /**
     * Iterates over the events in a file and collates all the
     * collection names which occur in any event. Method can also
     * print the data in the collections if a <code>toString(T,
     * int)</code> method (where <code>T</code> is the type of the
     * object) is implemented.
     * @param args - The command line arguments.
     * @throws IOException - Should never occur. This is used
     * internally to identify the end of the LCIO input file.
     * @throws NumberFormatException - Occurs if a command line
     * argument that is intended to be numerical is not.
     * @throws ConditionsNotFoundException Occurs if the specified
     * conditions set is not found.
     */
    public static void main(String[] args) throws IOException, NumberFormatException, ConditionsNotFoundException {
        // Define the command line arguments.
        UtilityArgumentParser argsParser = new UtilityArgumentParser("java -cp $HPS_JAVA org.hps.analysis.util.PrintCollectionsUtility");
        argsParser.addSingleValueArgument("-i", "--input", "Input file name", true);
        argsParser.addSingleValueArgument("-d", "--detector", "Detector name", true);
        argsParser.addSingleValueArgument("-R", "--run", "Run number", true);
        argsParser.addSingleValueArgument("-n", "--number", "Maximum events to process", false);
        argsParser.addMultipleValueArgument("-p", "--print", "Print collection data", false);
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
        
        // Get the maximum number of events to run.
        int maxEvents = Integer.MAX_VALUE;
        if(argsParser.isDefined("-n")) {
            maxEvents = Integer.parseInt(((SingleValueArgument) argsParser.getArgument("-n")).getValue());
        }
        
        // Get the collections which should be printed.
        List<String> printCollections = new ArrayList<String>();
        if(argsParser.isDefined("-p")) {
            printCollections = ((MultipleValueArgument) argsParser.getArgument("-p")).getValues();
        }
        
        // Validate the input file.
        String fileName = ((SingleValueArgument) argsParser.getArgument("-i")).getValue();
        File inputFile = new File(fileName);
        if(!inputFile.exists()) { throw new RuntimeException("Error: Input file \"" + args[0] + "\" does not exist."); }
        
        // Initialize the file reader.
        LCIOReader reader = new LCIOReader(inputFile);
        
        // Get all collections names.
        int processedEvents = 0;
        Set<Pair<String, String>> collectionSet = new HashSet<Pair<String, String>>();
        Map<String, Class<?>> collectionMap = new HashMap<String, Class<?>>();
        eventLoop:
        while(true) {
            EventHeader event = null;
            try { event = reader.read(); }
            catch(EOFException e) { break eventLoop; }
            
            @SuppressWarnings("rawtypes")
            Set<List> eventCollections = event.getLists();
            for(List<?> eventCollection : eventCollections) {
                LCMetaData collectionMetaData = event.getMetaData(eventCollection);
                collectionSet.add(new Pair<String, String>(collectionMetaData.getName(), collectionMetaData.getType().getSimpleName()));
                collectionMap.put(collectionMetaData.getName(), collectionMetaData.getType());
            }
            
            // If no collections were specified for printing, then
            // continue to the next iteration.
            if(printCollections.isEmpty()) {
                processedEvents++;
                if(processedEvents >= maxEvents) { break eventLoop; }
                continue eventLoop;
            }
            
            // Otherwise, print the data.
            System.out.println(BashParameter.format("Requested Print Collections for Event ", BashParameter.TEXT_LIGHT_BLUE)
                    + BashParameter.format(Integer.toString(event.getEventNumber()), BashParameter.TEXT_GREEN, BashParameter.PROPERTY_BOLD));
            collectionPrintLoop:
            for(int i = 0; i < printCollections.size(); i++) {
                // Get the class type of the collection.
                Class<?> collectionType = collectionMap.get(printCollections.get(i));
                
                // Do nothing unless the event contains the collection.
                if(!event.hasCollection(collectionType, printCollections.get(i))) { continue; }
                
                // Indicate the current collection.
                System.out.println("\tPrinting data for collection \""
                        + BashParameter.format(printCollections.get(i), BashParameter.TEXT_YELLOW, BashParameter.PROPERTY_BOLD) + "\"...");
                
                // Check to see if the collection exists in this event.
                if(!collectionMap.containsKey(printCollections.get(i))) {
                    System.out.println("\t\tCollection does not exist.");
                    continue collectionPrintLoop;
                }
                
                // Get the collection data.
                List<?> collectionData = event.get(collectionType, printCollections.get(i));
                
                // Print the data.
                Method toStringMethod = null;
                try {
                    toStringMethod = PrintCollectionsUtility.class.getDeclaredMethod("toString", collectionType, int.class);
                } catch(NoSuchMethodException e) {
                    System.out.println("\t\tNo print method exists for object type \"" + collectionType.getSimpleName() + "\".");
                    continue collectionPrintLoop;
                }
                for(int j = 0; j < collectionData.size(); j++) {
                    try {
                        System.out.print(((String) toStringMethod.invoke(null, collectionData.get(j), 2)));
                    } catch(IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                        System.out.println("\t\tError calling print method for object type \"" + collectionType.getSimpleName() + "\".");
                        continue collectionPrintLoop;
                    }
                    System.out.println();
                }
            }
            
            // Increment the number of processed events.
            processedEvents++;
            if(processedEvents >= maxEvents) { break eventLoop; }
        }
        
        // Move the collection names into a sorted list.
        List<Pair<String, String>> collectionNames = new ArrayList<Pair<String, String>>(collectionSet.size());
        collectionNames.addAll(collectionSet);
        collectionNames.sort(new Comparator<Pair<String, String>>() {
            @Override
            public int compare(Pair<String, String> arg0, Pair<String, String> arg1) {
                return arg0.getFirstElement().compareTo(arg1.getFirstElement());
            }
        });
        
        // Get the length of the longest collection name.
        int longestNameLength = 0;
        for(Pair<String, String> collectionName : collectionNames) {
            longestNameLength = Math.max(longestNameLength, collectionName.getFirstElement().length());
        }
        
        // Print the collections.
        System.out.println(BashParameter.format("All File Collections:", BashParameter.TEXT_LIGHT_BLUE));
        for(Pair<String, String> collectionName : collectionNames) {
            System.out.printf("\tName = " + BashParameter.format("%-" + longestNameLength + "s", BashParameter.TEXT_YELLOW)
                    + BashParameter.format(" | ", BashParameter.TEXT_LIGHT_GREY, BashParameter.PROPERTY_DIM)
                    + "Type = " + BashParameter.format("%s", BashParameter.TEXT_YELLOW) + "%n",
                    collectionName.getFirstElement(), collectionName.getSecondElement());
        }
    }
    
    /**
     * Creates an {@link java.util.String String} consisting of the
     * specified number of indents.
     * @param level - The number of indents.
     * @return Returns a <code>String</code> with the specified
     * number of indents.
     */
    private static final String getIndent(int level) {
        StringBuffer outputBuffer = new StringBuffer();
        for(int i = 0; i < level; i++) {
            outputBuffer.append('\t');
        }
        return outputBuffer.toString();
    }
    
    /**
     * Creates s textual representation of the data stored in a
     * {@link org.lcsim.event.CalorimeterHit CalorimeterHit}
     * object.
     * @param truthHit - The object.
     * @param indentLevel - The level of indention to prepend.
     * @return Returns the textual representation.
     */
    @SuppressWarnings("unused")
    private static final String toString(CalorimeterHit truthHit, int indentLevel) {
        // Create a buffer to store the data and generate the indent
        // level string.
        StringBuffer outputBuffer = new StringBuffer();
        String indent = getIndent(indentLevel);
        
        outputBuffer.append(String.format(indent + "Time      :: " + BashParameter.format("%f", BashParameter.TEXT_YELLOW) + " GeV%n", truthHit.getTime()));
        outputBuffer.append(String.format(indent + "Energy    :: " + BashParameter.format("%f", BashParameter.TEXT_YELLOW) + " GeV%n", truthHit.getCorrectedEnergy()));
        outputBuffer.append(String.format(indent + "Position  :: <" + BashParameter.format("%f, %f", BashParameter.TEXT_YELLOW) + ">%n", truthHit.getPosition()[0], truthHit.getPosition()[1]));
        outputBuffer.append(String.format(indent + "Indices   :: <" + BashParameter.format("%d, %d", BashParameter.TEXT_YELLOW) + ">%n",
                truthHit.getIdentifierFieldValue("ix"), truthHit.getIdentifierFieldValue("iy")));
        
        // Return the result.
        return outputBuffer.toString();
    }
    
    /**
     * Creates s textual representation of the data stored in a
     * {@link org.lcsim.event.SimCalorimeterHit SimCalorimeterHit}
     * object.
     * @param truthHit - The object.
     * @param indentLevel - The level of indention to prepend.
     * @return Returns the textual representation.
     */
    @SuppressWarnings("unused")
    private static final String toString(SimCalorimeterHit truthHit, int indentLevel) {
        // Create a buffer to store the data and generate the indent
        // level string.
        StringBuffer outputBuffer = new StringBuffer();
        String indent = getIndent(indentLevel);
        
        outputBuffer.append(String.format(indent + "Time      :: " + yellow("%f") + " GeV%n", truthHit.getTime()));
        outputBuffer.append(String.format(indent + "Energy    :: " + yellow("%f") + " GeV%n", truthHit.getCorrectedEnergy()));
        outputBuffer.append(String.format(indent + "Position  :: <" + yellow("%f") + ", " + yellow("%f") + ">%n", truthHit.getPosition()[0], truthHit.getPosition()[1]));
        outputBuffer.append(String.format(indent + "Indices   :: <" + yellow("%d") + ", " + yellow("%d") + ">%n",
                truthHit.getIdentifierFieldValue("ix"), truthHit.getIdentifierFieldValue("iy")));
        outputBuffer.append(String.format(indent + "Particles :: " + yellow("%d") + "%n", truthHit.getMCParticleCount()));
        for(int i = 0; i < truthHit.getMCParticleCount(); i++) {
            outputBuffer.append(String.format(indent + "\tPDGID = " + BashParameter.format("%3d", BashParameter.TEXT_YELLOW) + ";  t = " + yellow("%13.2f")
                    + ";  p = <" + yellow("%5.3f") + ", " + yellow("%5.3f") + ", " + yellow("%5.3f") + ">;   r = <" + yellow("%5.1f") + ", " + yellow("%5.1f")
                    + ", " + yellow("%5.1f") + ">%n",
                    truthHit.getMCParticle(i).getPDGID(), truthHit.getMCParticle(i).getProductionTime(), truthHit.getMCParticle(i).getMomentum().x(),
                    truthHit.getMCParticle(i).getMomentum().y(), truthHit.getMCParticle(i).getMomentum().z(), truthHit.getMCParticle(i).getOriginX(),
                    truthHit.getMCParticle(i).getOriginY(), truthHit.getMCParticle(i).getOriginZ()));
        }
        
        // Return the result.
        return outputBuffer.toString();
    }
    
    /**
     * Creates s textual representation of the data stored in a
     * {@link org.lcsim.event.MCParticle MCParticle} object.
     * @param truthHit - The object.
     * @param indentLevel - The level of indention to prepend.
     * @return Returns the textual representation.
     */
    @SuppressWarnings("unused")
    private static final String toString(MCParticle particle, int indentLevel) {
        // Create a buffer to store the data and generate the indent
        // level string.
        StringBuffer outputBuffer = new StringBuffer();
        String indent = getIndent(indentLevel);
        
        outputBuffer.append(String.format(indent + "Type     :: " + yellow("%d") + "%n", particle.getPDGID()));
        outputBuffer.append(String.format(indent + "Time     :: " + yellow("%f") + " ns%n", particle.getProductionTime()));
        outputBuffer.append(String.format(indent + "Momentum :: " + yellow("%f") + " GeV%n", particle.getMomentum().magnitude()));
        outputBuffer.append(String.format(indent + "Origin   :: <" + yellow("%f") + ", " + yellow("%f") + ", " + yellow("%f") + ">%n",
                particle.getOriginX(), particle.getOriginY(), particle.getOriginZ()));
        
        // Return the result.
        return outputBuffer.toString();
    }
    
    /**
     * Creates s textual representation of the data stored in a
     * {@link org.lcsim.event.GenericObject GenericObject} object.
     * @param truthHit - The object.
     * @param indentLevel - The level of indention to prepend.
     * @return Returns the textual representation.
     */
    @SuppressWarnings("unused")
    private static final String toString(GenericObject obj, int indentLevel) {
        // Create a buffer to store the data and generate the indent
        // level string.
        StringBuffer outputBuffer = new StringBuffer();
        String indent = getIndent(indentLevel);
        
        // Print the integer banks.
        outputBuffer.append(String.format(indent + "Number of integers :: " + yellow("%d") + "%n", obj.getNInt()));
        if(obj.getNInt() != 0) {
            outputBuffer.append(indent + "\t { ");
            for(int i = 0; i < obj.getNInt(); i++) {
                if(i != 0) { outputBuffer.append(", "); }
                outputBuffer.append(obj.getIntVal(i));
            }
            outputBuffer.append(String.format(" }%n"));
        }
        
        // Print the float banks.
        outputBuffer.append(String.format(indent + "Number of floats ::   " + yellow("%d") + "%n", obj.getNFloat()));
        if(obj.getNFloat() != 0) {
            outputBuffer.append(indent + "\t { ");
            for(int i = 0; i < obj.getNFloat(); i++) {
                if(i != 0) { outputBuffer.append(", "); }
                outputBuffer.append(obj.getFloatVal(i));
            }
            outputBuffer.append(String.format(" }%n"));
        }
        
        // Print the double banks.
        outputBuffer.append(String.format(indent + "Number of doubles ::  " + yellow("%d") + "%n", obj.getNDouble()));
        if(obj.getNDouble() != 0) {
            outputBuffer.append(indent + "\t { ");
            for(int i = 0; i < obj.getNDouble(); i++) {
                if(i != 0) { outputBuffer.append(", "); }
                outputBuffer.append(obj.getDoubleVal(i));
            }
            outputBuffer.append(String.format(" }%n"));
        }
        
        // Return the result.
        return outputBuffer.toString();
    }
    
    /**
     * Applies BASH formatting to the argument text such that it will
     * display using yellow text.
     * @param text - The text to format.
     * @return Returns the argument text with BASH formatting.
     */
    private static final String yellow(String text) { return BashParameter.format(text, BashParameter.TEXT_YELLOW); }
}