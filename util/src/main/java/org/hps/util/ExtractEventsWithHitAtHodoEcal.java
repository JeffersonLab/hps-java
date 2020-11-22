package org.hps.util;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.hps.logging.config.DefaultLoggingConfig;
import org.lcsim.event.EventHeader;
import org.lcsim.event.MCParticle;
import org.lcsim.event.SimCalorimeterHit;
import org.lcsim.event.SimTrackerHit;
import org.lcsim.event.base.BaseLCSimEvent;
import org.lcsim.lcio.LCIOReader;
import org.lcsim.lcio.LCIOWriter;
import org.lcsim.util.loop.LCSimConditionsManagerImplementation;

/**
 * Extracts LCIO events with positron hits at Ecal. For an event, loop all hits
 * at Ecal. If any hit has contribution from positron, then the event is
 * extracted to be saved in output. Hits could be cut with lower energy limit.
 *
 * @author Tongtong Cao <caot@jlab.org>
 */
public class ExtractEventsWithHitAtHodoEcal {
    static {
        LCSimConditionsManagerImplementation.register();
        DefaultLoggingConfig.initialize();
    }

    /**
     * Defines command line options for this program.
     *
     * @return The command line options.
     */
    private static Options createCommandLineOptions() {
        Options options = new Options();

        options.addOption(new Option("n", true, "Number of events to read"));
        options.addOption(new Option("w", true, "Number of events to write"));
        options.addOption(new Option("M", true, "Number of required hits at hodoscope"));
        options.addOption(new Option("N", true, "Number of required positron hits at Ecal"));
        options.addOption(new Option("E", true, "Lower energy threshold for hits at Ecal"));
        options.addOption(new Option("t", true, "Lower energy threshold for sum of energy of all hits at top or bot of Ecal"));
        options.addOption(new Option("e", true, "Interval between non-empty events"));
        
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
        
        int numHitHodo = 0;
        if (cl.hasOption("M")) numHitHodo = Integer.valueOf(cl.getOptionValue("M"));
        
        int numPositronHitEcal = 0;
        if (cl.hasOption("N")) numPositronHitEcal = Integer.valueOf(cl.getOptionValue("N"));
        
        double eCutHit = 0;
        if (cl.hasOption("E")) eCutHit = Double.valueOf(cl.getOptionValue("E"));
        
        double eCutTotal = 0;
        if (cl.hasOption("t")) eCutTotal = Double.valueOf(cl.getOptionValue("t"));

        int nEvents = -1;
        if (cl.hasOption("n")) nEvents = Integer.valueOf(cl.getOptionValue("n"));
        
        int nEventsToWrite = -1;
        if (cl.hasOption("w")) nEventsToWrite = Integer.valueOf(cl.getOptionValue("w"));
        
        int nEmpty = 0;
        if (cl.hasOption("e")) nEmpty = Integer.valueOf(cl.getOptionValue("e"));
        
        int readEvents = 0;
        int writtenEvents = 0;

        String detectorName = null;
        
        ExtractEventsWithHitAtHodoEcal extractor = new ExtractEventsWithHitAtHodoEcal();

        fileLoop: for (int fileNumber = 0; fileNumber < parsedArgs.length - 1; fileNumber++) {
            LCIOReader reader = null;
            String inFileName = parsedArgs[fileNumber];
            try {
                reader = new LCIOReader(new File(inFileName));
                System.out.println("Opened input file " + inFileName);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            eventLoop: while (true) {
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

                if (extractor.goodEvent(event, numHitHodo, numPositronHitEcal, eCutHit, eCutTotal)) {
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

        try {
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    /**
     * An event is regarded as good if there is positron hits at Ecal
     * @param event: LCIO event
     * @param numHitHodo: # of required hits at hodo
     * @param numPositronHitEcal: # of required positron hits at Ecal
     * @param eCutHit: Lower energy threshold for hits at Ecal
     * @param eCutTotal: Lower energy threshold for sum of energy of all hits at top or bot of Ecal
     * @return Return true if an event is good
     */
    public boolean goodEvent(EventHeader event, int numHitHodo, int numPositronHitEcal, double eCutHit, double eCutTotal) {        
        if(numHitHodo > 0) {
            List<SimTrackerHit> hodoHits = event.get(SimTrackerHit.class, "HodoscopeHits");
            if(hodoHits.size() < numHitHodo) return false;
        }
        
        List<SimCalorimeterHit> ecalHits = event.get(SimCalorimeterHit.class, "EcalHits");
        
        if(eCutTotal > 0) {
            double topE = 0, botE = 0;
            for (SimCalorimeterHit hit : ecalHits) {
                if (hit.getIdentifierFieldValue("iy") > 0) {
                    topE += hit.getRawEnergy();
                } else {
                    botE += hit.getRawEnergy();
                }
            }
            if(topE < eCutTotal || botE < eCutTotal) return false;
        }
        
        int numPosHit = 0;
        if(numPositronHitEcal > 0) {
            for (SimCalorimeterHit simHit : ecalHits) {            
                if(simHit.getRawEnergy() >= eCutHit) {
                    for(int i = 0; i < simHit.getMCParticleCount(); i++) {
                        MCParticle particle = simHit.getMCParticle(i);
                        if(particle.getPDGID() == -11) numPosHit++;
                    }            
                }
            }
            if (numPosHit < numPositronHitEcal) return false;
        }
        
        return true;
    }
}

