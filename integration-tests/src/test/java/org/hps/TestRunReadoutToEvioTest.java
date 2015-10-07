package org.hps;

import java.io.File;
import java.net.URL;
import java.util.logging.Level;

import junit.framework.TestCase;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.job.JobManager;
import org.lcsim.util.cache.FileCache;
import org.lcsim.util.test.TestUtil.TestOutputFile;

/**
 * <p>
 * This test will run the readout simulation on pre-filtered MC events
 * in a Test Run detector and write the output to EVIO.
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class TestRunReadoutToEvioTest extends TestCase {
    
    static final int nEvents = 10000;
    
    public void testTestRunReadoutToEvio() throws Exception {
        
        new TestOutputFile(this.getClass().getSimpleName()).mkdir();
        
        FileCache cache = new FileCache();
        File inputFile = cache.getCachedFile(new URL("http://www.lcsim.org/test/hps-java/TestRunReadoutToEvioTest.slcio"));
        
        JobManager job = new JobManager();
        DatabaseConditionsManager conditionsManager = DatabaseConditionsManager.getInstance();
        DatabaseConditionsManager.getInstance().setDetector("HPS-TestRun-v5", 1351);
        conditionsManager.freeze();
        job.addInputFile(inputFile);
        File outputFile = new TestOutputFile(this.getClass().getSimpleName() + File.separator + this.getClass().getSimpleName());
        job.addVariableDefinition("outputFile", outputFile.getPath());
        job.setup("/org/hps/steering/readout/TestRunReadoutToEvio.lcsim");
        job.setNumberOfEvents(nEvents);
        job.run();       
    }
}
