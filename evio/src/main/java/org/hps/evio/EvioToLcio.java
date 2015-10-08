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
import org.hps.detector.svt.SvtDetectorSetup;
import org.hps.job.JobManager;
import org.hps.logging.config.DefaultLoggingConfig;
import org.hps.record.LCSimEventBuilder;
import org.hps.record.evio.EvioEventQueue;
import org.hps.record.evio.EvioEventUtilities;
import org.hps.run.database.RunManager;
import org.jlab.coda.jevio.BaseStructure;
import org.jlab.coda.jevio.EvioEvent;
import org.jlab.coda.jevio.EvioException;
import org.jlab.coda.jevio.EvioReader;
import org.lcsim.conditions.ConditionsManager;
import org.lcsim.conditions.ConditionsManager.ConditionsNotFoundException;
import org.lcsim.event.EventHeader;
import org.lcsim.lcio.LCIOWriter;

/**
 * <p>
 * This class converts EVIO to LCIO, performing an LCSim job in the same session. The processed events are then
 * (optionally) written to disk using an LCIOWriter.
 * <p>
 * To run this class from the command line:<br>
 * java -cp hps-distribution-bin.jar EvioToLcio [options] [evioFiles]
 * <p>
 * The available arguments can be printed using:<br>
 * java -cp hps-distribution-bin.jar EvioToLcio -h
 * <p>
 * Extra arguments are treated as paths to EVIO files.
 * <p>
 * This class attempts to automatically configure itself for Test Run or Engineering Run based on the run numbers in the
 * EVIO file. It will use an appropriate default detector unless one is given on the command line, and it will also use
 * the correct event builder. It will not handle jobs correctly with files from both the Test and Engineering Run, so
 * don't do this!
 * <p>
 * The conditions system can be initialized in one of three ways.<br/>
 * <ol>
 * <li>user specified run number in which case the conditions system is frozen for the rest of the job</li>
 * <li>run number from an EVIO pre start event</li>
 * <li>run number from a header bank in an event</li>
 * </ol>
 * <p>
 * In the case where a file has no pre start event and there are header banks present, the "-m" command line option can
 * be used to buffer a number of EVIO events. If there is a head bank found while adding these events to queue, the
 * conditions system will be initialized from it.
 *
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 * @author Sho Uemura <meeg@slac.stanford.edu>
 */
public class EvioToLcio {

    /**
     * The default steering resource, which basically does nothing except print event numbers.
     */
    private static final String DEFAULT_STEERING_RESOURCE = "/org/hps/steering/EventMarker.lcsim";

    /**
     * Setup logging for this class.
     */
    private static Logger LOGGER = Logger.getLogger(EvioToLcio.class.getPackage().getName());

    /**
     * Run the EVIO to LCIO converter from the command line.
     *
     * @param args The command line arguments.
     */
    public static void main(final String[] args) {

        // Initialize default logging settings if no system props are set.
        DefaultLoggingConfig.initialize();
                
        final EvioToLcio evioToLcio = new EvioToLcio();        
        evioToLcio.parse(args);
        evioToLcio.run();
    }

    /**
     * The detector name for conditions.
     */
    private String detectorName;

    /**
     * The LCSim event builder used to convert from EVIO.
     */
    private LCSimEventBuilder eventBuilder = null;

    /**
     * The command line options which will be defined in the constructor.
     */
    private static Options OPTIONS = new Options();
    static {
        OPTIONS.addOption(new Option("d", true, "detector name (required)"));
        OPTIONS.getOption("d").setRequired(true);
        OPTIONS.addOption(new Option("f", true, "text file containing a list of EVIO files"));
        OPTIONS.addOption(new Option("L", true, "log level (INFO, FINE, etc.)"));
        OPTIONS.addOption(new Option("x", true, "LCSim steeering file for processing the LCIO events"));
        OPTIONS.addOption(new Option("r", false, "interpret steering from -x argument as a resource instead of a file"));
        OPTIONS.addOption(new Option("D", true, "define a steering file variable with format -Dname=value"));
        OPTIONS.addOption(new Option("l", true, "path of output LCIO file"));
        OPTIONS.addOption(new Option("R", true, "fixed run number which will override run numbers of input files"));
        OPTIONS.addOption(new Option("n", true, "maximum number of events to process in the job"));
        OPTIONS.addOption(new Option("b", false, "enable headless mode in which plots will not show"));
        OPTIONS.addOption(new Option("v", false, "print EVIO XML for each event"));
        OPTIONS.addOption(new Option("m", true, "set the max event buffer size"));
        OPTIONS.addOption(new Option("t", true, "specify a conditions tag to use"));
        OPTIONS.addOption(new Option("M", false, "use memory mapping instead of sequential reading"));
    }

