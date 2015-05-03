package org.hps.evio;

import java.io.File;

import org.jlab.coda.jevio.BaseStructure;
import org.jlab.coda.jevio.CompositeData;
import org.jlab.coda.jevio.DataType;
import org.jlab.coda.jevio.EvioEvent;
import org.jlab.coda.jevio.EvioException;

public class BasicEvioFileReader {

    static public void main(String[] args) {
        if (args.length < 1) {
            throw new RuntimeException("Missing EVIO file name.");
        }
        String evioFileName = args[0];
        File evioFile = new File(evioFileName);
        if (!evioFile.exists()) {
            throw new RuntimeException("File " + evioFileName + " does not exist.");
        }
        try {
            org.jlab.coda.jevio.EvioReader reader = new org.jlab.coda.jevio.EvioReader(evioFile,true,false);
            int eventN = 1;
            int badEvents = 0;
            fileLoop:
            while (true) {
                System.out.println("Reading event " + eventN);
                try {
                    EvioEvent event = reader.nextEvent();
                    if (event == null) {
                        break fileLoop;
                    }
                    reader.parseEvent(event);
                    //printBytes(event.getRawBytes()); // DEBUG
                    System.out.println("Successfully read event " + eventN);// + " which contains " + event.getTotalBytes() + " bytes.");
                    printBank(event, "");
                } catch (Exception e) {
                    System.out.println("Caught Exception processing event " + eventN + " which was...");
                    e.printStackTrace();
                    ++badEvents;
                }
                ++eventN;
                System.out.println("-------");
            }
            System.out.println("There were " + badEvents + " bad events out of " + eventN + " total.");
            reader.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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
