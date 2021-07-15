package org.hps.recon.tracking;

import java.io.File;
import java.net.URL;
import junit.framework.TestCase;
import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.detector.svt.SvtDetectorSetup;
import org.lcsim.recon.tracking.digitization.sisim.config.RawTrackerHitSensorSetup;
import org.lcsim.util.cache.FileCache;
import org.lcsim.util.loop.LCIODriver;
import org.lcsim.util.loop.LCSimLoop;

/**
 *
 * @author ngraf
 */
public class HelicalTrackHitDriverTest extends TestCase {

    static final String testFileName = "hps_010022.evio.00000-0-10.slcio";

    public void testIt() throws Exception {

        URL testURL = new URL("http://www.lcsim.org/test/hps-java/hps_010022.evio.00000-0-10.slcio");
        FileCache cache = new FileCache();
        File lcioInputFile = cache.getCachedFile(testURL);

        final DatabaseConditionsManager manager = DatabaseConditionsManager.getInstance();
        manager.addConditionsListener(new SvtDetectorSetup());

        //File lcioInputFile = TestUtil.downloadTestFile("hps_010022.evio.00000-0-10.slcio");
        LCSimLoop loop = new LCSimLoop();
        loop.setLCIORecordSource(lcioInputFile);

        RawTrackerHitSensorSetup d1 = new RawTrackerHitSensorSetup();
        d1.setReadoutCollections(new String[]{"SVTRawTrackerHits"});
        
        RawTrackerHitFitterDriver d2 = new RawTrackerHitFitterDriver();
        d2.setFitAlgorithm("Pileup");
        d2.setUseTimestamps(false);
        d2.setCorrectTimeOffset(true);
        d2.setCorrectT0Shift(false);
        d2.setUseTruthTime(false);
        d2.setSubtractTOF(true);
        d2.setSubtractTriggerTime(true);
        d2.setCorrectChanT0(false);
        d2.setDebug(false);
        
        DataTrackerHitDriver d3 = new DataTrackerHitDriver();
        d3.setNeighborDeltaT(8.0);
        d3.setDebug(false);
        
        HelicalTrackHitDriver d4 = new HelicalTrackHitDriver();
        d4.setClusterTimeCut(40.0);
        d4.setClusterAmplitudeCut(400.0);
        d4.setMaxDt(20.0);
        d4.setSaveAxialHits(false);
        d4.setDebug(true);
        
        loop.add(d1);
        loop.add(d2);
        loop.add(d3);
        loop.add(d4);
        loop.add(new LCIODriver("test.slcio"));
        loop.loop(1);
        loop.dispose();
    }
}
