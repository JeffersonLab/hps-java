package org.hps;

import java.io.File;
import java.net.URL;

import org.lcsim.job.JobControlManager;
import org.lcsim.util.cache.FileCache;
import org.lcsim.util.test.TestUtil.TestOutputFile;

import junit.framework.TestCase;

public class ReadoutNoPileupTest extends TestCase {
        
    public void testReadoutNoPileup() throws Exception {
        new TestOutputFile(this.getClass().getSimpleName()).mkdir();
        
        FileCache cache = new FileCache();
        File inputFile = cache.getCachedFile(new URL("http://www.lcsim.org/test/hps-java/ReadoutNoPileupTest.slcio"));
        
        JobControlManager job = new JobControlManager();
        job.addInputFile(inputFile);
        File outputFile = new TestOutputFile(this.getClass().getSimpleName() + File.separator + this.getClass().getSimpleName() + "_readout");
        job.addVariableDefinition("outputFile", outputFile.getPath());
        job.setup("/org/hps/steering/readout/HPS2014ReadoutNoPileup.lcsim");
        job.run();
    }
}
