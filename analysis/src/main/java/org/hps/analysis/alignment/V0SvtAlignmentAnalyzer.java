package org.hps.analysis.alignment;

import hep.aida.IAnalysisFactory;
import hep.aida.IAxis;
import hep.aida.IDataPoint;
import hep.aida.IDataPointSet;
import hep.aida.IDataPointSetFactory;
import hep.aida.IFitData;
import hep.aida.IFitFactory;
import hep.aida.IFitResult;
import hep.aida.IFitter;
import hep.aida.IFunction;
import hep.aida.IFunctionFactory;
import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.aida.IHistogramFactory;
import hep.aida.IPlotter;
import hep.aida.IPlotterFactory;
import hep.aida.IPlotterStyle;
import hep.aida.ITree;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.lcsim.util.aida.AIDA;

/**
 *
 * @author ngraf
 */
public class V0SvtAlignmentAnalyzer {

    public static void main(String[] args) throws IllegalArgumentException, IOException {
        AIDA aida = AIDA.defaultInstance();
        // Define the root directory for the plots.
        boolean showPlots = false;
        boolean writePlots = true;
        String fileType = "pdf"; // png, pdf, eps ps svg emf swf
        String rootDir = null;
        String plotFile = "D:/work/hps/analysis/pass9_tests/2015_5772_v2Detector_V0SvtAlignment.aida"; //2015_MC_-5mm_SvtAlignment_p2.aida";
//        String plotFile = "D:/work/hps/analysis/pass9_tests/2015_MC_-5mm_SvtAlignment_p2.aida";
//        String plotFile = "D:/work/hps/analysis/pass9_tests/2015_MC_-5mm_APrime_50_SvtAlignment.aida";
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
        IHistogramFactory hf = analysisFactory.createHistogramFactory(aida.tree());
        IFunctionFactory functionFactory = analysisFactory.createFunctionFactory(tree);
        IFitFactory fitFactory = analysisFactory.createFitFactory();
        IFitter fitter = fitFactory.createFitter("Chi2", "jminuit");
        IDataPointSetFactory dpsf = analysisFactory.createDataPointSetFactory(tree);
        IPlotterFactory plotterFactory = analysisFactory.createPlotterFactory();

        IPlotterStyle functionStyle = plotterFactory.createPlotterStyle();
        functionStyle.dataStyle().outlineStyle().setColor("red");
        functionStyle.dataStyle().outlineStyle().setThickness(5);
        functionStyle.legendBoxStyle().setVisible(true);
//        functionStyle.statisticsBoxStyle().setVisible(true);

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
        // Get the histograms names.
        List<String> objectNameList = getTreeFiles(tree, "/");
        for (String histo : objectNameList) {
            System.out.println(histo);
        }
        IHistogram2D bottomHiPPlot = (IHistogram2D) tree.find("/allTracks  electron  bottom  high mom track thetaY vs z0");
        IHistogram2D bottomLoPPlot = (IHistogram2D) tree.find("/allTracks  electron  bottom  low mom track thetaY vs z0");
        IHistogram2D topHiPPlot = (IHistogram2D) tree.find("/allTracks  electron  top  high mom track thetaY vs z0");
        IHistogram2D topLoPPlot = (IHistogram2D) tree.find("/allTracks  electron  top  low mom track thetaY vs z0");
//        IHistogram2D bottomPosiLoPPlot = (IHistogram2D) tree.find("/allTracks  positron  bottom  track thetaY vs z0");
//        IHistogram2D topPosiLoPPlot = (IHistogram2D) tree.find("/allTracks  positron  top  track thetaY vs z0");

        IDataPointSet bottomHiPdataPointSet = dpsf.create("dataPointSet", "bottom high mom", 2);
        IDataPointSet bottomLoPdataPointSet = dpsf.create("dataPointSet", "bottom low mom", 2);
        IDataPointSet topHiPdataPointSet = dpsf.create("dataPointSet", "top high mom", 2);
        IDataPointSet topLoPdataPointSet = dpsf.create("dataPointSet", "top low mom", 2);

//        IDataPointSet bottomPosiLoPdataPointSet = dpsf.create("dataPointSet", "bottom low mom", 2);
//        IDataPointSet topPosiLoPdataPointSet = dpsf.create("dataPointSet", "bottom low mom", 2);

        sliceAndFit(bottomHiPPlot, bottomHiPdataPointSet, hf);
        sliceAndFit(bottomLoPPlot, bottomLoPdataPointSet, hf);
        sliceAndFit(topHiPPlot, topHiPdataPointSet, hf);
        sliceAndFit(topLoPPlot, topLoPdataPointSet, hf);

//        sliceAndFit(bottomPosiLoPPlot, bottomPosiLoPdataPointSet, hf);
//        sliceAndFit(topPosiLoPPlot, topPosiLoPdataPointSet, hf);

        IPlotter plotter = analysisFactory.createPlotterFactory().create("slice gaussian fits");
        plotter.createRegions(2, 2);
        plotter.setParameter("plotterWidth", "1600");
        plotter.setParameter("plotterHeight", "900");

        plotter.region(0).plot(bottomHiPdataPointSet);
        IFunction line0 = functionFactory.createFunctionByName("line", "p1");
        IFitResult bottomHiPLineFitresult = fitter.fit(bottomHiPdataPointSet, line0);
        plotter.region(0).plot(bottomHiPLineFitresult.fittedFunction(), functionStyle);

        plotter.region(1).plot(bottomLoPdataPointSet);
        IFunction line1 = functionFactory.createFunctionByName("line", "p1");
        IFitResult bottomLoPLineFitresult = fitter.fit(bottomLoPdataPointSet, line1);
        plotter.region(1).plot(bottomLoPLineFitresult.fittedFunction(), functionStyle);

        plotter.region(2).plot(topHiPdataPointSet);
        IFunction line2 = functionFactory.createFunctionByName("line", "p1");
        IFitResult topHiPLineFitresult = fitter.fit(topHiPdataPointSet, line2);
        plotter.region(2).plot(topHiPLineFitresult.fittedFunction(), functionStyle);

        plotter.region(3).plot(topLoPdataPointSet);
        IFunction line3 = functionFactory.createFunctionByName("line", "p1");
        IFitResult topLoPLineFitresult = fitter.fit(topLoPdataPointSet, line3);
        plotter.region(3).plot(topLoPLineFitresult.fittedFunction(), functionStyle);

        System.out.println(" fit status: " + topLoPLineFitresult.fitStatus());
        double[] resFitLinePars = topLoPLineFitresult.fittedParameters();
        double[] resFitLineParErrors = topLoPLineFitresult.errors();
        String[] resFitLineParNames = topLoPLineFitresult.fittedParameterNames();

        System.out.println(" Energy Resolution Fit: ");
        for (int i = 0; i < resFitLinePars.length; ++i) {
            System.out.println(resFitLineParNames[i] + " : " + resFitLinePars[i] + " +/- " + resFitLineParErrors[i]);
        }

        // try to figure out the right style here...
        IPlotterStyle style = plotter.region(0).style();
//            style.xAxisStyle().setLabel("1/ \u221a Energy [1/GeV]");
//            style.yAxisStyle().setLabel("sigma/E");
        style.statisticsBoxStyle().setVisibileStatistics("000000");//011");
        style.statisticsBoxStyle().setVisible(true);
        style.legendBoxStyle().setVisible(true);
        style.dataStyle().fillStyle().setColor("blue");
        style.dataStyle().errorBarStyle().setColor("blue");
        style.dataStyle().lineStyle().setParameter("linetype", "none");

//            IFitResult resFitLine = jminuit.fit(resolutionFit, line);
//            System.out.println(" fit status: "+resFitLine.fitStatus());
//            double[] resFitLinePars = resFitLine.fittedParameters();
//            double[] resFitLineParErrors = resFitLine.errors();
//            String[] resFitLineParNames = resFitLine.fittedParameterNames();
//
//            System.out.println(" Energy Resolution Fit: ");
//            for (int i=0; i< resFitLinePars.length; ++i)
//            {
//                System.out.println(resFitLineParNames[i]+" : "+resFitLinePars[i]+" +/- "+resFitLineParErrors[i]);
//            }
        plotter.region(1).setStyle(style);
        plotter.region(2).setStyle(style);
        plotter.region(3).setStyle(style);

        plotter.show();

        plotter.writeToFile("SlopeInterceptFits." + fileType);
        plotter.writeToFile("SlopeInterceptFits.png");

    }

