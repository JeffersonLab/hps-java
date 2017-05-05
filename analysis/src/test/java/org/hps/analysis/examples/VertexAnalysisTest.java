/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hps.analysis.examples;

import java.io.File;
import java.net.URL;
import junit.framework.TestCase;
import org.hps.recon.tracking.gbl.HpsGblRefitter;
import org.lcsim.util.cache.FileCache;
import org.lcsim.util.loop.LCSimLoop;
import org.lcsim.util.test.TestUtil;

/**
 *
 */
public class VertexAnalysisTest extends TestCase {

    static final String testURLBase = "http://www.lcsim.org/test/hps-java/";
    static final String testFileName = "apsignalv2_displaced_HPS-EngRun2015-Nominal-v4-4-fieldmap_3.5-20151218.205540-15_pairs1_1-0-1000.slcio";
    private final int nEvents = 10;
    private boolean isMC = true;

    public void testIt() throws Exception {
        File outputDir = new TestUtil.TestOutputFile(this.getClass().getSimpleName());
        outputDir.mkdir();
        if (isMC) {
            System.setProperty("disableSvtAlignmentConstants", "true");
        }
        File lcioInputFile = null;
        URL testURL = new URL(testURLBase + "/" + testFileName);
        FileCache cache = new FileCache();
        lcioInputFile = cache.getCachedFile(testURL);
        LCSimLoop loop = new LCSimLoop();
        loop.setLCIORecordSource(lcioInputFile);

        VertexAnalysis va = new VertexAnalysis();
        loop.add(va);
        loop.loop(nEvents);
        loop.dispose();
    }

}
