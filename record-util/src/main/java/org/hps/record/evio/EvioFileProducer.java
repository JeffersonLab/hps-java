package org.hps.record.evio;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.jlab.coda.et.EtAttachment;
import org.jlab.coda.et.EtConstants;
import org.jlab.coda.et.EtEvent;
import org.jlab.coda.et.EtStation;
import org.jlab.coda.et.EtSystem;
import org.jlab.coda.et.EtSystemOpenConfig;
import org.jlab.coda.et.enums.Mode;
import org.jlab.coda.jevio.EventWriter;
import org.jlab.coda.jevio.EvioEvent;
import org.jlab.coda.jevio.EvioReader;

/**
 * A command line utility for streaming EVIO files to an ET server.
 * <p>
 * The original version was copied from the CODA ET module and modified.
 *
 * @author <a href="mailto:jeremym@slac.stanford.edu">Jeremy McCormick</a>
 */
public final class EvioFileProducer {

    private static final String GRAND_CENTRAL = "GRAND_CENTRAL";

    /**
     * Default event count for printing event sequence number.
     */
    private static final int DEFAULT_COUNT = 1000;

    /**
     * Default delay time in milliseconds between events.
     */
    private static final int DEFAULT_DELAY = 0;

    /**
     * The default ET group.
     */
    private static final int DEFAULT_ET_GROUP = 1;

    /**
     * Default event buffer size (200 KB).
     */
    private static final int DEFAULT_EVENT_SIZE = 200000;

    /**
     * Default host name.
     */
    private static final String DEFAULT_HOST = "localhost";

    /**
     * Default ET system name.
     */
    private static final String DEFAULT_NAME = "ETBuffer";

    /**
     * Maximum port number of ET server (maximum value of a TCP/IP port).
     */
    private static final int ET_PORT_MAX = 65535;

    /**
     * Minimum port number of ET server (lower port numbers not allowed).
     */
    private static final int ET_PORT_MIN = 1024;
   
    /**
     * Initialize the logger.
     */
    private static final Logger LOGGER = Logger.getLogger(EvioFileProducer.class.getPackage().getName());

    /**
     * The command line options.
     */
    private static final Options OPTIONS = new Options();

    /**
     * Define the command line options.
     */
    static {
        OPTIONS.addOption("", "help", false, "print help");
        OPTIONS.addOption("e", "event", true, "interval for printing event numbers");
        OPTIONS.addOption("d", "delay", true, "delay in milliseconds between events");
        OPTIONS.addOption("f", "name", true, "ET system name which should be a buffer file");
        OPTIONS.addOption("g", "group", true, "group number of the events");
        OPTIONS.addOption("h", "host", true, "server host name");
        OPTIONS.addOption("l", "list", true, "text file with list of EVIO files");
        OPTIONS.addOption("p", "port", true, "server port");
        OPTIONS.addOption("s", "size", true, "event buffer size in bytes");
    }

    /**
     * The externally accessible main method.
     *
     * @param args the command line arguments
     */
    public static void main(final String[] args) {
        EvioFileProducer efp = new EvioFileProducer();
        try {
            efp.parse(args);
        } catch (Exception e) {
            throw new RuntimeException("Error parsing command line", e);
        }
        efp.run();
    }

    /**
     * Print usage statement and exit.
     */
    private static void printUsage() {
        final HelpFormatter help = new HelpFormatter();
        help.printHelp(80, "EvioFileProducer [options] file.evio.0 file.evio.1 [...]", "EVIO File Producer", OPTIONS, 
                "Provide EVIO files as extra arguments or using the -l option to provide a file containing a list of them.");
        System.exit(1);
    }

    /**
     * Event count for printing message.
     */
    private int eventPrintInterval = DEFAULT_COUNT;

    /**
     * A delay in milliseconds between put operations.
     */
    private int delay = DEFAULT_DELAY;

    /**
     * The ET system name which maps to a buffer file.
     */
    private String etName = DEFAULT_NAME;

    /**
     * The master list of input EVIO files to stream.
     */
    private final List<File> evioFiles = new ArrayList<File>();

    /**
     * The ET group (default is 1).
     */
    private int group = DEFAULT_ET_GROUP;

    /**
     * The server host name (default is localhost).
     */
    private String host = DEFAULT_HOST;

    /**
     * The server's network port (default is standard ET server port).
     */
    private int port = EtConstants.serverPort;

    /**
     * The default ET event size (default is 200 KB).
     */
    private int size = DEFAULT_EVENT_SIZE;

    /**
     * Class constructor.
     */
    private EvioFileProducer() {
    }

