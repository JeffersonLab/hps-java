package org.hps.util.test;

import java.io.File;
import java.net.URL;

import org.lcsim.util.cache.FileCache;

/**
 * Static utilities for accessing test files at JLAB, such as EVIO and LCIO input data
 * and reference AIDA plots
 *
 * Additional test utility methods may be added here, as needed.
 */
public final class TestUtil {

    /**
     * Base URL for all test files
     */
    private static final String JLAB_BASE = "https://hpsweb.jlab.org/test/hps-java";

    /**
     * Base URL for test data files such as EVIO or LCIO input data
     */
    private static final String JLAB_DATA = JLAB_BASE + "/data";

    /**
     * Base URL for reference plots
     */
    private static final String JLAB_REF = JLAB_BASE + "/referencePlots";

    /**
     * Download a test data file by name
     * @param fileName The name of the file, relative to the test data directory
     * @return The test data file
     * @throws RuntimeException If there is an error downloading the file or it does not exist
     */
    public static File downloadTestFile(String fileName) {
        try {
            URL url = new URL(JLAB_DATA + "/" + fileName);
            FileCache cache = new FileCache();
            cache.setPrintStream(null);
            File file = cache.getCachedFile(url);
            return file;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Download reference AIDA plots by name
     * @param name The name of the IT
     * @return The AIDA reference file
     * @throws RuntimeException If there is an error downloading the plots or the file does not exist
     */
    public static File downloadRefPlots(String name) {
        try {
            URL url = new URL(JLAB_REF + "/" + name + "/" + name + "-ref.aida");
            FileCache cache = new FileCache();
            cache.setPrintStream(null);
            File file = cache.getCachedFile(url);
            return file;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Private constructor to disallow class instantiation
     */
    private TestUtil() {
    }
}
