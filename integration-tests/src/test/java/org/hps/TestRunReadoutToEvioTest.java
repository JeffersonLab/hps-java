package org.hps;

import java.io.File;
import java.net.URL;

import junit.framework.TestCase;

import org.lcsim.job.JobControlManager;
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
    
    static final int nEvents = 100;
    
    public void testTestRunReadoutToEvio() throws Exception {
        
        new TestOutputFile(this.getClass().getSimpleName()).mkdir();
        
        FileCache cache = new FileCache();
        File inputFile = cache.getCachedFile(new URL("http://www.lcsim.org/test/hps-java/TestRunReadoutToEvioTest.slcio"));
        
        JobControlManager job = new JobControlManager();
        job.addInputFile(inputFile);
        File outputFile = new TestOutputFile(this.getClass().getSimpleName() + File.separator + this.getClass().getSimpleName());
        job.addVariableDefinition("outputFile", outputFile.getPath());
        job.setup("/org/hps/steering/readout/TestRunReadoutToEvio.lcsim");
        job.setNumberOfEvents(nEvents);
        job.run();       
    }
}
