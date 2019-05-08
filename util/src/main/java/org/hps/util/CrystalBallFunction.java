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
import static java.lang.Math.PI;
import static java.lang.Math.abs;
import static java.lang.Math.exp;
import static java.lang.Math.pow;
import static java.lang.Math.sqrt;
import java.util.Random;
import static org.apache.commons.math3.special.Erf.erf;

/**
 *
 * @author Norman Graf
 */
public class CrystalBallFunction implements IFunction, Cloneable {

    double xmean;
    double sigma;
    double n;
    double alpha;
    double Norm;

    String[] vars = {"xmean", "sigma", "n", "alpha", "Norm"};

    @Override
    public String title() {
        return "Crystal Ball Function";
    }

    @Override
    public void setTitle(String string) throws IllegalArgumentException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public double value(double[] x) {

        double s = (x[0] - xmean) / sigma;
        double A = pow((n / abs(alpha)), n) * exp(-0.5 * abs(alpha) * abs(alpha));
        double B = (n / abs(alpha)) - abs(alpha);
        double C = (n / abs(alpha)) * (1.0 / (n - 1)) * exp(-0.5 * abs(alpha) * abs(alpha));
        double D = sqrt(PI / 2) * (1 + erf(abs(alpha) / sqrt(2)));
        double N = Norm / (sigma * (C + D));

        if (s > -alpha) {
            return N * exp(-0.5 * s * s);
        }
        return N * A * pow((B - s), -n);
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
        n = doubles[2];
        alpha = doubles[3];
        Norm = doubles[4];
    }

    @Override
    public double[] parameters() {
        return new double[]{xmean, sigma, n, alpha, Norm};
    }

    @Override
    public int numberOfParameters() {
        return 5;
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
            case "n":
                n = d;
                break;
            case "alpha":
                alpha = d;
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
            case "n":
                return n;
            case "alpha":
                return alpha;
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

    public CrystalBallFunction clone() {
        return new CrystalBallFunction();
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

        IFunction cb = new CrystalBallFunction();
        double xmean = 1.0;
        double sigma = 1.0;
        double n = 3.;
        double alpha = 1.0;
        double Norm = 1.;

        double[] cbpars = {1.0, 1.0, 3, 1.0, 1.0};
        cb.setParameters(cbpars);

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

        // fit the same distribution with the Crystal Ball function
        IFitResult cbFitResult = jminuit.fit(h1, cb);
        plotter.region(0).plot(cbFitResult.fittedFunction(), functionStyle);
        plotter.show();

        // Generate a Crystal Ball data distribution
        IPlotter plotter2 = analysisFactory.createPlotterFactory().create("Crystal Barrel Fit");
        IHistogram1D h2 = histogramFactory.createHistogram1D("Crystal Ball ", 50, -10., 10.);

        int nbins = h2.axis().bins();
        double[] xin = new double[1];
        for (int i = 0; i < nbins; ++i) {
            xin[0] = h2.binMean(i);
            h2.fill(xin[0], cb.value(xin));
        }
        IFunction cb2 = new CrystalBallFunction();
        cb2.setParameters(cbpars);

        // Fit the distribution with a Crystal Ball
        IFitResult cbFitResult2 = jminuit.fit(h2, cb2);
        plotter2.region(0).plot(h2);
        plotter2.region(0).plot(cbFitResult2.fittedFunction(), functionStyle);

        plotter2.show();
    }

}
