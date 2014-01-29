package org.lcsim.hps.users.ngraf;

import java.io.File;
import java.net.URL;
import junit.framework.TestCase;
import org.lcsim.util.cache.FileCache;
import org.lcsim.util.loop.LCSimLoop;

/**
 *
 * @author ngraf
 */
public class NearestNeighborClusterDriverTest extends TestCase
{
 static final String testURLBase = "http://www.slac.stanford.edu/~ngraf/hps_data/";
    static final String testFileName = "outfile3.slcio";
    private final int nEvents = 500;

    public void testClustering() throws Exception {

        File lcioInputFile = null;

        URL testURL = new URL(testURLBase + "/" + testFileName);
        FileCache cache = new FileCache();
        lcioInputFile = cache.getCachedFile(testURL);

        //Process and write out the file
        LCSimLoop loop = new LCSimLoop();
        loop.setLCIORecordSource(lcioInputFile);
        loop.add(new NearestNeighborClusterDriver());
        loop.loop(nEvents, null);
        loop.dispose();

    }
}