    private static void sliceAndFit(IHistogram2D hist2D, IDataPointSet dataPointSet, IHistogramFactory hf) {
        IAnalysisFactory analysisFactory = IAnalysisFactory.create();
        IPlotterFactory plotterFactory = analysisFactory.createPlotterFactory();
        String name = hist2D.title();
        IAxis xAxis = hist2D.xAxis();
        int nBins = xAxis.bins();
        IHistogram1D[] slices = new IHistogram1D[nBins];
        IDataPoint dp;
        int nDataPoints = 0;
        for (int i = 0; i < nBins; ++i) { // stepping through x axis bins
            if (xAxis.binCenter(i) > 0.025 && xAxis.binCenter(i) < 0.05) {
                slices[i] = hf.sliceY(name + " slice " + i, hist2D, i);
                System.out.println(name + " slice " + i + " has " + slices[i].allEntries() + " entries");
                if (slices[i].entries() > 500.) {
                    IPlotter plotter = plotterFactory.create("slice " + i + " plotter");
                    plotter.region(0).plot(slices[i]);
                    IFitResult fr = performGaussianFit(slices[i]);
                    if (fr != null) {
                        System.out.println(" fit status: " + fr.fitStatus());

                        plotter.region(0).plot(fr.fittedFunction());
                        plotter.show();
                        double[] frPars = fr.fittedParameters();
                        double[] frParErrors = fr.errors();
                        String[] frParNames = fr.fittedParameterNames();
                        System.out.println(" Energy Resolution Fit: ");
                        for (int jj = 0; jj < frPars.length; ++jj) {
                            System.out.println(frParNames[jj] + " : " + frPars[jj] + " +/- " + frParErrors[jj]);
                        }
                        // create a datapoint
                        dataPointSet.addPoint();
                        dp = dataPointSet.point(nDataPoints++);
                        dp.coordinate(0).setValue(xAxis.binCenter(i));
                        dp.coordinate(1).setValue(frPars[1]); // gaussian mean
                        dp.coordinate(1).setErrorPlus(frParErrors[1]);
                        dp.coordinate(1).setErrorMinus(frParErrors[1]);
                    }
                }
            }
        }
    }

