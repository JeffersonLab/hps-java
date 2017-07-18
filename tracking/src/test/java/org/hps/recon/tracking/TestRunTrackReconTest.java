package org.hps.recon.tracking;

import org.lcsim.util.aida.AIDA;

/**
 * Test class for raw->reco LCIO + producing histograms.
 * @author mdiamond <mdiamond@slac.stanford.edu>
 * @version $id: 2.0 06/04/17$
 */
public class TestRunTrackReconTest extends ReconTestSkeleton {

    //static final String testURLBaseCmp = "http://www.slac.stanford.edu/~phansson/files/hps_java_test/HPSTestRunv3/hps-java-1.7-SNAPSHOT-050113";
    //static final String testFileNameCmp = "egs_5.5gev_0.016x0_500mb_recoil_recon_1_hpsTestRunTrackingTest.aida";

    static final String inputFileName = null;
    private AIDA aida;

    @Override
    public void testRecon() throws Exception {

        //testURLBase = "http://www.slac.stanford.edu/~phansson/files/hps_java_test/HPSTestRunv3/hps-java-1.7-SNAPSHOT-050113";
        //testInputFileName = "target/test-output/hps_005772.0_recon_Rv4657-0-10000_raw.slcio";
        if (inputFileName == null)
            return;
        testInputFileName = inputFileName;
        aida = AIDA.defaultInstance();
        String aidaOutputName = "Plots" + inputFileName.replaceAll("slcio", "root");
        nEvents = -1;
        testTrackingDriver = new TrackingReconstructionPlots();
        ((TrackingReconstructionPlots) testTrackingDriver).setOutputPlots(aidaOutputName);
        ((TrackingReconstructionPlots) testTrackingDriver).aida = aida;
        super.testRecon();
    }

}
