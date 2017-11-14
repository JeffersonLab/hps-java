/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hps.analysis.vertex;

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
public class APrimeMCAnalysisDriverTest extends TestCase {

    static final String testURLBase = "http://www.lcsim.org/test/hps-java/";
    static final String testFileName = "ap40mev_SLIC-v06-00-00_QGSP_BERT_HPS-EngRun2015-Nominal-v5-0-fieldmap_SimpleMCReconTest.slcio";
    private final int nEvents = -1;
    private boolean isMC = true;

    public void testIt() throws Exception {
        File outputDir = new TestUtil.TestOutputFile(this.getClass().getSimpleName());
        outputDir.mkdir();
        if (isMC) {
            System.setProperty("disableSvtAlignmentConstants", "true");
        }
        URL testURL = new URL(testURLBase + "/" + testFileName);
        FileCache cache = new FileCache();
        File lcioInputFile = cache.getCachedFile(testURL);

        LCSimLoop loop = new LCSimLoop();
        loop.setLCIORecordSource(lcioInputFile);
        APrimeMCAnalysisDriver va = new APrimeMCAnalysisDriver();
        va.setOutputFile("target/test-output/APrimeMCAnalysis.aida");
        loop.add(va);
        loop.loop(nEvents);
        loop.dispose();
    }
}
