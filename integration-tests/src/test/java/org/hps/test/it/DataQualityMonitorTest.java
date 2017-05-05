package org.hps.test.it;

import java.io.File;

import junit.framework.TestCase;

import org.hps.data.test.TestDataUtility;
import org.hps.job.JobManager;

/**
 * Test package for data quality monitoring of reconstructed data.
 */
public class DataQualityMonitorTest extends TestCase {

    private static final String CLASS_NAME = DataQualityMonitorTest.class.getSimpleName();
    private static final File OUTPUT_DIR = new File("./target/test-output/" + CLASS_NAME);
    private static final File OUTPUT_FILE = new File(OUTPUT_DIR.getAbsolutePath() + File.separator + CLASS_NAME + ".aida");
    private static final String STEERING_RESOURCE = "/org/hps/steering/test/DataQualityTest.lcsim";

    public void setUp() {
        
        // Delete files if they already exist.     
        if (OUTPUT_FILE.exists())
            OUTPUT_FILE.delete();

        // Create output dir.
        OUTPUT_DIR.mkdirs();
        if (!OUTPUT_DIR.exists())
            throw new RuntimeException("Failed to create test output dir.");
    }

    public void testQualityMonitor() {
        File dataFile = new TestDataUtility().getTestData("DataQualityMonitorTest.slcio");
        System.out.println("running data quality job with steering resource " + STEERING_RESOURCE + " ...");
        JobManager jobManager = new JobManager();
        jobManager.addVariableDefinition("outputFile", OUTPUT_FILE.getPath());
        jobManager.addInputFile(dataFile);
        jobManager.setup(STEERING_RESOURCE);
        jobManager.run();
        System.out.println("Done!");
    }
}
