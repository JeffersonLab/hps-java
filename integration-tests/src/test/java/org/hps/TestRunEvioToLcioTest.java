package org.hps;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.hps.evio.TestRunEvioToLcio;
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
        
        List<String> argList = new ArrayList<String>();
        argList.add("-r");                       
        argList.add("-x");
        argList.add("/org/hps/steering/recon/TestRunOfflineRecon.lcsim");
        argList.add("-d");
        argList.add("HPS-TestRun-v5");
        argList.add("-D");
        argList.add("outputFile=" + new TestOutputFile("TestRunEvioToLcioTest").getPath());
        argList.add(inputFile.getPath());
        argList.add("-R");
        argList.add("1351");
        System.out.println("Running TestRunEvioToLcio.main ...");
        TestRunEvioToLcio.main(argList.toArray(new String[]{}));
    }

}
