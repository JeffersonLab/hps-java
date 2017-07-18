package org.hps.recon.tracking;

import java.io.File;
//import java.net.URL;

import junit.framework.TestCase;

import org.hps.conditions.database.DatabaseConditionsManager;
//import org.hps.job.DatabaseConditionsManagerSetup;
//import org.lcsim.util.cache.FileCache;
import org.lcsim.util.loop.LCIODriver;
import org.lcsim.util.loop.LCSimLoop;
import org.lcsim.recon.tracking.digitization.sisim.config.RawTrackerHitSensorSetup;
//import org.lcsim.recon.tracking.digitization.sisim.config.RawTrackerHitSensorSetup;
//import org.lcsim.job.ConditionsSetup;
import org.lcsim.recon.tracking.digitization.sisim.config.ReadoutCleanupDriver;
import org.lcsim.util.test.TestUtil.TestOutputFile;

/**
 * Test class to create set of histograms (aida/root) from reco LCIO.
 * 
 * @author Miriam Diamond <mdiamond@slac.stanford.edu>
 * @version $id: v1 05/30/2017$
 * 
 */
public class TrackingReconstructionPlotsTest extends TestCase {

    static final String testInput = null;
    // static final String testURLBase = "http://www.lcsim.org/test/hps-java";
    //static final String testFileName = "MCReconTest.slcio";
    static final String testOutput = "test.slcio";
    static final String aidaOutput = "TrackingRecoPlots2.aida";

    private final int nEvents = 9;

    public void testTrackRecoPlots() throws Exception {
        // URL testURL = new URL(testURLBase + "/" + testFileName);
        // FileCache cache = new FileCache();
        if (testInput == null)
            return;
        File lcioInputFile = new File("target/test-output/" + testInput);
        lcioInputFile.getParentFile().mkdirs();
        File outputFile = new TestOutputFile(testOutput);

        DatabaseConditionsManager.getInstance();

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

        loop2.add(new LCIODriver(outputFile));

        loop2.loop(nEvents, null);
        loop2.dispose();

    }

}
