package org.hps.util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.lcsim.event.EventHeader;
import org.lcsim.event.MCParticle;
import org.lcsim.event.RawCalorimeterHit;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.SimCalorimeterHit;
import org.lcsim.event.SimTrackerHit;
import org.lcsim.event.base.BaseLCSimEvent;
import org.lcsim.geometry.IDDecoder;
import org.lcsim.lcio.LCIOReader;
import org.lcsim.lcio.LCIOWriter;
import org.lcsim.util.loop.LCSimConditionsManagerImplementation;

/**
 * Selects LCIO events passing a cut; spaces out these events with blank events.
 * Intended use is to clean up a photon-run MC file before running trigger and
 * readout sim. Can also be used to chain multiple LCIO files together.
 *
 * @author Sho Uemura <meeg@slac.stanford.edu>
 * @version $Id: FilterMCBunches.java,v 1.9 2013/02/25 21:53:42 meeg Exp $
 */
public class FilterMCBunches {

    static {
        LCSimConditionsManagerImplementation.register();
    }
    
    /**
     * Defines command line options for this program.
     *
     * @return The command line options.
     */
    private static Options createCommandLineOptions() {
        Options options = new Options();
        Option opt_e = new Option("e", true, "Interval between non-empty events");
        opt_e.setRequired(true);
        options.addOption(opt_e);
        //options.addOption(new Option("e", true, "Interval between non-empty events"));
        options.addOption(new Option("n", true, "Number of events to read"));
        options.addOption(new Option("w", true, "Number of events to write"));
        options.addOption(new Option("a", false, "All events - no cuts"));
        options.addOption(new Option("d", false, "Filter requiring enough ECal hit energy in both top and bottom"));
        options.addOption(new Option("t", false, "Filter requiring enough SimTrackerHits to make tracks in top and bottom"));
        options.addOption(new Option("r", false, "Filter requiring enough RawTrackerHits to make tracks in top and bottom"));
        options.addOption(new Option("p", false, "Filter requiring at least 2 particles with enough hits to make tracks"));
        options.addOption(new Option("E", true, "Energy cut for EcalHit cut"));
        options.addOption(new Option("L", true, "Layer count required for tracker cuts"));
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

        EventTester tester = null;
        if (!cl.hasOption("a")) {
            if (cl.hasOption("E")) {
                tester = new EcalEventTester(Double.valueOf(cl.getOptionValue("E")));
            } else {
                tester = new EcalEventTester(0.05);
            }
        }
        if (cl.hasOption("d")) {
            if (cl.hasOption("E")) {
                tester = new EcalPairEventTester(Double.valueOf(cl.getOptionValue("E")));
            } else {
                tester = new EcalPairEventTester(0.05);
            }
        }
        if (cl.hasOption("t")) {
            if (cl.hasOption("L")) {
                tester = new TrackerEventTester(Integer.valueOf(cl.getOptionValue("L")));
            } else {
                tester = new TrackerEventTester(4);
            }
        }
        if (cl.hasOption("r")) {
            if (cl.hasOption("L")) {
                tester = new RTHEventTester(Integer.valueOf(cl.getOptionValue("L")));
            } else {
                tester = new RTHEventTester(4);
            }
        }
        if (cl.hasOption("p")) {
            if (cl.hasOption("L")) {
                tester = new PairEventTester(Integer.valueOf(cl.getOptionValue("L")));
            } else {
                tester = new PairEventTester(3);
            }
        }

        int nEmpty = 0;
        if (cl.hasOption("e")) {
            nEmpty = Integer.valueOf(cl.getOptionValue("e"));
        } else {
            System.out.println("You need to specify the number of empty bunches!");
            System.exit(1);
        }

        int nEvents = -1;
        int nEventsToWrite = -1;
        if (cl.hasOption("n")) {
            nEvents = Integer.valueOf(cl.getOptionValue("n"));
        }
        if (cl.hasOption("w")) {
            nEventsToWrite = Integer.valueOf(cl.getOptionValue("w"));
        }
        int readEvents = 0;
        int writtenEvents = 0;

        String detectorName = null;

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

                if (detectorName == null) {
                    detectorName = event.getDetectorName();
                }

                if (tester == null || tester.goodEvent(event)) {
                    writtenEvents++;
                    try {
                        writer.write(event);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }

                    for (int i = 1; i < nEmpty; i++) {
                        try {
                            writer.write(new BaseLCSimEvent(event.getRunNumber(), event.getEventNumber(), detectorName));
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    if (nEventsToWrite != -1 && writtenEvents == nEventsToWrite) {
                        break fileLoop;
                    }
                }
            }
            try {
                reader.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        System.out.format("Read %d events, wrote %d of them\n", readEvents, writtenEvents);
        if (tester != null) {
            tester.endOfRun();
        }

        try {
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static int countPairs(Set<Integer> ints) {
        int pairs = 0;
        while (!ints.isEmpty()) {
            Integer next = ints.iterator().next();
            ints.remove(next);
            if (ints.remove(next ^ 1)) {
                pairs++;
            }
        }
        return pairs;
    }

    private static abstract class EventTester {

        abstract boolean goodEvent(EventHeader event);

        void endOfRun() {
        }
//        abstract boolean goodEvent(List<SimCalorimeterHit> ecalHits, List<SimTrackerHit> trackerHits);
    }

    private static class RTHEventTester extends EventTester {

        int nEvents = 0;
        int nEcal = 0;
        int nRTH = 0;
        private int hitsNeeded;

        public RTHEventTester(int hitsNeeded) {
            this.hitsNeeded = hitsNeeded;
            LCSimConditionsManagerImplementation.register();
        }

        @Override
        public boolean goodEvent(EventHeader event) {
            nEvents++;
            List<RawCalorimeterHit> ecalHits = event.get(RawCalorimeterHit.class, "EcalReadoutHits");
            List<RawTrackerHit> trackerHits = event.get(RawTrackerHit.class, "SVTRawTrackerHits");
            nEcal += ecalHits.size();
            nRTH += trackerHits.size();

            Set<Integer> topLayers = new HashSet<Integer>();
            Set<Integer> botLayers = new HashSet<Integer>();
            for (RawTrackerHit hit : trackerHits) {
                IDDecoder dec = hit.getIDDecoder();
                dec.setID(hit.getCellID());
                if (dec.getValue("module") % 2 == 0) {
                    topLayers.add(dec.getValue("layer"));
                } else {
                    botLayers.add(dec.getValue("layer"));
                }
            }

//            System.out.format("%d SimCalorimeterHits, %d SimTrackerHits, %d top layers, %d bottom layers\n", ecalHits.size(), trackerHits.size(), topLayers.size(), botLayers.size());
            return (countPairs(topLayers) >= hitsNeeded && countPairs(botLayers) >= hitsNeeded);
        }

        @Override
        void endOfRun() {
            System.out.format("%d events, %f RawCalorimeterHits and %f RawTrackerHits on average\n", nEvents, ((double) nEcal) / nEvents, ((double) nRTH) / nEvents);
        }
    }

    private static class TrackerEventTester extends EventTester {

        private int hitsNeeded;

        public TrackerEventTester(int hitsNeeded) {
            this.hitsNeeded = hitsNeeded;
            LCSimConditionsManagerImplementation.register();
        }

        @Override
        public boolean goodEvent(EventHeader event) {
//            List<SimCalorimeterHit> ecalHits = event.getSimCalorimeterHits("EcalHits");
            List<SimTrackerHit> trackerHits = event.get(SimTrackerHit.class, "TrackerHits");

            Set<Integer> topLayers = new HashSet<Integer>();
            Set<Integer> botLayers = new HashSet<Integer>();
            for (SimTrackerHit hit : trackerHits) {
                IDDecoder dec = hit.getIDDecoder();
                dec.setID(hit.getCellID());
                if (dec.getValue("module") % 2 == 0) {
                    topLayers.add(dec.getValue("layer"));
                } else {
                    botLayers.add(dec.getValue("layer"));
                }
            }

//            System.out.format("%d SimCalorimeterHits, %d SimTrackerHits, %d top layers, %d bottom layers\n", ecalHits.size(), trackerHits.size(), topLayers.size(), botLayers.size());
            return (countPairs(topLayers) >= hitsNeeded && countPairs(botLayers) >= hitsNeeded);
        }
    }

    private static class EcalEventTester extends EventTester {

        double eCut;

        public EcalEventTester(double eCut) {
            this.eCut = eCut;
        }

        @Override
        public boolean goodEvent(EventHeader event) {
            List<SimCalorimeterHit> ecalHits = event.get(SimCalorimeterHit.class, "EcalHits");
//            List<SimTrackerHit> trackerHits = event.getSimTrackerHits("TrackerHits");

            double maxE = 0;
            double totalE = 0;
            for (SimCalorimeterHit hit : ecalHits) {
                totalE += hit.getRawEnergy();
                if (hit.getRawEnergy() > maxE) {
                    maxE = hit.getRawEnergy();
                }
            }

//        System.out.format("%d SimCalorimeterHits, %d SimTrackerHits, maxE %f, totalE %f\n", ecalHits.size(), trackerHits.size(), maxE, totalE);
//        return (ecalHits.size() + trackerHits.size() != 0);
//        return (totalE > 0.05 || !trackerHits.isEmpty());
            return (totalE > eCut);
        }
    }

    private static class EcalPairEventTester extends EventTester {

        double eCut;

        public EcalPairEventTester(double eCut) {
            this.eCut = eCut;
            LCSimConditionsManagerImplementation.register();
        }

        @Override
        public boolean goodEvent(EventHeader event) {
            List<SimCalorimeterHit> ecalHits = event.get(SimCalorimeterHit.class, "EcalHits");
//            List<SimTrackerHit> trackerHits = event.getSimTrackerHits("TrackerHits");

            double topE = 0, botE = 0;
            for (SimCalorimeterHit hit : ecalHits) {
                if (hit.getIdentifierFieldValue("iy") > 0) {
                    topE += hit.getRawEnergy();
                } else {
                    botE += hit.getRawEnergy();
                }
            }

//        System.out.format("%d SimCalorimeterHits, %d SimTrackerHits, maxE %f, totalE %f\n", ecalHits.size(), trackerHits.size(), maxE, totalE);
//        return (ecalHits.size() + trackerHits.size() != 0);
//        return (totalE > 0.05 || !trackerHits.isEmpty());
            return (topE > eCut && botE > eCut);
        }
    }

    private static class PairEventTester extends EventTester {

        private int hitsNeeded;

        public PairEventTester(int hitsNeeded) {
            this.hitsNeeded = hitsNeeded;
            LCSimConditionsManagerImplementation.register();
        }

        @Override
        public boolean goodEvent(EventHeader event) {
//            List<SimCalorimeterHit> ecalHits = event.getSimCalorimeterHits("EcalHits");
            List<SimTrackerHit> trackerHits = event.get(SimTrackerHit.class, "TrackerHits");
            List<MCParticle> mcParticles = event.getMCParticles();

            Map<MCParticle, Set<Integer>> particleMap = new HashMap<MCParticle, Set<Integer>>();
            List<MCParticle> particleList = new LinkedList<MCParticle>(mcParticles);
            List<MCParticle> priParticles = new ArrayList<MCParticle>();

            while (!particleList.isEmpty()) {
                Iterator<MCParticle> iter = particleList.iterator();
                while (iter.hasNext()) {
                    MCParticle particle = iter.next();

                    if (!particle.getSimulatorStatus().isCreatedInSimulation()) {
                        particleMap.put(particle, new HashSet<Integer>());
                        priParticles.add(particle);
                        iter.remove();
                        continue;
                    }

                    for (MCParticle parent : particle.getParents()) {
                        if (particleMap.containsKey(parent)) {
                            particleMap.put(particle, particleMap.get(parent));
                            iter.remove();
                            break;
                        }
                    }
                }
            }


            for (SimTrackerHit hit : trackerHits) {
                IDDecoder dec = hit.getIDDecoder();
                dec.setID(hit.getCellID());
                particleMap.get(hit.getMCParticle()).add(dec.getValue("layer"));
            }

            int particlesWithTracks = 0;
            for (MCParticle priParticle : priParticles) {
                int nLayers = countPairs(particleMap.get(priParticle));
//                System.out.format("layers hit: %d minimum number needed: %d\n", nLayers, nHits);
                if (nLayers >= hitsNeeded) {
                    particlesWithTracks++;
                }
            }

//            System.out.format("%d of %d primary particles have tracks\n", particlesWithTracks, priParticles.size());
            return (particlesWithTracks >= 2);
        }
    }
}
