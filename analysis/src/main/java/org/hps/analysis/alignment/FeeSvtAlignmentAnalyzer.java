/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hps.analysis.alignment;

import hep.aida.IAnalysisFactory;
import hep.aida.IDataPointSetFactory;
import hep.aida.IFitFactory;
import hep.aida.IFitter;
import hep.aida.IFunctionFactory;
import hep.aida.IPlotterFactory;
import hep.aida.IPlotterStyle;
import hep.aida.ITree;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author ngraf
 */
public class FeeSvtAlignmentAnalyzer {

    public static void main(String[] args) throws IllegalArgumentException, IOException {
        // Define the root directory for the plots.
        boolean showPlots = false;
        boolean writePlots = true;
        String fileType = "pdf"; // png, pdf, eps ps svg emf swf
        String rootDir = null;
        String plotFile = "D:/work/hps/analysis/pass9_tests/5772_FEE_v2Detector.aida";
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
        // Get the histograms names.
        List<String> objectNameList = getTreeFiles(tree, "/");
        for (String histo : objectNameList) {
            System.out.println(histo);
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
}
