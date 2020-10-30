package org.hps.util;

import hep.aida.IAnalysisFactory;
import hep.aida.IAnnotation;
import hep.aida.IFitFactory;
import hep.aida.IFitResult;
import hep.aida.IFitter;
import hep.aida.IFunction;
import hep.aida.IFunctionFactory;
import hep.aida.IHistogram1D;
import hep.aida.IHistogramFactory;
import hep.aida.IPlotter;
import hep.aida.IPlotterStyle;
import hep.aida.ITree;
import hep.aida.ITreeFactory;
import hep.aida.ref.Annotation;
import static java.lang.Math.cosh;
import java.util.Random;

/**
 *
 * @author Norman Graf
 */
public class LogisticDistributionFunction implements IFunction, Cloneable {

    double xmean;
    double sigma;
    double Norm;

    String[] vars = {"xmean", "sigma", "Norm"};

    @Override
    public String title() {
        return "Logistic Distribution Function";
    }

    @Override
    public void setTitle(String string) throws IllegalArgumentException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public double value(double[] x) {

        double arg = (x[0] - xmean) / (2. * sigma);

        return Norm / (4 * sigma * cosh(arg) * cosh(arg));
    }

    @Override
    public int dimension() {
        return 1;
    }

    @Override
    public boolean isEqual(IFunction i) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public double[] gradient(double[] doubles) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean providesGradient() {
        return false;
    }

    @Override
    public String variableName(int i) {
        return vars[i];
    }

    @Override
    public String[] variableNames() {
        return vars;
    }

    @Override
    public void setParameters(double[] doubles) throws IllegalArgumentException {
        xmean = doubles[0];
        sigma = doubles[1];
        Norm = doubles[2];
    }

    @Override
    public double[] parameters() {
        return new double[]{xmean, sigma, Norm};
    }

    @Override
    public int numberOfParameters() {
        return 3;
    }

    @Override
    public String[] parameterNames() {
        return vars;
    }

    @Override
    public void setParameter(String string, double d) throws IllegalArgumentException {
        switch (string) {
            case "xmean":
                xmean = d;
                break;
            case "sigma":
                sigma = d;
                break;
            case "Norm":
                Norm = d;
                break;
            default:
                throw new RuntimeException("variable " + string + " not recognized");
        }
    }

    @Override
    public double parameter(String string) {
        switch (string) {
            case "xmean":
                return xmean;
            case "sigma":
                return sigma;
            case "Norm":
                return Norm;
            default:
                throw new RuntimeException("variable " + string + " not recognized");
        }
    }

    @Override
    public int indexOfParameter(String string) {
        switch (string) {
            case "xmean":
                return 0;
            case "sigma":
                return 1;
            case "n":
                return 2;
            case "alpha":
                return 3;
            case "Norm":
                return 4;
            default:
                throw new RuntimeException("variable " + string + " not recognized");
        }
    }

    @Override
    public IAnnotation annotation() {
        return new Annotation();
    }

    @Override
    public String codeletString() {
        return "codelet:Crystal Ball Function:verbatim:java \\n { return cb(x); }";
    }

    @Override
    public String normalizationParameter() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public LogisticDistributionFunction clone() {
        return new LogisticDistributionFunction();
    }

    public static void main(String[] args) {
        // Create factories
        IAnalysisFactory analysisFactory = IAnalysisFactory.create();
        ITreeFactory treeFactory = analysisFactory.createTreeFactory();
        ITree tree = treeFactory.create();
        IPlotter plotter = analysisFactory.createPlotterFactory().create("Fit.java Plot");
        IPlotterStyle functionStyle = analysisFactory.createPlotterFactory().createPlotterStyle();
        functionStyle.dataStyle().outlineStyle().setColor("red");
        functionStyle.legendBoxStyle().setVisible(true);
        functionStyle.statisticsBoxStyle().setVisible(true);
        IFunctionFactory functionFactory = analysisFactory.createFunctionFactory(tree);
        IFunction gauss = functionFactory.createFunctionByName("Gaussian Fit", "G");
        IHistogramFactory histogramFactory = analysisFactory.createHistogramFactory(tree);
        IHistogram1D h1 = histogramFactory.createHistogram1D("Gaussian", 50, -10., 10.);

        Random r = new Random();

        IFunction ldf = new LogisticDistributionFunction();
        double xmean = 0.0;
        double sigma = 1.0;
        double Norm = 4.;

        double[] ldfPars = {xmean, sigma, Norm};
        ldf.setParameters(ldfPars);

        // Generate a gaussian data distribution
        int nTries = 10000;
        for (int i = 0; i < 100000; i++) {
            h1.fill(r.nextGaussian(), 1. / nTries);
        }

        plotter.region(0).plot(h1);
        double[] parameters = new double[3];
        parameters[0] = h1.maxBinHeight();
        parameters[1] = h1.mean();
        parameters[2] = h1.rms();
        gauss.setParameters(parameters);
        IFitFactory fitFactory = analysisFactory.createFitFactory();
        IFitter jminuit = fitFactory.createFitter("Chi2", "jminuit");

        // fit distribution with a gaussian
        IFitResult gaussFitResult = jminuit.fit(h1, gauss);
        plotter.region(0).plot(gaussFitResult.fittedFunction());

        // fit the same distribution with the Logistic Distribution function
        IFitResult ldfFitResult = jminuit.fit(h1, ldf);
        plotter.region(0).plot(ldfFitResult.fittedFunction(), functionStyle);
        plotter.show();

        // Generate a Crystal Ball data distribution
        IPlotter plotter2 = analysisFactory.createPlotterFactory().create("Logistic Distributio Fit");
        IHistogram1D h2 = histogramFactory.createHistogram1D("Logistic Distributio ", 50, -10., 10.);

        int nbins = h2.axis().bins();
        double[] xin = new double[1];
        for (int i = 0; i < nbins; ++i) {
            xin[0] = h2.binMean(i);
            System.out.println("x " + xin[0] + " ldf " + ldf.value(xin));
            h2.fill(xin[0], ldf.value(xin));
        }
        IFunction ldf2 = new LogisticDistributionFunction();
        ldf2.setParameters(new double[]{1.,2.0, 1.});

        // Fit the distribution with a Logistic Distribution
        IFitResult ldfFitResult2 = jminuit.fit(h2, ldf2);
        plotter2.region(0).plot(h2);
        plotter2.region(0).plot(ldfFitResult2.fittedFunction(), functionStyle);

        plotter2.show();
    }

}
