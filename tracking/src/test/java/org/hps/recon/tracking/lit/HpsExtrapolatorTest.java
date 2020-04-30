package org.hps.recon.tracking.lit;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Paths;
import junit.framework.TestCase;
import org.lcsim.util.Driver;
import org.lcsim.util.cache.FileCache;
import org.lcsim.util.loop.LCSimLoop;

/**
 *
 * @author Norman A Graf
 */
public class HpsExtrapolatorTest extends TestCase {

    public void testIt() throws Exception {
        System.out.println("Running from: " + Paths.get("").toAbsolutePath());
        //String fileName = "http://www.lcsim.org/test/hps-java//hpsMCForwardFullEnergyElectrons_z-7.5_bottom_0_SLIC-v06-00-00_QGSP_BERT_HPS-PhysicsRun2019-v1-4pt5_nomsc_strip14hits_recon.slcio";
//        String fileName = "http://www.lcsim.org/test/hps-java/tsthpsForwardFullEnergyElectrons_z-7.5_bottom_0_SLIC-v06-00-00_QGSP_BERT_HPS-PhysicsRun2019-v1-4pt5_nomsc_strip14hits_recon.slcio";
        // 2016
        String fileName = "http://www.lcsim.org/test/hps-java/2016/hpsForwardFullEnergyElectrons_z0.0_2.3GeV_bottom_SLIC-v06-00-01_QGSP_BERT_HPS-PhysicsRun2016-Pass2_strip12Hits_recon.slcio";
        FileCache cache = new FileCache();
        int nEvents = -1;
        LCSimLoop loop = new LCSimLoop();
        HpsExtrapolatorDriver d = new HpsExtrapolatorDriver();
        d.setDebug(false);
        d.setTrackit(false);
        //HpsTrfFitDriver d = new HpsTrfFitDriver();
        loop.add(d);
        try {
            File inputFile = cache.getCachedFile(new URL(fileName));
            loop.setLCIORecordSource(inputFile);
            loop.loop(nEvents);
            // d.showPlots();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println("Loop processed " + loop.getTotalSupplied() + " events.");
        System.out.println("Done!");
    }
}
