package org.hps.recon.tracking;

import java.io.File;
import java.net.URL;

import junit.framework.TestCase;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.detector.svt.SvtDetectorSetup;
import org.lcsim.recon.tracking.digitization.sisim.config.RawTrackerHitSensorSetup;
import org.lcsim.recon.tracking.digitization.sisim.config.ReadoutCleanupDriver;
import org.lcsim.util.Driver;
import org.lcsim.util.cache.FileCache;
import org.lcsim.util.loop.LCIODriver;
import org.lcsim.util.loop.LCSimLoop;
import org.lcsim.util.test.TestUtil.TestOutputFile;

/**
 * Skeleton class for raw->reco LCIO + tests.
 * Assign any driver to testTrackingDriver for tests on the reco LCIO, if desired
 *
 * @author mdiamond <mdiamond@slac.stanford.edu>
 */
public class ReconTestSkeleton extends TestCase {

    static {
        System.getProperties().setProperty("hep.aida.IAnalysisFactory", "hep.aida.ref.BatchAnalysisFactory");
    }
    protected String testInputFileName = "ap_prompt_raw.slcio";
    protected String testOutputFileName;
    protected String testURLBase = "http://www.lcsim.org/test/hps-java";
    protected long nEvents = -1;
    protected URL testURL;
    protected FileCache cache;
    protected Driver testTrackingDriver = null;

    public void testRecon() throws Exception {

        File inputFile = null;
        if (testURLBase == null) {
            inputFile = new File(testInputFileName);
        } else {
            URL testURL = new URL(testURLBase + "/" + testInputFileName);
            cache = new FileCache();
            inputFile = cache.getCachedFile(testURL);
        }

        testOutputFileName = "RecoTest_" + testInputFileName;
        File outputFile = new TestOutputFile(testOutputFileName);
        outputFile.getParentFile().mkdirs();
        boolean loop1Success = true;

        LCSimLoop loop = new LCSimLoop();
        loop.setLCIORecordSource(inputFile);

        final DatabaseConditionsManager manager = new DatabaseConditionsManager();
        manager.addConditionsListener(new SvtDetectorSetup());

        loop.add(new MainTrackingDriver());
        loop.add(new LCIODriver(outputFile));
        try {
            loop.loop(nEvents);
        } catch (Exception e) {
            System.out.println("Failure of main tracking sequence with following exception");
            System.out.println(e.toString());
            loop1Success = false;
        }
        loop.dispose();

        if (loop1Success && (testTrackingDriver != null)) {
            LCSimLoop loop2 = new LCSimLoop();
            loop2.add(testTrackingDriver);
            loop2.setLCIORecordSource(outputFile);
            try {
                loop2.loop(nEvents);
            } catch (Exception e) {
                System.out.println("Failure of testing sequence with following exception");
                System.out.println(e.toString());
            }
            loop2.dispose();
        }
    }

    protected class MainTrackingDriver extends Driver {

        public MainTrackingDriver() {

            RawTrackerHitSensorSetup rthss = new RawTrackerHitSensorSetup();
            String[] readoutColl = { "SVTRawTrackerHits" };
            rthss.setReadoutCollections(readoutColl);
            add(rthss);

            RawTrackerHitFitterDriver rthfd = new RawTrackerHitFitterDriver();
            rthfd.setFitAlgorithm("Pileup");
            rthfd.setUseTimestamps(false);
            rthfd.setUseTruthTime(false);
            rthfd.setSubtractTOF(true);
            rthfd.setSubtractTriggerTime(true);
            rthfd.setCorrectChanT0(true);
            rthfd.setCorrectTimeOffset(true);
            rthfd.setCorrectT0Shift(true);
            add(rthfd);

            org.hps.recon.tracking.DataTrackerHitDriver dthd = new org.hps.recon.tracking.DataTrackerHitDriver();
            dthd.setNeighborDeltaT(8.0);
            add(dthd);

            org.hps.recon.tracking.HelicalTrackHitDriver hthd = new org.hps.recon.tracking.HelicalTrackHitDriver();
            hthd.setClusterTimeCut(12.0);
            hthd.setClusterAmplitudeCut(400.0);
            hthd.setMaxDt(16.0);
            add(hthd);

            org.hps.recon.tracking.TrackerReconDriver trd2 = new org.hps.recon.tracking.TrackerReconDriver();
            trd2.setStrategyResource("HPS_s123_c4_e56.xml");
            trd2.setRmsTimeCut(8.0);
            trd2.setTrackCollectionName("s123_c5_e56");
            add(trd2);

            org.hps.recon.tracking.TrackerReconDriver trd3 = new org.hps.recon.tracking.TrackerReconDriver();
            trd3.setStrategyResource("HPS_s456_c3_e21.xml");
            trd3.setRmsTimeCut(8.0);
            trd3.setTrackCollectionName("s456_c3_e21");
            add(trd3);

            org.hps.recon.tracking.TrackerReconDriver trd4 = new org.hps.recon.tracking.TrackerReconDriver();
            trd4.setStrategyResource("HPS_s345_c2_e16.xml");
            trd4.setRmsTimeCut(8.0);
            trd4.setTrackCollectionName("s345_c2_e16");
            add(trd4);

            org.hps.recon.tracking.MergeTrackCollections mtc = new org.hps.recon.tracking.MergeTrackCollections();
            mtc.setInputTrackCollectionName("");
            mtc.setRemoveCollections(true);
            add(mtc);

            add(new org.hps.recon.tracking.gbl.GBLRefitterDriver());
            add(new org.hps.recon.tracking.gbl.GBLOutputDriver());
            add(new org.hps.recon.tracking.TrackDataDriver());

            add(new ReadoutCleanupDriver());
        }

    }
}
