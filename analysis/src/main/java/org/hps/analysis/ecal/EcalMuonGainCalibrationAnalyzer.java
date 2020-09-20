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
import java.util.Arrays;
import java.util.List;
import org.lcsim.util.aida.AIDA;

/**
 *
 * @author Norman A. Graf
 */
public class EcalMuonGainCalibrationAnalyzer {

    public static void main(String[] args) throws IllegalArgumentException, IOException {
        AIDA aida = AIDA.defaultInstance();
        int[] plotRegion = {0, 0, 2, 4, 1, 3};

//        String plotFile = "D:/work/hps/analysis/physrun2019/ecalibration/prodSingleMuonSkim/20200919/hps_010698_HPS_TY_iter4_muEcalGainCalibration.aida";
        String plotFile = "D:/work/hps/analysis/physrun2019/ecalibration/prodSingleMuonSkim/20200919/hps_010261_HPS_TY_iter4_muEcalGainCalibration.aida";
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
        List<IPlotter> plotters = new ArrayList<>();

        String plotToFit = "no gain ADC sum";
        String plotDir = "dimuon";
        double rms = 0.2;

        for (int ix = -23; ix < 24; ++ix) {
            for (int j = -1; j < 2; j = j + 2) {
                String type = ix < 0 ? types[1] : types[0];
                IDataPointSet dps1D = dpsf.create("dps1D", "Gaussian Mean", 1);
                IPlotter tst = analysisFactory.createPlotterFactory().create("ECal MIP Gaussian mean");
                tst.setParameter("plotterWidth", "1600");
                tst.setParameter("plotterHeight", "900");
                tst.createRegions(3, 2);
                IPlotterStyle style = tst.region(5).style();
                style.dataStyle().lineStyle().setVisible(false);
                String[] lineTypes = style.dataStyle().lineStyle().availableLineTypes();
                System.out.println(Arrays.toString(lineTypes));
                style.dataStyle().markerStyle().setParameter("color", "blue");
                style.dataStyle().markerStyle().setShape("circle");
                for (int yy = 1; yy < 6; ++yy) {

                    int iy = yy * j;
                    String histoName = ix + " " + iy + " " + type + " " + plotToFit;
                    System.out.println("/" + plotDir + "/clusterAnalysis/" + histoName);
                    dps1D.addPoint();
                    if (objectNameList.contains("/" + plotDir + "/clusterAnalysis/" + histoName)) {
                        IHistogram1D hist = (IHistogram1D) tree.find("/" + plotDir + "/clusterAnalysis/" + histoName);
                        IFitResult fitResult = fitit(hist, fitter, gaussian, rms);
                        tst.region(plotRegion[abs(iy)]).plot(hist, dataStyle);
                        if (fitResult != null) {
                            double lo = fitResult.fittedFunction().parameters()[1] - rms;
                            double hi = fitResult.fittedFunction().parameters()[1] + rms;
                            tst.region(plotRegion[abs(iy)]).plot(fitResult.fittedFunction(), functionStyle, "range=\"(" + lo + "," + hi + ")\"");

                            String[] paramNames = fitResult.fittedParameterNames();
                            double[] params = fitResult.fittedParameters();
                            double[] errors = fitResult.errors();
                            double[] errorsMinus = fitResult.errorsMinus();
                            double[] errorsPlus = fitResult.errorsPlus();

//                        dps1D.addPoint();
//                        dps1D.setCoordinate(abs(iy)-1, new double[]{params[1]}, new double[]{errors[1]});
                            dps1D.point(abs(iy) - 1).coordinate(0).setValue(params[1]);
                            dps1D.point(abs(iy) - 1).coordinate(0).setErrorPlus(errorsPlus[1]);
                            dps1D.point(abs(iy) - 1).coordinate(0).setErrorMinus(errorsMinus[1]);

                            //aida.histogram1D("fit means", 5, 0.5, 5.5).fill(iy,params[1], errors[1]);
                            System.out.println(histoName + "Fit: ");
                            for (int i = 0; i < paramNames.length; ++i) {
                                System.out.println(paramNames[i] + " " + params[i] + " " + errors[i]);
                            }
                        }
                    }
                }

                tst.region(5).plot(dps1D);
                tst.show();
            }
//            String[] plotterParameters = tst.availableParameters();
//            System.out.println(Arrays.toString(plotterParameters));
//            for (String s : plotterParameters) {
//                System.out.println(s);
//                System.out.println(tst.parameterValue(s));
//                System.out.println(Arrays.toString(tst.availableParameterOptions(s)));
//            }
        }
    }

    private static IFitResult fitit(IHistogram1D hist, IFitter fitter, IFunction gaussian, double rms) {
        IFitResult fitResult = null;
        if (hist.entries() > 25) {
            double[] par = new double[3];
            par[0] = hist.maxBinHeight();
            par[1] = hist.mean();
            par[2] = rms;//hist.rms();
//            double fitPar1lo = 0.5;
//            double fitPar1hi = 2.0;

            double lo = par[1] - 1.0 * par[2];
            double hi = par[1] + 1.0 * par[2];
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
            lo = par[1] - rms;//1. * par[2];
            hi = par[1] + rms;//1. * par[2];
            gaussian.setParameters(par);
            fitResult = fitter.fit(hist, gaussian, "range=\"(" + lo + "," + hi + ")\"");
            System.out.println(Arrays.toString(fitResult.constraints()));
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
