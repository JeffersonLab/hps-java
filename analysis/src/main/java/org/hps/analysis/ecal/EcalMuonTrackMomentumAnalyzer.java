package org.hps.analysis.ecal;

import hep.aida.IAnalysisFactory;
import hep.aida.IDataPointSet;
import hep.aida.IDataPointSetFactory;
import hep.aida.IFitFactory;
import hep.aida.IFitResult;
import hep.aida.IFitter;
import hep.aida.IFunction;
import hep.aida.IFunctionFactory;
import hep.aida.IHistogram1D;
import hep.aida.IPlotter;
import hep.aida.IPlotterFactory;
import hep.aida.IPlotterStyle;
import hep.aida.ITree;
import java.io.File;
import java.io.IOException;
import static java.lang.Math.abs;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.lcsim.util.aida.AIDA;

/**
 *
 * @author Norman A. Graf
 */
public class EcalMuonTrackMomentumAnalyzer {

    public static void main(String[] args) throws IllegalArgumentException, IOException {
        AIDA aida = AIDA.defaultInstance();
        String orientation = "landscape"; // or "portrait";
        String plotterHeight = "1100";//900";
        String plotterWidth = "850";//1600";
        if (orientation.equals("landscape")) {
            plotterHeight = "900";
            plotterWidth = "1600";
        }
        int[] plotRegion = {0, 0, 2, 4, 1, 3};
        boolean showPlots = false;
        boolean writePlots = true;
        String plotDir = "dimuon";
        String fileType = "pdf"; // png, pdf, eps ps svg emf swf
//        String plotFile = "D:/work/hps/analysis/physrun2019/ecalibration/prodSingleMuonSkim/20200919/hps_010698_HPS_TY_iter4_muEcalGainCalibration.aida";
        String plotFile = "D:/work/hps/analysis/physrun2019/ecalibration/prodSingleMuonSkim/20200919/hps_010261_HPS_TY_iter4_muEcalGainCalibration.aida";
        // parse command-line arguments
//        if (args.length == 0) {
//            System.out.println("Usage: java EcalMuonCalibrationAnalysisDriver histogramFile.aida <plotDir> <filetype>");
//            System.out.println("       available dirs are 'dimuon' , 'dimuon one cluster' and 'single muon' ");
//            System.out.println("       if plotDir is not specified, plots from 'single muon' will be analyzed");
//            System.out.println("       if filetype not specified, plots will be shown interactively");
//            System.out.println("       if filetype is specified, plots will be written to that format");
//            System.out.println("       available formats are: png, pdf, eps ps svg emf swf");
//            return;
//        }
//        if (args.length > 0) {
//            plotFile = args[0];
//        }
//        if (args.length > 1) {
//            plotDir = args[1];
//        }
//        if (args.length > 2) {
//            showPlots = false;
//            writePlots = true;
//            fileType = args[2];
//        }
        Map<String, String> titleMap = new HashMap<>();
        titleMap.put("dimuon", "diMuon");
        titleMap.put("dimuon one cluster", "diMuonOneCluster");
        titleMap.put("single muon", "singleMuon");
        IAnalysisFactory analysisFactory = IAnalysisFactory.create();
        ITree tree = analysisFactory.createTreeFactory().create(new File(plotFile).getAbsolutePath());
        IDataPointSetFactory dpsf = analysisFactory.createDataPointSetFactory(tree);
        IFunctionFactory functionFactory = analysisFactory.createFunctionFactory(tree);
        IFitFactory fitFactory = analysisFactory.createFitFactory();
        IFitter fitter = fitFactory.createFitter("Chi2", "jminuit");

        // Create Gaussian fitting function
        IFunction gaussian = functionFactory.createFunctionByName("Gaussian", "G");

        // standard style for histogram data
        IPlotterStyle dataStyle = analysisFactory.createPlotterFactory().createPlotterStyle();
        dataStyle.dataStyle().lineStyle().setVisible(true);
        dataStyle.dataStyle().lineStyle().setColor("BLUE");
        dataStyle.dataStyle().lineStyle().setThickness(3);
        dataStyle.dataStyle().fillStyle().setVisible(false);
        dataStyle.dataStyle().errorBarStyle().setColor("BLUE");
        dataStyle.dataStyle().errorBarStyle().setThickness(3);

        // standard style for function
        IPlotterFactory plotterFactory = analysisFactory.createPlotterFactory();
        IPlotterStyle functionStyle = plotterFactory.createPlotterStyle();
        functionStyle.dataStyle().outlineStyle().setColor("red");
        functionStyle.dataStyle().outlineStyle().setThickness(3);
        functionStyle.legendBoxStyle().setVisible(true);
        functionStyle.statisticsBoxStyle().setVisible(true);

        if (tree == null) {
            throw new IllegalArgumentException("Unable to load plot file.");
        }
        // Get the histograms names.
        List<String> objectNameList = getTreeFiles(tree);
        String[] types = {"mu+", "mu-"};

        String plotToFit = "no gain ADC sum";

        double rmsFactor = 1.25;

        //mu+
        {
            List<IPlotter> plotters = new ArrayList<>();
            for (int i = 0; i < 2; ++i) {
                String title = titleMap.get(plotDir) + "fiducial mu- single-crystal cluster track momentum";
                IPlotter plotter = analysisFactory.createPlotterFactory().create(title);
                plotter.setParameter("plotterWidth", plotterWidth);
                plotter.setParameter("plotterHeight", plotterHeight);
                plotter.createRegions(3, 3);
                IPlotterStyle style = plotter.region(8).style();
                style.dataStyle().lineStyle().setVisible(false);
//                String[] lineTypes = style.dataStyle().lineStyle().availableLineTypes();
//                System.out.println(Arrays.toString(lineTypes));
                style.dataStyle().markerStyle().setParameter("color", "blue");
                style.dataStyle().markerStyle().setShape("circle");
                style.dataStyle().errorBarStyle().setParameter("color", "blue");
                style.dataStyle().lineStyle().setParameter("color", "red");
                plotters.add(plotter);
            }
            // move on to track momentum analysis...
            IPlotter plotter = plotters.get(0);
            int currentRegion = 0;
            IDataPointSet dps2D = dpsf.create("dps2D", "Track Momentum Gaussian Mean", 2);
            int nPoint = 0;
            for (int ix = 6; ix < 23; ++ix) {
                if (ix == 15) {
                    //show existing plots
                    if (showPlots) {
                        plotter.show();
                    }
                    if (writePlots) {
                        plotter.writeToFile("hps_010261_Mu+TrackMomentumVsIx_1_" + orientation + ".pdf");
                        plotter.writeToFile("hps_010261_Mu+TrackMomentumVsIx_1_" + orientation + ".png");
                    }
                    // set up new page
                    if (writePlots) {
                        plotter = plotters.get(1);
                    }
                    currentRegion = 0;
                }
                //for (int j = -1; j < 2; j = j + 2) {
                String type = ix < 0 ? types[1] : types[0];
                String histoName = "fiducial " + type + " single-crystal cluster track momentum ix " + ix;
                System.out.println("/" + plotDir + "/clusterAnalysis/" + histoName);
                if (objectNameList.contains("/" + plotDir + "/clusterAnalysis/" + histoName)) {
                    IHistogram1D hist = (IHistogram1D) tree.find("/" + plotDir + "/clusterAnalysis/" + histoName);
                    IPlotterStyle style = plotter.region(currentRegion).style();
                    style.legendBoxStyle().setVisible(false);
//                    String[] legendParams = style.legendBoxStyle().availableParameters();
//                    for (String s : legendParams) {
//                        System.out.println(s);
//                        System.out.println(Arrays.toString(style.legendBoxStyle().availableParameterOptions(s)));
//                    }
                    style.statisticsBoxStyle().setVisible(false);
//                    String[] statParams = style.statisticsBoxStyle().availableParameters();
//                    for (String s : statParams) {
//                        System.out.println(s);
//                        System.out.println(Arrays.toString(style.statisticsBoxStyle().availableParameterOptions(s)));
//                    }
                    style.statisticsBoxStyle().setVisibileStatistics("1110000");
                    style.xAxisStyle().setLabel("Track Momentum [GeV]");
                    plotter.region(currentRegion).plot(hist, dataStyle);
                    dps2D.addPoint(); // do this here in case fit fails.
                    IFitResult fitResult = fitit(hist, fitter, gaussian, rmsFactor);
                    if (fitResult != null) {
                        double lo = fitResult.fittedFunction().parameters()[1] - rmsFactor * fitResult.fittedFunction().parameters()[2];
                        double hi = fitResult.fittedFunction().parameters()[1] + rmsFactor * fitResult.fittedFunction().parameters()[2];
                        plotter.region(currentRegion++).plot(fitResult.fittedFunction(), functionStyle, "range=\"(" + lo + "," + hi + ")\"");
                        double[] params = fitResult.fittedParameters();
                        double[] errors = fitResult.errors();
                        double[] errorsMinus = fitResult.errorsMinus();
                        double[] errorsPlus = fitResult.errorsPlus();
                        dps2D.point(nPoint).coordinate(0).setValue(ix);
                        dps2D.point(nPoint).coordinate(1).setValue(params[1]);
                        dps2D.point(nPoint).coordinate(1).setErrorPlus(errorsPlus[1]);
                        dps2D.point(nPoint).coordinate(1).setErrorMinus(errorsMinus[1]);
                        nPoint++;
                    }
                }
                //}
            }
            plotter.region(8).plot(dps2D);
            if (showPlots) {
                plotter.show();
            }
            if (writePlots) {
                plotter.writeToFile("hps_010261_Mu+TrackMomentumVsIx_2_" + orientation + ".pdf");
                plotter.writeToFile("hps_010261_Mu+TrackMomentumVsIx_2_" + orientation + ".png");
            }

        }
        //mu-
        {
            List<IPlotter> plotters = new ArrayList<>();
            for (int i = 0; i < 2; ++i) {
                String title = titleMap.get(plotDir) + "fiducial mu- single-crystal cluster track momentum";
                IPlotter plotter = analysisFactory.createPlotterFactory().create(title);
                plotter.setParameter("plotterWidth", plotterWidth);
                plotter.setParameter("plotterHeight", plotterHeight);
                plotter.createRegions(3, 3);
                IPlotterStyle style = plotter.region(8).style();
                style.dataStyle().lineStyle().setVisible(false);
//                String[] lineTypes = style.dataStyle().lineStyle().availableLineTypes();
//                System.out.println(Arrays.toString(lineTypes));
                style.dataStyle().markerStyle().setParameter("color", "blue");
                style.dataStyle().markerStyle().setShape("circle");
                style.dataStyle().errorBarStyle().setParameter("color", "blue");
                style.dataStyle().lineStyle().setParameter("color", "red");
                plotters.add(plotter);
            }
            // move on to track momentum analysis...
            IPlotter plotter = plotters.get(0);
            int currentRegion = 0;
            IDataPointSet dps2D = dpsf.create("dps2D", "Track Momentum Gaussian Mean", 2);
            int nPoint = 0;
            for (int ix = -22; ix < -5; ++ix) {
                if (ix == -13) {
                    //show existing plots
                    if (showPlots) {
                        plotter.show();
                    }
                    if (writePlots) {
                        plotter.writeToFile("hps_010261_Mu-TrackMomentumVsIx_1_" + orientation + ".pdf");
                        plotter.writeToFile("hps_010261_Mu-TrackMomentumVsIx_1_" + orientation + ".png");
                    }
                    // set up new page
                    plotter = plotters.get(1);
                    currentRegion = 0;
                }
                //for (int j = -1; j < 2; j = j + 2) {
                String type = ix < 0 ? types[1] : types[0];
                String histoName = "fiducial " + type + " single-crystal cluster track momentum ix " + ix;
                System.out.println("/" + plotDir + "/clusterAnalysis/" + histoName);
                if (objectNameList.contains("/" + plotDir + "/clusterAnalysis/" + histoName)) {
                    IHistogram1D hist = (IHistogram1D) tree.find("/" + plotDir + "/clusterAnalysis/" + histoName);
                    IPlotterStyle style = plotter.region(currentRegion).style();
                    style.legendBoxStyle().setVisible(false);
//                    String[] legendParams = style.legendBoxStyle().availableParameters();
//                    for (String s : legendParams) {
//                        System.out.println(s);
//                        System.out.println(Arrays.toString(style.legendBoxStyle().availableParameterOptions(s)));
//                    }
                    style.statisticsBoxStyle().setVisible(false);
//                    String[] statParams = style.statisticsBoxStyle().availableParameters();
//                    for (String s : statParams) {
//                        System.out.println(s);
//                        System.out.println(Arrays.toString(style.statisticsBoxStyle().availableParameterOptions(s)));
//                    }
                    style.statisticsBoxStyle().setVisibileStatistics("1110000");
                    style.xAxisStyle().setLabel("Track Momentum [GeV]");
                    plotter.region(currentRegion).plot(hist, dataStyle);
                    dps2D.addPoint(); // do this here in case fit fails.
                    IFitResult fitResult = fitit(hist, fitter, gaussian, rmsFactor);
                    if (fitResult != null) {
                        //System.out.println(Arrays.toString(fitter.listParameterSettings()));
                        double lo = fitResult.fittedFunction().parameters()[1] - rmsFactor * fitResult.fittedFunction().parameters()[2];
                        double hi = fitResult.fittedFunction().parameters()[1] + rmsFactor * fitResult.fittedFunction().parameters()[2];
                        plotter.region(currentRegion++).plot(fitResult.fittedFunction(), functionStyle, "range=\"(" + lo + "," + hi + ")\"");
                        double[] params = fitResult.fittedParameters();
                        double[] errors = fitResult.errors();
                        double[] errorsMinus = fitResult.errorsMinus();
                        double[] errorsPlus = fitResult.errorsPlus();
                        dps2D.point(nPoint).coordinate(0).setValue(ix);
                        dps2D.point(nPoint).coordinate(1).setValue(params[1]);
                        dps2D.point(nPoint).coordinate(1).setErrorPlus(errorsPlus[1]);
                        dps2D.point(nPoint).coordinate(1).setErrorMinus(errorsMinus[1]);
                        nPoint++;
                    }
                }
                //}
            }
            plotter.region(8).plot(dps2D);
            if (showPlots) {
                plotter.show();
            }
            if (writePlots) {
                plotter.writeToFile("hps_010261_Mu-TrackMomentumVsIx_2_" + orientation + ".pdf");
                plotter.writeToFile("hps_010261_Mu-TrackMomentumVsIx_2_" + orientation + ".png");
            }
        }
    }

