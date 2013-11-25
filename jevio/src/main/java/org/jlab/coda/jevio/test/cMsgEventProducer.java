package org.jlab.coda.jevio.test;

import org.jlab.coda.cMsg.*;
import org.jlab.coda.jevio.*;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.io.IOException;

/**
 * Class for testing EventTreeFrame graphics application with events
 * coming in cMsg messages.
 */
public class cMsgEventProducer {
    private String  subject = "evio";
    private String  type = "anything";
    private String  name = "producer";
    private String  description = "java event producer";
    private String  UDL = "cMsg://localhost/cMsg/myNameSpace";

    private int     delay, count = 50000;
    private boolean debug;


    /** Constructor. */
    cMsgEventProducer(String[] args) {
        decodeCommandLine(args);
    }


    /**
     * Method to decode the command line used to start this application.
     * @param args command line arguments
     */
    private void decodeCommandLine(String[] args) {

        // loop over all args
        for (int i = 0; i < args.length; i++) {

            if (args[i].equalsIgnoreCase("-h")) {
                usage();
                System.exit(-1);
            }
            else if (args[i].equalsIgnoreCase("-n")) {
                name = args[i + 1];
                i++;
            }
            else if (args[i].equalsIgnoreCase("-d")) {
                description = args[i + 1];
                i++;
            }
            else if (args[i].equalsIgnoreCase("-u")) {
                UDL= args[i + 1];
                i++;
            }
            else if (args[i].equalsIgnoreCase("-s")) {
                subject = args[i + 1];
                i++;
            }
            else if (args[i].equalsIgnoreCase("-t")) {
                type = args[i + 1];
                i++;
            }
            else if (args[i].equalsIgnoreCase("-c")) {
                count = Integer.parseInt(args[i + 1]);
                if (count < 1)
                    System.exit(-1);
                i++;
            }
            else if (args[i].equalsIgnoreCase("-delay")) {
                delay = Integer.parseInt(args[i + 1]);
                i++;
            }
            else if (args[i].equalsIgnoreCase("-debug")) {
                debug = true;
            }
            else {
                usage();
                System.exit(-1);
            }
        }

        return;
    }


    /** Method to print out correct program command line usage. */
    private static void usage() {
        System.out.println("\nUsage:\n\n" +
            "   java cMsgProducer\n" +
            "        [-n <name>]          set client name\n"+
            "        [-d <description>]   set description of client\n" +
            "        [-u <UDL>]           set UDL to connect to cMsg\n" +
            "        [-s <subject>]       set subject of sent messages\n" +
            "        [-t <type>]          set type of sent messages\n" +
            "        [-c <count>]         set # of messages to send before printing output\n" +
            "        [-delay <time>]      set time in millisec between sending of each message\n" +
            "        [-debug]             turn on printout\n" +
            "        [-h]                 print this help\n");
    }


    /**
     * Run as a stand-alone application.
     */
    public static void main(String[] args) {
        try {
            cMsgEventProducer producer = new cMsgEventProducer(args);
            producer.run();
        }
        catch (cMsgException e) {
            System.out.println(e.toString());
            System.exit(-1);
        }
    }


    /**
     * Method to convert a double to a string with a specified number of decimal places.
     *
     * @param d double to convert to a string
     * @param places number of decimal places
     * @return string representation of the double
     */
    private static String doubleToString(double d, int places) {
        if (places < 0) places = 0;

        double factor = Math.pow(10,places);
        String s = "" + (double) (Math.round(d * factor)) / factor;

        if (places == 0) {
            return s.substring(0, s.length()-2);
        }

        while (s.length() - s.indexOf(".") < places+1) {
            s += "0";
        }

        return s;
    }

    /**
     * This class defines the callback to be run when a message matching
     * our subscription arrives.
     */
    class myCallback extends cMsgCallbackAdapter {
        public void callback(cMsgMessage msg, Object userObject) {
            // keep track of how many messages we receive
            //count++;

            System.out.println("Received msg: ");
            System.out.println(msg.toString(true, false, true));
        }
     }



