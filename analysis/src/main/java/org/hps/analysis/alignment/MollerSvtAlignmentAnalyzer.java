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
import hep.aida.IPlotter;
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
        IFunctionFactory functionFactory = analysisFactory.createFunctionFactory(tree);
        IFitFactory fitFactory = analysisFactory.createFitFactory();
        IFitter fitter = fitFactory.createFitter("Chi2", "jminuit");
        IDataPointSetFactory dpsf = analysisFactory.createDataPointSetFactory(tree);

        if (tree == null) {
            throw new IllegalArgumentException("Unable to load plot file.");
        }
        // Get the histograms names.
        List<String> objectNameList = getTreeFiles(tree);

        for (String s : objectNameList) {
            IProfile1D prof = null;
            if (s.contains("Profile")) {
                System.out.println(s);
                prof = (IProfile1D) tree.find(s);
            }
            if (prof != null) {
                IPlotter plotter = analysisFactory.createPlotterFactory().create("Fit.java Plot");
                plotter.createRegions(1, 2);
                plotter.region(0).plot(prof);

                IFunction line = functionFactory.createFunctionByName("line", "p1");

                IFitData fitData = fitFactory.createFitData();
                fitData.create1DConnection(prof);
                fitData.range(0).excludeAll();
                fitData.range(0).include(-0.4, 0.4);

                IFitResult result = fitter.fit(fitData, line);
                plotter.region(0).plot(result.fittedFunction());

                System.out.println("Chi2=" + result.quality());

                // now try a data point set
                IDataPointSet profDataPointSet = dpsf.create("dpsFromProf", prof);
                //plotter.region(1).plot(profDataPointSet);
                // seems to be exactly the same
                //manually set uncertainties to be the error on the mean (not the rms)
                System.out.println(" profile plot has " + prof.axis().bins() + " bins");
                for (int i = 0; i < prof.axis().bins(); ++i) {
//            System.out.println("bin "+i+" error "+prof.binError(i)+" rms "+prof.binRms(i));
                    profDataPointSet.point(i).coordinate(1).setErrorPlus(prof.binError(i));
                    profDataPointSet.point(i).coordinate(1).setErrorMinus(prof.binError(i));
                }

                IFitData fitData1 = fitFactory.createFitData();
                fitData1.create1DConnection(profDataPointSet,0,1);
                fitData1.range(0).excludeAll();
                fitData1.range(0).include(-0.4, 0.4);
                plotter.region(1).plot(profDataPointSet);
                IFitResult result2 = fitter.fit(fitData1, line);
                plotter.region(1).plot(result2.fittedFunction());
                plotter.show();
            }
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
