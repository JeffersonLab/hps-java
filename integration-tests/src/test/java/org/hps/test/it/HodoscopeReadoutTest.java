package org.hps.test.it;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.hps.job.DatabaseConditionsManagerSetup;
import org.hps.job.JobManager;
import org.hps.test.util.TestOutputFile;
import org.lcsim.conditions.ConditionsManager.ConditionsNotFoundException;
import org.lcsim.util.cache.FileCache;

import junit.framework.TestCase;

public class HodoscopeReadoutTest extends TestCase {
    
    static final String TEST_FILE_URL = "http://www.lcsim.org/test/hps-java/slicHodoTestEvents.slcio";
    
    public void testHodoscopeReadout() throws IOException, ConditionsNotFoundException {

        FileCache cache = new FileCache();
        File testFile = cache.getCachedFile(new URL(TEST_FILE_URL));
                    
        File outputFile = new TestOutputFile(HodoscopeReadoutTest.class, "HodoscopeReadoutTest");
                
        DatabaseConditionsManagerSetup cond = new DatabaseConditionsManagerSetup();
        cond.setDetectorName("HPS-HodoscopeTest-v1");
        cond.setRun(1000000);
        cond.setFreeze(true);
        
        JobManager mgr = new JobManager();
        mgr.setConditionsSetup(cond);
        mgr.addVariableDefinition("outputFile", outputFile.getPath());
        mgr.addInputFile(testFile);
        mgr.setup("/org/hps/steering/test/HodoscopeEnergySplitDriverTest.lcsim");
        mgr.run();
    }

}