    /**
     * The run number for conditions.
     */
    private Integer runNumber = null;

    private int maxEvents = -1;
    private int nEvents = 0;
    private int maxBufferSize = 40;
    private List<String> evioFileList = null;
    private boolean printXml = false;
    private boolean useMemoryMapping = false;
    private JobManager jobManager = null;
    private String lcioFileName = null;
    private LCIOWriter writer = null;
    private InputStream steeringStream = null;

    /**
     * The default constructor, which defines command line arguments and sets the default log level.
     */
    public EvioToLcio() {
    }

    /**
     * Buffer up to <code>maxBufferSize</code> events in the <code>eventQueue</code>. This method will also initialize
     * the conditions system using a run number if a header bank is found.
     *
     * @param reader the EVIO reader
     * @param eventQueue the event queue
     * @param maxBufferSize the maximum number of records to buffer
     */
    private void bufferEvents(final EvioReader reader, final EvioEventQueue eventQueue, final int maxBufferSize) {
        EvioEvent evioEvent = null;
        while (eventQueue.size() < maxBufferSize) {
            try {
                // Read the next event.
                evioEvent = reader.nextEvent();

                if (evioEvent == null) { // This catches an end of file or bad event.
                    break;
                }

                // Add the event to the queue.
                eventQueue.addRecord(evioEvent);

            } catch (IOException | EvioException e) {
                LOGGER.log(Level.SEVERE, e.getMessage(), e);
            }

            if (evioEvent != null) {

                // Parse the event.
                try {
                    reader.parseEvent(evioEvent);
                } catch (final EvioException e) {
                    LOGGER.log(Level.SEVERE, e.getMessage(), e);
                    continue;
                }

                // Get head bank from event.
                final BaseStructure headBank = EvioEventUtilities.getHeadBank(evioEvent);

                // Is head bank available in this event?
                if (headBank != null) {

                    // Get the run number from the head bank.
                    runNumber = headBank.getIntData()[1];
                    LOGGER.finer("got head bank with run number " + runNumber);

                    // Is conditions system not frozen?
                    if (!DatabaseConditionsManager.getInstance().isFrozen()) {
                        // Check if the conditions system needs to be updated from the head bank.
                        this.checkConditions(runNumber, false);
                    }
                    RunManager.getRunManager().setRun(runNumber);
                } else {
                    LOGGER.finer("event " + evioEvent.getEventNumber() + " does not have a head bank");
                }
            }
        }
        LOGGER.finer("buffered " + eventQueue.size() + " events");
    }

    /**
     * Check if the conditions system and event builder need to be initialized or updated given a run number.
     *
     * @param runNumber The run number.
     * @param freeze True to freeze conditions system after it is setup.
     */
    private void checkConditions(final int runNumber, final boolean freeze) {

        // Is the event builder uninitialized?
        if (eventBuilder == null) {
            // Setup event builder.
            this.setupEventBuilder(runNumber);
        }

        // Update the conditions system with the new run number.
        try {
            // This call may be ignored by the conditions system if the run number is not new.
            ConditionsManager.defaultInstance().setDetector(detectorName, runNumber);
        } catch (final ConditionsNotFoundException e) {
            throw new RuntimeException("Error initializing conditions system.", e);
        }

        if (freeze) {
            // Freeze the conditions system so subsequent run numbers are ignored.
            DatabaseConditionsManager.getInstance().freeze();
        }
    }

    /**
     * Print the CLI usage and exit.
     */
    private void printUsage() {
        System.out.println("EvioToLcio [options] [evioFiles]");
        final HelpFormatter help = new HelpFormatter();
        help.printHelp(" ", OPTIONS);
        System.exit(1);
    }

