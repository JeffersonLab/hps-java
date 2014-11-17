package org.hps.record.evio;

import java.io.File;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

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
 * A utility class for streaming an EVIO file to an ET server.
 * 
 * NOTE: Original version was copied from the CODA group's ET java module.
 */
// TODO: Add option to set number of events in the put array.
public final class EvioFileProducer {

    private List<File> evioFiles = new ArrayList<File>();
    private EvioReader reader;
    private ByteBuffer byteBuffer;
    private String etName, host;
    private int port = EtConstants.serverPort;
    private int group = 1;
    private int size = 10000; // Default event size.
    private int delay = 0;
    private static final boolean debug = false;

    EvioFileProducer() {
    }

    /**
     * Print usage statement.
     */
    private static void usage() {
        System.out.println("\nUsage: java Producer -f <et name> -e <evio file> [-p <server port>] [-host <host>]"
                + " [-d <delay in millisec>] [-g <group #>]\n\n"
                + "       -f     ET system's name\n"
                + "       -s     size in bytes for requested events\n"
                + "       -p     port number for a udp broadcast\n"
                + "       -g     group number of new events to get\n"
                + "       -host  host the ET system resides on (defaults to anywhere)\n\n"
                + "        This consumer works by making a connection to the\n"
                + "        ET system's tcp server port.\n");
        System.exit(1);
    }

    /**
     * Copy byte buffer to an <code>EtEvent</code>.
     * @param event The target EtEvent.
     */
    public void copyToEtEvent(EtEvent event) {
        event.getDataBuffer().put(byteBuffer);
    }

    /**
     * The externally accessible main method.
     * @param args The command line arguments.
     */
    public static void main(String[] args) {
        (new EvioFileProducer()).doMain(args); // call wrapper method
    }

