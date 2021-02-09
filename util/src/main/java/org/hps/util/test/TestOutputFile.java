package org.hps.util.test;

import java.io.File;

/**
 * Convenience class for test file output
 *
 * @author Jeremy McCormick, SLAC
 */
@SuppressWarnings("serial")
public class TestOutputFile extends File {

    private static final String TEST_OUTPUT_DIR = "target/test-output";

    /**
     * Create an output file in a test-specific directory
     *
     * @param testClass the TestCase class
     * @param filename The name of the file
     */
    public TestOutputFile(Class<?> testClass, String filename) {
        super(TEST_OUTPUT_DIR + File.separator + testClass.getSimpleName() + File.separator + filename);
        File dir = getParentFile();
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    /**
     * Create an output file in the generic output directory
     * @param filename The name of the file
     */
    public TestOutputFile(String filename) {
        super(TEST_OUTPUT_DIR + File.separator + filename);
        File dir = getParentFile();
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }
}
