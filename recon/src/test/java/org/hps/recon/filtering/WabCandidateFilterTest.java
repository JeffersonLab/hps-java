/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hps.recon.filtering;

import java.io.File;
import java.net.URL;
import junit.framework.TestCase;
import org.hps.conditions.database.DatabaseConditionsManager;
import org.lcsim.conditions.ConditionsManager;
import org.lcsim.util.cache.FileCache;
import org.lcsim.util.loop.LCSimLoop;
import org.lcsim.util.test.TestUtil;

/**
 *
 * @author ngraf
 */
public class WabCandidateFilterTest extends TestCase {

    static final String testURLBase = "http://www.lcsim.org/test/hps-java/";
    static final String testFileName = "hps_005772.100_recon_Rv4657.slcio";
    private final int nEvents = -1;

    public void testIt() throws Exception {

        new DatabaseConditionsManager();
        File outputDir = new TestUtil.TestOutputFile(this.getClass().getSimpleName());
        outputDir.mkdir();
        File lcioInputFile = null;
        URL testURL = new URL(testURLBase + "/" + testFileName);
        FileCache cache = new FileCache();
        lcioInputFile = cache.getCachedFile(testURL);

        LCSimLoop loop = new LCSimLoop();
        loop.setLCIORecordSource(lcioInputFile);
//        loop.setLCIORecordSource(new File("C:/hps_data/engrun2015/postTriSummitFixes/mc/recon/ap/1pt05/50/apsignalv2_displaced_1mm_epsilon-4_HPS-EngRun2015-Nominal-v5-0-fieldmap_3.10-20160813_pairs1_1.slcio"));
        WabCandidateFilter wabFilter = new WabCandidateFilter();
        loop.add(wabFilter);
        loop.loop(nEvents);
        loop.dispose();
    }
    
}
