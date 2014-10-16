package org.hps.recon.tracking.gbl;

import java.io.File;
import java.net.URL;
import junit.framework.TestCase;
import org.hps.conditions.deprecated.CalibrationDriver;
import org.hps.recon.tracking.HelicalTrackHitDriver;
import org.hps.recon.tracking.TrackerReconDriver;
import static org.hps.recon.tracking.gbl.GBLDriverTest.testURLBase;
import org.lcsim.event.EventHeader;
import org.lcsim.geometry.Detector;
import org.lcsim.util.cache.FileCache;
import org.lcsim.util.loop.LCSimLoop;

/**
 *
 * @author Norman A. Graf
 *
 * @version $Id$
 */
public class HpsGblRefitterTest extends TestCase
{

    static final String testURLBase = "http://www.lcsim.org/test/hps-java/";
    static final String testFileName = "HpsGblRefitterTest.slcio";
    private final int nEvents = 10;

    public void testHpsGblRefitter() throws Exception
    {
        File lcioInputFile = null;
        URL testURL = new URL(testURLBase + "/" + testFileName);
        FileCache cache = new FileCache();
        lcioInputFile = cache.getCachedFile(testURL);
        LCSimLoop loop = new LCSimLoop();
        loop.setLCIORecordSource(lcioInputFile);

        HpsGblRefitter fitter = new HpsGblRefitter();
        fitter.setDebug(false);
       
        loop.add(fitter);
        loop.loop(nEvents);
        loop.dispose();
    }
}