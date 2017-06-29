package org.hps.recon.tracking;

import java.io.File;
//import java.net.URL;

import junit.framework.TestCase;

import org.hps.conditions.database.DatabaseConditionsManager;
//import org.hps.conditions.svt.SvtConditions;
//import org.hps.detector.svt.SvtDetectorSetup;
//import org.lcsim.geometry.Detector;
//import org.lcsim.geometry.compact.Subdetector;
import org.lcsim.recon.tracking.digitization.sisim.config.RawTrackerHitSensorSetup;
//import org.lcsim.util.cache.FileCache;
import org.lcsim.util.loop.LCSimLoop;

//import org.hps.evio.RfFitterDriver;

/**
 * This provides a template for testing track reconstruction issues
 * 
 * @author Norman A Graf
 *
 * @version $Id:
 */
public class TrackRecoFromScratchTest extends TestCase {
    //static final String testURLBase = "http://www.lcsim.org/test/hps-java";
    static final String testFileName = "raw_skim5766.slcio";
    private final int nEvents = 10;

    public void testRecon() throws Exception {
        File lcioInputFile = null;
        //URL testURL = new URL(testURLBase + "/" + testFileName);
        //FileCache cache = new FileCache();
        lcioInputFile = new File(testFileName);

        LCSimLoop loop = new LCSimLoop();
        loop.setLCIORecordSource(lcioInputFile);

        final DatabaseConditionsManager manager = DatabaseConditionsManager.getInstance();
        //manager.setDetector(DETECTOR, run);
        //        manager.addConditionsListener(new SvtDetectorSetup());
        manager.setDetector("HPS-EngRun2015-Nominal-v4-4-fieldmap", 5766);

        //        final Detector detector = manager.getCachedConditions(Detector.class, "compact.xml").getCachedData();
        //        final SvtConditions cond = manager.getCachedConditions(SvtConditions.class, "svt_conditions").getCachedData();
        //        Subdetector subdetector = detector.getSubdetector("Tracker");
        //        final SvtDetectorSetup loader = new SvtDetectorSetup("Tracker");
        //        loader.loadDefault(subdetector, cond);

        //loop.add(new org.hps.recon.tracking.SimpleTrackerDigiDriver());

        RawTrackerHitSensorSetup rthss = new RawTrackerHitSensorSetup();
        String[] readoutColl = { "SVTRawTrackerHits" };
        rthss.setReadoutCollections(readoutColl);
        loop.add(rthss);

        RawTrackerHitFitterDriver rthfd = new RawTrackerHitFitterDriver();
        rthfd.setFitAlgorithm("Analytic");
        rthfd.setUseTimestamps(false);
        rthfd.setUseTruthTime(false);
        rthfd.setSubtractTOF(true);
        rthfd.setSubtractTriggerTime(true);
        rthfd.setCorrectChanT0(true);
        rthfd.setCorrectTimeOffset(true);
        rthfd.setCorrectT0Shift(true);
        loop.add(rthfd);

        org.hps.recon.tracking.DataTrackerHitDriver dthd = new org.hps.recon.tracking.DataTrackerHitDriver();
        dthd.setNeighborDeltaT(8.0);
        loop.add(dthd);

        org.hps.recon.tracking.HelicalTrackHitDriver hthd = new org.hps.recon.tracking.HelicalTrackHitDriver();
        hthd.setClusterTimeCut(12.0);
        hthd.setClusterAmplitudeCut(400.0);
        hthd.setMaxDt(16.0);
        loop.add(hthd);

        org.hps.recon.tracking.TrackerReconDriver trd = new org.hps.recon.tracking.TrackerReconDriver();
        trd.setStrategyResource("HPS_s123_c5_e46.xml");
        trd.setRmsTimeCut(8.0);
        loop.add(trd);

        org.hps.recon.tracking.TrackerReconDriver trd2 = new org.hps.recon.tracking.TrackerReconDriver();
        trd2.setStrategyResource("HPS_s123_c4_e56.xml");
        trd2.setRmsTimeCut(8.0);
        loop.add(trd2);

        org.hps.recon.tracking.TrackerReconDriver trd3 = new org.hps.recon.tracking.TrackerReconDriver();
        trd3.setStrategyResource("HPS_s456_c3_e21.xml");
        trd3.setRmsTimeCut(8.0);
        loop.add(trd3);

        org.hps.recon.tracking.TrackerReconDriver trd4 = new org.hps.recon.tracking.TrackerReconDriver();
        trd4.setStrategyResource("HPS_s345_c2_e16.xml");
        trd4.setRmsTimeCut(8.0);
        loop.add(trd4);

        org.hps.recon.tracking.MergeTrackCollections mtc = new org.hps.recon.tracking.MergeTrackCollections();
        mtc.setInputTrackCollectionName("");
        mtc.setRemoveCollections(false);
        loop.add(mtc);

        loop.add(new org.hps.recon.tracking.gbl.GBLRefitterDriver());
        loop.add(new org.hps.recon.tracking.gbl.GBLOutputDriver());

        org.lcsim.util.loop.LCIODriver lciodriver = new org.lcsim.util.loop.LCIODriver();
        lciodriver.setOutputFilePath("reconLCIOtest.slcio");
        loop.add(lciodriver);

        try {
            loop.loop(nEvents);
        } catch (Exception e) {
            System.out.println("test should have failed");
            System.out.println("e");
        }

        loop.dispose();
    }
}