    private static final List<String> getTreeFiles(ITree tree, String rootDir) {
        // Make a list to contain the plot names.
        List<String> list = new ArrayList<String>();

        // Iterate over the objects at the indicated directory of the tree.
        String[] objectNames = tree.listObjectNames(rootDir);
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

    private static IFitResult performGaussianFit(IHistogram1D histogram) {
        IFunctionFactory functionFactory = IAnalysisFactory.create().createFunctionFactory(null);
        IFitFactory fitFactory = IAnalysisFactory.create().createFitFactory();
        IFunction function = functionFactory.createFunctionByName("Gaussian Fit", "G");
        IFitter fitter = fitFactory.createFitter("chi2", "jminuit");
        double[] parameters = new double[3];
        parameters[0] = histogram.maxBinHeight();
        parameters[1] = histogram.mean();
        parameters[2] = 0.1; // histogram.rms();// why is the rms of a slice wrong?
        function.setParameters(parameters);
        IFitResult fitResult = null;
        // try something here...
        double fitrangeLo = histogram.mean() - 0.3;
        double fitrangeHi = histogram.mean() + 0.2;

        IFitData fitData = fitFactory.createFitData();
        fitData.create1DConnection(histogram);
        fitData.range(0).excludeAll();
        fitData.range(0).include(fitrangeLo, fitrangeHi);
//                    profilePlotter.region(j).plot(profDataPointSet, profilePlotStyle);
//                    IFitResult result = fitter.fit(fitData, line);
//                    profilePlotter.region(j).plot(result.fittedFunction(), functionStyle, "range=\"(-0.4,0.4)\"");
        //
        Logger minuitLogger = Logger.getLogger("org.freehep.math.minuit");
        minuitLogger.setLevel(Level.OFF);
        minuitLogger.info("minuit logger test");

        try {
            fitResult = fitter.fit(fitData, function);
        } catch (RuntimeException e) {
            System.out.println("fit failed.");
        }
        return fitResult;
    }
}