    private static IFitResult fitit(IHistogram1D hist, IFitter fitter, IFunction gaussian, double rmsFactor) {
        IFitResult fitResult = null;
        if (hist.entries() > 25) {
            double[] par = new double[3];
            par[0] = hist.maxBinHeight();
            par[1] = hist.mean();
            par[2] = hist.rms(); //rms
//            double fitPar1lo = 0.5;
//            double fitPar1hi = 2.0;

            double lo = par[1] - rmsFactor * par[2];
            double hi = par[1] + rmsFactor * par[2];
            gaussian.setParameters(par);

            fitResult = fitter.fit(hist, gaussian, "range=\"(" + lo + "," + hi + ")\"");
            if (!fitResult.isValid()) {
                System.out.println("fit result not valid!");
                return null;
            }
            //iterate once?
            IFunction f = fitResult.fittedFunction();
            double[] fitPars = f.parameters();

            par[0] = fitPars[0];
            par[1] = fitPars[1];
            par[2] = abs(fitPars[2]);
            lo = par[1] - rmsFactor * par[2]; //rms;
            hi = par[1] + rmsFactor * par[2]; //rms;
            gaussian.setParameters(par);
            fitResult = fitter.fit(hist, gaussian, "range=\"(" + lo + "," + hi + ")\"");
            //System.out.println(Arrays.toString(fitResult.constraints()));
        }
        return fitResult;
    }

    private static final List<String> getTreeFiles(ITree tree) {
        return getTreeFiles(tree, "/");
    }

    private static final List<String> getTreeFiles(ITree tree, String rootDir) {
        // Make a list to contain the plot names.
        List<String> list = new ArrayList<String>();

        // Iterate over the objects at the indicated directory of the tree.
        String objectNames[] = tree.listObjectNames(rootDir);
        for (String objectName : objectNames) {
            // Convert the object name to a char array and check the
            // last character. Directories end in '/'.
            char[] plotChars = objectName.toCharArray();

            // If the object is a directory, process any objects inside
            // of it as well.
            if (plotChars[plotChars.length - 1] == '/') {
                List<String> dirList = getTreeFiles(tree, objectName);
                list.addAll(dirList);
            } // Otherwise, just add the object to the list.
            else {
                list.add(objectName);
            }
        }

        // Return the compiled list.
        return list;
    }
}
