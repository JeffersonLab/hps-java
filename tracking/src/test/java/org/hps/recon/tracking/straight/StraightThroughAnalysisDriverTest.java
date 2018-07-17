/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hps.recon.tracking.straight;

import java.io.File;
import java.net.URL;
import junit.framework.TestCase;
import org.lcsim.util.cache.FileCache;
import org.lcsim.util.loop.LCSimLoop;

/**
 *
 * @author ngraf
 */
public class StraightThroughAnalysisDriverTest extends TestCase {

    static final String testURLBase = "http://www.lcsim.org/test/hps-java/";
    static final String testFileName = "hps_008100_0_fullRecon.slcio";
//    static final String testFileName = "hpsForwardFullEnergyElectrons_z-2338_top_0_SLIC-v06-00-00_QGSP_BERT_HPS-PhysicsRun2016-Nominal-v5-0-nofield_nomsc_recon.slcio";
    private final int nEvents = -1;

    public void testIt() throws Exception {
        URL testURL = new URL(testURLBase + "/" + testFileName);
        FileCache cache = new FileCache();
        File lcioInputFile = cache.getCachedFile(testURL);
        LCSimLoop loop = new LCSimLoop();
        loop.setLCIORecordSource(lcioInputFile);
        StraightThroughAnalysisDriver sta = new StraightThroughAnalysisDriver();
        loop.add(sta);
        loop.loop(nEvents);
        loop.dispose();
    }
}
