package org.hps.analysis.examples;

import java.io.File;

import org.hps.util.test.TestUtil;
import org.lcsim.util.loop.LCSimLoop;

import junit.framework.TestCase;

/**
 *
 * @author ngraf
 */
public class VertexAnalysisTest extends TestCase {

    static final String testFileName = "apsignalv2_displaced_HPS-EngRun2015-Nominal-v4-4-fieldmap_3.5-20151218.205540-15_pairs1_1-0-1000.slcio";
    private final int nEvents = 10;

    public void testIt() throws Exception {

        File lcioInputFile = TestUtil.downloadTestFile(testFileName);

        LCSimLoop loop = new LCSimLoop();
        loop.setLCIORecordSource(lcioInputFile);

        VertexAnalysis va = new VertexAnalysis();
        loop.add(va);
        loop.loop(nEvents);
        loop.dispose();
    }

}
