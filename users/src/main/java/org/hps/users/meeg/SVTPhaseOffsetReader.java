package org.hps.users.meeg;

import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.hps.evio.SvtEvioReader;
import org.hps.record.evio.EvioEventUtilities;
import org.hps.record.svt.SvtEvioUtils;
import org.hps.record.triggerbank.AbstractIntData.IntBankDefinition;
import org.hps.record.triggerbank.TIData;
import org.jlab.coda.jevio.BaseStructure;
import org.jlab.coda.jevio.EvioEvent;
import org.lcsim.util.aida.AIDA;

public class SVTPhaseOffsetReader {

    static public void main(String[] args) {

        Options options = new Options();
        options.addOption(new Option("d", false, "debug"));
        options.addOption(new Option("o", true, "output ROOT file name"));

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

        if (cl.getArgs().length != 1) {
            throw new RuntimeException("Need EVIO file name.");
        }

        AIDA aida = AIDA.defaultInstance();
        IHistogram2D ratioVsPhase = aida.histogram2D("ratio vs phase", 200, -1.0, 2.0, 6, 0.0, 24.0);
        IHistogram1D[] ratios = new IHistogram1D[6];
        for (int i = 0; i < 6; i++) {
            ratios[i] = aida.histogram1D("ratio, phase " + i, 200, -1.0, 2.0);
        }

        boolean debug = cl.hasOption("d");
        boolean seqRead = true;

        IntBankDefinition tiBankDefinition = new IntBankDefinition(TIData.class, new int[]{0x2e, 0xe10a});

        String evioFileName = cl.getArgs()[0];
        File evioFile = new File(evioFileName);
        if (!evioFile.exists()) {
            throw new RuntimeException("File " + evioFileName + " does not exist.");
        }
        System.out.println("Opened file " + evioFileName);
        try {
            org.jlab.coda.jevio.EvioReader reader = new org.jlab.coda.jevio.EvioReader(evioFile, true, seqRead);
            int eventN = 1;
            fileLoop:
            while (true) {
                if (debug) {
                    System.out.println("Reading event " + eventN);
                }
                try {
                    EvioEvent event = reader.nextEvent();
                    if (event == null) {
                        break fileLoop;
                    }
                    reader.parseEvent(event);
                    //printBytes(event.getRawBytes()); // DEBUG
                    if (EvioEventUtilities.isPhysicsEvent(event)) {
                        BaseStructure tiBank = tiBankDefinition.findBank(event);
                        TIData tiData = null;
                        if (tiBank != null) {
                            tiData = new TIData(tiBank.getIntData());
                            if (debug) {
                                System.out.format("%d %d\n", tiData.getTime(), tiData.getTime() % 24);
                            }
                        }

                        List<BaseStructure> dataBanks = SvtEvioUtils.getDataBanks(event, SvtEvioReader.MIN_ROC_BANK_TAG, SvtEvioReader.MAX_ROC_BANK_TAG, SvtEvioReader.DATA_BANK_TAG, SvtEvioReader.DATA_BANK_TAG);
                        for (BaseStructure data : dataBanks) {
                            List<int[]> multisampleList = SvtEvioUtils.getMultisamples(data.getIntData(), data.getIntData().length - 2, 1);
                            for (int[] multisample : multisampleList) {
                                if (SvtEvioUtils.isMultisampleHeader(multisample) || SvtEvioUtils.isMultisampleTail(multisample)) {
                                    continue;
                                }
                                short[] samples = SvtEvioUtils.getSamples(multisample);
//                                    System.out.format("%d %d %d %d %d %d\n", samples[0], samples[1], samples[2], samples[3], samples[4], samples[5]);

                                if (tiData != null && samples[3] - samples[0] > 1000) {
//                                        System.out.format("%f %f %f \n", ((double) (samples[4] - samples[0])), ((double) (samples[2] - samples[0])), ((double) (samples[4] - samples[0])) / ((double) (samples[2] - samples[0])));
                                    ratioVsPhase.fill(((double) (samples[4] - samples[2])) / ((double) (samples[3] - samples[0])), tiData.getTime() % 24);
                                    ratios[((int) (tiData.getTime() % 24)) / 4].fill(((double) (samples[4] - samples[2])) / ((double) (samples[3] - samples[0])));
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    System.out.println("Caught Exception processing event " + eventN + " which was...");
                    e.printStackTrace();
                }
                ++eventN;
                if (debug) {
                    System.out.println("-------");
                }
            }
            double[] peaks = new double[6];
            double minPeak = Double.MIN_VALUE;
            int minPhase = -1;
            for (int i = 0; i < 6; i++) {
                peaks[i] = -1;
                double maxValue = Double.MIN_VALUE;
                for (int j = 0; j <= ratios[i].axis().bins(); j++) {
                    double value = ratios[i].binHeight(j);
                    if (value > maxValue) {
                        maxValue = value;
                        peaks[i] = ratios[i].binMean(j);
                    }
                }
                if (peaks[i] < minPeak) {
                    minPeak = peaks[i];
                    minPhase = i;
                }
            }
            boolean isGood = true;
            System.out.format("phase %d: peak at %f\n", (minPhase) % 6, peaks[(minPhase) % 6]);
            for (int i = 0; i < 5; i++) {
                System.out.format("phase %d: peak at %f\n", (minPhase + i + 1) % 6, peaks[(minPhase + i + 1) % 6]);
                if (peaks[(minPhase + i) % 6] > peaks[(minPhase + i + 1) % 6]) {
                    isGood = false;
                }
            }
            System.out.format("offset phase %d, isGood = %b,\n", minPhase, isGood);
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (cl.hasOption("o")) {
            try {
                aida.saveAs(cl.getOptionValue("o") + ".root");
            } catch (IOException ex) {
                Logger.getLogger(SVTPhaseOffsetReader.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private static void printUsage(Options options) {
        System.out.println("SVTPhaseOffsetReader [options] [evioFiles]");
        final HelpFormatter help = new HelpFormatter();
        help.printHelp(" ", options);
        System.exit(1);
    }
}
