package org.hps.users.meeg;

import hep.physics.event.generator.MCEvent;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.lcsim.event.EventHeader;
import org.lcsim.event.MCParticle;
import org.lcsim.event.SimCalorimeterHit;
import org.lcsim.event.SimTrackerHit;
import org.lcsim.event.base.BaseLCSimEvent;
import org.lcsim.lcio.LCIOReader;
import org.lcsim.lcio.LCIOWriter;

/**
 * Selects LCIO events passing a cut; spaces out these events with blank events.
 * Intended use is to clean up a photon-run MC file before running trigger and readout sim.
 * Can also be used to chain multiple LCIO files together.
 */
public class MergeMCBunches {

    /**
     * Defines command line options for this program.
     *
     * @return The command line options.
     */
    private static Options createCommandLineOptions() {
        Options options = new Options();
        options.addOption(new Option("f", true, "Number of bunches merged for front SVT layers (default 4)"));
        options.addOption(new Option("r", true, "Number of bunches merged for rear SVT layers (default 8)"));
        options.addOption(new Option("s", true, "LCIO file containing signal events"));
        options.addOption(new Option("n", true, "Number of events to read"));
        return options;
    }

    public static void main(String[] args) {
        // Set up command line parsing.
        Options options = createCommandLineOptions();
        CommandLineParser parser = new PosixParser();

        // Parse command line arguments.
        CommandLine cl = null;
        try {
            cl = parser.parse(options, args);
        } catch (ParseException e) {
            throw new RuntimeException("Problem parsing command line options.", e);
        }

        String[] parsedArgs = cl.getArgs();

        if (parsedArgs.length < 2) {
            System.out.println("FilterMCBunches <input files> <output file>");
            HelpFormatter help = new HelpFormatter();
            help.printHelp(" ", options);
            System.exit(1);
        }

        String outFileName = parsedArgs[parsedArgs.length - 1];

        LCIOWriter writer = null;
        try {
            writer = new LCIOWriter(new File(outFileName));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        int nEvents = -1;
        if (cl.hasOption("n")) {
            nEvents = Integer.valueOf(cl.getOptionValue("n"));
        }
        int readEvents = 0;
        int writtenEvents = 0;

        int mergeN = 8;
        if (cl.hasOption("r")) {
            mergeN = Integer.valueOf(cl.getOptionValue("r"));
        }
        int mergeFront = 4;
        if (cl.hasOption("f")) {
            mergeFront = Integer.valueOf(cl.getOptionValue("f"));
        }
        int frontLayers = 6;

        LCIOReader signalReader = null;
        if (cl.hasOption("s")) {
            try {
                signalReader = new LCIOReader(new File(cl.getOptionValue("s")));
                System.out.println("Opened input file " + cl.getOptionValue("s"));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        String detectorName = null;

        List<MCParticle> mcParticles = new ArrayList<MCParticle>();
        List<SimTrackerHit> trackerHits = new ArrayList<SimTrackerHit>();
        List<SimCalorimeterHit> ecalHits = new ArrayList<SimCalorimeterHit>();

        fileLoop:
        for (int fileNumber = 0; fileNumber < parsedArgs.length - 1; fileNumber++) {
            LCIOReader reader = null;
            String inFileName = parsedArgs[fileNumber];
            try {
                reader = new LCIOReader(new File(inFileName));
                System.out.println("Opened input file " + inFileName);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            eventLoop:
            while (true) {
                if (nEvents != -1 && readEvents == nEvents) {
                    break fileLoop;
                }
                EventHeader event;
                try {
                    event = reader.read();
                } catch (IOException e) {
                    break eventLoop;
                }
                readEvents++;
                mcParticles.addAll(event.get(MCParticle.class, MCEvent.MC_PARTICLES));
                ecalHits.addAll(event.get(SimCalorimeterHit.class, "EcalHits"));
//                trackerHits.addAll(event.get(SimTrackerHit.class, "TrackerHits"));
                for (SimTrackerHit hit : event.get(SimTrackerHit.class, "TrackerHits")) {
                    if (hit.getLayer() < frontLayers) {
                        if (readEvents % mergeN < mergeFront) {
                            trackerHits.add(hit);
                        }
                    } else {
                        trackerHits.add(hit);
                    }
                }

                if (detectorName == null) {
                    detectorName = event.getDetectorName();
                }

                if (readEvents % mergeN == 0) {
                    EventHeader newEvent = new BaseLCSimEvent(event.getRunNumber(), event.getEventNumber(), detectorName);
                    if (cl.hasOption("s")) {
                        EventHeader signalEvent = null;
                        try {
                            signalEvent = signalReader.read();
                        } catch (IOException e) {
                            break fileLoop;
                        }
                        mcParticles.addAll(signalEvent.get(MCParticle.class, MCEvent.MC_PARTICLES));
                        ecalHits.addAll(signalEvent.get(SimCalorimeterHit.class, "EcalHits"));
                        trackerHits.addAll(signalEvent.get(SimTrackerHit.class, "TrackerHits"));
                    }
                    newEvent.put(MCEvent.MC_PARTICLES, mcParticles);
                    newEvent.put("EcalHits", ecalHits);
                    newEvent.put("TrackerHits", trackerHits);
                    try {
                        writer.write(newEvent);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    writtenEvents++;
                    mcParticles.clear();
                    ecalHits.clear();
                    trackerHits.clear();
                }
            }
            try {
                reader.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        System.out.format("Read %d events, wrote %d\n", readEvents, writtenEvents);
        try {
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
