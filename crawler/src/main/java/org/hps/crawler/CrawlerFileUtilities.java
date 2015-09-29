package org.hps.crawler;

import java.io.File;

/**
 * File utilities for crawler.
 *
 * @author Jeremy McCormick, SLAC
 */
public class CrawlerFileUtilities {

    /**
     * Get run number from file name assuming it looks like "hps_001234".
     *
     * @param file the file
     * @return the run number
     */
    static int getRunFromFileName(final File file) {
        final String name = file.getName();
        return Integer.parseInt(name.substring(4, 10));
    }
}
