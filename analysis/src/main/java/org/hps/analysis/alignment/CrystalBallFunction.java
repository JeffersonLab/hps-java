package org.hps.analysis.alignment;

import hep.aida.IAnalysisFactory;
import hep.aida.IAnnotation;
import hep.aida.IFitFactory;
import hep.aida.IFitter;
import hep.aida.IFunction;
import hep.aida.IFunctionFactory;
import hep.aida.IHistogram1D;
import hep.aida.IHistogramFactory;
import hep.aida.IPlotter;
import hep.aida.ITree;
import hep.aida.ITreeFactory;
import hep.aida.ref.Annotation;
import static java.lang.Math.abs;
import static java.lang.Math.exp;
import static java.lang.Math.pow;
import java.util.Random;

/**
 *
 * @author ngraf
 */
public class CrystalBallFunction implements IFunction {

    double N;
    double alpha;
    double n;
    double x;
    double sig;

    String[] vars = {"N", "alpha", "n", "x", "sig"};

    @Override
    public String title() {
        return "Crystal Ball Function";
    }

    @Override
    public void setTitle(String string) throws IllegalArgumentException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public double value(double[] doubles) {

        double val = doubles[0];
        double aa = abs(alpha);

        double A = pow(n / aa, n) * exp(-aa * aa / 2.);

        double B = n / aa - aa;

        double factor = (val - x) / sig;

        double total = 0.;

        if (factor > -alpha) {
            total = N * exp(-factor * factor / 2.);
        } else {
            total = N * A * pow(B - factor, -n);
        }
        return total;
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
        N = doubles[0];
        alpha = doubles[1];
        n = doubles[2];
        x = doubles[3];
        sig = doubles[4];
    }

    @Override
    public double[] parameters() {
        return new double[]{N, alpha, n, x, sig};
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
            case "N":
                N = d;
            case "alpha":
                alpha = d;
            case "n":
                n = d;
            case "x":
                x = d;
            case "sig":
                sig = d;
            default:
                throw new RuntimeException("variable " + string + " not recognized");
        }
    }

    @Override
    public double parameter(String string) {
        switch (string) {
            case "N":
                return N;
            case "alpha":
                return alpha;
            case "n":
                return n;
            case "x":
                return x;
            case "sig":
                return sig;
            default:
                throw new RuntimeException("variable " + string + " not recognized");
        }
    }

    @Override
    public int indexOfParameter(String string) {
        switch (string) {
            case "N":
                return 0;
            case "alpha":
                return 1;
            case "n":
                return 2;
            case "x":
                return 3;
            case "sig":
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
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public static void main(String[] args) {
        // Create factories
        IAnalysisFactory analysisFactory = IAnalysisFactory.create();
        ITreeFactory treeFactory = analysisFactory.createTreeFactory();
        ITree tree = treeFactory.create();
        IPlotter plotter = analysisFactory.createPlotterFactory().create("Fit.java Plot");
        IFunctionFactory functionFactory = analysisFactory.createFunctionFactory(tree);
        IFunction gauss = functionFactory.createFunctionFromScript("gauss", 1, "background+a*exp(-(x[0]-mean)*(x[0]-mean)/sigma/sigma)", "a,mean,sigma,background", "A Gaussian");
        IHistogramFactory histogramFactory = analysisFactory.createHistogramFactory(tree);
        IHistogram1D h1 = histogramFactory.createHistogram1D("Histogram 1D", 50, -10., 10.);

        Random r = new Random();

        IFunction cb = new CrystalBallFunction();
        //                 N alpha n x sig
        double[] cbpars = {1., 1., 1., 0., 1.};
        cb.setParameters(cbpars);
//        double[] x = {-3., -2.5, -2. - 1., 0., 1., 2., 2.5, 3.};
//        double[] xin = new double[1];
//        for (int i = 0; i < x.length; ++i) {
//            xin[0] = x[i];
//            System.out.println(x[i] + " " + cb.value(xin));
//        }

        int nTries = 10000;
        for (int i = 0; i < 100000; i++) {
            h1.fill(r.nextGaussian(), 1. / nTries);
        }

        plotter.region(0).plot(h1);
        gauss.setParameter("a", h1.maxBinHeight());
        gauss.setParameter("mean", h1.mean());
        gauss.setParameter("sigma", h1.rms());
        IFitFactory fitFactory = analysisFactory.createFitFactory();
        IFitter jminuit = fitFactory.createFitter("Chi2", "jminuit");

//       IFitResult jminuitResult = jminuit.fit(h1, cb);
//
//        plotter.region(0).plot(jminuitResult.fittedFunction());
        plotter.region(0).plot(cb);
        plotter.show();
    }

}
