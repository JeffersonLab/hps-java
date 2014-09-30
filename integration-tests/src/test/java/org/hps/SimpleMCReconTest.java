package org.hps;

import java.io.File;
import java.net.URL;

import junit.framework.TestCase;

import org.lcsim.job.JobControlManager;
import org.lcsim.util.Driver;
import org.lcsim.util.cache.FileCache;
import org.lcsim.util.test.TestUtil.TestOutputFile;

public class SimpleMCReconTest extends TestCase {

	public void testSimpleMCReconTest() throws Exception {
		
        new TestOutputFile(this.getClass().getSimpleName()).mkdir();
		
        FileCache cache = new FileCache();
        File inputFile = cache.getCachedFile(new URL("http://www.lcsim.org/test/hps-java/SimpleMCReconTest.slcio"));
		
		JobControlManager job = new JobControlManager();
        File outputFile = new TestOutputFile(this.getClass().getSimpleName() + File.separator + this.getClass().getSimpleName() + "_recon");
        job.addVariableDefinition("outputFile", outputFile.getPath());
        job.addInputFile(inputFile);
        job.setup("/org/hps/steering/recon/SimpleMCRecon.lcsim");
        job.run();
	}	
}