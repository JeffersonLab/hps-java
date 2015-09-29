package org.hps.crawler;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * This is a metadata reader for ROOT DQM files.
 * <p>
 * It currently only gets the run number from the file name.
 *
 * @author Jeremy McCormick, SLAC
 */
public class AidaMetadataReader implements FileMetadataReader {

    /**
     * Get the metadata for a ROOT DQM file.
     *
     * @return the metadata for a ROOT DQM file
     */
    @Override
    public Map<String, Object> getMetadata(final File file) throws IOException {
        final Map<String, Object> metadata = new HashMap<String, Object>();
        final int run = CrawlerFileUtilities.getRunFromFileName(file);
        metadata.put("runMin", run);
        metadata.put("runMax", run);
        return metadata;
    }
}
