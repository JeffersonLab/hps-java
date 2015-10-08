package org.hps.test.it;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import junit.framework.TestCase;

import org.hps.job.JobManager;
import org.lcsim.util.aida.AIDA;
import org.lcsim.util.cache.FileCache;

/**
 * Test package for data quality monitoring of reconstructed data
 *
 * @author mgraham <mgraham@slac.stanford.edu>
 * created on 10/16/2014
 */
public class DataQualityMonitorTest extends TestCase {
    static final String fileLocation = "http://www.lcsim.org/test/hps-java/DataQualityMonitorTest.slcio";

//    static final String fileLocation = "file:///Users/mgraham/HPS/DataQualityMonitorTest.slcio";

    static final String className = DataQualityMonitorTest.class.getSimpleName();
    static final File outputDir = new File("./target/test-output/" + className);
    static final File outputFile = new File(outputDir.getAbsolutePath() + File.separator + className);
    static final File aidaFile = new File(outputFile.getAbsolutePath() + ".aida");
    static final String steeringResource = "/org/hps/steering/test/DataQualityTest.lcsim";
    AIDA aida = AIDA.defaultInstance();

    public void setUp() {
        
        System.out.println("Setting up DQM Test");
        // Delete files if they already exist.     
        if (aidaFile.exists())
            aidaFile.delete();

        // Create output dir.
        outputDir.mkdirs();
        if (!outputDir.exists())
            throw new RuntimeException("Failed to create test output dir.");
    }

    public void testQualityMonitor() {
        System.out.println("caching file ...");
        System.out.println(fileLocation);

        File dataFile = null;
        try {
            FileCache cache = new FileCache();
            dataFile = cache.getCachedFile(new URL(fileLocation));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }        
        System.out.println("running data quality job with steering resource " + steeringResource);
        JobManager jobManager = new JobManager();
        jobManager.addVariableDefinition("outputFile", outputFile.getPath());
        jobManager.addInputFile(dataFile);
        jobManager.setup(steeringResource);
        jobManager.run();

    }
}
