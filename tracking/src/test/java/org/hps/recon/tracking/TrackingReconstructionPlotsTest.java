package org.hps.recon.tracking;

import java.io.File;
//import java.net.URL;

import java.net.URL;

import junit.framework.TestCase;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.detector.svt.SvtDetectorSetup;
import org.lcsim.util.cache.FileCache;
//import org.hps.job.DatabaseConditionsManagerSetup;
//import org.lcsim.util.loop.LCIODriver;
import org.lcsim.util.loop.LCSimLoop;
import org.lcsim.recon.tracking.digitization.sisim.config.RawTrackerHitSensorSetup;
//import org.lcsim.job.ConditionsSetup;
import org.lcsim.recon.tracking.digitization.sisim.config.ReadoutCleanupDriver;

//import org.lcsim.util.test.TestUtil.TestOutputFile;

/**
 * Test class to create set of histograms (aida/root) from reco LCIO.
 * 
 * @author Miriam Diamond <mdiamond@slac.stanford.edu> 
 */
public class TrackingReconstructionPlotsTest extends TestCase {

    static final String testInput = "v0skim.slcio";
    static final String testURLBase = null;
    static final String testOutput = "RecoCopy_" + testInput;
    static final String aidaOutput = "target/test-output/TestPlots_" + testInput.replaceAll("slcio", "aida");

    private final int nEvents = 1;

    public void testTrackRecoPlots() throws Exception {
        File lcioInputFile = null;
        if (testURLBase != null) {
            URL testURL = new URL(testURLBase + "/" + testInput);
            FileCache cache = new FileCache();
            lcioInputFile = cache.getCachedFile(testURL);
        } else
            lcioInputFile = new File(testInput);

        //File outputFile = new TestOutputFile(testOutput);

        final DatabaseConditionsManager manager = new DatabaseConditionsManager();
        manager.addConditionsListener(new SvtDetectorSetup());

        LCSimLoop loop2 = new LCSimLoop();
        loop2.setLCIORecordSource(lcioInputFile);

        RawTrackerHitSensorSetup rthss = new RawTrackerHitSensorSetup();
        String[] readoutColl = { "SVTRawTrackerHits" };
        rthss.setReadoutCollections(readoutColl);
        loop2.add(rthss);

        TrackingReconstructionPlots trp = new TrackingReconstructionPlots();
        trp.setOutputPlots(aidaOutput);
        loop2.add(trp);

        ReadoutCleanupDriver rcd = new ReadoutCleanupDriver();
        loop2.add(rcd);

        //loop2.add(new LCIODriver(outputFile));

        loop2.loop(nEvents, null);
        loop2.dispose();

    }

}