    /**
     * This method is executed as a thread.
     */
    public void run() throws cMsgException {

        if (debug) {
            System.out.println("Running cMsg producer sending to:\n" +
                                 "    subject = " + subject +
                               "\n    type    = " + type +
                               "\n    UDL     = " + UDL);
        }

        // connect to cMsg server
        cMsg coda = new cMsg(UDL, name, description);
        try {
            coda.connect();
        }
        catch (cMsgException e) {
            e.printStackTrace();
            return;
        }

        // create a message
        cMsgMessage msg = new cMsgMessage();
        msg.setSubject(subject);
        msg.setType(type);

        //--------------------------------------
        // create an array of simple evio events
        //--------------------------------------

        ByteBuffer myBuf = ByteBuffer.allocate(10000);
        myBuf.order(ByteOrder.LITTLE_ENDIAN);

        // xml dictionary
        // xml dictionary
        String dictionary =
                "<xmlDict>\n" +
                        "  <xmldumpDictEntry name=\"bank\"           tag=\"1\"   num=\"1\"/>\n" +
                        "  <xmldumpDictEntry name=\"bank of short banks\" tag=\"2\"   num=\"2\"/>\n" +
                        "  <xmldumpDictEntry name=\"shorts pad0\"    tag=\"3\"   num=\"3\"/>\n" +
                        "  <xmldumpDictEntry name=\"shorts pad2\"    tag=\"4\"   num=\"4\"/>\n" +
                        "  <xmldumpDictEntry name=\"bank of char banks\"  tag=\"5\"   num=\"5\"/>\n" +
                        "  <xmldumpDictEntry name=\"chars pad0\"     tag=\"6\"   num=\"6\"/>\n" +
                        "  <xmldumpDictEntry name=\"chars pad3\"     tag=\"7\"   num=\"7\"/>\n" +
                        "  <xmldumpDictEntry name=\"chars pad2\"     tag=\"8\"   num=\"8\"/>\n" +
                        "  <xmldumpDictEntry name=\"chars pad1\"     tag=\"9\"   num=\"9\"/>\n" +
                "</xmlDict>";

        // use just a bunch of zeros for data
        byte[]  byteData1   = new byte[]  {1,2,3,4};
        byte[]  byteData2   = new byte[]  {1,2,3,4,5};
        byte[]  byteData3   = new byte[]  {1,2,3,4,5,6};
        byte[]  byteData4   = new byte[]  {1,2,3,4,5,6,7};
        short[] shortData1  = new short[] {11,22};
        short[] shortData2  = new short[] {11,22,33};

        try {
            EventWriter eventWriterNew = new EventWriter(myBuf, 100, 3, dictionary, null);

            // event - bank of banks
            EventBuilder eventBuilder2 = new EventBuilder(1, DataType.BANK, 1);
            EvioEvent eventShort = eventBuilder2.getEvent();

            // bank of short banks
            EvioBank bankBanks = new EvioBank(2, DataType.BANK, 2);

            // 3 shorts
            EvioBank shortBank1 = new EvioBank(3, DataType.SHORT16, 3);
            shortBank1.appendShortData(shortData1);
            eventBuilder2.addChild(bankBanks, shortBank1);

            EvioBank shortBank2 = new EvioBank(4, DataType.SHORT16, 4);
            shortBank2.appendShortData(shortData2);
            eventBuilder2.addChild(bankBanks, shortBank2);

            eventBuilder2.addChild(eventShort, bankBanks);
            eventWriterNew.writeEvent(eventShort);



            // each event is a trivial event containing an array of ints - all zeros
            EventBuilder eventBuilder = new EventBuilder(5, DataType.BANK, 5);
            EvioEvent event = eventBuilder.getEvent();

            // event 1
            EvioBank charBank1 = new EvioBank(6, DataType.CHAR8, 6);
            charBank1.appendByteData(byteData1);
            eventBuilder.addChild(event, charBank1);

            // event 2
            EvioBank charBank2 = new EvioBank(7, DataType.CHAR8, 7);
            charBank2.appendByteData(byteData2);
            eventBuilder.addChild(event, charBank2);

            // event 3
            EvioBank charBank3 = new EvioBank(8, DataType.CHAR8, 8);
            charBank3.appendByteData(byteData3);
            eventBuilder.addChild(event, charBank3);

            // event 4
            EvioBank charBank4 = new EvioBank(9, DataType.CHAR8, 9);
            charBank4.appendByteData(byteData4);
            eventBuilder.addChild(event, charBank4);

            eventWriterNew.writeEvent(event);

            // all done writing
            eventWriterNew.close();
            myBuf.flip();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        catch (EvioException e) {
            e.printStackTrace();
        }


        // Add event buffer as byte array
        msg.setByteArray(myBuf.array(), 0, myBuf.limit());


        // variables to track message rate
        double freq=0., freqAvg=0.;
        long t1, t2, deltaT, totalT=0, totalC=0;

        // Ignore the first N values found for freq in order
        // to get better avg statistics. Since the JIT compiler in java
        // takes some time to analyze & compile code, freq may initially be low.
        long ignore=0;

        while (true) {
            t1 = System.currentTimeMillis();
            for (int i = 0; i < count; i++) {
                System.out.println("SEND MSG");
                coda.send(msg);
                coda.flush(0);

                // delay between messages sent
                if (delay != 0) {
                    try {Thread.sleep(delay);}
                    catch (InterruptedException e) {}
                }
            }
            t2 = System.currentTimeMillis();

            if (ignore == 0) {
                deltaT = t2 - t1; // millisec
                freq = (double) count / deltaT * 1000;
                totalT += deltaT;
                totalC += count;
                freqAvg = (double) totalC / totalT * 1000;

                if (debug) {
                    System.out.println(doubleToString(freq, 1) + " Hz, Avg = " +
                                       doubleToString(freqAvg, 1) + " Hz");
                }
            }
            else {
                ignore--;
            }
        }
    }

}
