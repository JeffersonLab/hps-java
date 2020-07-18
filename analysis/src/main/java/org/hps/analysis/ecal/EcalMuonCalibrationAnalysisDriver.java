package org.hps.analysis.ecal;

import hep.aida.IAnalysisFactory;
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
 * Analyzes output plots from EcalMuonGainCalibrationDriver
 *
 * @author Norman A. Graf
 */
public class EcalMuonCalibrationAnalysisDriver {

    public static void main(String[] args) throws IllegalArgumentException, IOException {
        AIDA aida = AIDA.defaultInstance();
        Map<Integer, Integer> regionMap = new HashMap<>();
        regionMap.put(5, 0);
        regionMap.put(4, 1);
        regionMap.put(3, 2);
        regionMap.put(2, 3);
        regionMap.put(1, 4);
        regionMap.put(-1, 5);
        regionMap.put(-2, 6);
        regionMap.put(-3, 7);
        regionMap.put(-4, 8);
        regionMap.put(-5, 9);

        // Define the root directory for the plots.
        boolean showPlots = true;
        boolean writePlots = false;
        String plotDir = "single muon";
        String fileType = "pdf"; // png, pdf, eps ps svg emf swf
        String rootDir = null;
        String plotFile = null;
        boolean debug = false;
        if (debug) {
            plotFile = "D:/work/hps/analysis/physrun2019/ecalibration/prodSingleMuonSkim/combined_plots_20200712.aida";
        } else {
            if (args.length == 0) {
                System.out.println("Usage: java EcalMuonCalibrationAnalysisDriver histogramFile.aida <plotDir> <filetype>");
                System.out.println("       available dirs are 'dimuon' , 'dimuon one cluster' and 'single muon' ");
                System.out.println("       if plotDir is not specified, plots from 'single muon' will be analyzed");
                System.out.println("       if filetype not specified, plots will be shown interactively");
                System.out.println("       if filetype is specified, plots will be written to that format");
                System.out.println("       available formats are: png, pdf, eps ps svg emf swf");
                return;
            }
            if (args.length > 0) {
                plotFile = args[0];
            }
            if (args.length > 1) {
                plotDir = args[1];
            }
            if (args.length > 2) {
                showPlots = false;
                writePlots = true;
                fileType = args[2];
            }
        }

        // Get the plots file and open it.
        IAnalysisFactory analysisFactory = IAnalysisFactory.create();
//        IPlotter plotter = analysisFactory.createPlotterFactory().create("Fit.java Plot");
        ITree tree = analysisFactory.createTreeFactory().create(new File(plotFile).getAbsolutePath());
        if (tree == null) {
            throw new IllegalArgumentException("Unable to load plot file.");
        }
        IFunctionFactory functionFactory = analysisFactory.createFunctionFactory(tree);
        IFitFactory fitFactory = analysisFactory.createFitFactory();
        IFitter fitter = fitFactory.createFitter("Chi2", "jminuit");

        // Create Gaussian fitting function
        IFunction gaussian = functionFactory.createFunctionByName("Gaussian", "G");

        // standard style for histogram data
        IPlotterStyle dataStyle = analysisFactory.createPlotterFactory().createPlotterStyle();
        dataStyle.dataStyle().lineStyle().setVisible(true);
        dataStyle.dataStyle().lineStyle().setColor("BLUE");
        dataStyle.dataStyle().lineStyle().setThickness(5);
        dataStyle.dataStyle().fillStyle().setVisible(false);
        dataStyle.dataStyle().errorBarStyle().setColor("BLUE");
        dataStyle.dataStyle().errorBarStyle().setThickness(5);

        IDataPointSetFactory dpsf = analysisFactory.createDataPointSetFactory(tree);
        IPlotterFactory plotterFactory = analysisFactory.createPlotterFactory();

        IPlotterStyle functionStyle = plotterFactory.createPlotterStyle();
        functionStyle.dataStyle().outlineStyle().setColor("red");
        functionStyle.dataStyle().outlineStyle().setThickness(7);
        functionStyle.legendBoxStyle().setVisible(true);
        functionStyle.statisticsBoxStyle().setVisible(true);

        IPlotterStyle profilePlotStyle = plotterFactory.createPlotterStyle();
        profilePlotStyle.dataStyle().fillStyle().setColor("blue");
        profilePlotStyle.dataStyle().errorBarStyle().setColor("blue");
        profilePlotStyle.legendBoxStyle().setVisible(true);
        profilePlotStyle.statisticsBoxStyle().setVisible(true);
        profilePlotStyle.yAxisStyle().setParameter("lowerLimit", "-0.005");
        profilePlotStyle.yAxisStyle().setParameter("upperLimit", "0.005");

        IPlotterStyle scatterPlotStyle = plotterFactory.createPlotterStyle();
//        scatterPlotStyle.dataStyle().fillStyle().setColor("blue");
//        scatterPlotStyle.dataStyle().errorBarStyle().setColor("blue");
        scatterPlotStyle.legendBoxStyle().setVisible(false);
        scatterPlotStyle.statisticsBoxStyle().setVisible(false);
        scatterPlotStyle.setParameter("hist2DStyle", "colorMap");
        scatterPlotStyle.dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
        scatterPlotStyle.zAxisStyle().setParameter("scale", "logarithmic");
//        scatterPlotStyle.yAxisStyle().setParameter("lowerLimit", "-0.005");
//        scatterPlotStyle.yAxisStyle().setParameter("upperLimit", "0.005");

//        IPlotterStyle axisStyle = plotterFactory.createPlotterStyle();
//        String[] params = axisStyle.yAxisStyle().availableParameters();
//        for (String s : params) {
//            System.out.println("parameter " + s);
//            String[] opts = axisStyle.yAxisStyle().availableParameterOptions(s);
//            for (String opt : opts) {
//                System.out.println("option " + opt);
//            }
//        }
        // Get the histograms names.
        List<String> objectNameList = getTreeFiles(tree);
        String[] types = {"mu+", "mu-"};
        List<IPlotter> plotters = new ArrayList<>();
        IPlotter tst = analysisFactory.createPlotterFactory().create("ECal MIP Gaussian mean");
        tst.createRegions(1, 10);
//        tst.setParameter("plotterWidth", "1600");
//        tst.setParameter("plotterHeight", "900");
        for (String type : types) {
            for (int indexx = 1; indexx < 24; ++indexx) {
                int ix = indexx;
                if (type.equals("mu-")) {
                    ix = -indexx;
                }
//                int region = 0;
                for (int iy = -5; iy < 6; ++iy) {
//                    if (iy != 0) {
//                        region++;
//                    }

                    String histoName = ix + " " + iy + " " + type + " crystal energy";
                    if (objectNameList.contains("/" + plotDir + "/clusterAnalysis/" + histoName)) {
                        IHistogram1D hist = (IHistogram1D) tree.find("/" + plotDir + "/clusterAnalysis/" + histoName);

                        if (hist.entries() > 1000) {
                            double[] par = new double[3];
                            par[0] = hist.maxBinHeight();
                            par[1] = ix < 0 ? 0.176 : 0.184;
                            par[2] = 0.02;
                            if (plotDir.equals("mc")) {
                                par[1] = hist.mean();
//                                par[2] = hist.rms();
                            }
                            double lo = par[1] - 1.5 * par[2];
                            double hi = par[1] + 1.5 * par[2];
                            gaussian.setParameters(par);
                            try {
                                IFitResult fitResult = fitter.fit(hist, gaussian, "range=\"(" + lo + "," + hi + ")\"");
                                if (!fitResult.isValid()) {
                                    System.out.println("fit result not valid!");
                                }
                                IPlotter plotter = analysisFactory.createPlotterFactory().create(histoName);
                                plotter.region(0).plot(hist, dataStyle);
//                                plotter.region(0).plot(fitResult.fittedFunction(), functionStyle, "range=\"(" + lo + "," + hi + ")\"");
//                                plotter.show();
                                //iterate once?
                                IFunction f = fitResult.fittedFunction();
                                double[] fitPars = f.parameters();

                                par[0] = fitPars[0];
                                par[1] = fitPars[1];
                                par[2] = abs(fitPars[2]);
                                lo = par[1] - 1. * par[2];
                                hi = par[1] + 1. * par[2];
                                gaussian.setParameters(par);
                                fitResult = fitter.fit(hist, gaussian, "range=\"(" + lo + "," + hi + ")\"");
                                f = fitResult.fittedFunction();
                                fitPars = f.parameters();
                                String[] parNames = f.parameterNames();
//                            for (int i = 0; i < f.numberOfParameters(); ++i) {
//                                System.out.println(parNames[i] + " : " + fitPars[i]);
//                            }
                                System.out.println(histoName + " has " + hist.allEntries() + " entries: " + parNames[0] + " : " + fitPars[0] + " " + parNames[1] + " : " + fitPars[1] + " " + parNames[2] + " : " + fitPars[2]);
                                //plotter.region(0).plot(hist, dataStyle);
                                plotter.region(0).plot(fitResult.fittedFunction(), functionStyle, "range=\"(" + lo + "," + hi + ")\"");
                                if (fitPars[1] > 0.1 && fitPars[1] < 0.3) { // only continue with "good" fits
                                    aida.histogram1D(type + " ECal Row " + iy + " MIP mean", 47, -23.5, 23.5).fill(ix, fitPars[1]);
                                    aida.histogram2D("Cluster ix iy MIP peak mean energy", 47, -23.5, 23.5, 11, -5.5, 5.5).fill(ix, iy, fitPars[1]);
                                    aida.histogram2D(type + " Cluster ix iy MIP peak mean energy", 47, -23.5, 23.5, 11, -5.5, 5.5).fill(ix, iy, fitPars[1]);
                                    tst.region(regionMap.get(iy)).plot(aida.histogram1D(type + " ECal Row " + iy + " MIP mean"));

                                    // test multiple plot plotter
//                            tst.region(region).plot(hist, dataStyle);
//                            tst.region(region).plot(fitResult.fittedFunction(), functionStyle, "range=\"(" + lo + "," + hi + ")\"");
                                    if (showPlots) {
                                        plotter.show();
//                                tst.show();
//                                plotter.clearRegions();
                                    }

                                    if (writePlots) {
                                        plotter.writeToFile(plotDir + " Single Crystal Cluster " + ix + " " + iy + " " + type + " MIP peak mean energy.pdf");
                                        plotter.writeToFile(plotDir + " Single Crystal Cluster " + ix + " " + iy + " " + type + " MIP peak mean energy.png");
                                        plotters.add(plotter);
                                    }
                                }
                            } catch (Exception ex) {
                                //System.out.println("problem with histogram /single muon/clusterAnalysis/" + histoName);
                                ex.printStackTrace();
                            }
                        }
                    }
                }
            }
        }
        aida.saveAs(plotDir + " ECalibration.aida");
        aida.saveAs(plotDir + " ECalibration.root");
//        if(writePlots)
//        {
//            String[] comments = {"test output"};
//            ExportPdf.write(plotters, "ECalibrationPlots.pdf",comments);
//        }
//        int[] eBins = {35, 4, 45, 5, 55, 6, 65, 7, 75};
//        String[] halves = {"Top", "Bottom"};
////        for (String s : objectNameList) {
////            IProfile1D prof = null;
////            if (s.contains("Profile")) {
////                System.out.println(s);
////                prof = (IProfile1D) tree.find(s);
////            }
//        IPlotter scatterXvsYPlotter = analysisFactory.createPlotterFactory().create("ThetaX vs ThetaY scatter plots");
//        scatterXvsYPlotter.createRegions(3, 3);
//        scatterXvsYPlotter.setParameter("plotterWidth", "1600");
//        scatterXvsYPlotter.setParameter("plotterHeight", "900");
//
//        for (int j = 0; j < eBins.length; ++j) {
//            IHistogram2D scatter = (IHistogram2D) tree.find("0." + eBins[j] + " Track thetaX vs ThetaY ");
//            scatterXvsYPlotter.region(j).plot(scatter, scatterPlotStyle);
//        }
//        if (showPlots) {
//            scatterXvsYPlotter.show();
//        }
//        if (writePlots) {
//            scatterXvsYPlotter.writeToFile("thetaXvsThetaY." + fileType);
//        }
//
//        //let's store some parameters that we're going to be fitting...
//        double[] p0fit_bottom = new double[eBins.length];
//        double[] p0fiterror_bottom = new double[eBins.length];
//        double[] p0fit_top = new double[eBins.length];
//        double[] p0fiterror_top = new double[eBins.length];
//        double[] p1fit_bottom = new double[eBins.length];
//        double[] p1fiterror_bottom = new double[eBins.length];
//        double[] p1fit_top = new double[eBins.length];
//        double[] p1fiterror_top = new double[eBins.length];
//        double[] binEnergies = {375, 425, 475, 525, 575, 625, 675, 725, 775};
//
//        // loop over detector halves
//        for (String half : halves) {
//            IPlotter profilePlotter = analysisFactory.createPlotterFactory().create(half + " profile plots");
//            profilePlotter.createRegions(3, 3);
//            profilePlotter.setParameter("plotterWidth", "1600");
//            profilePlotter.setParameter("plotterHeight", "900");
//            IPlotter scatterPlotter = analysisFactory.createPlotterFactory().create(half + " scatter plots");
//            scatterPlotter.createRegions(3, 3);
//            scatterPlotter.setParameter("plotterWidth", "1600");
//            scatterPlotter.setParameter("plotterHeight", "900");
//
//            //loop over interesting energy bins
//            for (int j = 0; j < eBins.length; ++j) {
//                IHistogram2D scatter = (IHistogram2D) tree.find("0." + eBins[j] + " " + half + " Track phi vs dTheta");
//                scatterPlotter.region(j).plot(scatter, scatterPlotStyle);
//
//                IProfile1D prof = (IProfile1D) tree.find("0." + eBins[j] + " " + half + " Track phi vs dTheta Profile");
//                if (prof != null) {
//                    IFunction line = functionFactory.createFunctionByName("line", "p1");
//                    IDataPointSet profDataPointSet = dpsf.create("dpsFromProf", prof);
//                    //plotter.region(1).plot(profDataPointSet);
//                    //manually set uncertainties to be the error on the mean (not the rms)
//                    for (int i = 0; i < prof.axis().bins(); ++i) {
//                        profDataPointSet.point(i).coordinate(1).setErrorPlus(prof.binError(i));
//                        profDataPointSet.point(i).coordinate(1).setErrorMinus(prof.binError(i));
//                    }
//
//                    IFitData fitData = fitFactory.createFitData();
//                    fitData.create1DConnection(profDataPointSet, 0, 1);
//                    fitData.range(0).excludeAll();
//                    fitData.range(0).include(-0.4, 0.4);
//                    profilePlotter.region(j).plot(profDataPointSet, profilePlotStyle);
//                    IFitResult result = fitter.fit(fitData, line);
//                    profilePlotter.region(j).plot(result.fittedFunction(), functionStyle, "range=\"(-0.4,0.4)\"");
//                    //store the results of the fit...
//                    double[] parameters = result.fittedParameters();
//                    double[] errors = result.errors();
//                    if (half.equals("Top")) {
//                        p0fit_top[j] = parameters[0];
//                        p0fiterror_top[j] = errors[0];
//                        p1fit_top[j] = parameters[1];
//                        p1fiterror_top[j] = errors[1];
//                    } else {
//                        p0fit_bottom[j] = parameters[0];
//                        p0fiterror_bottom[j] = errors[0];
//                        p1fit_bottom[j] = parameters[1];
//                        p1fiterror_bottom[j] = errors[1];
//                    }
//                }
//            }
//            if (showPlots) {
//                profilePlotter.show();
//                scatterPlotter.show();
//            }
//            if (writePlots) {
//                profilePlotter.writeToFile(half + "profilePlots." + fileType);
//                scatterPlotter.writeToFile(half + "scatterPlots." + fileType);
//            }
//        }
//        // Create a two dimensional IDataPointSet.
//        IDataPointSet topP0dataPointSet = dpsf.create("dataPointSet", "top p0", 2);
//        IDataPointSet topP1dataPointSet = dpsf.create("dataPointSet", "top p1", 2);
//        IDataPointSet bottomP0dataPointSet = dpsf.create("dataPointSet", "bottom p0", 2);
//        IDataPointSet bottomP1dataPointSet = dpsf.create("dataPointSet", "bottom p1", 2);
//
//        IDataPoint dp;
//        for (int jj = 0; jj < binEnergies.length; ++jj) {
////            System.out.println(binEnergies[jj] + " :");
////            System.out.println("top p0 " + p0fit_top[jj]);
////            System.out.println("top p1 " + p1fit_top[jj]);
////            System.out.println("bottom p0 " + p0fit_bottom[jj]);
////            System.out.println("bottom p1 " + p1fit_bottom[jj]);
//            topP0dataPointSet.addPoint();
//            dp = topP0dataPointSet.point(jj);
//            dp.coordinate(0).setValue(binEnergies[jj]);
//            dp.coordinate(1).setValue(p0fit_top[jj]);
//            dp.coordinate(1).setErrorPlus(p0fiterror_top[jj]);
//            dp.coordinate(1).setErrorMinus(p0fiterror_top[jj]);
//
//            topP1dataPointSet.addPoint();
//            dp = topP1dataPointSet.point(jj);
//            dp.coordinate(0).setValue(binEnergies[jj]);
//            dp.coordinate(1).setValue(p1fit_top[jj]);
//            dp.coordinate(1).setErrorPlus(p1fiterror_top[jj]);
//            dp.coordinate(1).setErrorMinus(p1fiterror_top[jj]);
//
//            bottomP0dataPointSet.addPoint();
//            dp = bottomP0dataPointSet.point(jj);
//            dp.coordinate(0).setValue(binEnergies[jj]);
//            dp.coordinate(1).setValue(p0fit_bottom[jj]);
//            dp.coordinate(1).setErrorPlus(p0fiterror_bottom[jj]);
//            dp.coordinate(1).setErrorMinus(p0fiterror_bottom[jj]);
//
//            bottomP1dataPointSet.addPoint();
//            dp = bottomP1dataPointSet.point(jj);
//            dp.coordinate(0).setValue(binEnergies[jj]);
//            dp.coordinate(1).setValue(p1fit_bottom[jj]);
//            dp.coordinate(1).setErrorPlus(p1fiterror_bottom[jj]);
//            dp.coordinate(1).setErrorMinus(p1fiterror_bottom[jj]);
//        }
//
//        IPlotter plotter = analysisFactory.createPlotterFactory().create("parameter fits");
//        plotter.setParameter("plotterWidth", "1600");
//        plotter.setParameter("plotterHeight", "900");
//
//        plotter.createRegions(2, 2);
//        plotter.region(0).plot(topP0dataPointSet);
//        plotter.region(1).plot(topP1dataPointSet);
//        plotter.region(2).plot(bottomP0dataPointSet);
//        plotter.region(3).plot(bottomP1dataPointSet);
//
//        IFunction line = functionFactory.createFunctionByName("line", "p1");
//
//        IFitResult topP0vsPLineFitresult = fitter.fit(topP0dataPointSet, line);
//        plotter.region(0).plot(topP0vsPLineFitresult.fittedFunction());
//        IFitResult bottomP0vsPLineFitresult = fitter.fit(bottomP0dataPointSet, line);
//        plotter.region(2).plot(bottomP0vsPLineFitresult.fittedFunction());
//        if (showPlots) {
//            plotter.show();
//        }
//        if (writePlots) {
//            plotter.writeToFile("parameterFits." + fileType);
//        }
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
