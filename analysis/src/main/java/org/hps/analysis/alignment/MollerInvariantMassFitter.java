package org.hps.analysis.alignment;

import hep.aida.IAnalysisFactory;
import hep.aida.IDataPointSetFactory;
import hep.aida.IFitData;
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
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Norman Graf
 */
public class MollerInvariantMassFitter {
    
    static double fitrangeLo = 0.045;
    static double fitrangeHi = 0.0525;

    public static void main(String[] args) throws IllegalArgumentException, IOException {
        // Define the root directory for the plots.
        boolean showPlots = true;
        boolean writePlots = false;
        String fileType = "pdf"; // png, pdf, eps ps svg emf swf
        String rootDir = null;
        String plotFile = "D:/work/hps_data/physrun2016/pass2/2016_Pass2_MollerSkim.aida";
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
        IPlotter plotter = analysisFactory.createPlotterFactory().create("Fit.java Plot");
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
        functionStyle.statisticsBoxStyle().setVisible(true);

        IPlotterStyle massPlotStyle = plotterFactory.createPlotterStyle();
        massPlotStyle.dataStyle().fillStyle().setColor("blue");
        massPlotStyle.dataStyle().errorBarStyle().setColor("blue");
        massPlotStyle.legendBoxStyle().setVisible(true);
        massPlotStyle.statisticsBoxStyle().setVisible(true);        
        massPlotStyle.xAxisStyle().setParameter("lowerLimit", "0.03");
        massPlotStyle.xAxisStyle().setParameter("upperLimit", "0.07");
        List<String> objectNameList = getTreeFiles(tree);


//        for(String s : objectNameList)
//        {
//            System.out.println(s);
//        }
        IHistogram1D unconstrained = (IHistogram1D) tree.find("/UnconstrainedMollerVertices/Moller Invariant Mass");
        IHistogram1D beamspotconstrained = (IHistogram1D) tree.find("/BeamspotConstrainedMollerVertices/Moller Invariant Mass");
        IHistogram1D targetconstrained = (IHistogram1D) tree.find("/TargetConstrainedMollerVertices/Moller Invariant Mass");

        IFitResult fr = performGaussianFit(targetconstrained);
        if (fr != null) {
            System.out.println(" fit status: " + fr.fitStatus());
            plotter.region(0).plot(targetconstrained,massPlotStyle);
            plotter.region(0).plot(fr.fittedFunction(), functionStyle,"range=\"(0.045,0.0525)\"");// "range=\"("+fitrangeLo+","+fitrangeHi+"\"");

            double[] frPars = fr.fittedParameters();
            double[] frParErrors = fr.errors();
            String[] frParNames = fr.fittedParameterNames();
            System.out.println(" Invariant Mass Fit: ");
            for (int jj = 0; jj < frPars.length; ++jj) {
                System.out.println(frParNames[jj] + " : " + frPars[jj] + " +/- " + frParErrors[jj]);
            }
        }

//        IFunction cb = new CrystalBallFunction();
//
//        double sig = unconstrained.rms();
//        double x = unconstrained.mean();
//        double N = fr.fittedParameter("amplitude");
//        double alpha = 0.1;
//        double n = N;
//        //                 N alpha n x sig
//        double[] cbpars = {N, alpha, n, x, sig};
//        cb.setParameters(cbpars);
//        plotter.region(0).plot(cb);
        plotter.show();
    }

    private static IFitResult performGaussianFit(IHistogram1D histogram) {
        IFunctionFactory functionFactory = IAnalysisFactory.create().createFunctionFactory(null);
        IFitFactory fitFactory = IAnalysisFactory.create().createFitFactory();
        IFunction function = functionFactory.createFunctionByName("Gaussian Fit", "G");
        IFitter fitter = fitFactory.createFitter("chi2", "jminuit");
        double[] parameters = new double[3];
        parameters[0] = histogram.maxBinHeight();
        parameters[1] = histogram.mean();
        parameters[2] = histogram.rms();
        function.setParameters(parameters);
        IFitResult fitResult = null;
        // try something here...
//        double fitrangeLo = 0.043;
//        double fitrangeHi = 0.053;

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
