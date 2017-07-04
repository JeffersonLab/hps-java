package org.hps.recon.tracking;

import java.io.File;
import java.net.URL;

import junit.framework.TestCase;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.detector.svt.SvtDetectorSetup;
import org.lcsim.recon.tracking.digitization.sisim.config.RawTrackerHitSensorSetup;
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
 * @version $id: 1.0 06/04/17$
 */

public class ReconTestSkeleton extends TestCase {
    protected String testInputFileName = "raw_skim5766.slcio";
    protected String testOutputFileName = "test.slcio";
    protected String testURLBase = null;
    protected long nEvents = 100;
    protected URL testURL;
    protected FileCache cache;
    protected Driver testTrackingDriver = null;

    public void testRecon() throws Exception {
        File inputFile = null;
        if (testURLBase == null) {
            inputFile = new File(testInputFileName);

            //  inputFile.getParentFile().mkdirs();
        } else {
            URL testURL = new URL(testURLBase + "/" + testInputFileName);
            cache = new FileCache();
            inputFile = cache.getCachedFile(testURL);
        }

        File outputFile = new TestOutputFile(testOutputFileName);
        outputFile.getParentFile().mkdirs();
        boolean loop1Success = true;

        LCSimLoop loop = new LCSimLoop();
        loop.setLCIORecordSource(inputFile);

        final DatabaseConditionsManager manager = DatabaseConditionsManager.getInstance();
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
            rthfd.setFitAlgorithm("Analytic");
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

            org.hps.recon.tracking.TrackerReconDriver trd = new org.hps.recon.tracking.TrackerReconDriver();
            trd.setStrategyResource("HPS_s123_c5_e46.xml");
            trd.setRmsTimeCut(8.0);
            add(trd);

            org.hps.recon.tracking.TrackerReconDriver trd2 = new org.hps.recon.tracking.TrackerReconDriver();
            trd2.setStrategyResource("HPS_s123_c4_e56.xml");
            trd2.setRmsTimeCut(8.0);
            add(trd2);

            org.hps.recon.tracking.TrackerReconDriver trd3 = new org.hps.recon.tracking.TrackerReconDriver();
            trd3.setStrategyResource("HPS_s456_c3_e21.xml");
            trd3.setRmsTimeCut(8.0);
            add(trd3);

            org.hps.recon.tracking.TrackerReconDriver trd4 = new org.hps.recon.tracking.TrackerReconDriver();
            trd4.setStrategyResource("HPS_s345_c2_e16.xml");
            trd4.setRmsTimeCut(8.0);
            add(trd4);

            org.hps.recon.tracking.MergeTrackCollections mtc = new org.hps.recon.tracking.MergeTrackCollections();
            mtc.setInputTrackCollectionName("");
            mtc.setRemoveCollections(false);
            add(mtc);

            add(new org.hps.recon.tracking.gbl.GBLRefitterDriver());
            add(new org.hps.recon.tracking.gbl.GBLOutputDriver());
        }

    }
}