    /**
     * Print the command line job configuration to the log.
     */
    private void logConfig() {
        final StringBuffer sb = new StringBuffer();
        sb.append("count: " + this.eventPrintInterval + '\n');
        sb.append("delay: " + this.delay + '\n');
        sb.append("etName: " + this.etName + '\n');
        sb.append("group: " + this.group + '\n');
        sb.append("host: " + this.host + '\n');
        sb.append("port: " + this.port + '\n');
        sb.append("size: " + this.size + '\n');
        sb.append("EVIO files: " + '\n');
        for (final File evioFile : this.evioFiles) {
            sb.append(evioFile.getPath() + '\n');
        }
        LOGGER.config(sb.toString());
    }

    /**
     * Run the job by streaming all the EVIO files to the ET server using the command line arguments.
     *
     * @param args the command line arguments
     */
    private void parse(final String[] args) throws ParseException {

        // Command line parser.
        final DefaultParser parser = new DefaultParser();

        // Parse the command line arguments.
        final CommandLine cl = parser.parse(OPTIONS, args);

        // Print usage and exit.
        if (cl.hasOption("help")) {
            printUsage();
            System.exit(0);
        }
        
        // Add EVIO files to the job from extra args.
        List<String> extraArgs = cl.getArgList();
        for (final String extraArg : extraArgs) {
            final File evioFile = new File(extraArg);
            LOGGER.config("Adding EVIO file: " + evioFile.getPath());
            this.evioFiles.add(evioFile);
        }

        // Set ET name which is the buffer file.
        if (cl.hasOption("f")) {
            this.etName = cl.getOptionValue("f");
        }

        // Add EVIO files from a text file list, assuming one file path per line.
        // Option cannot be used when providing a list of EVIO files on the command line
        // or an exception will be thrown.
        if (cl.hasOption("l")) {
            if (cl.getArgList().size() > 0) {
                throw new RuntimeException("The -l option is not allowed with EVIO files on the command line.");
            }
            final String filePath = cl.getOptionValue("l");
            final File listFile = new File(filePath);
            if (!listFile.exists()) {
                throw new IllegalArgumentException("The file " + listFile.getPath() + " does not exist.");
            }
            BufferedReader br = null;
            try {
                br = new BufferedReader(new InputStreamReader(new FileInputStream(listFile)));
                String line;
                while ((line = br.readLine()) != null) {
                    this.evioFiles.add(new File(line.trim()));
                }
            } catch (IOException e) {
                throw new RuntimeException("Error reading EVIO file list", e);
            } finally {
                if (br != null) {
                    try {
                        br.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        // Set host name.
        if (cl.hasOption("h")) {
            this.host = cl.getOptionValue("h");
        }
        // if (this.host == null) {
        // host = EtConstants.hostAnywhere;
        // this.host = InetAddress.getLocalHost().getHostName();
        // }

        // Set the port number.
        if (cl.hasOption("p")) {
            this.port = Integer.parseInt(cl.getOptionValue("p"));
            if (this.port < ET_PORT_MIN || this.port > ET_PORT_MAX) {
                throw new IllegalArgumentException("Port number must be between 1024 and 65535.");
            }
        }
        // Set the size of the event buffer in bytes.
        if (cl.hasOption("s")) {
            this.size = Integer.parseInt(cl.getOptionValue("s"));
            if (this.size < 1) {
                throw new IllegalArgumentException("Size needs to be a positive int.");
            }
        }

        // Set the group number.
        if (cl.hasOption("g")) {
            this.group = Integer.parseInt(cl.getOptionValue("g"));
            if (this.group < 1 || this.group > 10) {
                throw new IllegalArgumentException("Group number must be between 0 and 10.");
            }
        }

        // Set the delay in milliseconds between putting events.
        if (cl.hasOption("d")) {
            this.delay = Integer.parseInt(cl.getOptionValue("d"));
            if (this.delay < 1) {
                throw new IllegalArgumentException("The delay must be > 0.");
            }
        }

        if (cl.hasOption("e")) {
            this.eventPrintInterval = Integer.parseInt(cl.getOptionValue("e"));
            if (this.eventPrintInterval < 1) {
                throw new IllegalArgumentException("The count must be > 0.");
            }
        }

        // At least one EVIO file must be present from parsing command line arguments.
        if (this.evioFiles.size() == 0) {
            throw new RuntimeException("At least one EVIO file is required.");
        }

        // Check existence of EVIO files.
        LOGGER.info("Checking EVIO files");
        if (this.evioFiles.size() == 0) {
            throw new RuntimeException("No EVIO files were given by command line options!");
        }
        for (final File evioFile : this.evioFiles) {
            if (!evioFile.exists()) {
                throw new IllegalArgumentException("EVIO file does not exist: " + evioFile.getPath());
            }
        }

        // Print out the configuration for the job to the log.
        logConfig();        
    }
    
    private void run() {
        
        EtSystem sys = null;
        EvioReader reader = null;
        
        try {

            // Setup ET system from the command line options.
            LOGGER.info("Opening ET system on host " + this.host + ", port " + this.port + " and file " + this.etName);
            final EtSystemOpenConfig config = new EtSystemOpenConfig(this.etName, this.host, this.port);
            sys = new EtSystem(config, EtConstants.debugInfo);
            sys.open();
            final EtStation gc = sys.stationNameToObject(GRAND_CENTRAL);
            final EtAttachment att = sys.attach(gc);

            // Array of ET events.
            EtEvent[] mevs;

            // Loop over input EVIO file list.
            for (final File evioFile : this.evioFiles) {

                // Open a new EVIO reader in sequential read mode so events are immediately streamed to server.
                LOGGER.info("Opening next EVIO file " + evioFile.getPath());
                reader = new EvioReader(evioFile.getPath(), false, true);

                // Reference to the current EVIO event.
                EvioEvent event;

                // Event sequence number.
                int eventCount = 0;

                // Loop until event source is exhausted.
                while ((event = reader.nextEvent()) != null) {

                    // Increment event count.
                    ++eventCount;

                    // Print event sequence.
                    if (eventCount % this.eventPrintInterval == 0) {
                        LOGGER.info("EVIO event " + eventCount);
                    }

                    try {
                        // Parse the next EVIO event.
                        reader.parseEvent(event);
                        LOGGER.finest("EVIO event " + event.getEventNumber() + " is " + event.getTotalBytes()
                                + " bytes.");
                    } catch (final Exception e) { /* Catches parse errors reading the EVIO events. */
                        e.printStackTrace();
                        LOGGER.severe("Error making EVIO event with seq number " + eventCount + " in file "
                                + evioFile.getPath());
                        // Attempt to recover from errors by skipping to next event if there are exceptions.
                        continue;
                    }

                    //LOGGER.finest("New events: size=" + this.size + "; group=" + this.group);

                    final int eventTag = EvioEventUtilities.getEventTag(event);

                    // Create a new array of ET events. This always has one event.
                    mevs = sys.newEvents(att, // attachment
                            Mode.SLEEP, // wait mode
                            false, // create a buffer
                            0, // delay
                            1, // number of events
                            this.size, // size of event but overwritten later
                            this.group); // group number; default value is arbitrary

                    // Create control data array for event selection.
                    final int[] control = new int[EtConstants.stationSelectInts];
                    Arrays.fill(control, eventTag);
                    mevs[0].setControl(control);

                    // Apply delay in milliseconds.
                    if (this.delay > 0) {
                        Thread.sleep(this.delay);
                    }

                    // Write the next EVIO event to the EtEvent's buffer.
                    final ByteBuffer buf = mevs[0].getDataBuffer();
                    buf.order(ByteOrder.nativeOrder());
                    final EventWriter writer = new EventWriter(buf, 100000, 100, null, null);
                    writer.writeEvent(event);
                    try {
                        writer.close();
                    } catch (final Exception e) {
                        LOGGER.log(Level.WARNING, "Error while closing writer.", e);
                    }
                    mevs[0].setLength(buf.position());
                    mevs[0].setByteOrder(ByteOrder.nativeOrder());

                    for (final EtEvent mev : mevs) {
                        LOGGER.finest("event length = " + mev.getLength() + ", remaining bytes: "
                                + mev.getDataBuffer().remaining());
                    }

                    // Put events onto the ET ring.
                    sys.putEvents(att, mevs);

                    LOGGER.finest("Sucessfully wrote " + eventCount + " event to ET which was EVIO event number "
                            + event.getEventNumber() + " from file " + evioFile.getPath() + ".");
                }
                LOGGER.info(eventCount + " events were read from " + evioFile.getPath());
                reader.close();
            }

        } catch (final Exception e) { /* Catches all event processing errors. */
            // This catches and re-throws all errors from processing the EVIO events and configuring the ET system.
            throw new RuntimeException("Error streaming EVIO events to ET system", e);
        } finally {
            // Cleanup the EVIO reader if needed.
            if (reader != null && !reader.isClosed()) {
                try {
                    reader.close();
                } catch (final IOException e) {
                    LOGGER.log(Level.WARNING, e.getMessage(), e);
                }
            }
            // Cleanup the ET system.
            if (sys != null && sys.alive()) {
                sys.close();
            }
        }

        LOGGER.info("Done!");
    }
}