    public void parse(String[] args) {
        // Parse the command line options.
        if (args.length == 0) {
            this.printUsage();
        }
        final CommandLineParser parser = new PosixParser();
        CommandLine cl = null;
        try {
            cl = parser.parse(OPTIONS, args);
        } catch (final ParseException e) {
            throw new RuntimeException("Problem parsing command line options.", e);
        }

        // Set the log level.
        if (cl.hasOption("L")) {
            final Level level = Level.parse(cl.getOptionValue("L").toUpperCase());

            // Set log level on this class.
            LOGGER.config("setting log level to " + level);
            LOGGER.setLevel(level);

            // Set log level on conditions manager.
            Logger.getLogger(DatabaseConditionsManager.class.getPackage().getName()).setLevel(level);
        }

        // Add all extra arguments to the EVIO file list.
        evioFileList = new ArrayList<String>(Arrays.asList(cl.getArgs()));

        // Process text file containing list of EVIO file paths, one per line.
        if (cl.hasOption("f")) {
            // Add additional EVIO files to process from text file.
            final File file = new File(cl.getOptionValue("f"));
            if (!file.exists()) {
                throw new RuntimeException("The file " + file.getPath() + " does not exist.");
            }
            final Path filePath = file.toPath();
            List<String> lines = null;
            try {
                lines = Files.readAllLines(filePath, Charset.defaultCharset());
            } catch (final IOException e) {
                throw new RuntimeException(e);
            }
            for (final String line : lines) {
                final File evioFile = new File(line);
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
            LOGGER.severe("No EVIO files were provided with command line arguments or -f option.");
            this.printUsage();
        }

        // Get the LCIO output file.
        if (cl.hasOption("l")) {
            LOGGER.config("set LCIO file to " + lcioFileName);
            lcioFileName = cl.getOptionValue("l");
        }

        // Get the steering file or resource for the LCSim job.
        if (cl.hasOption("x")) {
            final String lcsimXmlName = cl.getOptionValue("x");
            if (cl.hasOption("r")) {
                steeringStream = EvioToLcio.class.getResourceAsStream(lcsimXmlName);
                if (steeringStream == null) {
                    final IllegalArgumentException e = new IllegalArgumentException(
                            "XML steering resource was not found.");
                    LOGGER.log(Level.SEVERE, "LCSim steering resource " + lcsimXmlName + " was not found.", e);
                    throw e;
                }
                LOGGER.config("using steering resource " + lcsimXmlName);
            } else {
                try {
                    steeringStream = new FileInputStream(lcsimXmlName);
                    LOGGER.config("using steering file " + lcsimXmlName);
                } catch (final FileNotFoundException e) {
                    LOGGER.severe("The XML steering file " + lcsimXmlName + " does not exist.");
                    throw new IllegalArgumentException("Steering file does not exist.", e);
                }
            }
        }

        // Setup the default steering which just prints event numbers.
        if (steeringStream == null) {
            steeringStream = EvioToLcio.class.getResourceAsStream(DEFAULT_STEERING_RESOURCE);
            LOGGER.config("using default steering resource " + DEFAULT_STEERING_RESOURCE);
        }

        // Get the max number of events to process.
        if (cl.hasOption("n")) {
            maxEvents = Integer.valueOf(cl.getOptionValue("n"));
            if (maxEvents <= 0) {
                throw new IllegalArgumentException("Value of -n option is invalid: " + maxEvents);
            }
            LOGGER.config("set max events to " + maxEvents);
        }

        // Setup the LCIO writer.
        if (lcioFileName != null) {
            try {
                writer = new LCIOWriter(lcioFileName);
                LOGGER.config("initialized LCIO writer with file " + lcioFileName);
            } catch (final IOException e) {
                LOGGER.severe("Problem initializing the LCIO writer.");
                throw new RuntimeException(e);
            }
        }

        // Process the LCSim job variable definitions, if any.
        jobManager = new JobManager();
        if (cl.hasOption("D")) {
            final String[] steeringOptions = cl.getOptionValues("D");
            for (final String def : steeringOptions) {
                final String[] s = def.split("=");
                if (s.length != 2) {
                    final IllegalArgumentException e = new IllegalArgumentException("Bad variable format: " + def);
                    LOGGER.log(Level.SEVERE, "bad variable format: " + def, e);
                    throw e;
                }
                final String key = s[0];
                final String value = s[1];
                jobManager.addVariableDefinition(key, value);
                LOGGER.config("set steering variable: " + key + "=" + value);
            }
        }

        // Enable headless mode so no plots are shown.
        if (cl.hasOption("b")) {
            LOGGER.config("Headless mode is enabled.  No plots will be shown.");
            jobManager.enableHeadlessMode();
        }

        // Configure the LCSim job manager.
        jobManager.setup(steeringStream);
        jobManager.configure();
        LOGGER.config("LCSim job manager was successfully configured.");

        // Get the user specified detector name.
        if (cl.hasOption("d")) {
            detectorName = cl.getOptionValue("d");
            LOGGER.config("User set detector to " + detectorName + " with command option.");
        } else {
            throw new RuntimeException("Missing -d argument with name of detector.");
        }

        // Get the user specified run number.
        if (cl.hasOption("R")) {
            runNumber = Integer.parseInt(cl.getOptionValue("R"));
            LOGGER.config("User set run number to " + runNumber + " with command option.");
        }

        // Add conditions system tag filters.
        if (cl.hasOption("t")) {
            final String[] tags = cl.getOptionValues("t");
            for (final String tag : tags) {
                LOGGER.config("adding conditions tag " + tag);
                DatabaseConditionsManager.getInstance().addTag(tag);
            }
        }

        // Print out the EVIO file list before the job starts.
        final StringBuffer buff = new StringBuffer();
        buff.append("The job will include the following EVIO files ...");
        buff.append('\n');
        for (final String evioFileName : evioFileList) {
            buff.append(evioFileName);
            buff.append('\n');
        }
        LOGGER.config(buff.toString());

        // Get whether to debug print XML from the EVIO events.
        if (cl.hasOption("v")) {
            printXml = true;
            LOGGER.config("print XML enabled");
        }

        // Get the maximum number of EVIO events to buffer.
        if (cl.hasOption("m")) {
            maxBufferSize = Integer.parseInt(cl.getOptionValue("m"));
            LOGGER.config("max event buffer size set to " + maxBufferSize);
        }

        if (cl.hasOption("M")) {
            useMemoryMapping = true;
            LOGGER.config("EVIO reader memory mapping is enabled.");
        }
    }

    /**
     * This method will execute the EVIO to LCIO conversion and optionally process the events with LCSim Drivers from a
     * steering file. Then the resultant LCIO events will be written to disk if this option is enabled in the command
     * line arguments.
     *
     * @param args The command line arguments.
     */
    public void run() {

        // Register class for setting up SVT detector state from conditions data.
        DatabaseConditionsManager.getInstance().addConditionsListener(new SvtDetectorSetup());

        // Is there a run number from the command line options?
        if (runNumber != null) {
            // Initialize the conditions system before the job starts and freeze it.
            this.checkConditions(runNumber, true);
        }

        // Loop over the input EVIO files.
        EvioReader reader = null;
        fileLoop: for (final String evioFileName : evioFileList) {

            // Get the next EVIO input file.
            final File evioFile = new File(evioFileName);
            if (!evioFile.exists()) {
                throw new RuntimeException("EVIO file " + evioFile.getPath() + " does not exist.");
            }
            LOGGER.info("Opening EVIO file " + evioFileName);

            // Open the EVIO reader.
            try {
                reader = new EvioReader(evioFile, false, !useMemoryMapping);
            } catch (final Exception e) {
                throw new RuntimeException("Error opening the EVIO file reader.", e);
            }

            boolean firstEvent = true;
            long eventTime = 0; // in ms

            // Loop over events.
            final EvioEventQueue eventQueue = new EvioEventQueue(-1L, maxBufferSize);
            eventLoop: for (;;) {

                // Buffer the EVIO events into the queue.
                this.bufferEvents(reader, eventQueue, maxBufferSize);

                LOGGER.fine("buffered " + eventQueue.size() + " events");
                
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
                            new RuntimeException("Failed to read EVIO event.").printStackTrace();
                            continue recordLoop;
                        }
                        LOGGER.finer("processing EVIO event " + evioEvent.getEventNumber());
                    } catch (final IOException e) {
                        // This means the EVIO event has bad data.
                        LOGGER.log(Level.SEVERE, e.getMessage(), e);
                        continue recordLoop;
                    } catch (final NoSuchRecordException e) {
                        // This means the queue does not have any more events.
                        // We checked hasNext() already so it should not happen.
                        LOGGER.log(Level.SEVERE, e.getMessage(), e);
                        break recordLoop;
                    }

                    // Print out event XML if enabled.
                    if (printXml) {
                        LOGGER.info(evioEvent.toXML());
                    }

                    // Is this a pre start event?
                    if (EvioEventUtilities.isPreStartEvent(evioEvent)) {

                        LOGGER.info("got PRESTART event");
                        
                        // Get the pre start event's data bank.
                        final int[] data = EvioEventUtilities.getControlEventData(evioEvent);

                        if (data == null) {
                            // This should never happen but just ignore it.
                            LOGGER.severe("Pre start event is missing a data bank.");
                        } else {
                            // Check if conditions system needs to be updated from the pre start data.
                            this.checkConditions(data[1], false);
                        }
                    }

                    // Is this an end event?
                    if (EvioEventUtilities.isEndEvent(evioEvent)) {
                        
                        LOGGER.info("got END event");
                        
                        final int[] data = EvioEventUtilities.getControlEventData(evioEvent);
                        if (data == null) {
                            // This should never happen but just ignore it.
                            LOGGER.severe("The end event is missing a data bank.");
                        } else {
                            final int seconds = data[0];
                            final int totalEvents = data[2];
                            LOGGER.info("EVIO end event with " + totalEvents + " events and " + seconds + " seconds");
                        }
                    }

                    // Setup state in the LCSimEventBuilder based on the EVIO control event.
                    if (eventBuilder != null) {
                        LOGGER.finer("event builder reading in EVIO event " + evioEvent.getEventNumber());
                        eventBuilder.readEvioEvent(evioEvent);
                    }

                    // Is this a physics event?
                    if (EvioEventUtilities.isPhysicsEvent(evioEvent)) {

                        // Print physics event number, which is actually a sequence number from
                        // the reader, not the actual event number from the data.
                        LOGGER.finer("got physics event " + evioEvent.getEventNumber());

                        // Is the event builder initialized?
                        if (eventBuilder == null) {
                            // Die here, because the event builder should be setup by now.
                            throw new RuntimeException("The LCSimEventBuilder was never initialized.");
                        }

                        // Build the LCIO event.
                        final EventHeader lcioEvent = eventBuilder.makeLCSimEvent(evioEvent);
                        eventTime = lcioEvent.getTimeStamp() / 1000000;
                        LOGGER.finer("created LCIO event " + lcioEvent.getEventNumber() + " with timestamp " + eventTime);
                        if (firstEvent) {
                            LOGGER.info("first physics event time: " + eventTime / 1000 + " - " + new Date(eventTime));
                            firstEvent = false;
                        }

                        // Activate Driver process methods.
                        LOGGER.finer("Job manager processing event " + lcioEvent.getEventNumber());
                        jobManager.processEvent(lcioEvent);

                        // Write out this LCIO event.
                        if (writer != null) {
                            try {
                                writer.write(lcioEvent);
                                writer.flush();
                                LOGGER.finer("wrote LCSim event " + lcioEvent.getEventNumber());
                            } catch (final IOException e) {
                                throw new RuntimeException("Error writing LCIO file.", e);
                            }
                            LOGGER.finer("wrote event #" + lcioEvent.getEventNumber());
                        }

                        // Increment number of events processed.
                        nEvents++;

                        // Check if max events was reached and end job if this is true.
                        if (maxEvents != -1 && nEvents >= maxEvents) {
                            LOGGER.info("maxEvents " + maxEvents + " was reached");
                            break fileLoop;
                        }
                    }
                }
            } // eventLoop
            LOGGER.info("Last physics event time: " + eventTime / 1000 + " - " + new Date(eventTime));

