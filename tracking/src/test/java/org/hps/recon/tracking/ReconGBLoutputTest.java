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
public class ReconGBLoutputTest extends TestCase {

    static {
        System.getProperties().setProperty("hep.aida.IAnalysisFactory", "hep.aida.ref.BatchAnalysisFactory");
    }
    protected String testInputFileName = "ap_prompt_raw.slcio";
    protected String testOutputFileName;
    protected String testURLBase = "http://www.lcsim.org/test/hps-java";
    protected long nEvents = 100;
    protected URL testURL;
    protected FileCache cache;

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

        LCSimLoop loop = new LCSimLoop();
        loop.setLCIORecordSource(inputFile);

        final DatabaseConditionsManager manager = DatabaseConditionsManager.getInstance();
        manager.addConditionsListener(new SvtDetectorSetup());

        loop.add(new MainTrackingDriver());
        loop.add(new LCIODriver(outputFile));
        try {
            loop.loop(nEvents);
        } catch (Exception e) {
            System.out.println("Failure of recon and GBLoutput test sequence with following exception");
            System.out.println(e.toString());
        }
        loop.dispose();

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

            org.hps.recon.tracking.TrackerReconDriver trd4 = new org.hps.recon.tracking.TrackerReconDriver();
            trd4.setStrategyResource("HPS_s345_c2_e16.xml");
            trd4.setRmsTimeCut(8.0);
            trd4.setTrackCollectionName("s345_c2_e16");
            add(trd4);

            org.hps.recon.tracking.MergeTrackCollections mtc = new org.hps.recon.tracking.MergeTrackCollections();
            mtc.setInputTrackCollectionName(new String[]{""});
            mtc.setRemoveCollections(true);
            add(mtc);

            org.hps.recon.tracking.gbl.GBLRefitterDriver GBLrd = new org.hps.recon.tracking.gbl.GBLRefitterDriver();
            GBLrd.setStoreTrackStates(true);
            GBLrd.setWriteMilleBinary(true);
            GBLrd.setMilleBinaryFileName("target/test-output/milleTest1.dat");
            add(GBLrd);

            add(new org.hps.recon.tracking.TrackDataDriver());

            org.hps.recon.tracking.gbl.GBLOutputDriver GBLod = new org.hps.recon.tracking.gbl.GBLOutputDriver();
            GBLod.setOutputPlotsFilename("target/test-output/GBLplots.root");
            add(GBLod);

            add(new ReadoutCleanupDriver());
        }

    }
}
