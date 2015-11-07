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
     * @param the input file for extracting metadata 
     * @return the metadata map
     * @throws IOException if there is an error reading the file
     */
    public Map<String, Object> getMetadata(File file) throws IOException;
}