            // Close the EVIO reader.
            try {
                reader.close();
                LOGGER.fine("EVIO reader closed.");
            } catch (final IOException e) {
                LOGGER.warning(e.getMessage());
                e.printStackTrace();
            }
        } // fileLoop

        // If the processing stopped because of max events then need to cleanup the reader.
        if (reader != null) {
            if (!reader.isClosed()) {
                // Close the EVIO reader.
                try {
                    reader.close();
                    LOGGER.fine("EVIO reader closed.");
                } catch (final IOException e) {
                    LOGGER.log(Level.WARNING, e.getMessage(), e);
                }
            }
        }

        // Trigger endOfData on LCSim Drivers.
        jobManager.finish();

        // Close the LCIO writer.
        if (writer != null) {
            try {
                writer.close();
                LOGGER.info("LCIO output writer closed okay.");
            } catch (final IOException e) {
                e.printStackTrace();
                LOGGER.warning(e.getMessage());
            }
        }

        LOGGER.info("Job finished successfully!");
    }

    /**
     * Setup the LCSimEventBuilder based on the current detector name and run number.
     *
     * @param detectorName The detector name to be assigned to the event builder.
     * @param runNumber The run number which determines which event builder to use.
     * @return The LCSimEventBuilder for the Test Run or Engineering Run.
     */
    private void setupEventBuilder(final int runNumber) {
        // Is this run number from the Test Run?
        if (DatabaseConditionsManager.isTestRun(runNumber)) {
            // Configure conditions system for Test Run.
            LOGGER.info("using Test Run event builder");
            eventBuilder = new LCSimTestRunEventBuilder();
        } else {
            // Configure conditions system for Eng Run or default.
            LOGGER.info("using Eng Run event builder");
            eventBuilder = new LCSimEngRunEventBuilder();
        }
        final ConditionsManager conditions = ConditionsManager.defaultInstance();
        conditions.addConditionsListener(eventBuilder);
    }
}
