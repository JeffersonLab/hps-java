package org.hps.recon.tracking.ztrack;

import java.io.File;
import java.net.URL;
import junit.framework.TestCase;
import org.lcsim.util.cache.FileCache;
import org.lcsim.util.loop.LCSimLoop;

/**
 *
 * @author ngraf
 */
public class HpsEventTest extends TestCase {

    public void testIt() throws Exception {
        FileCache cache = new FileCache();
        int nEvents = 1; //-1;
        LCSimLoop loop = new LCSimLoop();
        HpsAnalysisDriver d = new HpsAnalysisDriver();
        loop.add(d);
        String fileName = "e-_2.3GeV_SLIC-v06-00-00_QGSP_BERT_HPS-PhysicsRun2016-Pass2_nomsc_9k_12SimTrackerHits.slcio";
//        String fileName = "mu-_1.056GeV_slic-3.1.5_geant4-v9r6p1_QGSP_BERT_HPS-EngRun2015-Nominal-v1_fieldOff_++_reco.slcio";
        File inputFile = cache.getCachedFile(new URL("http://www.lcsim.org/test/hps-java/" + fileName));
        loop.setLCIORecordSource(inputFile);
        loop.loop(nEvents);

        System.out.println("Loop processed " + loop.getTotalSupplied() + " events.");
        System.out.println("Done!");
    }

}
