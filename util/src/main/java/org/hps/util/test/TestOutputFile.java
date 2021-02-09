package org.hps.util.test;

import java.io.File;

/**
 * Convenience class for test file output (basically copied from lcsim.org).
 *
 * @author Jeremy McCormick, SLAC
 */
@SuppressWarnings("serial")
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
    public TestOutputFile(Class<?> testClass, String filename) {
        super(TEST_OUTPUT_DIR + File.separator + testClass.getSimpleName() + File.separator + filename);
        File dir = this.getParentFile();
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    public TestOutputFile(String filename) {
        super(TestUtil.getTestOutputDir() + File.separator + filename);
        File dir = new File(TestUtil.getTestOutputDir());
        if (!dir.exists())
            dir.mkdir();
    }
}
