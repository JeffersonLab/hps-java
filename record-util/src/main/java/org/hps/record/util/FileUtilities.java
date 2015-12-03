package org.hps.record.util;

import java.io.File;

/**
 * File utilities for the datacat crawler.
 *
 * @author Jeremy McCormick, SLAC
 */
public final class FileUtilities {
    
    /**
     * Get run number from file name assuming it looks like "hps_001234".
     *
     * @param file the file
     * @return the run number
     */
    public static int getRunFromFileName(final File file) {
        final String name = file.getName();
        return Integer.parseInt(name.substring(4, 10));
    }
    
    /**
     * Get a cached file path, assuming that the input file path is on the JLAB MSS e.g. it starts with "/mss".
     * If the file is not on the JLAB MSS an error will be thrown.
     * <p>
     * If the file is already on the cache disk just return the same file.
     *
     * @param mssFile the MSS file path
     * @return the cached file path (prepends "/cache" to the path)
     * @throws IllegalArgumentException if the file is not on the MSS (e.g. path does not start with "/mss")
     */
    public static File getCachedFile(final File mssFile) {
        if (!isMssFile(mssFile)) {
            throw new IllegalArgumentException("File " + mssFile.getPath() + " is not on the JLab MSS.");
        }
        File cacheFile = mssFile;
        if (!isCachedFile(mssFile)) {
            cacheFile = new File("/cache" + mssFile.getAbsolutePath());
        }
        return cacheFile;
    }
    
    /**
     * Return <code>true</code> if this is a file on the cache disk e.g. the path starts with "/cache".
     *
     * @param file the file
     * @return <code>true</code> if the file is a cached file
     */
    public static boolean isCachedFile(final File file) {
        return file.getPath().startsWith("/cache");
    }
    
    /**
     * Return <code>true</code> if this file is on the JLAB MSS e.g. the path starts with "/mss".
     *
     * @param file the file
     * @return <code>true</code> if the file is on the MSS
     */
    public static boolean isMssFile(final File file) {
        return file.getPath().startsWith("/mss");
    }
    
    private FileUtilities() {
    }
}
