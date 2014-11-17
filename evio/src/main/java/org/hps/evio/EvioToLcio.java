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
import org.hps.job.JobManager;
import org.hps.record.LCSimEventBuilder;
import org.hps.record.evio.EvioEventUtilities;
import org.jlab.coda.jevio.EvioEvent;
import org.jlab.coda.jevio.EvioReader;
import org.lcsim.conditions.ConditionsManager;
import org.lcsim.event.EventHeader;
import org.lcsim.lcio.LCIOWriter;
import org.lcsim.util.log.LogUtil;

/**
 * <p>
 * This class converts EVIO to LCIO, performing an LCSim job in the same
 * session. The processed events are then (optionally) written to disk using an
 * LCIOWriter.
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
 * based on the run numbers in the EVIO file.  It will use an appropriate default detector
 * unless one is given on the command line, and it will also use the correct event builder.
 * However, it will NOT correctly handle a mixed list of EVIO files from both runs, so don't
 * do this! 
 *
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
// TODO: Add run number override argument.  This should cause EVIO run numbers to be ignored.
public class EvioToLcio {

    // Events with run numbers greater than this will use the new LCSimEngRunEventBuilder.
    private static final int TEST_RUN_END_RUN = 1365;

    // The default Test Run detector.
    private static final String DEFAULT_TEST_RUN_DETECTOR = "HPS-TestRun-v8-5";

    // The default Engineering Run detector.
    private static final String DEFAULT_ENG_RUN_DETECTOR = "HPS-Proposal2014-v8-6pt6";

    // The default steering resource, which basically does nothing except
    // initialize the conditions system.
    private static final String DEFAULT_STEERING_RESOURCE = "/org/hps/steering/EventMarker.lcsim";

    // The command line options which will be defined in the constructor.
    Options options = null;

    // The class's logger.
    Logger logger = LogUtil.create(EvioToLcio.class);

    /**
     * The default constructor, which defines command line arguments and sets the log level.
     */
    protected EvioToLcio() {
        logger.config("initializing");
        options = new Options();
        options.addOption(new Option("l", true, "The name of the output LCIO file"));
        options.addOption(new Option("d", true, "The name of the detector to use for LCSim conditions"));
        options.addOption(new Option("x", true, "The XML steeering file to process the LCIO events"));
        options.addOption(new Option("n", true, "Maximum number of events to process"));
        options.addOption(new Option("D", true, "Pass a variable to the steering file with format -Dname=value"));
        options.addOption(new Option("r", false, "Interpret steering from -x argument as a resource instead of a file"));
        options.addOption(new Option("e", false, "Continue if errors occur during event processing"));
        options.addOption(new Option("L", true, "Set the log level"));
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
     * This method will execute the EVIO to LCIO conversion and perform an
     * intermediate LCSim job. Then the resultant LCIO events will be written to
     * disk. 
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
            throw new RuntimeException("Problem parsing command line options.",
                    e);
        }

        // Is the extra argument list empty?
        if (cl.getArgs().length == 0) {
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

        // LCIO output file.
        if (cl.hasOption("l")) {
            logger.config("set LCIO file to " + lcioFileName);
            lcioFileName = cl.getOptionValue("l");
        }

        // LCSim XML file to execute on each event created by the builder.
        if (cl.hasOption("x")) {
            String lcsimXmlName = cl.getOptionValue("x");
            if (cl.hasOption("r")) {
                steeringStream = EvioToLcio.class.getResourceAsStream(lcsimXmlName);
                if (steeringStream == null) {
                    logger.severe("Resource " + lcsimXmlName + " was not found.");
                    throw new IllegalArgumentException("Resource was not found.");
                }
                logger.config("using steering resource " + lcsimXmlName);
            } else {
                try {
                    steeringStream = new FileInputStream(lcsimXmlName);
                    logger.config("using steering file " + lcsimXmlName);
                } catch (FileNotFoundException e) {
                    logger.severe("The steering file " + lcsimXmlName + " does not exist.");
                    throw new IllegalArgumentException("Steering file does not exist.", e);
                }
            }
        }

        if (steeringStream == null) {
            steeringStream = EvioToLcio.class.getResourceAsStream(DEFAULT_STEERING_RESOURCE);
            logger.config("using default steering resource " + DEFAULT_STEERING_RESOURCE);
        }

        // Max number of events.
        if (cl.hasOption("n")) {
            maxEvents = Integer.valueOf(cl.getOptionValue("n"));
            logger.config("set max events to " + maxEvents);
        }

        // Whether to continue on errors or stop.
        boolean continueOnErrors = false;
        if (cl.hasOption("e")) {
            continueOnErrors = true;
            logger.config("continue on errors enabled");
        }

        // LCIO writer.
        if (lcioFileName != null) {
            try {
                writer = new LCIOWriter(new File(lcioFileName));
                logger.config("initialized LCIO writer with file " + lcioFileName);
            } catch (IOException e) {
                logger.severe("Problem initializing the LCIO writer.");
                throw new RuntimeException(e);
            }
        }

        // LCSim job manager.
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
        jobManager.setup(steeringStream);
        jobManager.configure();
        logger.config("LCSim job manager was successfully configured.");

        // Name of detector.
        String detectorName = null;
        if (cl.hasOption("d")) {
            detectorName = cl.getOptionValue("d");
            logger.config("user set detector to " + detectorName);
        }

        // LCSim event builder which will be initialized within the loop based on the run number.
        LCSimEventBuilder eventBuilder = null;

        for (String evioFileName : cl.getArgs()) {
            // EVIO input file.
            File evioFile = new File(evioFileName);
            if (!evioFile.exists()) {
                throw new RuntimeException("EVIO file " + evioFile.getPath() + " does not exist.");
            }
            logger.info("Opening EVIO file " + evioFileName);

            // EVIO reader.
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
                        try {
                            reader.parseEvent(evioEvent);
                        } catch (Exception e) {
                            Logger.getLogger(EvioToLcio.class.getName()).log(Level.SEVERE, "Error reading EVIO event.", e);
                            if (!continueOnErrors) {
                                throw new RuntimeException(e);
                            } else {
                                continue eventLoop;
                            }
                        }
                    }

                    // Handlers for different event types.
                    if (EvioEventUtilities.isPreStartEvent(evioEvent)) {
                        
                        int[] data = evioEvent.getIntData();
                        // int seconds = data[0];
                        int runNumber = data[1];
                        
                        logger.info("got EVIO pre start event with run #" + runNumber);
                                                                       
                        // Has the event builder been initialized?
                        if (eventBuilder == null) {
                            // Initialize the event builder.
                            eventBuilder = setupEventBuilder(detectorName, runNumber);
                        }
                    }

                    // Setup state in the LCSimEventBuilder based on the EVIO event.
                    eventBuilder.readEvioEvent(evioEvent);

                    if (EvioEventUtilities.isEndEvent(evioEvent)) {
                        logger.info("got EVIO end event");
                        // int[] data = evioEvent.getIntData();
                        // int seconds = data[0];
                        // int nevents = data[2];
                        // calibListener.endRun(seconds, nevents);
                    } else if (EvioEventUtilities.isPhysicsEvent(evioEvent)) {

                        logger.finest("got EVIO physics event #" + evioEvent.getEventNumber());

                        // Is the event builder initialized?
                        if (eventBuilder == null) {
                            // This can happen if there are no pre-start events in the EVIO file.
                            throw new RuntimeException( "The LCSimEventBuilder was never setup.");
                        }

                        EventHeader lcioEvent = eventBuilder.makeLCSimEvent(evioEvent);
                        logger.finest("created LCIO event #" + lcioEvent.getEventNumber());

                        time = (lcioEvent.getTimeStamp() / 1000000);

                        if (firstEvent) {
                            logger.info("First physics event time: " + time / 1000 + " - " + new Date(time));
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
                    if (!continueOnErrors) {
                        throw new RuntimeException(e);
                    } else {
                        continue;
                    }
                }                
            }
            logger.info("Last physics event time: " + time / 1000 + " - " + new Date(time));
            try {
                logger.fine("closing reader");
                reader.close();
                logger.fine("reader closed");
            } catch (IOException e) {
                logger.log(Level.SEVERE, "An IO error occurred.", e);
                if (!continueOnErrors) {
                    throw new RuntimeException(e);
                }
            }
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
                throw new RuntimeException(e);
            }
            logger.info("closed LCIO writer");
        }
        
        logger.info("job finished successfully");
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
     * Setup and return the LCSimEventBuilder.
     * @param detectorName The detector name to be assigned to the event builder.
     * @param runNumber The run number which determines which event builder to use.
     * @return The LCSimEventBuilder for the Test Run or Engineering Run.
     */
    private LCSimEventBuilder setupEventBuilder(String detectorName, int runNumber) {         
        LCSimEventBuilder eventBuilder = null;
        // Is this run number not part of the Test Run?
        if (runNumber > 0 && runNumber <= EvioToLcio.TEST_RUN_END_RUN) {
            // This looks like a Test Run file.
            logger.info("using LCSimTestRunEventBuilder");
            eventBuilder = new LCSimTestRunEventBuilder();
            if (detectorName == null) {
                detectorName = DEFAULT_TEST_RUN_DETECTOR;
            }
        } else {
            logger.info("using LCSimEngRunEventBuilder");
            eventBuilder = new LCSimEngRunEventBuilder();
            if (detectorName == null) {
                detectorName = DEFAULT_ENG_RUN_DETECTOR;
            }
        }
        eventBuilder.setDetectorName(detectorName);
        ConditionsManager.defaultInstance().addConditionsListener(eventBuilder);
        logger.config("initialized " + eventBuilder.getClass().getCanonicalName() + " with detector " + detectorName + " and run number " + runNumber);
        return eventBuilder;
    }
}
