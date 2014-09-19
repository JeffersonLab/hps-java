package org.hps;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.hps.evio.TestRunEvioToLcio;
import org.lcsim.util.test.TestUtil.TestOutputFile;

/**
 * Test to run the reconstruction on Test Run 2012 EVIO data.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class TestRunEvioToLcioTest extends TestCase {
        
    public void testTestRunEvioToLcio() {
        
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
        argList.add("/nfs/slac/g/hps3/data/testrun/runs/evio/hps_001351.evio.0");
        TestRunEvioToLcio.main(argList.toArray(new String[]{}));
    }

}
