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
    
    final static String fileLocation = "ftp://ftp-hps.slac.stanford.edu/hps/hps_data/hps_java_test_case_data/TestRunEvioToLcioTest.evio"; 
    
    public void testTestRunEvioToLcio() throws Exception {
        
        System.out.println("Caching file...");
        FileCache cache = new FileCache();
        File inputFile = cache.getCachedFile(new URL(fileLocation));
        
        List<String> argList = new ArrayList<String>();
        argList.add("-r");                       
        argList.add("-x");
        argList.add("/org/hps/steering/recon/TestRunOfflineRecon.lcsim");
        argList.add("-d");
        argList.add("HPS-TestRun-v8-5");
        argList.add("-D");
        argList.add("runNumber=1351");
        argList.add("-D");
        argList.add("outputFile=" + new TestOutputFile("TestRunEvioToLcioTest").getPath());
        argList.add(inputFile.getPath());
        System.out.println("Running TestRunEvioToLcio.main ...");
        TestRunEvioToLcio.main(argList.toArray(new String[]{}));
    }

}
