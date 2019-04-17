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

public class PrintCollectionsUtility {
    public static void main(String[] args) throws IOException, NumberFormatException, ConditionsNotFoundException {
        if(args[0].compareTo("-h") == 0 || args[0].compareTo("--help") == 0) {
            System.out.println(BashParameter.format("java -cp $HPS_JAVA org.hps.analysis.util.PrintCollectionsUtility [", BashParameter.TEXT_LIGHT_BLUE)
                    + BashParameter.format(" INPUT_FILE ", BashParameter.TEXT_YELLOW, BashParameter.PROPERTY_BOLD)
                    + BashParameter.format("]", BashParameter.TEXT_LIGHT_BLUE));
            System.out.println(getFormattedArgumentText('d', "Detector name             ", true));
            System.out.println(getFormattedArgumentText('R', "Run number                ", true));
            System.out.println(getFormattedArgumentText('n', "Maximum events to process ", false));
            System.out.println(getFormattedArgumentText('p', "Print collection data     ", false));
        }
        
        // Validate the input file.
        if(args.length == 0) { throw new RuntimeException("Error: Input file is not defined."); }
        File inputFile = new File(args[0]);
        if(!inputFile.exists()) { throw new RuntimeException("Error: Input file \"" + args[0] + "\" does not exist."); }
        
        // Parse the command line arguments.
        String detectorName = null;
        int runNumber = Integer.MIN_VALUE;
        int maxEvents = Integer.MAX_VALUE;
        List<String> printCollections = new ArrayList<String>();
        for(int i = 1; i < args.length; i++) {
            if(args[i].compareTo("-d") == 0) {
                i++;
                if(args.length > i) { detectorName = args[i]; } else { throwArgumentError(); }
            } else if(args[i].compareTo("-R") == 0) {
                i++;
                if(args.length > i) { runNumber = Integer.parseInt(args[i]); } else { throwArgumentError(); }
            } else if(args[i].compareTo("-n") == 0) {
                i++;
                if(args.length > i) { maxEvents = Integer.parseInt(args[i]); } else { throwArgumentError(); }
            } else if(args[i].compareTo("-p") == 0) {
                i++;
                if(args.length > i) { printCollections.add(args[i]); } else { throwArgumentError(); }
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
        
        // Initialize the file reader.
        File input = new File(args[0]);
        LCIOReader reader = new LCIOReader(input);
        
        // Get all collections names.
        int processedEvents = 0;
        Set<Pair<String, String>> collectionSet = new HashSet<Pair<String, String>>();
        Map<String, Class<?>> collectionMap = new HashMap<String, Class<?>>();
        while(true && processedEvents < maxEvents) {
            EventHeader event = null;
            try { event = reader.read(); }
            catch(EOFException e) { break; }
            
            @SuppressWarnings("rawtypes")
            Set<List> eventCollections = event.getLists();
            for(List<?> eventCollection : eventCollections) {
                LCMetaData collectionMetaData = event.getMetaData(eventCollection);
                collectionSet.add(new Pair<String, String>(collectionMetaData.getName(), collectionMetaData.getType().getSimpleName()));
                collectionMap.put(collectionMetaData.getName(), collectionMetaData.getType());
            }
            
            // If no collections were specified for printing, then
            // continue to the next iteration.
            if(printCollections.isEmpty()) { continue; }
            
            // Otherwise, print the data.
            System.out.println(BashParameter.format("Requested Print Collections for Event ", BashParameter.TEXT_LIGHT_BLUE)
                    + BashParameter.format(Integer.toString(event.getEventNumber()), BashParameter.TEXT_GREEN, BashParameter.PROPERTY_BOLD));
            collectionPrintLoop:
            for(int i = 0; i < printCollections.size(); i++) {
                // Indicate the current colection.
                System.out.println("\tPrinting data for collection \"" + BashParameter.format(printCollections.get(i), BashParameter.TEXT_YELLOW, BashParameter.PROPERTY_BOLD) + "\"...");
                
                // Check to see if the collection exists in this event.
                if(!collectionMap.containsKey(printCollections.get(i))) {
                    System.out.println("\t\tCollection does not exist.");
                    continue collectionPrintLoop;
                }
                
                // Get the class type of the collection.
                Class<?> collectionType = collectionMap.get(printCollections.get(i));
                
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
    
    private static final String getIndent(int level) {
        StringBuffer outputBuffer = new StringBuffer();
        for(int i = 0; i < level; i++) {
            outputBuffer.append('\t');
        }
        return outputBuffer.toString();
    }
    
    private static final void throwArgumentError() {
        throw new RuntimeException("Error: invalid commandline arguments.");
    }
    
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
    
    @SuppressWarnings("unused")
    private static final String toString(SimCalorimeterHit truthHit, int indentLevel) {
        // Create a buffer to store the data and generate the indent
        // level string.
        StringBuffer outputBuffer = new StringBuffer();
        String indent = getIndent(indentLevel);
        
        outputBuffer.append(String.format(indent + "Time      :: " + BashParameter.format("%f", BashParameter.TEXT_YELLOW) + " GeV%n", truthHit.getTime()));
        outputBuffer.append(String.format(indent + "Energy    :: " + BashParameter.format("%f", BashParameter.TEXT_YELLOW) + " GeV%n", truthHit.getCorrectedEnergy()));
        outputBuffer.append(String.format(indent + "Position  :: <" + BashParameter.format("%f, %f", BashParameter.TEXT_YELLOW) + ">%n", truthHit.getPosition()[0], truthHit.getPosition()[1]));
        outputBuffer.append(String.format(indent + "Indices   :: <" + BashParameter.format("%d, %d", BashParameter.TEXT_YELLOW) + ">%n",
                truthHit.getIdentifierFieldValue("ix"), truthHit.getIdentifierFieldValue("iy")));
        outputBuffer.append(String.format(indent + "Particles :: " + BashParameter.format("%d", BashParameter.TEXT_YELLOW) + "%n", truthHit.getMCParticleCount()));
        for(int i = 0; i < truthHit.getMCParticleCount(); i++) {
            outputBuffer.append(String.format(indent + "\tPDGID = %3d;  t = %13.2f;  p = <%5.3f, %5.3f, %5.3f>;   r = <%5.1f, %5.1f, %5.1f>%n",
                    truthHit.getMCParticle(i).getPDGID(), truthHit.getMCParticle(i).getProductionTime(), truthHit.getMCParticle(i).getMomentum().x(),
                    truthHit.getMCParticle(i).getMomentum().y(), truthHit.getMCParticle(i).getMomentum().z(), truthHit.getMCParticle(i).getOriginX(),
                    truthHit.getMCParticle(i).getOriginY(), truthHit.getMCParticle(i).getOriginZ()));
        }
        
        // Return the result.
        return outputBuffer.toString();
    }
    
    @SuppressWarnings("unused")
    private static final String toString(MCParticle particle, int indentLevel) {
        // Create a buffer to store the data and generate the indent
        // level string.
        StringBuffer outputBuffer = new StringBuffer();
        String indent = getIndent(indentLevel);
        
        outputBuffer.append(String.format(indent + "Type     :: " + BashParameter.format("%d", BashParameter.TEXT_YELLOW) + " GeV%n", particle.getPDGID()));
        outputBuffer.append(String.format(indent + "Time     :: " + BashParameter.format("%f", BashParameter.TEXT_YELLOW) + " GeV%n", particle.getProductionTime()));
        outputBuffer.append(String.format(indent + "Momentum :: " + BashParameter.format("%f", BashParameter.TEXT_YELLOW) + " GeV%n", particle.getMomentum().magnitude()));
        outputBuffer.append(String.format(indent + "Origin   :: <" + BashParameter.format("%f, %f, %f", BashParameter.TEXT_YELLOW) + ">%n", particle.getOriginX(),
                particle.getOriginY(), particle.getOriginZ()));
        
        // Return the result.
        return outputBuffer.toString();
    }
    
    @SuppressWarnings("unused")
    private static final String toString(GenericObject obj, int indentLevel) {
        // Create a buffer to store the data and generate the indent
        // level string.
        StringBuffer outputBuffer = new StringBuffer();
        String indent = getIndent(indentLevel);
        
        // Print the integer banks.
        outputBuffer.append(String.format(indent + "Number of integers :: " + BashParameter.format("%d", BashParameter.TEXT_YELLOW) + "%n", obj.getNInt()));
        if(obj.getNInt() != 0) {
            outputBuffer.append(indent + "\t { ");
            for(int i = 0; i < obj.getNInt(); i++) {
                if(i != 0) { outputBuffer.append(", "); }
                outputBuffer.append(obj.getIntVal(i));
            }
            outputBuffer.append(String.format(" }%n"));
        }
        
        // Print the float banks.
        outputBuffer.append(String.format(indent + "Number of floats ::   " + BashParameter.format("%d", BashParameter.TEXT_YELLOW) + "%n", obj.getNFloat()));
        if(obj.getNFloat() != 0) {
            outputBuffer.append(indent + "\t { ");
            for(int i = 0; i < obj.getNFloat(); i++) {
                if(i != 0) { outputBuffer.append(", "); }
                outputBuffer.append(obj.getFloatVal(i));
            }
            outputBuffer.append(String.format(" }%n"));
        }
        
        // Print the double banks.
        outputBuffer.append(String.format(indent + "Number of doubles ::  " + BashParameter.format("%d", BashParameter.TEXT_YELLOW) + "%n", obj.getNDouble()));
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
}