package org.hps.analysis.fieldoff;

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
public class StripEventDriverTest extends TestCase {

    static final String testURLBase = "http://www.lcsim.org/test/hps-java/fieldoff";
    static final String testFileName = "fullRecon_stripFieldOffFee_topSlot_test.slcio";
    private final int nEvents = -1;

    public void testIt() throws Exception {
        File outputDir = new TestUtil.TestOutputFile(this.getClass().getSimpleName());
        outputDir.mkdir();
        System.setProperty("disableSvtAlignmentConstants", "true");
        File lcioInputFile = null;
        URL testURL = new URL(testURLBase + "/" + testFileName);
        FileCache cache = new FileCache();
        lcioInputFile = cache.getCachedFile(testURL);
        LCSimLoop loop = new LCSimLoop();
        loop.setLCIORecordSource(lcioInputFile);

        StripEventDriver sed = new StripEventDriver();
        sed.setSelectAllLayers(true);
        sed.setMinNumberOfStripHits(12);
        sed.setRequireTwoStripHitInLayerOne(true);
        loop.add(sed);
        loop.loop(nEvents);
        loop.dispose();
    }

}
