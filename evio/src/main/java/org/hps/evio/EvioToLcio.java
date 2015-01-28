package org.hps.evio;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.freehep.record.source.NoSuchRecordException;
import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.job.JobManager;
import org.hps.record.LCSimEventBuilder;
import org.hps.record.evio.EvioEventQueue;
import org.hps.record.evio.EvioEventUtilities;
import org.jlab.coda.jevio.BaseStructure;
import org.jlab.coda.jevio.EvioEvent;
import org.jlab.coda.jevio.EvioException;
import org.jlab.coda.jevio.EvioReader;
import org.lcsim.conditions.ConditionsManager;
import org.lcsim.conditions.ConditionsManager.ConditionsNotFoundException;
import org.lcsim.event.EventHeader;
import org.lcsim.lcio.LCIOWriter;
import org.lcsim.util.log.LogUtil;

/**
 * <p>
 * This class converts EVIO to LCIO, performing an LCSim job in the same session. The processed
 * events are then (optionally) written to disk using an LCIOWriter.
 * <p>
 * To run this class from the command line:<br>
 * java -cp hps-distribution-bin.jar EvioToLcio [options] [evioFiles]
 * <p>
 * The available arguments can be printed using:<br>
 * java -cp hps-distribution-bin.jar EvioToLcio -h
 * <p>
 * Extra arguments are treated as paths to EVIO files.
 * <p>
 * This class attempts to automatically configure itself for Test Run or Engineering Run based on
 * the run numbers in the EVIO file. It will use an appropriate default detector unless one is given
 * on the command line, and it will also use the correct event builder. It will not handle jobs
 * correctly with files from both the Test and Engineering Run, so don't do this!
 * <p>
 * The conditions system can be initialized in one of three ways.<br/>
 * <ol>
 * <li>user specified run number in which case the conditions system is frozen for the rest of the job</li>
 * <li>run number from an EVIO pre start event</li>
 * <li>run number from a header bank in an event</li>
 * </ol>
 * <p>
 * In the case where a file has no pre start event and there are header banks present, 
 * the "-m" command line option can be used to buffer a number of EVIO events.  If there is a
 * head bank found while adding these events to queue, the conditions system will be initialized
 * from it.
 *
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 * @author Sho Uemura <meeg@slac.stanford.edu>
 */
// TODO: Logger should print tracebacks.  See Driver's setup for example of this.
public class EvioToLcio {

    // The default steering resource, which basically does nothing except print event numbers.
    private static final String DEFAULT_STEERING_RESOURCE = "/org/hps/steering/EventMarker.lcsim";

    // The command line options which will be defined in the constructor.
    Options options = null;

    // The class's logger.
    Logger logger = LogUtil.create(EvioToLcio.class);

    // The LCSim event builder used to convert from EVIO.
    LCSimEventBuilder eventBuilder = null;

    // The detector name for conditions.
    String detectorName;

    // The run number for conditions.
    Integer runNumber = null;

