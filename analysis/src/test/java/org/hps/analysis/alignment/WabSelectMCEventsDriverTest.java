package org.hps.analysis.alignment;

import java.io.File;
import java.net.URL;
import junit.framework.TestCase;
import org.hps.analysis.examples.VertexAnalysis;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.lcsim.event.EventHeader;
import org.lcsim.util.cache.FileCache;
import org.lcsim.util.loop.LCSimLoop;
import org.lcsim.util.test.TestUtil;

/**
 *
 * @author Norman A. Graf
 */
public class WabSelectMCEventsDriverTest extends TestCase {

    static final String testURLBase = "http://www.lcsim.org/test/hps-java/";
    static final String testFileName = "wab_1_SLIC-v06-00-00_QGSP_BERT_HPS-PhysicsRun2019-v1-4pt5.slcio";
    private final int nEvents = -1;
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

        WabSelectMCEventsDriver wabSelect = new WabSelectMCEventsDriver();
        loop.add(wabSelect);
        loop.loop(nEvents);
        loop.dispose();
    }
}
