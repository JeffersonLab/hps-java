package org.hps.users.jeremym;

import hep.aida.IFitFactory;
import hep.aida.IFitResult;
import hep.aida.IFitter;
import hep.aida.IFunction;
import hep.aida.IPlotter;
import hep.aida.IProfile1D;
import hep.aida.ref.fitter.FitResult;

import java.io.IOException;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import junit.framework.TestCase;

import org.lcsim.util.aida.AIDA;

/**
 * <p>
 * Test of Landau distribution functions ported from C++ and originally defined in the file
 * <p>
 * mathcore/src/ProbFuncMathCore.cxx
 * <p>
 * within ROOT (version 5.34.18).
 */
public class LandauTest extends TestCase {

    static double[] p1 = { 0.4259894875, -0.1249762550, 0.03984243700, -0.006298287635, 0.001511162253 };
    static double[] q1 = { 1.0, -0.3388260629, 0.09594393323, -0.01608042283, 0.003778942063 };

    static double[] p2 = { 0.1788541609, 0.1173957403, 0.01488850518, -0.001394989411, 0.0001283617211 };
    static double[] q2 = { 1.0, 0.7428795082, 0.3153932961, 0.06694219548, 0.008790609714 };

    static double[] p3 = { 0.1788544503, 0.09359161662, 0.006325387654, 0.00006611667319, -0.000002031049101 };
    static double[] q3 = { 1.0, 0.6097809921, 0.2560616665, 0.04746722384, 0.006957301675 };

    static double[] p4 = { 0.9874054407, 118.6723273, 849.2794360, -743.7792444, 427.0262186 };
    static double[] q4 = { 1.0, 106.8615961, 337.6496214, 2016.712389, 1597.063511 };

    static double[] p5 = { 1.003675074, 167.5702434, 4789.711289, 21217.86767, -22324.94910 };
    static double[] q5 = { 1.0, 156.9424537, 3745.310488, 9834.698876, 66924.28357 };

    static double[] p6 = { 1.000827619, 664.9143136, 62972.92665, 475554.6998, -5743609.109 };
    static double[] q6 = { 1.0, 651.4101098, 56974.73333, 165917.4725, -2815759.939 };

    static double[] a1 = { 0.04166666667, -0.01996527778, 0.02709538966 };
    static double[] a2 = { -1.845568670, -4.284640743 };
        
    public void testLandauDistribution() {
        AIDA aida = AIDA.defaultInstance();
        double mean = 10.0;
        double sigma = 2.0;
        
        LandauPdf landauPdf = new LandauPdf();
        landauPdf.setMean(mean);
        landauPdf.setSigma(sigma);
        
        Random random = new Random();
        
        IProfile1D profile = aida.profile1D("Landau P1D", 50, -0.5, 49.5);
        for (int i = 0; i < 100; i++) {
            for (int x = 0; x < 2000; x++) {
                double xVal = x / 10;
                double landauVal = landauPdf.getValue(xVal);            
                double smearedLandau = landauVal * (1.0 + 0.1 * random.nextGaussian());
                profile.fill(xVal, smearedLandau);
            }                       
        }
                                        
        IPlotter plotter = aida.analysisFactory().createPlotterFactory().create();
        plotter.createRegion();
        plotter.region(0).plot(profile);        
        
        LandauFunction landau = new LandauFunction();
        //IFunctionFactory functionFactory = aida.analysisFactory().createFunctionFactory(null);
        IFitFactory fitFactory = aida.analysisFactory().createFitFactory();
        //functionFactory.catalog().add("landau", landau);
        
        landau.setParameter("sigma", sigma + 0.1);
        landau.setParameter("mean", mean + 0.1);
        
        IFitter fitter = fitFactory.createFitter();
        
        Logger minuitLogger = Logger.getLogger("org.freehep.math.minuit");
        minuitLogger.setLevel(Level.ALL);
        minuitLogger.info("minuit logger test");
        
        IFitResult fitResult = fitter.fit(profile, landau);
        
        IFunction fittedFunction = fitResult.fittedFunction();
        
        plotter.region(0).plot(fittedFunction);
        
        System.out.println();
        System.out.println("fitted parameters");
        for (String fittedParameterName : fitResult.fittedParameterNames()) {
            System.out.println(fittedParameterName + " = " + fitResult.fittedParameter(fittedParameterName));
        }
        System.out.println("fit status = " + fitResult.fitStatus());
        
        System.out.println();
        System.out.println("fitted function");
        for (String parameterName : fittedFunction.parameterNames()) {
            System.out.println(parameterName + " = " + fittedFunction.parameter(parameterName));
        }
        
        ((FitResult)fitResult).printResult();
        
        try {
            plotter.writeToFile("Landau.png");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }            
}
