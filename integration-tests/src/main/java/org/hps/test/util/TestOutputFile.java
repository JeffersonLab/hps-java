package org.hps.test.util;

import java.io.File;

import junit.framework.TestCase;

/**
 * Convenience class for test file output (basically copied from lcsim.org).
 * 
 * @author Jeremy McCormick, SLAC
 */
public class TestOutputFile extends File {
    
    /**
     * Root output area in target dir.
     */
    private static String TEST_OUTPUT_DIR = "target/test-output/";   
    
    /**
     * Create output file in target dir for a specific test case.
     * 
     * @param testClass the TestCase class
     * @param filename the file name (should not use a directory)
     */
    public TestOutputFile(Class<? extends TestCase> testClass, String filename) {
        super(TEST_OUTPUT_DIR + File.separator + testClass.getSimpleName() + File.separator + filename);
        File dir = this.getParentFile();
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }
}
