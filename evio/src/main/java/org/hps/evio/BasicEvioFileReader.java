package org.hps.evio;

import java.io.File;
import java.util.Date;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.hps.record.evio.EvioEventUtilities;
import org.jlab.coda.jevio.BaseStructure;
import org.jlab.coda.jevio.CompositeData;
import org.jlab.coda.jevio.DataType;
import org.jlab.coda.jevio.EvioEvent;
import org.jlab.coda.jevio.EvioException;

public class BasicEvioFileReader {

    static public void main(String[] args) {

        Options options = new Options();
        options.addOption(new Option("q", false, "quiet - don't print event contents"));
        options.addOption(new Option("c", false, "print control events"));
        options.addOption(new Option("s", false, "sequential read (not mem-mapped)"));

        // Parse the command line options.
        if (args.length == 0) {
            printUsage(options);
        }
        final CommandLineParser parser = new PosixParser();
        CommandLine cl = null;
        try {
            cl = parser.parse(options, args);
        } catch (final ParseException e) {
            throw new RuntimeException("Problem parsing command line options.", e);
        }

        if (cl.getArgs().length < 1) {
            throw new RuntimeException("Missing EVIO file name.");
        }

        boolean quiet = cl.hasOption("q");
        boolean printControlEvents = cl.hasOption("c");
        boolean seqRead = cl.hasOption("s");

//        String evioFileName = args[0];
        for (String evioFileName : cl.getArgs()) {
            File evioFile = new File(evioFileName);
            if (!evioFile.exists()) {
                throw new RuntimeException("File " + evioFileName + " does not exist.");
            }
            System.out.println("Opened file " + evioFileName);
            try {
                org.jlab.coda.jevio.EvioReader reader = new org.jlab.coda.jevio.EvioReader(evioFile, true, seqRead);
                int eventN = 1;
                int badEvents = 0;
                fileLoop:
                while (true) {
                    if (!quiet) {
                        System.out.println("Reading event " + eventN);
                    }
                    try {
                        EvioEvent event = reader.nextEvent();
                        if (event == null) {
                            break fileLoop;
                        }
                        reader.parseEvent(event);
                        //printBytes(event.getRawBytes()); // DEBUG
                        if (!quiet) {
                            System.out.println("Successfully read event " + eventN);// + " which contains " + event.getTotalBytes() + " bytes.");
                            printBank(event, "");
                        }
                        if (printControlEvents && EvioEventUtilities.isControlEvent(event) && !EvioEventUtilities.isEpicsEvent(event)) {
                            int[] controlEventData = EvioEventUtilities.getControlEventData(event);
                            if (controlEventData == null) {
                                printBank(event, "");
                            }
                            System.out.print(event.getHeader().getTag() + "\t");

                            for (int i = 0; i < controlEventData.length; i++) {
                                System.out.print(controlEventData[i] + "\t");
                            }
                            Date timestamp = new Date(controlEventData[0] * 1000L);
                            System.out.println(timestamp);
                        }
                    } catch (Exception e) {
                        System.out.println("Caught Exception processing event " + eventN + " which was...");
                        e.printStackTrace();
                        ++badEvents;
                    }
                    ++eventN;
                    if (!quiet) {
                        System.out.println("-------");
                    }
                }
                System.out.println("There were " + badEvents + " bad events out of " + eventN + " total.");
                reader.close();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static void printUsage(Options options) {
        System.out.println("BasicEvioFileReader [options] [evioFiles]");
        final HelpFormatter help = new HelpFormatter();
        help.printHelp(" ", options);
        System.exit(1);
    }

    private static void printBank(BaseStructure bank, String indent) throws EvioException {
        System.out.println(indent + "Bank contains " + bank.getTotalBytes() + " bytes.");
        System.out.println(indent + "Bank has " + bank.getChildCount() + " sub-banks.");
        System.out.format(indent + "Bank tag: 0x%x length: %d type: %s num: %d\n", bank.getHeader().getTag(), bank.getHeader().getLength(), bank.getHeader().getDataType(), bank.getHeader().getNumber());
        if (bank.getChildCount() > 0) {
            for (BaseStructure child : bank.getChildrenList()) {
                printBank(child, indent + "\t");
            }
        }
        if (bank.getHeader().getDataType() == DataType.COMPOSITE) {
//            for (CompositeData cdata : bank.getCompositeData()) {
            CompositeData cdatalist[] = bank.getCompositeData();
            for (CompositeData cdata : cdatalist) {
                switch (bank.getHeader().getTag()) {
                    case 0xe101:
                        printWindow(cdata, indent + "\t");
                        break;
                    case 0xe102:
                        printComposite(cdata, indent + "\t");
                        break;
                    case 0xe103:
                        printComposite(cdata, indent + "\t");
                        break;
                }
            }
//            }
        }
        if (bank.getHeader().getDataType() == DataType.UINT32) {
            int[] data = bank.getIntData();
            if (data.length < 100) {
                for (int i = 0; i < data.length; i++) {
                    System.out.format(indent + "0x%x\n", data[i]);
                }
            }
        }
    }

    private static void printComposite(CompositeData cdata, String indent) {
        System.out.println(indent + "Raw byte count: " + cdata.getRawBytes().length);
        System.out.println(cdata.toString(indent));
    }

    private static void printWindow(CompositeData cdata, String indent) {
        while (cdata.index() + 1 < cdata.getItems().size()) {
            System.out.println(indent + "Byte count: " + cdata.getRawBytes().length);
            System.out.println(indent + "Slot: " + cdata.getByte());
            System.out.println(indent + "Trigger: " + cdata.getInt());
            System.out.println(indent + "Timestamp: " + cdata.getLong());
            int nchannels = cdata.getNValue();
            System.out.println(indent + "NChannels: " + nchannels);
            for (int j = 0; j < nchannels; j++) {
                System.out.println(indent + "Channel: " + cdata.getByte());
                int nSamples = cdata.getNValue();
                System.out.println(indent + "NSamples: " + nSamples);
                for (int i = 0; i < nSamples; i++) {
                    cdata.getShort();
                }
            }
        }
    }

    /*
     * private static void printBytes(final byte[] bytes) { for (int i=0;
     * i<bytes.length; i++) { if (i%4==0) System.out.println();
     * System.out.println(Byte.toString(bytes[i])); } }
     */
}
