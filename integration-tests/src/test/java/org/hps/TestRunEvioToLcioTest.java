package org.hps;

import java.io.File;
import java.net.URL;

import junit.framework.TestCase;

import org.hps.evio.EvioToLcio;
import org.lcsim.util.cache.FileCache;
import org.lcsim.util.test.TestUtil.TestOutputFile;

/**
 * Test to run the reconstruction on Test Run 2012 EVIO data.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class TestRunEvioToLcioTest extends TestCase {
    
    final static String fileLocation = "http://www.lcsim.org/test/hps-java/TestRunEvioToLcioTest.evio"; 
    
    public void testTestRunEvioToLcio() throws Exception {
        System.out.println("Caching file...");
        FileCache cache = new FileCache();
        File inputFile = cache.getCachedFile(new URL(fileLocation));
        String args[] = {
                "-r",                       
                "-x",
                "/org/hps/steering/recon/TestRunOfflineRecon.lcsim",
                "-d",
                "HPS-TestRun-v5",
                "-D",
                "outputFile=" + new TestOutputFile("TestRunEvioToLcioTest").getPath(),
                inputFile.getPath(),
                "-n",
                "100"
        };
        System.out.println("Running TestRunEvioToLcio.main ...");
        EvioToLcio.main(args);
    }

}