    /**
     * Wrapper method called in main.
     * @param args The command line arguments.
     */
    public void doMain(String[] args) {
        try {
            for (int i = 0; i < args.length; i++) {
                if (args[i].equalsIgnoreCase("-e")) {
                    //evioFileName = new String(args[++i]);
                    evioFiles.add(new File(args[++i]));
                } else if (args[i].equalsIgnoreCase("-f")) {
                    etName = args[++i];
                } else if (args[i].equalsIgnoreCase("-host")) {
                    host = args[++i];
                } else if (args[i].equalsIgnoreCase("-p")) {
                    try {
                        port = Integer.parseInt(args[++i]);
                        if ((port < 1024) || (port > 65535)) {
                            System.out.println("Port number must be between 1024 and 65535.");
                            usage();
                            return;
                        }
                    } catch (NumberFormatException ex) {
                        System.out.println("Did not specify a proper port number.");
                        usage();
                        return;
                    }
                } else if (args[i].equalsIgnoreCase("-s")) {
                    try {
                        size = Integer.parseInt(args[++i]);
                        if (size < 1) {
                            System.out.println("Size needs to be positive int.");
                            usage();
                            return;
                        }
                    } catch (NumberFormatException ex) {
                        System.out.println("Did not specify a proper size.");
                        usage();
                        return;
                    }
                } else if (args[i].equalsIgnoreCase("-g")) {
                    try {
                        group = Integer.parseInt(args[++i]);
                        if ((group < 1) || (group > 10)) {
                            System.out.println("Group number must be between 0 and 10.");
                            usage();
                            return;
                        }
                    } catch (NumberFormatException ex) {
                        System.out.println("Did not specify a proper group number.");
                        usage();
                        return;
                    }
                } else if (args[i].equalsIgnoreCase("-d")) {
                    try {
                        delay = Integer.parseInt(args[++i]);
                        if (delay < 1) {
                            System.out.println("delay must be > 0.");
                            usage();
                            return;
                        }
                    } catch (NumberFormatException ex) {
                        System.out.println("Did not specify a proper delay.");
                        usage();
                        return;
                    }
                } else {
                    usage();
                    return;
                }
            }

            if (host == null) {
                //host = EtConstants.hostAnywhere;
                host = InetAddress.getLocalHost().getHostName();
            }

            // ET name is required.
            if (etName == null) {
                System.out.println("EVIO file name argument is required");
                usage();
                return;
            }
                        
            if (this.evioFiles.size() == 0) {
                System.out.println("At least one input EVIO file is required.");
                usage();
                return;
            }

            // Check existence of EVIO files.
            System.out.println("EVIO input files ...");
            for (File evioFile : evioFiles) {         
                System.out.println(evioFile.getPath());
                if (!evioFile.exists()) {
                    System.err.println("EVIO file does not exist: " + evioFile.getPath());
                    throw new RuntimeException("EVIO input file does not exist.");
                }
            }

            // Setup ET system with the command line config.
            EtSystemOpenConfig config = new EtSystemOpenConfig(etName, host, port);
            EtSystem sys = new EtSystem(config, EtConstants.debugInfo);
            sys.open();
            EtStation gc = sys.stationNameToObject("GRAND_CENTRAL");
            EtAttachment att = sys.attach(gc);

            // array of events
            EtEvent[] mevs;
            
            // Loop over input EVIO file list.
            for (File evioFile : evioFiles) {
                        
                // Open EVIO reader.
                System.out.println("Opening next EVIO file: " + evioFile.getPath());
                reader = new EvioReader(evioFile.getPath(), false);

                // Print number of events.
                if (debug) {
                    System.out.println("EVIO file opened with " + reader.getEventCount() + " events.");
                }

                // Ref to current EVIO event.
                EvioEvent event;

                // Event sequence number; starts with 1.
                int eventCount = 0;
               
                // Loop until event source is exhausted.
                while (true) {

                    // Get next event.
                    event = reader.nextEvent();                   
                    ++eventCount;
                    if (eventCount % 1000 == 0) {
                        System.out.println("EvioFileProducer - event <" + eventCount + ">");
                    }
                    if (event == null) {
                        break;
                    }
                   
                    // Try to parse the next event.  
                    try {
                        reader.parseEvent(event);
                        if (debug) {
                            System.out.println("event #" + event.getEventNumber() + " is " + event.getTotalBytes() + " bytes");
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        System.out.println("Error making EVIO event with sequence number <" + eventCount + "> in file <" + evioFile.getPath() + ">.");
                        // Attempt to recover from errors by skipping to next event if there are exceptions.
                        continue;
                    }

                    if (debug) {
                        System.out.println("new events - size=" + size + "; group=" + group);
                    }

                    // Create a new array of ET events.  This always has one event.
                    mevs = sys.newEvents(
                            att, // attachment
                            Mode.SLEEP, // wait mode
                            false, // create a buffer
                            0, // delay 
                            1, // number of events
                            size, // size of event but overwritten later
                            group); // group number; default value is arbitrary

                    // Delay for X millis if applicable.
                    if (delay > 0) {
                        Thread.sleep(delay);
                    }

                    // Write the next EVIO event to the EtEvent's buffer.
                    ByteBuffer buf = mevs[0].getDataBuffer();
                    buf.order(ByteOrder.nativeOrder());
                    EventWriter writer = new EventWriter(buf, 100000, 100, null, null);
                    writer.writeEvent(event);
                    try {
                        writer.close();
                    } catch (Exception e) {
                        System.out.println("Caught exception while closing writer.");
                        e.printStackTrace();
                    }
                    mevs[0].setLength(buf.position());
                    mevs[0].setByteOrder(ByteOrder.nativeOrder());
                    if (debug) {
                        for (EtEvent mev : mevs) {
                            System.out.println("event length = " + mev.getLength() + ", remaining bytes: " + mev.getDataBuffer().remaining());
                        }
                    }

                    // Put events onto the ET ring.
                    sys.putEvents(att, mevs);

                    if (debug) {
                        System.out.println("Wrote event #" + eventCount + " to ET");
                        System.out.println("-------------------------------");
                        ++eventCount;
                    }
                }

                reader.close();
            }

            // Cleanup.
            sys.close();            
            
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
