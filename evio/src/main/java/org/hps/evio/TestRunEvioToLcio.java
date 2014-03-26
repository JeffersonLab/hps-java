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
import org.jlab.coda.jevio.EvioEvent;
import org.jlab.coda.jevio.EvioReader;
import org.lcsim.event.EventHeader;
import org.lcsim.hps.util.RunControlDialog;
import org.lcsim.job.JobControlManager;
import org.lcsim.lcio.LCIOWriter;

/**
 * This class is for converting Test Run EVIO to LCIO events and performing an
 * LCSim job in the same session. The processed events are then written to disk
 * using an LCIOWriter.
 *
 * To run this class from command line:
 *
 * TestRunEvioToLcio [evioFile] -l [lcioFile] -d [detectorName] -x
 * [lcsimXmlFile]
 *
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class TestRunEvioToLcio {

    private static final String defaultDetectorName = "";
    private static final String defaultSteeringFile = "/org/lcsim/hps/steering/monitoring/DummyMonitoring.lcsim";

    /**
     * Defines command line options for this program.
     *
     * @return The command line options.
     */
    private static Options createCommandLineOptions() {
        Options options = new Options();

        options.addOption(new Option("l", true, "The name of the output LCIO file."));
        options.addOption(new Option("d", true, "The name of the detector to use for LCSim conditions."));
        options.addOption(new Option("x", true, "The LCSim XML file to process the LCIO events."));
        options.addOption(new Option("s", true, "Sleep duration between events (in ms)"));
        options.addOption(new Option("n", true, "Stop after N events"));
        options.addOption(new Option("w", false, "Wait after end of data"));
        options.addOption(new Option("c", false, "Show run control window"));
        options.addOption(new Option("D", true, "Pass a variable to the steering file"));
        options.addOption(new Option("r", false, "Interpret -x argument as a steering resource instead of a file path"));

        return options;
    }

    /**
     * This method will execute the EVIO to LCIO conversion and perform an
     * intermediate LCSim job. Then the resultant LCIO events will be written to
     * disk.
     *
     * @param args The command line arguments.
     */
    public static void main(String[] args) {
        int maxEvents = 0;
        int nEvents = 0;

        // Set up command line parsing.
        Options options = createCommandLineOptions();
        if (args.length == 0) {
            System.out.println("TestRunEvioToLcio [options] [evioFiles]");
            HelpFormatter help = new HelpFormatter();
            help.printHelp(" ", options);
            System.exit(1);
        }
        CommandLineParser parser = new PosixParser();

        // Parse command line arguments.
        CommandLine cl = null;
        try {
            cl = parser.parse(options, args);
        } catch (ParseException e) {
            throw new RuntimeException("Problem parsing command line options.", e);
        }

        String lcioFileName = null;
        LCIOWriter writer = null;
        String detectorName = defaultDetectorName;
        InputStream steeringStream = null;
        int sleepTime = -1;

        // Remind people not to use -e any more
        //if (cl.hasOption("e")) {
        //    System.out.println("Option -e is deprecated; EVIO file name is now a non-option argument");
        //}

        // LCIO output file.
        if (cl.hasOption("l")) {
            lcioFileName = cl.getOptionValue("l");
        }

        // Name of detector.
        if (cl.hasOption("d")) {
            detectorName = cl.getOptionValue("d");
        }

        if ("".equals(detectorName)) {
            throw new RuntimeException("You need to specify a valid detector name as input, use the -d option");
        }

        // LCSim XML file to execute inline.
        if (cl.hasOption("x")) {
            String lcsimXmlName = cl.getOptionValue("x");
            if (cl.hasOption("r")) {
                steeringStream = TestRunEvioToLcio.class.getResourceAsStream(lcsimXmlName);
            } else {
                try {
                    steeringStream = new FileInputStream(lcsimXmlName);
                } catch (FileNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        if (steeringStream == null) {
            steeringStream = TestRunEvioToLcio.class.getResourceAsStream(defaultSteeringFile);
        }

        // Sleep time.
        if (cl.hasOption("s")) {
            sleepTime = Integer.valueOf(cl.getOptionValue("s"));
        }

        // Sleep time.
        if (cl.hasOption("n")) {
            maxEvents = Integer.valueOf(cl.getOptionValue("n"));
        }

        RunControlDialog runControl = null;

        if (cl.hasOption("c")) {
            runControl = new RunControlDialog();
        }

        // LCIO writer.
        if (lcioFileName != null) {
            try {
                writer = new LCIOWriter(new File(lcioFileName));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        // LCSim job manager.
        JobControlManager jobManager = new JobControlManager();

        if (cl.hasOption("D")) {
            String[] steeringOptions = cl.getOptionValues("D");
            for (String def : steeringOptions) {
                String[] s = def.split("=");
                if (s.length != 2) {
                    throw new RuntimeException("Bad variable format: " + def);
                }
                String key = s[0];
                String value = s[1];
                jobManager.addVariableDefinition(key, value);
            }
        }

        jobManager.setup(steeringStream);
        jobManager.configure();

        // LCSim event builder.
        LCSimEventBuilder eventBuilder = new LCSimTestRunEventBuilder();
        eventBuilder.setDetectorName(detectorName);

        for (String evioFileName : cl.getArgs()) {
            // EVIO input file.
            File evioFile = new File(evioFileName);
            System.out.println("Opening file " + evioFileName);
            // EVIO reader.
            EvioReader reader = null;
            try {
                reader = new EvioReader(evioFile);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            boolean firstEvent = true;
            long time = 0; //in ms

            // Loop over EVIO events, build LCSim events, process them, and then
            // write events to disk.
            fileLoop:
            while (maxEvents == 0 || nEvents < maxEvents) {
                EvioEvent evioEvent = null;
                try {
                    eventLoop:
                    while (evioEvent == null) {
                        evioEvent = reader.nextEvent();
                        if (evioEvent == null) {
                            break fileLoop;
                        }
                        try {
                            reader.parseEvent(evioEvent);
                        } catch (Exception e) {
                            Logger.getLogger(TestRunEvioToLcio.class.getName()).log(Level.SEVERE, "Error reading EVIO event", e);
                            continue eventLoop;
                        }
                    }
                    //let event builder check for run information
                    eventBuilder.readEvioEvent(evioEvent);
                    // Handlers for different event types.
                    if (EventConstants.isPreStartEvent(evioEvent)) {
                        int[] data = evioEvent.getIntData();
                        int seconds = data[0];
                        int runNumber = data[1];
//                        calibListener.prestart(seconds, runNumber);
                    } else if (EventConstants.isEndEvent(evioEvent)) {
                        int[] data = evioEvent.getIntData();
                        int seconds = data[0];
                        int nevents = data[2];
//                        calibListener.endRun(seconds, nevents);
                    } else if (eventBuilder.isPhysicsEvent(evioEvent)) {
                        EventHeader lcioEvent = eventBuilder.makeLCSimEvent(evioEvent);
                        if (runControl == null || runControl.process(lcioEvent)) {
                            time = (lcioEvent.getTimeStamp() / 1000000);

                            if (firstEvent) {
                                System.out.println("First physics event time: " + time / 1000 + " - " + new Date(time));
                                firstEvent = false;
                            }
//                            if (lcioEvent.hasCollection(BaseRawCalorimeterHit.class, "EcalReadoutHits") && !lcioEvent.get(BaseRawCalorimeterHit.class, "EcalReadoutHits").isEmpty()) {
//                                continue;
//                            }
//                            if (lcioEvent.hasCollection(TriggerData.class, "TriggerBank")) {
//                                List<TriggerData> triggerList = lcioEvent.get(TriggerData.class, "TriggerBank");
//                                if (!triggerList.isEmpty()) {
//                                    TriggerData triggerData = triggerList.get(0);
//
//                                    int orTrig = triggerData.getOrTrig();
//                                    int topTrig = triggerData.getTopTrig();
//                                    int botTrig = triggerData.getBotTrig();
//                                    int pairTrig = triggerData.getPairTrig();
//                                    if (topTrig != 0 && botTrig != 0) {
//                                        System.out.format("%x\t%x\t%x\t%x\n", orTrig, topTrig, botTrig, pairTrig);
//                                    } else {
//                                        continue;
//                                    }
//                                }
//                            }
                            jobManager.processEvent(lcioEvent);
                            if (writer != null) {
                                writer.write(lcioEvent);
                                writer.flush();
                            }
                        }
                    }
                } catch (Exception e) {
                    // Catch all event processing errors and continue.
                    Logger.getLogger(TestRunEvioToLcio.class.getName()).log(Level.SEVERE, "Error in event processing", e);
                    continue;
                } finally {
                    if (sleepTime > 0) {
                        try {
                            Thread.sleep(sleepTime);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
                nEvents++;
            }
            System.out.println("Last physics event time: " + time / 1000 + " - " + new Date(time));
            reader.close();
        }
        System.out.println("No more data");

        if (!cl.hasOption("w")) {
            System.out.println("Exiting");
            jobManager.finish();
            System.out.println("jobManager finished");
        }

        if (writer != null) {
            System.out.println("close writer");
            try {
                writer.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            System.out.println("closed writer");
        }
    }
}