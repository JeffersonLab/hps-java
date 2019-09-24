package org.hps.analysis.examples;

import hep.aida.IAnalysisFactory;
import hep.aida.IDataPoint;
import hep.aida.IDataPointSet;
import hep.aida.IDataPointSetFactory;
import hep.aida.IFitData;
import hep.aida.IFitFactory;
import hep.aida.IFitResult;
import hep.aida.IFitter;
import hep.aida.IFunction;
import hep.aida.IFunctionFactory;
import hep.aida.IHistogram2D;
import hep.aida.IPlotter;
import hep.aida.IPlotterFactory;
import hep.aida.IPlotterStyle;
import hep.aida.IProfile1D;
import hep.aida.ITree;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author ngraf
 */
public class FitAlignment {

    public static void main(String[] args) throws IllegalArgumentException, IOException {
        // Define the root directory for the plots.
        boolean showPlots = true;
        boolean writePlots = false;
        String fileType = "pdf"; // png, pdf, eps ps svg emf swf
        String rootDir = null;
        String plotFile = "D:/work/hps/analysis/mollerAlignment/2015_MollerSkim_pass8_PC.aida";
        if (args.length > 0) {
            plotFile = args[0];
        }
        if (args.length > 1) {
            showPlots = false;
            writePlots = true;
            fileType = args[1];
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
        IDataPointSetFactory dpsf = analysisFactory.createDataPointSetFactory(tree);
        IPlotterFactory plotterFactory = analysisFactory.createPlotterFactory();

        IPlotterStyle functionStyle = plotterFactory.createPlotterStyle();
        functionStyle.dataStyle().outlineStyle().setColor("red");
        functionStyle.dataStyle().outlineStyle().setThickness(5);
        functionStyle.legendBoxStyle().setVisible(false);
        functionStyle.statisticsBoxStyle().setVisible(false);

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
        int[] eBins = {35, 4, 45, 5, 55, 6, 65, 7, 75};
        String[] halves = {"Top", "Bottom"};
//        for (String s : objectNameList) {
//            IProfile1D prof = null;
//            if (s.contains("Profile")) {
//                System.out.println(s);
//                prof = (IProfile1D) tree.find(s);
//            }
        IPlotter scatterXvsYPlotter = analysisFactory.createPlotterFactory().create("ThetaX vs ThetaY scatter plots");
        scatterXvsYPlotter.createRegions(3, 3);
        scatterXvsYPlotter.setParameter("plotterWidth", "1600");
        scatterXvsYPlotter.setParameter("plotterHeight", "900");

        for (int j = 0; j < eBins.length; ++j) {
            IHistogram2D scatter = (IHistogram2D) tree.find("0." + eBins[j] + " Track thetaX vs ThetaY ");
            scatterXvsYPlotter.region(j).plot(scatter, scatterPlotStyle);
        }
        if (showPlots) {
            scatterXvsYPlotter.show();
        }
        if (writePlots) {
            scatterXvsYPlotter.writeToFile("thetaXvsThetaY." + fileType);
        }

        //let's store some parameters that we're going to be fitting...
        double[] p0fit_bottom = new double[eBins.length];
        double[] p0fiterror_bottom = new double[eBins.length];
        double[] p0fit_top = new double[eBins.length];
        double[] p0fiterror_top = new double[eBins.length];
        double[] p1fit_bottom = new double[eBins.length];
        double[] p1fiterror_bottom = new double[eBins.length];
        double[] p1fit_top = new double[eBins.length];
        double[] p1fiterror_top = new double[eBins.length];
        double[] binEnergies = {375, 425, 475, 525, 575, 625, 675, 725, 775};

        // loop over detector halves
        for (String half : halves) {
            IPlotter profilePlotter = analysisFactory.createPlotterFactory().create(half + " profile plots");
            profilePlotter.createRegions(3, 3);
            profilePlotter.setParameter("plotterWidth", "1600");
            profilePlotter.setParameter("plotterHeight", "900");
            IPlotter scatterPlotter = analysisFactory.createPlotterFactory().create(half + " scatter plots");
            scatterPlotter.createRegions(3, 3);
            scatterPlotter.setParameter("plotterWidth", "1600");
            scatterPlotter.setParameter("plotterHeight", "900");

            //loop over interesting energy bins
            for (int j = 0; j < eBins.length; ++j) {
                IHistogram2D scatter = (IHistogram2D) tree.find("0." + eBins[j] + " " + half + " Track phi vs dTheta");
                scatterPlotter.region(j).plot(scatter, scatterPlotStyle);

                IProfile1D prof = (IProfile1D) tree.find("0." + eBins[j] + " " + half + " Track phi vs dTheta Profile");
                if (prof != null) {
                    IFunction line = functionFactory.createFunctionByName("line", "p1");
                    IDataPointSet profDataPointSet = dpsf.create("dpsFromProf", prof);
                    //plotter.region(1).plot(profDataPointSet);
                    //manually set uncertainties to be the error on the mean (not the rms)
                    for (int i = 0; i < prof.axis().bins(); ++i) {
                        profDataPointSet.point(i).coordinate(1).setErrorPlus(prof.binError(i));
                        profDataPointSet.point(i).coordinate(1).setErrorMinus(prof.binError(i));
                    }

                    IFitData fitData = fitFactory.createFitData();
                    fitData.create1DConnection(profDataPointSet, 0, 1);
                    fitData.range(0).excludeAll();
                    fitData.range(0).include(-0.4, 0.4);
                    profilePlotter.region(j).plot(profDataPointSet, profilePlotStyle);
                    IFitResult result = fitter.fit(fitData, line);
                    profilePlotter.region(j).plot(result.fittedFunction(), functionStyle, "range=\"(-0.4,0.4)\"");
                    //store the results of the fit...
                    double[] parameters = result.fittedParameters();
                    double[] errors = result.errors();
                    if (half.equals("Top")) {
                        p0fit_top[j] = parameters[0];
                        p0fiterror_top[j] = errors[0];
                        p1fit_top[j] = parameters[1];
                        p1fiterror_top[j] = errors[1];
                    } else {
                        p0fit_bottom[j] = parameters[0];
                        p0fiterror_bottom[j] = errors[0];
                        p1fit_bottom[j] = parameters[1];
                        p1fiterror_bottom[j] = errors[1];
                    }
                }
            }
            if (showPlots) {
                profilePlotter.show();
                scatterPlotter.show();
            }
            if (writePlots) {
                profilePlotter.writeToFile(half + "profilePlots." + fileType);
                scatterPlotter.writeToFile(half + "scatterPlots." + fileType);
            }
        }
        // Create a two dimensional IDataPointSet.
        IDataPointSet topP0dataPointSet = dpsf.create("dataPointSet", "top p0", 2);
        IDataPointSet topP1dataPointSet = dpsf.create("dataPointSet", "top p1", 2);
        IDataPointSet bottomP0dataPointSet = dpsf.create("dataPointSet", "bottom p0", 2);
        IDataPointSet bottomP1dataPointSet = dpsf.create("dataPointSet", "bottom p1", 2);

        IDataPoint dp;
        for (int jj = 0; jj < binEnergies.length; ++jj) {
//            System.out.println(binEnergies[jj] + " :");
//            System.out.println("top p0 " + p0fit_top[jj]);
//            System.out.println("top p1 " + p1fit_top[jj]);
//            System.out.println("bottom p0 " + p0fit_bottom[jj]);
//            System.out.println("bottom p1 " + p1fit_bottom[jj]);
            topP0dataPointSet.addPoint();
            dp = topP0dataPointSet.point(jj);
            dp.coordinate(0).setValue(binEnergies[jj]);
            dp.coordinate(1).setValue(p0fit_top[jj]);
            dp.coordinate(1).setErrorPlus(p0fiterror_top[jj]);
            dp.coordinate(1).setErrorMinus(p0fiterror_top[jj]);

            topP1dataPointSet.addPoint();
            dp = topP1dataPointSet.point(jj);
            dp.coordinate(0).setValue(binEnergies[jj]);
            dp.coordinate(1).setValue(p1fit_top[jj]);
            dp.coordinate(1).setErrorPlus(p1fiterror_top[jj]);
            dp.coordinate(1).setErrorMinus(p1fiterror_top[jj]);

            bottomP0dataPointSet.addPoint();
            dp = bottomP0dataPointSet.point(jj);
            dp.coordinate(0).setValue(binEnergies[jj]);
            dp.coordinate(1).setValue(p0fit_bottom[jj]);
            dp.coordinate(1).setErrorPlus(p0fiterror_bottom[jj]);
            dp.coordinate(1).setErrorMinus(p0fiterror_bottom[jj]);

            bottomP1dataPointSet.addPoint();
            dp = bottomP1dataPointSet.point(jj);
            dp.coordinate(0).setValue(binEnergies[jj]);
            dp.coordinate(1).setValue(p1fit_bottom[jj]);
            dp.coordinate(1).setErrorPlus(p1fiterror_bottom[jj]);
            dp.coordinate(1).setErrorMinus(p1fiterror_bottom[jj]);
        }

        IPlotter plotter = analysisFactory.createPlotterFactory().create("parameter fits");
        plotter.setParameter("plotterWidth", "1600");
        plotter.setParameter("plotterHeight", "900");

        plotter.createRegions(2, 2);
        plotter.region(0).plot(topP0dataPointSet);
        plotter.region(1).plot(topP1dataPointSet);
        plotter.region(2).plot(bottomP0dataPointSet);
        plotter.region(3).plot(bottomP1dataPointSet);

        IFunction line = functionFactory.createFunctionByName("line", "p1");

        IFitResult topP0vsPLineFitresult = fitter.fit(topP0dataPointSet, line);
        plotter.region(0).plot(topP0vsPLineFitresult.fittedFunction());
        IFitResult bottomP0vsPLineFitresult = fitter.fit(bottomP0dataPointSet, line);
        plotter.region(2).plot(bottomP0vsPLineFitresult.fittedFunction());
        if (showPlots) {
            plotter.show();
        }
        if (writePlots) {
            plotter.writeToFile("parameterFits." + fileType);
        }
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