    /**
     * The default constructor, which defines command line arguments and sets the log level.
     */
    protected EvioToLcio() {
        logger.config("initializing EVIO to LCIO converter");
        options = new Options();
        options.addOption(new Option("f", true, "path to a text file containing a list of EVIO files to process"));
        options.addOption(new Option("L", true, "log level (INFO, FINE, FINEST, etc.)"));
        options.addOption(new Option("x", true, "XML steeering file for processing LCIO events"));
        options.addOption(new Option("r", false, "interpret steering from -x argument as a resource instead of a file"));
        options.addOption(new Option("D", true, "pass a variable to the steering file with format -Dname=value"));
        options.addOption(new Option("l", true, "path of output LCIO file"));
        options.addOption(new Option("d", true, "name of the detector to use for LCSim conditions"));
        options.addOption(new Option("R", true, "fixed run number which will override run numbers of input files"));
        options.addOption(new Option("n", true, "maximum number of events to process in the job"));
        options.addOption(new Option("b", false, "enable headless mode which will not show plots OR allow writing them to graphics files"));
        options.addOption(new Option("v", false, "print EVIO XML for each event"));
        options.addOption(new Option("m", true, "set the max event buffer size"));
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
     * This method will execute the EVIO to LCIO conversion and optionally process the events with
     * LCSim Drivers from a steering file. Then the resultant LCIO events will be written to disk if
     * this option is enabled in the command line arguments.
     * @param args The command line arguments.
     */
    public void run(String[] args) {

        int maxEvents = -1;
        int nEvents = 0;
        int maxBufferSize = 40;

        // Parse the command line options.
        if (args.length == 0) {
            printUsage();
        }
        CommandLineParser parser = new PosixParser();
        CommandLine cl = null;
        try {
            cl = parser.parse(options, args);
        } catch (ParseException e) {
            throw new RuntimeException("Problem parsing command line options.", e);
        }

        // Set the log level.
        if (cl.hasOption("L")) {
            Level level = Level.parse(cl.getOptionValue("L").toUpperCase());
            logger.config("setting log level to " + level);
            logger.setLevel(level);
        }

        // Add all extra arguments to the EVIO file list.
        List<String> evioFileList = new ArrayList<String>(Arrays.asList(cl.getArgs()));

        // Process text file containing list of EVIO file paths, one per line.
        if (cl.hasOption("f")) {
            // Add additional EVIO files to process from text file.
            File file = new File(cl.getOptionValue("f"));
            if (!file.exists()) {
                throw new RuntimeException("The file " + file.getPath() + " does not exist.");
            }
            Path filePath = file.toPath();
            List<String> lines = null;
            try {
                lines = Files.readAllLines(filePath, Charset.defaultCharset());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            for (String line : lines) {
                File evioFile = new File(line);
                if (evioFile.exists()) {
                    evioFileList.add(evioFile.getPath());
                } else {
                    throw new RuntimeException("The EVIO file " + line + " does not exist.");
                }
            }
        }

        // Is the EVIO file list empty?
        if (evioFileList.isEmpty()) {
            // There weren't any EVIO files provided on the command line so exit.
            logger.severe("No EVIO files were provided with command line arguments or -f option.");
            printUsage();
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

        // Setup the default steering which just prints event numbers.
        if (steeringStream == null) {
            steeringStream = EvioToLcio.class.getResourceAsStream(DEFAULT_STEERING_RESOURCE);
            logger.config("using default steering resource " + DEFAULT_STEERING_RESOURCE);
        }

        // Get the max number of events to process.
        if (cl.hasOption("n")) {
            maxEvents = Integer.valueOf(cl.getOptionValue("n"));
            if (maxEvents <= 0) {
                throw new IllegalArgumentException("Value of -n option is invalid: " + maxEvents);
            }
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

        // Enable headless mode so no plots are shown.
        if (cl.hasOption("b")) {
            logger.config("Headless mode is enabled.  No plots will be shown.");
            jobManager.enableHeadlessMode();
        }

        // Configure the LCSim job manager.
        jobManager.setup(steeringStream);
        jobManager.configure();
        logger.config("LCSim job manager was successfully configured.");

        // Get the user specified detector name.
        if (cl.hasOption("d")) {
            detectorName = cl.getOptionValue("d");
            logger.config("User set detector to " + detectorName + " with command option.");
        }

        // Get the user specified run number.
        if (cl.hasOption("R")) {
            runNumber = Integer.parseInt(cl.getOptionValue("R"));
            logger.config("User set run number to " + runNumber + " with command option.");
        }

        // Is there a run number from the command line options?
        if (runNumber != null) {
            // Initialize the conditions system before the job starts and freeze it.
            checkConditions(runNumber, true);
        }

        // Print out the EVIO file list before the job starts.
        StringBuffer buff = new StringBuffer();
        buff.append("The job will include the following EVIO files ...");
        buff.append('\n');
        for (String evioFileName : evioFileList) {            
            buff.append(evioFileName);
            buff.append('\n');
        }
        logger.config(buff.toString());

        // Get whether to debug print XML from the EVIO events.
        boolean printXml = false;
        if (cl.hasOption("v")) {
            printXml = true;
        }

        // Get the maximum number of EVIO events to buffer.
        if (cl.hasOption("m")) {
            maxBufferSize = Integer.parseInt(cl.getOptionValue("m"));
        }

        // Loop over the input EVIO files.
        EvioReader reader = null;
        fileLoop: for (String evioFileName : evioFileList) {

            // Get the next EVIO input file.
            File evioFile = new File(evioFileName);
            if (!evioFile.exists()) {
                throw new RuntimeException("EVIO file " + evioFile.getPath() + " does not exist.");
            }
            logger.info("Opening EVIO file " + evioFileName + " ...");

            // Open the EVIO reader.
            try {
                reader = new EvioReader(evioFile);
            } catch (Exception e) {
                throw new RuntimeException("Error opening the EVIO file reader.", e);
            }

            boolean firstEvent = true;
            long eventTime = 0; // in ms

            // Loop over events.
            EvioEventQueue eventQueue = new EvioEventQueue(-1L, maxBufferSize);
            eventLoop: for (;;) {

                // Buffer the EVIO events into the queue.
                bufferEvents(reader, eventQueue, maxBufferSize);

                // Is the event queue empty?
                if (eventQueue.size() == 0) {
                    // Break from the event processing loop.
                    break eventLoop;
                }

                // Loop over the EVIO events in the buffer until it is empty.
                recordLoop: while (eventQueue.hasNext()) {

                    // Read and parse the next EVIO event.
                    EvioEvent evioEvent = null;
                    try {
                        eventQueue.next();
                        evioEvent = (EvioEvent) eventQueue.getCurrentRecord();         
                        // The parseEvent method does not need to be called here.
                        // The events were already parsed when buffering.
                        if (evioEvent == null) {
                            new RuntimeException().printStackTrace();
                            continue recordLoop;
                        }
                    } catch (IOException e) {
                        // This means the EVIO event has bad data.  
                        logger.severe(e.getMessage());
                        e.printStackTrace();
                        continue recordLoop;
                    } catch (NoSuchRecordException e) {
                        // This means the queue does not have any more events.
                        // We checked hasNext() already so it should not happen.
                        logger.severe(e.getMessage());
                        e.printStackTrace();
                        break recordLoop;
                    }
                    
                    // Print out event XML if enabled.
                    if (printXml) {
                        logger.info(evioEvent.toXML());
                    }

                    // Is this a pre start event?
                    if (EvioEventUtilities.isPreStartEvent(evioEvent)) {

                        // Get the pre start event's data bank.
                        int[] data = EvioEventUtilities.getControlEventData(evioEvent);

                        if (data == null) {
                            // This should never happen but just ignore it.
                            logger.severe("Pre start event is missing a data bank.");
                        } else {
                            // Check if conditions system needs to be updated from the pre start data.
                            checkConditions(data[1], false);                        
                        }
                    } 

                    // Is this an end event?
                    if (EvioEventUtilities.isEndEvent(evioEvent)) {
                        int[] data = EvioEventUtilities.getControlEventData(evioEvent);
                        if (data == null) {
                            // This should never happen but just ignore it.
                            logger.severe("The end event is missing a data bank.");
                        } else {
                            int seconds = data[0];
                            int totalEvents = data[2];
                            logger.info("EVIO end event with " + totalEvents + " events and " + seconds + " seconds");
                        }
                    } 

                    // Setup state in the LCSimEventBuilder based on the EVIO control event.
                    if (eventBuilder != null) {
                        eventBuilder.readEvioEvent(evioEvent);
                    } 

                    // Is this a physics event?
                    if (EvioEventUtilities.isPhysicsEvent(evioEvent)) {

                        // Print physics event number, which is actually a sequence number from
                        // the reader, not the actual event number from the data.
                        logger.finest("physics event seq #" + evioEvent.getEventNumber());

                        // Is the event builder initialized?
                        if (eventBuilder == null) {
                            // Die here, because the event builder should be setup by now.
                            throw new RuntimeException("The LCSimEventBuilder was never initialized.");
                        }

                        // Build the LCIO event.                        
                        EventHeader lcioEvent = eventBuilder.makeLCSimEvent(evioEvent);
                        eventTime = (lcioEvent.getTimeStamp() / 1000000);                           
                        logger.finest("created LCIO event #" + lcioEvent.getEventNumber() + " with time " + new Date(eventTime));
                        if (firstEvent) {
                            logger.info("first physics event time: " + eventTime / 1000 + " - " + new Date(eventTime));
                            firstEvent = false;
                        }

                        // Activate Driver process methods.
                        logger.finest("jobManager processing event " + lcioEvent.getEventNumber());
                        jobManager.processEvent(lcioEvent);

                        // Write out this LCIO event.
                        if (writer != null) {
                            try {
                                writer.write(lcioEvent);
                                writer.flush();
                            } catch (IOException e) {
                                throw new RuntimeException("Error writing LCIO file.", e);
                            }
                            logger.finest("wrote event #" + lcioEvent.getEventNumber());
                        }

                        // Increment number of events processed.
                        nEvents++;                        

                        // Check if max events was reached and end job if this is true.
                        if (maxEvents != -1 && nEvents >= maxEvents) {
                            logger.info("maxEvents " + maxEvents + " was reached");
                            break fileLoop;
                        }
                    }
                }
            } // eventLoop
            logger.info("Last physics event time: " + eventTime / 1000 + " - " + new Date(eventTime));

            // Close the EVIO reader.
            try {
                reader.close();
                logger.fine("EVIO reader closed.");
            } catch (IOException e) {
                logger.warning(e.getMessage());
                e.printStackTrace();
            }
        } // fileLoop
        
        // If the processing stopped because of max events then need to cleanup the reader.
        if (reader != null) {
            if (!reader.isClosed()) {
                // Close the EVIO reader.
                try {
                    reader.close();
                    logger.fine("EVIO reader closed.");
                } catch (IOException e) {
                    logger.warning(e.getMessage());
                    e.printStackTrace();
                }   
            }
        }

        // Trigger endOfData on LCSim Drivers.
        jobManager.finish();

        // Close the LCIO writer.
        if (writer != null) {
            try {
                writer.close();
                logger.info("LCIO output writer closed okay.");
            } catch (IOException e) {
                e.printStackTrace();
                logger.warning(e.getMessage());
            }
        }

        logger.info("Job finished successfully!");
    }

    /**
     * Buffer up to <code>maxBufferSize</code> events in the <code>eventQueue</code>.
     * This method will also initialize the conditions system using a run number
     * if a header bank is found.
     * @param reader The EVIO reader.
     * @param eventQueue The event queue.
     * @param maxBufferSize The maximum number of records to buffer.
     */
    private void bufferEvents(EvioReader reader, EvioEventQueue eventQueue, int maxBufferSize) {
        EvioEvent evioEvent = null;
        while (eventQueue.size() < maxBufferSize) {
            try {
                // Break if no more events from reader.
                if (reader.getNumEventsRemaining() == 0) {
                    break;
                }

                // Read the next event.
                evioEvent = reader.nextEvent();

                if (evioEvent == null) {
                    break;
                }

                // Add the event to the queue.
                eventQueue.addRecord(evioEvent); 

            } catch (IOException | EvioException e) {
                logger.severe(e.getMessage());
                e.printStackTrace();
            }

            if (evioEvent != null) {
                
                // Parse the event.
                try {
                    reader.parseEvent(evioEvent);
                } catch (EvioException e) {
                    logger.severe(e.getMessage());
                    e.printStackTrace();
                    continue;
                }

                // Is conditions system not frozen?
                if (!DatabaseConditionsManager.getInstance().isFrozen()) {
                
                    // Get head bank from event.
                    BaseStructure headBank = EvioEventUtilities.getHeadBank(evioEvent);
                
                    // Is head bank available in this event?
                    if (headBank != null) { 
                                        
                        // Get the run number from the head bank.
                        runNumber = headBank.getIntData()[1];                    
                        logger.finer("got head bank with run number " + runNumber);

                        // Check if the conditions system needs to be updated from the head bank.
                        checkConditions(runNumber, false);                    
                    } else {
                        logger.finest("event " + evioEvent.getEventNumber() + " does not have a head bank");
                    }
                }
            } 
        }
        logger.finer("buffered " + eventQueue.size() + " events");
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
     * Setup the LCSimEventBuilder based on the current detector name and run number.
     * @param detectorName The detector name to be assigned to the event builder.
     * @param runNumber The run number which determines which event builder to use.
     * @return The LCSimEventBuilder for the Test Run or Engineering Run.
     */
    private void setupEventBuilder(int runNumber) {
        // Is this run number from the Test Run?
        if (DatabaseConditionsManager.isTestRun(runNumber)) {
            // Configure conditions system for Test Run.
            logger.info("using LCSimTestRunEventBuilder");
            eventBuilder = new LCSimTestRunEventBuilder();
            if (detectorName == null) {
                this.detectorName = DatabaseConditionsManager.getDefaultTestRunDetectorName();
                logger.info("using default Test Run detector name " + detectorName);
            }
        } else {
            // Configure conditions system for Eng Run or default.
            logger.info("using LCSimEngRunEventBuilder");
            eventBuilder = new LCSimEngRunEventBuilder();
            if (detectorName == null) {
                this.detectorName = DatabaseConditionsManager.getDefaultEngRunDetectorName();
                logger.info("using default Eng Run detector name " + detectorName);
            }
        }
        ConditionsManager conditions = ConditionsManager.defaultInstance();
        conditions.addConditionsListener(eventBuilder);        
    }
    
    /**
     * Check if the conditions system and event builder need to be initialized 
     * or updated given a run number.
     * @param runNumber The run number.
     * @param freeze True to freeze conditions system after it is setup.
     */
    private void checkConditions(int runNumber, boolean freeze) {
                        
        // Is the event builder uninitialized?
        if (eventBuilder == null) {
            // Setup event builder.
            setupEventBuilder(runNumber);
        }
                
        // Update the conditions system with the new run number.       
        try {
            // This call may be ignored by the conditions system if the run number is not new.
            ConditionsManager.defaultInstance().setDetector(detectorName, runNumber);
        } catch (ConditionsNotFoundException e) {
            throw new RuntimeException("Error initializing conditions system.", e);
        }     
        
        if (freeze) {
            // Freeze the conditions system so subsequent run numbers are ignored.
            DatabaseConditionsManager.getInstance().freeze();
        }
    }
}
