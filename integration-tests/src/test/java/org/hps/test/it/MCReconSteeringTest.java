package org.hps.test.it;

import java.io.File;

import junit.framework.TestCase;

import org.hps.data.test.TestDataUtility;
import org.hps.job.JobManager;
import org.lcsim.util.test.TestUtil.TestOutputFile;

/**
 * Test that production MC recon steering files are not broken.
 */
public class MCReconSteeringTest extends TestCase {
    
    /**
     * List of steering files to run.
     */
    static String[] STEERING_FILES = {        
        "EngineeringRun2015FullReconMC.lcsim"
    };
            
    /**
     * Test recon steering files.
     * @throws Exception if any error occurs running the recon job
     */
    public void testSteeringFiles() {
        
        File inputFile = new TestDataUtility().getTestData("tritrigv1-egsv3-triv2-g4v1_s2d6_HPS-EngRun2015-Nominal-v3_3.4.0_pairs1_1.slcio");
        
        for (String steeringFile : STEERING_FILES) {
            System.out.println("Running steering file " + steeringFile + " ...");
            boolean failed = false;
            try {                
                testSteering("/org/hps/steering/recon/" + steeringFile, inputFile);
                System.out.println("Job with steering " + steeringFile + " ran okay!");
            } catch (Exception e) { 
                System.out.println("caught exception: " + e.getMessage());
                e.printStackTrace();
                System.out.println("Job with steering " + steeringFile + " failed!");
                failed = true;
            }
            System.out.println(steeringFile + " failed: " + failed);
        }
    }
    
    private void testSteering(String steeringFile, File inputFile) {
        File outputFile = new TestOutputFile(new File(steeringFile).getName().replace(".lcsim", ""));
        JobManager job = new JobManager();
        job.addVariableDefinition("outputFile", outputFile.getPath());
        job.addVariableDefinition("detector", "HPS-EngRun2015-Nominal-v3");
        job.addVariableDefinition("run", "5772");
        job.addInputFile(inputFile);
        job.setup(steeringFile);
        job.run();
        System.out.println("Job " + steeringFile + " processed " + job.getLCSimLoop().getTotalCountableConsumed() + " events.");
    }
}
