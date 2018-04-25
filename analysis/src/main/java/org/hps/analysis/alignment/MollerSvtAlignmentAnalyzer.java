package org.hps.analysis.alignment;

import hep.aida.IAnalysisFactory;
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
 * @author Norman Graf
 */
public class MollerSvtAlignmentAnalyzer {

    public static void main(String[] args) throws IllegalArgumentException, IOException {
        // Define the root directory for the plots.

        String rootDir = null;
        String plotFile = "D:/work/hps/analysis/mollerAlignment/2015_MollerSkim_pass8_PC.aida";
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
        for (int j = 0; j < eBins.length; ++j) {
            IHistogram2D scatter = (IHistogram2D) tree.find("0." + eBins[j] + " Track thetaX vs ThetaY ");
            scatterXvsYPlotter.region(j).plot(scatter, scatterPlotStyle);
        }
        scatterXvsYPlotter.show();
        
        for (String half : halves) {
            IPlotter profilePlotter = analysisFactory.createPlotterFactory().create(half + " profile plots");
            profilePlotter.createRegions(3, 3);
            IPlotter scatterPlotter = analysisFactory.createPlotterFactory().create(half + " scatter plots");
            scatterPlotter.createRegions(3, 3);

            //0.65 Track thetaX vs ThetaY 
            for (int j = 0; j < eBins.length; ++j) {
                IHistogram2D scatter = (IHistogram2D) tree.find("0." + eBins[j] + " " + half + " Track phi vs dTheta");
                scatterPlotter.region(j).plot(scatter, scatterPlotStyle);

                IProfile1D prof = (IProfile1D) tree.find("0." + eBins[j] + " " + half + " Track phi vs dTheta Profile");
                if (prof != null) {
                    IFunction line = functionFactory.createFunctionByName("line", "p1");

//                IFitData fitData = fitFactory.createFitData();
//                fitData.create1DConnection(prof);
//                fitData.range(0).excludeAll();
//                fitData.range(0).include(-0.4, 0.4);
//
//                IFitResult result = fitter.fit(fitData, line);
//                bottomPlotter.region(0).plot(result.fittedFunction());
//
//                System.out.println("Chi2=" + result.quality());
                    // now try a data point set
                    IDataPointSet profDataPointSet = dpsf.create("dpsFromProf", prof);
                    //plotter.region(1).plot(profDataPointSet);
                    // seems to be exactly the same
                    //manually set uncertainties to be the error on the mean (not the rms)
//                System.out.println(" profile plot has " + prof.axis().bins() + " bins");
                    for (int i = 0; i < prof.axis().bins(); ++i) {
//            System.out.println("bin "+i+" error "+prof.binError(i)+" rms "+prof.binRms(i));
                        profDataPointSet.point(i).coordinate(1).setErrorPlus(prof.binError(i));
                        profDataPointSet.point(i).coordinate(1).setErrorMinus(prof.binError(i));
                    }

                    IFitData fitData1 = fitFactory.createFitData();
                    fitData1.create1DConnection(profDataPointSet, 0, 1);
                    fitData1.range(0).excludeAll();
                    fitData1.range(0).include(-0.4, 0.4);
                    profilePlotter.region(j).plot(profDataPointSet, profilePlotStyle);
                    IFitResult result2 = fitter.fit(fitData1, line);
                    profilePlotter.region(j).plot(result2.fittedFunction(), functionStyle, "range=\"(-0.4,0.4)\"");

                }
            }
            profilePlotter.show();
            scatterPlotter.show();
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
