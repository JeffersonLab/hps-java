package org.hps.recon.filtering;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import junit.framework.TestCase;
import org.hps.conditions.database.DatabaseConditionsManager;
import org.lcsim.util.cache.FileCache;
import org.lcsim.util.loop.LCSimLoop;

/**
 *
 * @author ngraf
 */
public class V0CandidateFilter2015Pass7Test extends TestCase {

//    static final String testFileName = "http://www.lcsim.org/test/hps-java/run5772_pass6_V0CandidateSkim.slcio";
    static final String testFileName = "http://www.lcsim.org/test/hps-java/EngRun2015V0ReconTest.slcio";

    public void testIt() throws Exception {
        URL testURL = new URL(testFileName);
        FileCache cache = new FileCache();
        File inputFile = cache.getCachedFile(testURL);
        LCSimLoop loop = new LCSimLoop();
        DatabaseConditionsManager.getInstance();
        V0CandidateFilter2015Pass7 driver = new V0CandidateFilter2015Pass7();
        loop.add(driver);
        try {
            loop.setLCIORecordSource(inputFile);
            loop.loop(-1);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
