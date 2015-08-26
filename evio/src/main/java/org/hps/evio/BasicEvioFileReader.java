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
import org.hps.record.triggerbank.AbstractIntData.IntBankDefinition;
import org.hps.record.triggerbank.HeadBankData;
import org.hps.record.triggerbank.TIData;
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
        options.addOption(new Option("t", false, "print event timestamps"));
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
        boolean printTimestamps = cl.hasOption("t");

        IntBankDefinition headBankDefinition = new IntBankDefinition(HeadBankData.class, new int[]{0x2e, 0xe10f});
        IntBankDefinition tiBankDefinition = new IntBankDefinition(TIData.class, new int[]{0x2e, 0xe10a});

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
                int[] lastData = new int[]{0, 0, 0, 0, 0};
                long minDelta = 0, maxDelta = 0;
                long lastTI = 0;
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

                        if (printTimestamps) {
                            int thisTimestamp = 0;
                            BaseStructure headBank = headBankDefinition.findBank(event);
                            if (headBank != null) {
                                int[] data = headBank.getIntData();
                                thisTimestamp = data[3];
                                if (data[3] != 0) {
                                    if (lastData[3] == 0) {
                                        System.out.print("first_head\t");
                                        printInts(data);
                                    }
                                    lastData = data;
                                }
                            }
                            BaseStructure tiBank = tiBankDefinition.findBank(event);
                            if (tiBank != null) {
                                TIData tiData = new TIData(tiBank.getIntData());
                                if (lastTI == 0) {
                                    System.out.format("first_TItime\t%d\n", tiData.getTime());
                                }
                                lastTI = tiData.getTime();
                                if (thisTimestamp != 0) {
                                    long delta = thisTimestamp * 1000000000L - tiData.getTime();
                                    if (minDelta == 0 || minDelta > delta) {
                                        minDelta = delta;
                                    }
                                    if (maxDelta == 0 || maxDelta < delta) {
                                        maxDelta = delta;
                                    }
//                                    System.out.format("%d %d %d %d %d %d\n", thisTimestamp, tiData.getTime(), delta, minDelta, maxDelta, maxDelta-minDelta);
                                }
                            }
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
                if (printTimestamps) {
                    System.out.print("last_head\t");
                    printInts(lastData);
                    System.out.format("last_TItime\t%d\n", lastTI);
                    System.out.format("ti_offset\t%d\t%d\t%d\n", minDelta, maxDelta, maxDelta - minDelta);
                }
                reader.close();
            } catch (Exception e) {
//                throw new RuntimeException(e);
                e.printStackTrace();
            }
        }
    }

    private static void printInts(int[] data) {
        for (int i = 0; i < data.length; i++) {
            System.out.format("%d\t", data[i]);
        }
        System.out.println();
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
