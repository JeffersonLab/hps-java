package org.hps.crawler;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * Interface for reading metadata for the datacat from files.
 * 
 * @author Jeremy McCormick, SLAC
 */
public interface FileMetadataReader {   
    
    /**
     * Create a metadata map with keys and values from the contents of a file.
     * 
     * @param file the input file from which to extract metadata
     * @return the metadata map of field names to values
     * @throws IOException if there is an error reading the file
     */
    Map<String, Object> getMetadata(File file) throws IOException;
}
