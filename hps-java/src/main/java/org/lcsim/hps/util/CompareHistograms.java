package org.lcsim.hps.util;

import hep.aida.IAxis;
import hep.aida.IHistogram1D;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.math.MathException;
import org.apache.commons.math.stat.descriptive.StatisticalSummaryValues;
import org.apache.commons.math.stat.inference.TTestImpl;
import org.apache.commons.math3.distribution.KolmogorovSmirnovDistribution;

/**
 *
 * Class to do various comparisons of histograms Singleton instance with lazy
 * instantiation
 *
 * @author phansson
 */
public class CompareHistograms {

    public static CompareHistograms _instance = null;
    TTestImpl tTest;

    private CompareHistograms() {
        tTest = new TTestImpl();
    }

    public static CompareHistograms instance() {
        if (_instance == null) {
            _instance = new CompareHistograms();
        }
        return _instance;
    }

    public double getTTestPValue(double m1, double m2, double v1, double v2, int n1, int n2) {
        StatisticalSummaryValues stat1 = new StatisticalSummaryValues(m1, v1, n1, 1., 0., 0.);
        StatisticalSummaryValues stat2 = new StatisticalSummaryValues(m2, v2, n2, 1., 0., 0.);

        double p_value = -1;
        try {
            p_value = tTest.tTest(stat1, stat2);
        } catch (IllegalArgumentException ex) {
            Logger.getLogger(CompareHistograms.class.getName()).log(Level.SEVERE, null, ex);
        } catch (MathException ex) {
            Logger.getLogger(CompareHistograms.class.getName()).log(Level.SEVERE, null, ex);
        }
        return p_value;

    }

    public boolean getTTest(double alpha, double m1, double m2, double v1, double v2, int n1, int n2) {
        StatisticalSummaryValues stat1 = new StatisticalSummaryValues(m1, v1, n1, 1., 0., 0.);
        StatisticalSummaryValues stat2 = new StatisticalSummaryValues(m2, v2, n2, 1., 0., 0.);

        boolean nullHypoIsRejected = false;
        try {
            nullHypoIsRejected = tTest.tTest(stat1, stat2, alpha);
        } catch (IllegalArgumentException ex) {
            Logger.getLogger(CompareHistograms.class.getName()).log(Level.SEVERE, null, ex);
        } catch (MathException ex) {
            Logger.getLogger(CompareHistograms.class.getName()).log(Level.SEVERE, null, ex);
        }
        return nullHypoIsRejected;
    }

    public static double getKolmogorovPValue(IHistogram1D reference, IHistogram1D test) {
        double integralMax = 0.0;
        double refIntegral = reference.binHeight(IAxis.UNDERFLOW_BIN);
        double testIntegral = test.binHeight(IAxis.UNDERFLOW_BIN);
        double integralDiff = Math.abs(refIntegral / reference.allEntries() - testIntegral / test.allEntries());
        if (integralDiff > integralMax) {
            integralMax = integralDiff;
        }
        for (int i = 0; i < reference.axis().bins(); i++) {
            refIntegral += reference.binHeight(i);
            testIntegral += test.binHeight(i);

            integralDiff = Math.abs(refIntegral / reference.allEntries() - testIntegral / test.allEntries());
            if (integralDiff > integralMax) {
                integralMax = integralDiff;
            }
        }
        int n = (int) Math.ceil(Math.sqrt((reference.allEntries() * test.allEntries()) / (reference.allEntries() + test.allEntries())));
        KolmogorovSmirnovDistribution dist = new KolmogorovSmirnovDistribution(n);
        return 1.0 - dist.cdf(integralMax);
    }
}
