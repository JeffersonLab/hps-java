package org.hps.recon.tracking;

import java.io.File;
import java.net.URL;
import junit.framework.TestCase;
import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.detector.svt.SvtDetectorSetup;
import org.lcsim.recon.tracking.digitization.sisim.config.RawTrackerHitSensorSetup;
import org.lcsim.recon.tracking.digitization.sisim.config.ReadoutCleanupDriver;
import org.lcsim.util.cache.FileCache;
import org.lcsim.util.loop.LCSimLoop;

/**
 *
 * This class provides a testbed for SVT hit reconstruction on data. Running
 * once with rerunFromFittedHits set false emulates the full recon chain from
 * scratch. Running again with rerunFromFittedHits set true tests whether we can
 * successfully reconstruct from an output LCIO file with the
 * FittedRawTrackerHit collections. Should check that the output
 * StripClusterer_SiTrackerHitStrip1D are the same in both cases.
 *
 * [ DataTrackerHitDriver ] - StripClusterer_SiTrackerHitStrip1D has 56 hits.
 * layer 0, count 9 layer 1, count 9 layer 2, count 3 layer 3, count 5 layer 4,
 * count 7 layer 5, count 9 layer 6, count 2 layer 7, count 4 layer 8, count 1
 * layer 9, count 2 layer 10, count 1 layer 11, count 1 layer 12, count 3 layer
 * 13, count 0
 *
 * I could automate this but not worth the effort at this time.
 *
 *
 * @author Norman A Graf
 */
public class DataTrackerHitDriverTest extends TestCase {

    public static void testIt() throws Exception {
        FileCache cache = new FileCache();
        int nEvents = 1;
        LCSimLoop loop = new LCSimLoop();

        final DatabaseConditionsManager manager = DatabaseConditionsManager.getInstance();
        manager.addConditionsListener(new SvtDetectorSetup());

        RawTrackerHitSensorSetup rawSensorSetup = new RawTrackerHitSensorSetup();
        String[] readoutCollections = {"SVTRawTrackerHits"};
        rawSensorSetup.setReadoutCollections(readoutCollections);

        RawTrackerHitFitterDriver rthfDriver = new RawTrackerHitFitterDriver();
        rthfDriver.setFitAlgorithm("Pileup");
        rthfDriver.setUseTimestamps(false);
        rthfDriver.setCorrectTimeOffset(true);
        rthfDriver.setCorrectT0Shift(false);
        rthfDriver.setUseTruthTime(false);
        rthfDriver.setSubtractTOF(true);
        rthfDriver.setSubtractTriggerTime(true);
        rthfDriver.setCorrectChanT0(false);
        rthfDriver.setDebug(false);

        boolean rerunFromFittedHits = false;
        DataTrackerHitDriver dthDriver = new DataTrackerHitDriver();
        dthDriver.setDebug(true);

        loop.add(rawSensorSetup);
        loop.add(rthfDriver);
        loop.add(dthDriver);
        loop.add(new ReadoutCleanupDriver());

        String fileName = "tstDataTrackerHitDriver.slcio";
        File inputFile = cache.getCachedFile(new URL("http://www.lcsim.org/test/hps-java/" + fileName));
        loop.setLCIORecordSource(inputFile);
        loop.loop(nEvents);
        loop.dispose();

        System.out.println("Loop from raw processed " + loop.getTotalSupplied() + " events.");

        System.out.println("rerunning from LCIO fitted hits");

        SensorSetup sensorSetup = new SensorSetup();
        sensorSetup.setReadoutCollections(new String[]{"SVTRawTrackerHits"});
        sensorSetup.setFittedHitCollection("SVTFittedRawTrackerHits");

        LCSimLoop loop2 = new LCSimLoop();
        loop2.setLCIORecordSource(inputFile);
        
        loop2.add(sensorSetup);
//        loop2.add(dthDriver);
        loop.add(new ReadoutCleanupDriver());
        loop2.loop(nEvents);
        loop2.dispose();

        System.out.println("Loop from fitted LCIO processed " + loop.getTotalSupplied() + " events.");

        System.out.println("Done!");
    }

}
