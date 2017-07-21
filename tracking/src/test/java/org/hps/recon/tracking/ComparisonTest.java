/**
 * 
 */
package org.hps.recon.tracking;

import hep.aida.IAnalysisFactory;
import hep.aida.IHistogram1D;
import hep.aida.ITree;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import junit.framework.TestCase;

import org.hps.util.CompareHistograms;
import org.lcsim.util.aida.AIDA;

/**
 * Test class to check a set of histograms against a reference set.
 * @author mdiamond <mdiamond@slac.stanford.edu>
 * @version $id: 1.0 06/04/17$
 */
public class ComparisonTest extends ReconTestSkeleton {
    static final List<String> histograms = Arrays.asList("Tracks per Event", "Hits per Track");
    static final double TtestAlpha = 0.05;
    static final double KStestAlpha = 0.05;
    static final double EqualityTolerance = 0.1;
    static final String inputFileName = "";
    static final String testReferenceFileName = "hps_005772.0_recon_Rv4657-0-10000_raw.slcio";

    private AIDA aida;

    public void testRecon() throws Exception {

        if (inputFileName == null || testReferenceFileName == null)
            return;
        testInputFileName = inputFileName;
        aida = AIDA.defaultInstance();
        String aidaOutputName = "Plots_" + inputFileName.replaceAll("slcio", "aida");
        nEvents = -1;
        testTrackingDriver = new TrackingReconstructionPlots();
        ((TrackingReconstructionPlots) testTrackingDriver).setOutputPlots(aidaOutputName);
        ((TrackingReconstructionPlots) testTrackingDriver).aida = aida;
        super.testRecon();

        IHistogram1D ntracks = aida.histogram1D("Tracks per Event");
        assertTrue("No events in plots", ntracks.entries() > 0);
        assertTrue("No tracks in plots", ntracks.mean() > 0);

        final IAnalysisFactory af = aida.analysisFactory();
        File refFile = new File(testReferenceFileName);
        ITree tree_cmpRef = af.createTreeFactory().create(refFile.getAbsolutePath());

        for (String histname : histograms) {
            IHistogram1D h_ref = (IHistogram1D) tree_cmpRef.find(histname);
            IHistogram1D h_test = aida.histogram1D(histname);
            boolean nullHypoIsRejected = false;
            double p_value = 0;
            if (TtestAlpha > 0) {
                nullHypoIsRejected = CompareHistograms.instance().getTTest(TtestAlpha, h_test.mean(), h_ref.mean(), h_test.rms() * h_test.rms(), h_ref.rms() * h_ref.rms(), h_test.allEntries(), h_ref.allEntries());
                p_value = CompareHistograms.instance().getTTestPValue(h_test.mean(), h_ref.mean(), h_test.rms() * h_test.rms(), h_ref.rms() * h_ref.rms(), h_test.allEntries(), h_ref.allEntries());
                System.out.printf("%s: %s %s T-Test (%.1f%s C.L.) with p-value=%.3f\n", ComparisonTest.class.getName(), histname, (nullHypoIsRejected ? "FAILED" : "PASSED"), (1 - TtestAlpha) * 100, "%", p_value);
                assertTrue("Failed T-Test (" + (1 - TtestAlpha) * 100 + "% C.L. p-value=" + p_value + ") comparing histogram " + histname, !nullHypoIsRejected);
            }
            if (KStestAlpha > 0) {
                p_value = CompareHistograms.getKolmogorovPValue(h_test, h_ref);
                nullHypoIsRejected = (p_value < KStestAlpha);
                System.out.printf("%s: %s %s Kolmogorov-Smirnov test (%.1f%s C.L.) with p-value=%.3f\n", ComparisonTest.class.getName(), histname, (nullHypoIsRejected ? "FAILED" : "PASSED"), (1 - KStestAlpha) * 100, "%", p_value);
                assertTrue("Failed Kolmogorov-Smirnov test (" + (1 - KStestAlpha) * 100 + "% C.L. p-value=" + p_value + ") comparing histogram " + histname, !nullHypoIsRejected);
            }
            if (EqualityTolerance > 0) {
                nullHypoIsRejected = (Math.abs(h_ref.sumAllBinHeights() - h_test.sumAllBinHeights())) > EqualityTolerance;
                assertTrue("Bin heights do not match within tolerance for histogram " + histname, !nullHypoIsRejected);
                nullHypoIsRejected = (Math.abs(h_ref.mean() - h_test.mean())) > EqualityTolerance;
                assertTrue("Means do not match within tolerance for histogram " + histname, !nullHypoIsRejected);
            }
        }
    }

}
