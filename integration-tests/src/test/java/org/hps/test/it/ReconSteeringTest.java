package org.hps.test.it;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import junit.framework.TestCase;

import org.hps.data.test.TestDataUtility;
import org.hps.job.JobManager;
import org.lcsim.util.cache.FileCache;
import org.lcsim.util.test.TestUtil.TestOutputFile;

/**
 * Test that production MC recon steering files are not broken by running an LCSim job on them
 * using an LCIO file.
 * 
 * @author Jeremy McCormick, SLAC
 */
public class ReconSteeringTest extends TestCase {
    
    /**
     * List of steering files to run.
     */
    final static String[] STEERING_FILES = {
        "EngineeringRun2014EcalRecon_Pass1.lcsim",
        "EngineeringRun2014EcalRecon.lcsim",
        "EngineeringRun2015EcalRecon.lcsim",
        "EngineeringRun2015FullRecon.lcsim",
        "EngineeringRun2015FullRecon_Pass2.lcsim",
        "EngineeringRun2015HitRecon.lcsim",
        "HPSTrackingDefaultsRecon.lcsim"
    };
            
    /**
     * Test recon steering files.
     * @throws Exception if any error occurs running the recon job
     */
    public void testSteeringFiles() {
        
        File inputFile = new TestDataUtility().getTestData("tritrigv1-egsv3-triv2-g4v1_s2d6_HPS-EngRun2015-Nominal-v3_3.4.0_pairs1_1.slcio");
        
        for (String steeringFile : STEERING_FILES) {
            
            // Run the reconstruction steering file.
            File outputFile = null;
            try {
                outputFile = new TestOutputFile(new File(steeringFile).getName().replace(".lcsim", ""));
                runSteering("/org/hps/steering/recon/" + steeringFile, inputFile, outputFile);
            } catch (Throwable e) {
                System.err.println("Job with steering " + steeringFile + " failed!");
                throw new RuntimeException("Recon job failed.", e);
            }
            
            Runtime runtime = Runtime.getRuntime();
            
            int mb = 1024 * 1024;
            
            System.out.println("total memory: " + runtime.totalMemory() / mb);
            System.out.println("free memory: " + runtime.freeMemory() / mb);
            System.out.println("max memory: " + runtime.maxMemory() / mb);
            System.out.println("used memory: " + (runtime.totalMemory() - runtime.freeMemory()) / mb);
            
            System.gc();
            
            // Create DQM output for QA.
            try {
                runDQM(outputFile);
            } catch (Throwable e) {
                throw new RuntimeException("The DQM job failed.", e);
            }
        }
    }
       
    private void runSteering(String steeringFile, File inputFile, File outputFile) {
        System.out.println("Testing steering file " + steeringFile + " ...");
        JobManager job = new JobManager();
        job.addVariableDefinition("outputFile", outputFile.getPath());
        job.addVariableDefinition("detector", "HPS-EngRun2015-Nominal-v3");
        job.addVariableDefinition("run", "5772");
        job.addVariableDefinition("isMC", "true");
        job.addInputFile(inputFile);
        job.setup(steeringFile);
        job.run();
        System.out.println("Job with steering " + steeringFile + " successfully processed " + job.getLCSimLoop().getTotalCountableConsumed() + " events.");
    }
    
    private void runDQM(File outputFile) {
        System.out.println("Running DQM on " + outputFile.getPath() + " ...");
        JobManager job = new JobManager();
        File inputFile = new File(outputFile.getPath() + ".slcio");
        job.addInputFile(inputFile);
        job.addVariableDefinition("outputFile", outputFile.getPath().replace(".slcio", ""));
        job.setup("/org/hps/steering/production/DataQualityRecon.lcsim");
        job.run();
        System.out.println("DQM processed " + job.getLCSimLoop().getTotalCountableConsumed() + " events from " + outputFile + ".");
    }
}


