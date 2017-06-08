/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hps.recon.filtering;

import java.io.File;
import java.net.URL;
import junit.framework.TestCase;
import org.lcsim.util.cache.FileCache;
import org.lcsim.util.loop.LCSimLoop;
import org.lcsim.util.test.TestUtil;

/**
 *
 * @author ngraf
 */
public class V0CandidateFilterTest extends TestCase {

    static final String testURLBase = "http://www.lcsim.org/test/hps-java/";
    static final String testFileName = "run5772_pass6_V0CandidateSkim.slcio";
    private final int nEvents = -1;

    public void testIt() throws Exception {

        File outputDir = new TestUtil.TestOutputFile(this.getClass().getSimpleName());
        outputDir.mkdir();
        File lcioInputFile = null;
        URL testURL = new URL(testURLBase + "/" + testFileName);
        FileCache cache = new FileCache();
        lcioInputFile = cache.getCachedFile(testURL);

        LCSimLoop loop = new LCSimLoop();
        loop.setLCIORecordSource(lcioInputFile);
//        loop.setLCIORecordSource(new File("C:/hps_data/engrun2015/postTriSummitFixes/mc/recon/ap/1pt05/50/apsignalv2_displaced_1mm_epsilon-4_HPS-EngRun2015-Nominal-v5-0-fieldmap_3.10-20160813_pairs1_1.slcio"));
        V0CandidateFilter va = new V0CandidateFilter();
        va.setTightConstraint(true);
        loop.add(va);
        loop.loop(nEvents);
        loop.dispose();
    }
}
