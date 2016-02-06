package org.hps.test.it;

import java.io.File;

import junit.framework.TestCase;

import org.hps.data.test.TestDataUtility;
import org.hps.job.JobManager;
import org.lcsim.util.test.TestUtil.TestOutputFile;

/**
 * Run a test job on Eng Run 2015 data.
 * 
 * @author Jeremy McCormick, SLAC
 */
public class ReconSteeringTest extends TestCase {
    
    final static String STEERING_RESOURCE = "/org/hps/steering/recon/EngineeringRun2015FullRecon.lcsim";
              
    public void testReconSteering() throws Exception {
        
        File inputFile = new TestDataUtility().getTestData("run_5772_data_only.slcio");
                           
        File outputFile = null;
        outputFile = new TestOutputFile(new File(STEERING_RESOURCE).getName().replace(".lcsim", ""));
        System.out.println("Testing steering " + STEERING_RESOURCE + " ...");
        JobManager job = new JobManager();
        job.addVariableDefinition("outputFile", outputFile.getPath());
        job.addInputFile(inputFile);
        job.setup(STEERING_RESOURCE);
        job.run();
        System.out.println("Done processing " + job.getLCSimLoop().getTotalCountableConsumed() + " events.");
                            
        Runtime runtime = Runtime.getRuntime();
        int mb = 1024 * 1024; 
        System.out.printf("total memory: %d mb\n", runtime.totalMemory() / mb); 
        System.out.printf("free memory: %d mb\n", runtime.freeMemory() / mb);
        System.out.printf("max memory: %d mb\n", runtime.maxMemory() / mb);
        System.out.printf("used memory: %d mb\n", (runtime.totalMemory() - runtime.freeMemory()) / mb);
    }
}
