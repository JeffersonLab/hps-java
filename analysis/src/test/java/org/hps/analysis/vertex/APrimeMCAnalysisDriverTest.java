/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hps.analysis.vertex;

import java.io.File;

import org.hps.util.test.TestUtil;
import org.hps.util.test.TestOutputFile;
import org.lcsim.util.loop.LCSimLoop;

import junit.framework.TestCase;

/**
 *
 * @author ngraf
 */
public class APrimeMCAnalysisDriverTest extends TestCase {

    static final String testFileName = "ap40mev_SLIC-v06-00-00_QGSP_BERT_HPS-EngRun2015-Nominal-v5-0-fieldmap_SimpleMCReconTest.slcio";
    private final int nEvents = -1;

    public void testIt() throws Exception {
        File outputFile = new TestOutputFile("APrimeMCAnalysis.aida");

        File lcioInputFile = TestUtil.downloadTestFile(testFileName);

        LCSimLoop loop = new LCSimLoop();
        loop.setLCIORecordSource(lcioInputFile);
        APrimeMCAnalysisDriver va = new APrimeMCAnalysisDriver();
        va.setOutputFile(outputFile.getPath());
        loop.add(va);
        loop.loop(nEvents);
        loop.dispose();
    }
}
