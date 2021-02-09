package org.hps.util.test;

import java.io.File;
import java.net.URL;

import org.lcsim.util.cache.FileCache;

/**
 * Simple utilities for unit and integration tests
 */
public final class TestUtil {

    /**
     * Base URL for all test files
     */
    static final String JLAB_BASE = "https://hpsweb.jlab.org/test/hps-java";

    /**
     * Base URL for test data files
     */
    static final String JLAB_DATA = JLAB_BASE + "/data";

    /**
     * Base URL for reference plots
     */
    static final String JLAB_REF = JLAB_BASE + "/referencePlots";

    public static File downloadTestFile(String relPath) {
        return downloadTestFile(relPath, JLAB_DATA);
    }

    private static File downloadTestFile(String relPath, String baseUrl) {
        try {
            URL url = new URL(baseUrl + "/" + relPath);
            FileCache cache = new FileCache();
            File file = cache.getCachedFile(url);
            System.out.println("Cached test file: " + file.getPath());
            return file;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static File downloadRefPlot(String path) {
        try {
            URL url = new URL(JLAB_REF + "/" + path);
            FileCache cache = new FileCache();
            File file = cache.getCachedFile(url);
            System.out.println("Cached ref plot: " + file.getPath());
            return file;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String getTestOutputDir() {
        return "target/test-output";
    }

    private TestUtil() {
    }
}
