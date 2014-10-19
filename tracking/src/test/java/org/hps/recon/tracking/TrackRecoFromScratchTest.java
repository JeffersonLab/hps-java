package org.hps.recon.tracking;

import java.io.File;
import java.net.URL;
import junit.framework.TestCase;
import org.lcsim.util.cache.FileCache;
import org.lcsim.util.loop.LCSimLoop;

/**
 * This provides a template for testing track reconstruction issues
 * 
 * @author Norman A Graf
 *
 * @version $Id:
 */
public class TrackRecoFromScratchTest extends TestCase
{
    static final String testURLBase = "http://www.lcsim.org/test/hps-java";
    static final String testFileName = "radmuon_12.lcio-1-1788.slcio";
    private final int nEvents = 10;

    public void testRecon() throws Exception
    {
        File lcioInputFile = null;
        URL testURL = new URL(testURLBase + "/" + testFileName);
        FileCache cache = new FileCache();
        lcioInputFile = cache.getCachedFile(testURL);
        LCSimLoop loop = new LCSimLoop();
        loop.setLCIORecordSource(lcioInputFile);

        loop.add(new org.hps.conditions.deprecated.CalibrationDriver());
        loop.add(new org.hps.recon.tracking.SimpleTrackerDigiDriver());
        loop.add(new org.hps.recon.tracking.HelicalTrackHitDriver());
        loop.add(new org.hps.recon.tracking.TrackerReconDriver());
        loop.add(new org.hps.recon.tracking.gbl.GBLOutputDriver());

        try {
            loop.loop(nEvents);
        } catch (Exception e) {
            System.out.println("test should have failed");
            System.out.println("e");
        }
        
        loop.dispose();
    }
}
