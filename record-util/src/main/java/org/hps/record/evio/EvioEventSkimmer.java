package org.hps.record.evio;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.DefaultParser;
import org.jlab.coda.jevio.EventWriter;
import org.jlab.coda.jevio.EvioEvent;
import org.jlab.coda.jevio.EvioReader;

/**
 * Skim EVIO events into a new file based on a list of event numbers to include.
 * 
 * @author Jeremy McCormick
 * @author Norman Graf
 */
public class EvioEventSkimmer {

    /**
     * Initialize the logger.
     */
    private static final Logger LOGGER = Logger.getLogger(EvioEventSkimmer.class.getPackage().getName());

    /**
     * Define command line options.
     */
    private static Options OPTIONS = new Options();
    static {
        OPTIONS.addOption("s", "skim-file", true, "list of event numbers to include in skim (text file)");
        OPTIONS.addOption("o", "output-file", true, "output EVIO file with skimmed events");
        OPTIONS.addOption("e", "evio-list", true, "input EVIO files to process (text file)");
        OPTIONS.addOption("L", "log-level", true, "set log level (Java conventions)");
        OPTIONS.addOption("n", "max-events", true, "max number of events to read");
    }

    /**
     * Run the skim from the command line.
     * 
     * @param args the command line arguments (parsed using Apache CLI)
     */
    public static void main(String[] args) {

        DefaultParser parser = new DefaultParser();

        CommandLine commandLine = null;
        try {
            commandLine = parser.parse(OPTIONS, args);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        
        if (commandLine.hasOption("L")) {
            Level newLevel = Level.parse(commandLine.getOptionValue("L"));
            LOGGER.config("setting new log level to " + newLevel);
            LOGGER.setLevel(newLevel);
        }

        // Get list of EVIO files to process.
        String evioTxtFile = null;
        if (commandLine.hasOption("e")) {
            evioTxtFile = commandLine.getOptionValue("e");
        } else {
            throw new RuntimeException("missing -e argument");
        }
        List<String> evioFilePaths = getEvioFilePaths(evioTxtFile);

        // Get the EVIO output file path.
        String evioFileOutPath = null;
        if (commandLine.hasOption("o")) {
            evioFileOutPath = commandLine.getOptionValue("o");
            if (new File(evioFileOutPath).exists()) {
                throw new RuntimeException("output file already exists: " + evioFileOutPath);
            }
        } else {
            throw new RuntimeException("misisng -o argument");
        }        
        LOGGER.config("output will be written to " + evioFileOutPath);
        
        // Get the max number of events to read.
        int maxEvents = Integer.MAX_VALUE;
        if (commandLine.hasOption("n")) {
            maxEvents = Integer.parseInt(commandLine.getOptionValue("n"));
            LOGGER.config("max events set to " + maxEvents);
        }
        
        // Get the list of events to include in the skim.
        String skimFilePath = null;
        if (commandLine.hasOption("s")) {
            skimFilePath = commandLine.getOptionValue("s");
            LOGGER.config("skim events will be read from " + skimFilePath);
        } else {
            throw new RuntimeException("missing -s argument with skim events");
        }
        Set<Integer> skimEvents = getSkimEvents(skimFilePath);
        LOGGER.config("got " + skimEvents.size() + " event numbers for skim");

        EventWriter writer = null;
        EvioReader reader = null;
        try {

            // Open writer with EVIO output path.
            writer = new EventWriter(evioFileOutPath, false);
            
            int nEventsRead = 0;

            // Loop over input files.
            fileLoop: for (String evioFileInPath : evioFilePaths) {

                LOGGER.info("opening " + evioFileInPath + " for reading");
                reader = new EvioReader(evioFileInPath, false, true);
                
                // Read all events and include in the skim if they are in the event list.
                EvioEvent evioEvent = null;
                while ((evioEvent = reader.parseNextEvent()) != null) {                    
                    LOGGER.finer("read EVIO event " + evioEvent.getEventNumber());
                    
                    if (nEventsRead >= maxEvents) {
                        LOGGER.info("max events " + maxEvents + " was reached");
                        break fileLoop;
                    }

                    // Set event number from event ID bank.
                    EvioEventUtilities.setEventNumber(evioEvent);

                    LOGGER.finest("event number set to " + evioEvent.getEventNumber() + " from event ID");
                    
                    if (skimEvents.contains(evioEvent.getEventNumber())) {
                        // Event is accepted.
                        LOGGER.info("including event " + evioEvent.getEventNumber() + " in skim");
                        writer.writeEvent(evioEvent);
                        LOGGER.finer("wrote " + writer.getEventsWritten() + " events so far");
                    } else {
                        // Event is rejected.
                        LOGGER.finer("event " + evioEvent.getEventNumber() + " rejected");
                    }
                    ++nEventsRead;
                }
                
                // Close reader.
                LOGGER.info("closing reader");
                reader.close();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            writer.close();
            try {
                reader.close();
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, e.getMessage(), e);
            }
        }
        
        LOGGER.info("Done!");
    }

    /**
     * Get the list of EVIO files to process from a text file list.
     * 
     * @param txtFilePath the text file with EVIO file list
     * @return a list of paths to EVIO files
     */
    static List<String> getEvioFilePaths(String txtFilePath) {
        List<String> evioFilePaths = new ArrayList<String>();
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(txtFilePath));
            String currentLine = null;
            while ((currentLine = br.readLine()) != null) {
                evioFilePaths.add(currentLine.trim());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                br.close();
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, e.getMessage(), e);
            }
        }
        return evioFilePaths;
    }
    
    /**
     * Get a list of event numbers to include in the skim.
     * 
     * @param txtFilePath text file with list of events to include (one per line)
     * @return the list of event numbers to include
     */
    static Set<Integer> getSkimEvents(String txtFilePath) {
        Set<Integer> skimEvents = new LinkedHashSet<Integer>();
        BufferedReader br = null;
        String currentLine = null;
        try {
            br = new BufferedReader(new FileReader(txtFilePath));            
            while ((currentLine = br.readLine()) != null) {
                String fileName = currentLine.trim();
                LOGGER.config("including " + fileName + " in EVIO input files");
                Integer skimEvent = Integer.parseInt(fileName);
                skimEvents.add(skimEvent);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (NumberFormatException e) {
            LOGGER.log(Level.SEVERE, "bad number format in skim list: " + currentLine);
            throw new RuntimeException(e);
        } finally {
            try {
                br.close();
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, e.getMessage(), e);
            }
        }
        return skimEvents;
    }
}
