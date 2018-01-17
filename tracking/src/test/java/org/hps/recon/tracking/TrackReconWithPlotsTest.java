package org.hps.recon.tracking;

import hep.aida.IHistogram1D;

import org.lcsim.util.aida.AIDA;

/**
 * Test class for raw->reco LCIO + producing histograms.
 * @author mdiamond <mdiamond@slac.stanford.edu>
 */
public class TrackReconWithPlotsTest extends ReconTestSkeleton {

    static final String inputFileName = "ap_prompt_raw.slcio";
    private AIDA aida;

    @Override
    public void testRecon() throws Exception {

        testInputFileName = inputFileName;
        aida = AIDA.defaultInstance();
        String aidaOutputName = "target/test-output/TestPlots_" + inputFileName.replaceAll("slcio", "aida");
        nEvents = 2;

        IHistogram1D xdiff = aida.histogram1D("extrap [old-new] x", 100, -10, 10);
        IHistogram1D ydiff = aida.histogram1D("extrap [old-new] y", 100, -10, 10);
        IHistogram1D zdiff = aida.histogram1D("extrap [old-new] z", 100, -1, 1);

        testTrackingDriver = new TrackingReconstructionPlots();
        ((TrackingReconstructionPlots) testTrackingDriver).setOutputPlots(aidaOutputName);
        ((TrackingReconstructionPlots) testTrackingDriver).aida = aida;
        super.testRecon();

        IHistogram1D ntracks = aida.histogram1D("Tracks per Event");
        assertTrue("No events in plots", ntracks.entries() > 0);
        assertTrue("No tracks in plots", ntracks.mean() > 0);
    }

}
