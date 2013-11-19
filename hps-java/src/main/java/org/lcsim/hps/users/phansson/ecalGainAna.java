package org.lcsim.hps.users.phansson;

import hep.aida.*;
import java.io.IOException;
import java.io.PrintStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.cli.*;
import org.lcsim.geometry.util.DetectorLocator;
import org.lcsim.hps.recon.ecal.ECalUtils;
import org.lcsim.hps.recon.ecal.EcalConditions;
import org.lcsim.util.aida.AIDA;

/**
 *
 * @author phansson
 */
public class ecalGainAna {

//    private AIDA aida = AIDA.defaultInstance();
//    private IAnalysisFactory af = aida.analysisFactory();
//    IHistogramFactory hf = aida.histogramFactory();
//    ITree tree = aida.tree();//(ITreeFactory) af.createTreeFactory().create();
    private static void printObjectsInTree(ITree tree) {
        System.out.println("-----\nObject names in tree " + tree.name() + ":");
        for (String str : tree.listObjectNames()) {
            System.out.println(str);
        }
        System.out.println("-----");
    }

    private static Options createCommandLineOptions() {
        Options option = new Options();
        option.addOption("f", true, "Input file for Pelle's analysis");
        option.addOption("s", false, "Save to file (Pelle)");
        option.addOption("n", true, "Name added to plots");
        option.addOption("m", true, "Minimum weight for calibration");
        option.addOption("d", true, "Detector used for calibration");
        option.addOption("g", false, "Use HV groups in calibration");
        return option;
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here


        IAnalysisFactory af = IAnalysisFactory.create();


        Options opts = createCommandLineOptions();
        if (args.length == 0) {
            System.out.println("ecalGainAna [options]");
            HelpFormatter help = new HelpFormatter();
            help.printHelp(" ", opts);
            System.exit(1);
        }
        CommandLineParser parser = new PosixParser();
        CommandLine cmd = null;
        try {
            cmd = parser.parse(opts, args);
        } catch (ParseException ex) {
            Logger.getLogger(ecalGainAna.class.getName()).log(Level.SEVERE, null, ex);
        }

        String outName = "ecalgainplots";
        if (cmd.hasOption("n")) {
            outName = cmd.getOptionValue("n");
        }

        if (cmd.hasOption("f")) {
            boolean savePlots = false;
            if (cmd.hasOption("s")) {
                savePlots = true;
            }

            String fileName = "";
            fileName = cmd.getOptionValue("f");
            System.out.println("File: " + fileName);
            doPelleAnalysis(af, savePlots, fileName, outName);
        }

        if (cmd.getArgs().length != 2) {
            System.err.println("Expected two arguments: ecalGainAna gaindriver_sim.aida gaindriver.aida");
            System.exit(1);
        }

        double minCount = 20.0;
        if (cmd.hasOption("m")) {
            minCount = Double.valueOf(cmd.getOptionValue("m"));
        }

        boolean useHVGroups = cmd.hasOption("g");

        ITree sim = null;
        ITree real = null;
        ITree outTree = null;
        try {
            sim = af.createTreeFactory().create(cmd.getArgs()[0]);
            real = af.createTreeFactory().create(cmd.getArgs()[1]);
            outTree = af.createTreeFactory().createTree(outName + ".aida", null, ITreeFactory.RECREATE);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        IHistogram1D pePlotsSim[][][] = new IHistogram1D[47][11][5];
        IHistogram1D pePlotsReal[][][] = new IHistogram1D[47][11][5];

        for (int iE = 0; iE <= 4; ++iE) {
            for (int irow = -5; irow <= 5; ++irow) {
                for (int icol = -23; icol <= 23; ++icol) {
                    if (iE == 0) {
                        pePlotsSim[icol + 23][irow + 5][iE] = (IHistogram1D) sim.find("E over p x=" + icol + " y=" + irow);
                        pePlotsReal[icol + 23][irow + 5][iE] = (IHistogram1D) real.find("E over p x=" + icol + " y=" + irow);
                    } else {
                        pePlotsSim[icol + 23][irow + 5][iE] = (IHistogram1D) sim.find("E over p x=" + icol + " y=" + irow + " iE=" + iE);
                        pePlotsReal[icol + 23][irow + 5][iE] = (IHistogram1D) real.find("E over p x=" + icol + " y=" + irow + " iE=" + iE);
                    }
                }
            }
        }

        IHistogram2D weightPlotSim = (IHistogram2D) sim.find("Weights for correction factor");
        IHistogram2D sumPlotSim = (IHistogram2D) sim.find("Sum for correction factor");
        IHistogram2D weightPlotReal = (IHistogram2D) real.find("Weights for correction factor");
        IHistogram2D sumPlotReal = (IHistogram2D) real.find("Sum for correction factor");

//        IHistogram2D mpePlotSim = (IHistogram2D) sim.find("<E over p>");
//        IHistogram2D spePlotSim = (IHistogram2D) sim.find("RMS(E over p)");
//        IHistogram2D mpePlotReal = (IHistogram2D) real.find("<E over p>");
//        IHistogram2D spePlotReal = (IHistogram2D) real.find("RMS(E over p)");

        String detectorName = "HPS-TestRun-v3";
        if (cmd.hasOption("d")) {
            detectorName = cmd.getOptionValue("d");
        }

        EcalConditions.detectorChanged(DetectorLocator.findDetector(detectorName), "Ecal");
        EcalConditions.loadCalibration();

        IHistogramFactory hf = af.createHistogramFactory(outTree);

        IHistogram2D corrPlotSim = hf.createHistogram2D("Weighted E over p - simulation", 46, -23, 23, 11, -5.5, 5.5);
        IHistogram2D corrPlotReal = hf.createHistogram2D("Weighted E over p - data", 46, -23, 23, 11, -5.5, 5.5);

        for (int x = -23; x <= 23; x++) {
            int ix = x > 0 ? x + 22 : x + 23;
            for (int y = -5; y <= 5; y++) {
                double ep;
                if (weightPlotSim.binHeight(ix, y + 5) > minCount / 4.0) {
                    ep = weightPlotSim.binHeight(ix, y + 5) / sumPlotSim.binHeight(ix, y + 5);
                    if (ep > 1.0) {
                        ep = 1.0;
                    }
                    corrPlotSim.fill(x - 0.5 * Math.signum(x), y, ep);
                }
                if (weightPlotReal.binHeight(ix, y + 5) > minCount / 4.0) {
                    ep = weightPlotReal.binHeight(ix, y + 5) / sumPlotReal.binHeight(ix, y + 5);
                    if (ep > 1.0) {
                        ep = 1.0;
                    }
                    corrPlotReal.fill(x - 0.5 * Math.signum(x), y, ep);
                }
            }
        }


        IPlotter plotter = af.createPlotterFactory().create();
        plotter.setParameter("plotterWidth", "600");
        plotter.setParameter("plotterHeight", "400");
        plotter.createRegion();
        plotter.region(0).style().statisticsBoxStyle().setVisible(false);
        plotter.region(0).style().setParameter("hist2DStyle", "colorMap");
        plotter.region(0).style().dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");


        plotter.region(0).plot(weightPlotSim);
        plotter.region(0).setTitle(weightPlotSim.title() + " - simulation");
        try {
            plotter.writeToFile(outName + "_weights_sim.png", "png");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        plotter.region(0).remove(weightPlotSim);
        plotter.region(0).plot(weightPlotReal);
        plotter.region(0).setTitle(weightPlotReal.title() + " - data");
        plotter.region(0).refresh();
        try {
            plotter.writeToFile(outName + "_weights_data.png", "png");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        plotter.region(0).remove(weightPlotReal);

//        plotter.region(0).style().zAxisStyle().setParameter("scale", "log");
        plotter.region(0).plot(corrPlotSim);
//        plotter.region(0).setZLimits(0, 10.0);
        plotter.region(0).style().zAxisStyle().setParameter("upperLimit", "10.0");
        plotter.region(0).setTitle(corrPlotSim.title());
        plotter.region(0).refresh();
        plotter.region(0).style().zAxisStyle().setParameter("upperLimit", "10.0");
        try {
            plotter.writeToFile(outName + "_corr_sim.png", "png");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        plotter.region(0).remove(corrPlotSim);
        plotter.region(0).plot(corrPlotReal);
//        plotter.region(0).setZLimits(0, 10.0);
//for (String opt :plotter.region(0).style().zAxisStyle().availableParameters()) {
//            System.out.println(opt);
//        }
        plotter.region(0).setTitle(corrPlotReal.title());
        plotter.region(0).refresh();
        try {
            plotter.writeToFile(outName + "_corr_data.png", "png");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        plotter.region(0).remove(corrPlotReal);
//        plotter.region(0).style().zAxisStyle().setParameter("scale", "lin");
//        plotter.region(0).setZLimits();



        IHistogram2D ecalPlot = hf.createHistogram2D("ECal map", 46, -23, 23, 11, -5.5, 5.5);
        plotter.region(0).plot(ecalPlot);


        IHistogram1D pePlotsHVSim[][] = new IHistogram1D[4][12];
        IHistogram1D pePlotsHVReal[][] = new IHistogram1D[4][12];
        double[][] weightsHVSim = new double[4][12];
        double[][] sumsHVSim = new double[4][12];
        double[][] weightsHVReal = new double[4][12];
        double[][] sumsHVReal = new double[4][12];


        double allWeightsReal = 0;
        double allWeightsSim = 0;

        for (int quad = 1; quad <= 4; ++quad) {
            for (int module = 1; module <= 12; ++module) {
                pePlotsHVSim[quad - 1][module - 1] = hf.createHistogram1D("E over p quadrant=" + quad + " HV group=" + module, 50, 0, 2);
                pePlotsHVReal[quad - 1][module - 1] = hf.createHistogram1D("E over p quadrant=" + quad + " HV group=" + module, 50, 0, 2);
            }
        }
        for (int x = -23; x <= 23; x++) {
            for (int y = -5; y <= 5; y++) {
                pePlotsHVSim[ECalUtils.getQuadrant(x, y) - 1][ECalUtils.getHVGroup(x, y) - 1].add(pePlotsSim[x + 23][y + 5][0]);
                pePlotsHVReal[ECalUtils.getQuadrant(x, y) - 1][ECalUtils.getHVGroup(x, y) - 1].add(pePlotsReal[x + 23][y + 5][0]);
                weightsHVSim[ECalUtils.getQuadrant(x, y) - 1][ECalUtils.getHVGroup(x, y) - 1] += weightPlotSim.binHeight(x + 23, y + 5);
                sumsHVSim[ECalUtils.getQuadrant(x, y) - 1][ECalUtils.getHVGroup(x, y) - 1] += sumPlotSim.binHeight(x + 23, y + 5);
                weightsHVReal[ECalUtils.getQuadrant(x, y) - 1][ECalUtils.getHVGroup(x, y) - 1] += weightPlotReal.binHeight(x + 23, y + 5);
                sumsHVReal[ECalUtils.getQuadrant(x, y) - 1][ECalUtils.getHVGroup(x, y) - 1] += sumPlotReal.binHeight(x + 23, y + 5);
                allWeightsReal += weightPlotReal.binHeight(x + 23, y + 5);
                allWeightsSim += weightPlotSim.binHeight(x + 23, y + 5);
            }
        }

        for (int x = -23; x <= 23; x++) {
            for (int y = -5; y <= 5; y++) {
                Double gain = EcalConditions.physicalToGain(EcalConditions.makePhysicalID(x, y));
                if (gain != null) {
                    ecalPlot.fill(x - 0.5 * Math.signum(x), y, gain);
//                    gainsPlot.fill(x, y, HPSECalUtils.getHVGroup(x, y));
                }
            }
        }

        plotter.region(0).setTitle("Crystal gains before iterating");
        plotter.region(0).refresh();
        try {
            plotter.writeToFile(outName + "_gains_initial.png", "png");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        ecalPlot.reset();

        for (int x = -23; x <= 23; x++) {
            for (int y = -5; y <= 5; y++) {
                ecalPlot.fill(x - 0.5 * Math.signum(x), y, pePlotsSim[x + 23][y + 5][0].allEntries());
            }
        }

        plotter.region(0).setTitle("Matched cluster counts in simulation");
        plotter.region(0).refresh();
        try {
            plotter.writeToFile(outName + "_counts_sim.png", "png");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        ecalPlot.reset();

        for (int x = -23; x <= 23; x++) {
            for (int y = -5; y <= 5; y++) {
                ecalPlot.fill(x - 0.5 * Math.signum(x), y, pePlotsReal[x + 23][y + 5][0].allEntries());
            }
        }

        plotter.region(0).setTitle("Matched cluster counts in data");
        plotter.region(0).refresh();
        try {
            plotter.writeToFile(outName + "_counts_data.png", "png");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        ecalPlot.reset();

        PrintStream gainStream;
        try {
            gainStream = new PrintStream(outName + ".gain");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


        gainStream.format("#x\ty\tgain\n");
        for (int side = 0; side < 2; side++) {
            for (int x = -23; x <= 23; x++) {
                for (int iy = 1; iy <= 5; iy++) {
                    int y = side == 0 ? iy : -iy;
                    Double gain = EcalConditions.physicalToGain(EcalConditions.makePhysicalID(x, y));
                    IHistogram1D peSim = pePlotsSim[x + 23][y + 5][0];
                    IHistogram1D peReal = pePlotsReal[x + 23][y + 5][0];
                    IHistogram1D peHVSim = pePlotsHVSim[ECalUtils.getQuadrant(x, y) - 1][ECalUtils.getHVGroup(x, y) - 1];
                    IHistogram1D peHVReal = pePlotsHVReal[ECalUtils.getQuadrant(x, y) - 1][ECalUtils.getHVGroup(x, y) - 1];
                    double weightHVReal = weightsHVReal[ECalUtils.getQuadrant(x, y) - 1][ECalUtils.getHVGroup(x, y) - 1];
                    double sumHVReal = sumsHVReal[ECalUtils.getQuadrant(x, y) - 1][ECalUtils.getHVGroup(x, y) - 1];
                    double weightHVSim = weightsHVSim[ECalUtils.getQuadrant(x, y) - 1][ECalUtils.getHVGroup(x, y) - 1];
                    double sumHVSim = sumsHVSim[ECalUtils.getQuadrant(x, y) - 1][ECalUtils.getHVGroup(x, y) - 1];
                    if (gain != null) {
                        if (weightPlotSim.binHeight(x + 23, y + 5) > minCount && weightPlotReal.binHeight(x + 23, y + 5) > minCount) {
                            gain *= (corrPlotSim.binHeight(x + 23, y + 5) / corrPlotReal.binHeight(x + 23, y + 5));
                        } else if (useHVGroups && weightHVReal > 4 * minCount && weightHVSim > 4 * minCount) {
                            gain *= (weightHVSim / sumHVSim) / (weightHVReal / sumHVReal);
//                        } else if (weightHVReal / allWeightsReal > 4 * weightHVSim / allWeightsSim) {
//                            gain /= 1.5;
//                        } else if (weightHVSim / allWeightsSim > 4 * weightHVReal / allWeightsReal) {
//                            gain *= 1.5;
                        }
//                    if (peSim.allEntries() >= minCount && peReal.allEntries() >= minCount) {
//                        gain *= (peSim.mean() / peReal.mean());
//                    } else if (peHVSim.allEntries() >= minCount && peHVReal.allEntries() >= minCount) {
////                        gain *= (peHVSim.mean() / peHVReal.mean());
//                    }
                        gainStream.format("%d\t%d\t%f\n", x, y, gain);
                        ecalPlot.fill(x - 0.5 * Math.signum(x), y, gain);
//                    System.out.printf("%d\t%d\t%d\t%f\t%f\n", x, y, pePlotsSim[x + 23][y + 5][0].allEntries(), pePlotsSim[x + 23][y + 5][0].mean(), pePlotsSim[x + 23][y + 5][0].rms());
                    }
                }
            }
        }
        gainStream.close();

        plotter.region(0).setTitle("Crystal gains after iterating");
        plotter.region(0).refresh();
        try {
            plotter.writeToFile(outName + "_gains_final.png", "png");
            outTree.commit();
            outTree.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void doPelleAnalysis(IAnalysisFactory af, boolean savePlots, String fileName, String outName) {
        ITree tree = null;
        try {
            tree = af.createTreeFactory().create(fileName);
        } catch (IllegalArgumentException ex) {
            Logger.getLogger(ecalGainAna.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(ecalGainAna.class.getName()).log(Level.SEVERE, null, ex);
        }
        //printObjectsInTree(tree);

        IDataPointSetFactory dpsf = af.createDataPointSetFactory(null);
        IDataPointSet[] dpsEp = new IDataPointSet[2];
        dpsEp[0] = dpsf.create("dpsEp_t", "E/p vs p top", 2);
        dpsEp[1] = dpsf.create("dpsEp_b", "E/p vs p bot", 2);
        double[] ebins = {100, 600, 800, 1000, 1300};
        for (int iE = 0; iE <= 4; ++iE) {
            String str = iE == 0 ? "" : (" iE=" + iE);
            IPlotter pl = af.createPlotterFactory().create();
            pl.createRegions(1, 2, 0);
            pl.setTitle("E over p" + str);
            pl.style().statisticsBoxStyle().setVisible(true);
            pl.show();
            for (int iside = 0; iside <= 1; ++iside) {
                String side = iside == 0 ? "top" : "bottom";

                //String name = "E over p x="+icol+" y="+irow+str;
                String name = "E over p " + side + str;
                IHistogram1D h = (IHistogram1D) tree.find(name);

                pl.region(iside).plot(h);
                pl.region(iside).style().xAxisStyle().setLabel("E over p " + side);
                pl.region(iside).style().yAxisStyle().setLabel("Events");

                dpsEp[iside].addPoint();
                dpsEp[iside].point(iE).coordinate(0).setValue(ebins[iE]);
                dpsEp[iside].point(iE).coordinate(1).setValue(h.mean());
                double err = 0.;//(h.entries()>5||h.rms()>0.0)?h.rms()/Math.sqrt(h.entries()):0;
                System.out.println("N " + h.entries() + " rms " + h.rms() + " err " + h.rms() / Math.sqrt(h.entries()));
                dpsEp[iside].point(iE).coordinate(1).setErrorMinus(err / 2);
                dpsEp[iside].point(iE).coordinate(1).setErrorPlus(err / 2);


//            for(int irow=-5;irow<=5;++irow) {
//                for(int icol=-23;icol<=23;++icol) {
//                    String name;
//                    if(iE==0) name = "E over p x="+icol+" y="+irow;
//                    else      name = "E over p x="+icol+" y="+irow+" iE="+iE;
//                    
//                    IHistogram1D h = (IHistogram1D) tree.find(name);
//                    int N = h.entries();
//                    double m = h.mean();
//                    double c 
//                }
//            }
            }

            if (savePlots) {
                try {
                    pl.writeToFile(outName + "iE" + iE + ".png", "png");
                } catch (IOException ex) {
                    Logger.getLogger(ecalGainAna.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

        }

        IPlotter plM = af.createPlotterFactory().create();
        plM.createRegions(1, 1, 0);
        plM.setTitle("<E over p>");
        plM.style().statisticsBoxStyle().setVisible(false);
        plM.region(0).style().xAxisStyle().setLabel("Track momentum bins");
        plM.region(0).style().yAxisStyle().setLabel("Mean E/p");
        plM.show();
        plM.region(0).plot(dpsEp[0]);
        plM.region(0).plot(dpsEp[1], "mode=overlay");

        if (savePlots) {
            try {
                plM.writeToFile(outName + "_mean_vs_p.png", "png");
            } catch (IOException ex) {
                Logger.getLogger(ecalGainAna.class.getName()).log(Level.SEVERE, null, ex);
            }
        }


    }
}