package org.hps.evio;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.job.JobManager;
import org.hps.record.LCSimEventBuilder;
import org.hps.record.evio.EvioEventUtilities;
import org.jlab.coda.jevio.EvioEvent;
import org.jlab.coda.jevio.EvioReader;
import org.lcsim.conditions.ConditionsManager;
import org.lcsim.conditions.ConditionsManager.ConditionsNotFoundException;
import org.lcsim.event.EventHeader;
import org.lcsim.lcio.LCIOWriter;
import org.lcsim.util.log.LogUtil;

/**
 * <p>
 * This class converts EVIO to LCIO, performing an LCSim job in the same session. 
 * The processed events are then (optionally) written to disk using an LCIOWriter.
 * <p>
 * To run this class from the command line:<br>
 * java -cp hps-distribution-bin.jar EvioToLcio [options] [evioFiles]
 * <p>
 * The available arguments can be printed using:<br>
 * java -cp hps-distribution-bin.jar EvioToLcio -h
 * <p>
 * Extra arguments are treated as paths to EVIO files.
 * <p>
 * This class attempts to automatically configure itself for Test Run or Engineering Run 
 * based on the run numbers in the EVIO file. It will use an appropriate default detector 
 * unless one is given on the command line, and it will also use the correct event builder. 
 * However, it will NOT correctly handle a mixed list of EVIO files from both runs, so
 * don't do this!
 *
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class EvioToLcio {


    // The default steering resource, which basically does nothing except print event numbers.
    private static final String DEFAULT_STEERING_RESOURCE = "/org/hps/steering/EventMarker.lcsim";

    // The command line options which will be defined in the constructor.
    Options options = null;

    // The class's logger.
    Logger logger = LogUtil.create(EvioToLcio.class);

    LCSimEventBuilder eventBuilder = null;

    String detectorName;

    /**
     * The default constructor, which defines command line arguments and sets the log level.
     */
    protected EvioToLcio() {
        logger.config("initializing EVIO to LCIO converter");
        options = new Options();
        options.addOption(new Option("l", true, "The name of the output LCIO file"));
        options.addOption(new Option("d", true, "The name of the detector to use for LCSim conditions"));
        options.addOption(new Option("R", true, "A run number which will override those found in the input files"));
        options.addOption(new Option("x", true, "The XML steeering file to process the LCIO events"));
        options.addOption(new Option("n", true, "Maximum number of events to process per input file"));
        options.addOption(new Option("D", true, "Pass a variable to the steering file with format -Dname=value"));
        options.addOption(new Option("r", false, "Interpret steering from -x argument as a resource instead of a file"));
        options.addOption(new Option("L", true, "Set the log level (INFO, FINE, FINEST, etc.)"));
        options.addOption(new Option("b", false, "Enable headless mode which will suppress showing of AIDA plots"));
        logger.setLevel(Level.FINE);
    }

    /**
     * Run the EVIO to LCIO converter from the command line. 
     * @param args The command line arguments.
     */
    public static void main(String[] args) {
        EvioToLcio evioToLcio = new EvioToLcio();
        evioToLcio.run(args);
    }

    /**
     * This method will execute the EVIO to LCIO conversion and optionally process the events with LCSim Drivers
     * from a steering file.  Then the resultant LCIO events will be written to disk if the <code>-l</code> option 
     * is present.         
     * @param args The command line arguments.
     */
    public void run(String[] args) {

        int maxEvents = 0;
        int nEvents = 0;

        // Set up command line parsing.
        if (args.length == 0) {
            printUsage();
        }
        CommandLineParser parser = new PosixParser();

        // Parse command line arguments.
        CommandLine cl = null;
        try {
            cl = parser.parse(options, args);
        } catch (ParseException e) {
            throw new RuntimeException("Problem parsing command line options.", e);
        }

        // Is the extra argument list empty?
        if (cl.getArgs().length == 0) {
            logger.severe("No EVIO file names were given on the command line.");
            // User didn't supply any EVIO files so exit.
            printUsage();
        }

        // Set log level.
        if (cl.hasOption("L")) {
            Level level = Level.parse(cl.getOptionValue("L").toUpperCase());
            logger.config("setting log level to " + level);
            logger.setLevel(level);
        }

        String lcioFileName = null;
        LCIOWriter writer = null;
        InputStream steeringStream = null;

        // Get the LCIO output file.
        if (cl.hasOption("l")) {
            logger.config("set LCIO file to " + lcioFileName);
            lcioFileName = cl.getOptionValue("l");
        }

        // Get the steering file or resource for the LCSim job.
        if (cl.hasOption("x")) {
            String lcsimXmlName = cl.getOptionValue("x");
            if (cl.hasOption("r")) {
                steeringStream = EvioToLcio.class.getResourceAsStream(lcsimXmlName);
                if (steeringStream == null) {
                    logger.severe("LCSim steering resource " + lcsimXmlName + " was not found.");
                    throw new IllegalArgumentException("XML steering resource was not found.");
                }
                logger.config("using steering resource " + lcsimXmlName);
            } else {
                try {
                    steeringStream = new FileInputStream(lcsimXmlName);
                    logger.config("using steering file " + lcsimXmlName);
                } catch (FileNotFoundException e) {
                    logger.severe("The XML steering file " + lcsimXmlName + " does not exist.");
                    throw new IllegalArgumentException("Steering file does not exist.", e);
                }
            }
        }

        if (steeringStream == null) {
            steeringStream = EvioToLcio.class.getResourceAsStream(DEFAULT_STEERING_RESOURCE);
            logger.config("using default steering resource " + DEFAULT_STEERING_RESOURCE);
        }

        // Get the max number of events to process.
        if (cl.hasOption("n")) {
            maxEvents = Integer.valueOf(cl.getOptionValue("n"));
            logger.config("set max events to " + maxEvents);
        }

        // Setup the LCIO writer.
        if (lcioFileName != null) {
            try {
                writer = new LCIOWriter(new File(lcioFileName));
                logger.config("initialized LCIO writer with file " + lcioFileName);
            } catch (IOException e) {
                logger.severe("Problem initializing the LCIO writer.");
                throw new RuntimeException(e);
            }
        }

        // Process the LCSim job variable definitions, if any.
        JobManager jobManager = new JobManager();
        if (cl.hasOption("D")) {
            String[] steeringOptions = cl.getOptionValues("D");
            for (String def : steeringOptions) {
                String[] s = def.split("=");
                if (s.length != 2) {
                    logger.severe("bad variable format: " + def);
                    throw new IllegalArgumentException("Bad variable format: " + def);
                }
                String key = s[0];
                String value = s[1];
                jobManager.addVariableDefinition(key, value);
                logger.config("set steering variable: " + key + "=" + value);
            }
        }
        if (cl.hasOption("b")) {
            // Enable headless mode so no plots are shown.
            logger.config("Headless mode is enabled.  No plots will be shown.");
            jobManager.enableHeadlessMode();
        }
        
        // Configure the LCSim job manager.
        jobManager.setup(steeringStream);
        jobManager.configure();
        logger.config("LCSim job manager was successfully configured.");
 
        if (cl.hasOption("d")) {
            // Get the user specified detector name.
            detectorName = cl.getOptionValue("d");
            logger.config("User set detector to " + detectorName + " with command option.");
        }

        Integer runNumber = null;
        if (cl.hasOption("R")) {       
            // Get the user specified run number.
            runNumber = Integer.parseInt(cl.getOptionValue("R"));
            logger.config("User set run number to " + runNumber + " with command option.");
        }

        // Initialize the HPS database conditions system.
        DatabaseConditionsManager conditionsManager = DatabaseConditionsManager.getInstance();

        // Is there a user specified run number?
        if (runNumber != null) {
            // Setup the event builder at the beginning of the job with the run number and detector name.
            this.setupEventBuilder(detectorName, runNumber);
            try {
                // Setup the conditions system, which will initialize the event builder via the ConditionsListener callback.
                conditionsManager.setDetector(detectorName, runNumber);
            } catch (ConditionsNotFoundException e) {
                throw new RuntimeException();
            }
            // Since there is a user specified run number override, the conditions system is frozen so that run numbers
            // from data are ignored.
            logger.config("Conditions system will be frozen to use specified run number and detector!");
            conditionsManager.freeze();
        }

        // Loop over the input EVIO files.
        for (String evioFileName : cl.getArgs()) {
            
            // Get the next EVIO input file.
            File evioFile = new File(evioFileName);
            if (!evioFile.exists()) {
                throw new RuntimeException("EVIO file " + evioFile.getPath() + " does not exist.");
            }
            logger.info("Opening EVIO file " + evioFileName + " for reading.");

            // Open the EVIO reader.
            EvioReader reader = null;
            try {
                reader = new EvioReader(evioFile);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            boolean firstEvent = true;
            long time = 0; // in ms

            // Loop over EVIO events, build LCSim events, process them, and then
            // write events to disk.
            fileLoop: while (maxEvents == 0 || nEvents < maxEvents) {
                EvioEvent evioEvent = null;
                try {
                    eventLoop: while (evioEvent == null) {
                        evioEvent = reader.nextEvent();
                        if (evioEvent == null) {
                            break fileLoop;
                        }
                        reader.parseEvent(evioEvent);
                    }

                    // Handle a pre start event.
                    if (EvioEventUtilities.isPreStartEvent(evioEvent)) {

                        // Get the pre start event's int data bank.
                        int[] data = EvioEventUtilities.getControlEventData(evioEvent);

                        if (data == null) {
                            logger.severe("Pre start data bank was not found.");
                            throw new RuntimeException("Pre start data bank is null.");
                        }
                        
                            // int seconds = data[0];
                        int preStartRunNumber = data[1];

                        logger.info("EVIO pre start event with run #" + preStartRunNumber);

                        // Is the event builder uninitialized?
                        if (eventBuilder == null) {
                            // Initialize the event builder.
                            setupEventBuilder(detectorName, preStartRunNumber);
                        }
                    }

                    // Setup state in the LCSimEventBuilder based on the EVIO event.
                    if (eventBuilder != null) {
                        eventBuilder.readEvioEvent(evioEvent);
                    } else {
                        throw new RuntimeException("The event builder was never setup.  Try manually setting a run number using the -R switch.");
                    }

                    // Handle an end event.
                    if (EvioEventUtilities.isEndEvent(evioEvent)) {
                        int[] data = EvioEventUtilities.getControlEventData(evioEvent);
                        if (data == null) {
                            logger.severe("The end event data bank was not found.");
                            throw new RuntimeException("The end event data bank is null.");
                        }
                        int seconds = data[0];
                        int totalEvents = data[2];
                        logger.info("EVIO end event with " + totalEvents + " events and " + seconds + " seconds");
                    // Handle a physics event.
                    } else if (EvioEventUtilities.isPhysicsEvent(evioEvent)) {

                        logger.finest("got EVIO physics event #" + evioEvent.getEventNumber());

                        // Is the event builder initialized?
                        if (eventBuilder == null) {
                            // This can happen if there are no pre start events in the EVIO file and no run number was explicitly given on the command line.
                            throw new RuntimeException("The LCSimEventBuilder was never initialized.  You may need to manually specify a run number using the -R switch.");
                        }

                        EventHeader lcioEvent = eventBuilder.makeLCSimEvent(evioEvent);
                        logger.finest("created LCIO event #" + lcioEvent.getEventNumber());

                        time = (lcioEvent.getTimeStamp() / 1000000);

                        if (firstEvent) {
                            logger.info("first physics event time: " + time / 1000 + " - " + new Date(time));
                            firstEvent = false;
                        }

                        logger.finest("processing LCIO event in LCSim");
                        jobManager.processEvent(lcioEvent);
                        logger.finest("done processing LCIO event in LCSim");
                        if (writer != null) {
                            writer.write(lcioEvent);
                            writer.flush();
                            logger.finest("wrote LCIO event #" + lcioEvent.getEventNumber());
                        }

                        nEvents++;
                    }

                } catch (Exception e) {
                    logger.log(Level.SEVERE, "Error in LCIO event processing.", e);
                    throw new RuntimeException(e);
                }
            }
            logger.info("Last physics event time: " + time / 1000 + " - " + new Date(time));
            logger.fine("closing EVIO reader");
            try {
                reader.close();
            } catch (IOException e) {
                logger.warning(e.getMessage());
                e.printStackTrace();
            }
            logger.fine("EVIO reader closed");
        }
        logger.info("no more data");

        logger.info("executing jobManager.finish");
        jobManager.finish();
        logger.info("jobManager is done");

        if (writer != null) {
            logger.info("closing LCIO writer");
            try {
                writer.close();
            } catch (IOException e) {
                logger.warning(e.getMessage());
                e.printStackTrace();
            }
            logger.info("LCIO writer closed");
        }

        logger.info("Job finished successfully!");
    }

    /**
     * Print the CLI usage and exit.
     */
    private void printUsage() {
        System.out.println("EvioToLcio [options] [evioFiles]");
        HelpFormatter help = new HelpFormatter();
        help.printHelp(" ", options);
        System.exit(1);
    }

    /**
     * Setup the LCSimEventBuilder based on the given detector name and run number. 
     * @param detectorName The detector name to be assigned to the event builder.
     * @param runNumber The run number which determines which event builder to use.
     * @return The LCSimEventBuilder for the Test Run or Engineering Run.
     */
    private void setupEventBuilder(String detectorName, int runNumber) {
        // Is this run number from the Test Run?
        if (DatabaseConditionsManager.isTestRun(runNumber)) {
            // Configure conditions system for Test Run.
            logger.info("using LCSimTestRunEventBuilder");
            eventBuilder = new LCSimTestRunEventBuilder();
            if (detectorName == null) {                
                this.detectorName = DatabaseConditionsManager.getDefaultTestRunDetectorName();
                logger.info("using default Test Run detector name " + this.detectorName);
            }
        } else {
            // Configure conditions system for Eng Run or default.
            logger.info("using LCSimEngRunEventBuilder");
            eventBuilder = new LCSimEngRunEventBuilder();
            if (detectorName == null) {
                this.detectorName = DatabaseConditionsManager.getDefaultEngRunDetectorName();
                logger.info("using default Eng Run detector name " + this.detectorName);
            }
        }
        eventBuilder.setDetectorName(this.detectorName);
        ConditionsManager.defaultInstance().addConditionsListener(eventBuilder);
        logger.config("initialized " + eventBuilder.getClass().getCanonicalName() + " with detector " + this.detectorName + " and run number " + runNumber);
    }
}
