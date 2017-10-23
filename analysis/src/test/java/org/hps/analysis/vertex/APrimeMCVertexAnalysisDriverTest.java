package org.hps.analysis.vertex;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import junit.framework.TestCase;
import org.lcsim.util.cache.FileCache;
import org.lcsim.util.loop.FileList;
import org.lcsim.util.loop.LCIOEventSource;
import org.lcsim.util.loop.LCSimLoop;
import org.lcsim.util.test.TestUtil;

/**
 *
 * @author Norman A. Graf
 */
public class APrimeMCVertexAnalysisDriverTest extends TestCase {

    static final String testURLBase = "http://www.lcsim.org/test/hps-java/";
    static final String testFileName = "apsignalv2_displaced_100mm_epsilon-6_HPS-EngRun2015-Nominal-v5-0-fieldmap_3.11-SNAPSHOT_pairs1_V0Skim.slcio";
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
//        List<File> files = new ArrayList<File>();
//        for(int i=1; i<10; ++i)
//        {
//            files.add(new File("C:/hps_data/engrun2015/postTriSummitFixes/mc/recon/ap/1pt05/50/apsignalv2_displaced_1mm_epsilon-4_HPS-EngRun2015-Nominal-v5-0-fieldmap_3.10-20160813_pairs1_"+i+".slcio"));
//        }
//        LCIOEventSource source = new LCIOEventSource(new FileList(files, "aprime 50MeV"));
//        loop.setLCIORecordSource(source);
        loop.setLCIORecordSource(lcioInputFile);
        APrimeMCVertexAnalysisDriver va = new APrimeMCVertexAnalysisDriver();
        loop.add(va);
        loop.loop(nEvents);
        loop.dispose();
    }
}
