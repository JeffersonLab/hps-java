package org.hps.recon.tracking.gbl;

import java.io.File;
import java.net.URL;
import junit.framework.TestCase;
import org.lcsim.util.cache.FileCache;
import org.lcsim.util.loop.LCSimLoop;
import org.lcsim.util.test.TestUtil;

/**
 *
 * @author Norman A. Graf
 *
 */
public class GBLRefitterDriverTest extends TestCase
{
    static final String testURLBase = "http://www.lcsim.org/test/hps-java/";
    static final String testFileName = "hpsrun2016_run7636_onetrackonecluster_1000events.slcio";
    private final int nEvents = 10;

    public void testIt() throws Exception
    {
        File outputDir = new TestUtil.TestOutputFile(this.getClass().getSimpleName());
        outputDir.mkdir();

        URL testURL = new URL(testURLBase + "/" + testFileName);
        FileCache cache = new FileCache();
        File lcioInputFile = cache.getCachedFile(testURL);
        LCSimLoop loop = new LCSimLoop();
        loop.setLCIORecordSource(lcioInputFile);

        GBLRefitterDriver fitter = new GBLRefitterDriver();

        loop.add(fitter);
        loop.loop(nEvents);
        loop.dispose();
    }
    
}
