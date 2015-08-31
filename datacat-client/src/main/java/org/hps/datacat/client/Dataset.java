package org.hps.datacat.client;

import java.util.Date;
import java.util.List;

/**
 * 
 * @author Jeremy McCormick, SLAC
 *
 */
public interface Dataset {
    
    /**
     * Get the name of the dataset without the path component e.g. "dataset01".
     * 
     * @return the name of the dataset
     */
    String getName();
    
    /**
     * Get the path of the dataset e.g. "/HPS/folder/dataset01".
     * 
     * @return the path of the dataset
     */
    String getPath();
    
    /**
     * Get the dataset locations.
     * 
     * @return the dataset locations
     */
    List<DatasetLocation> getLocations();
        
    /**
     * Get the file format e.g. EVIO, LCIO, etc.
     * 
     * @return the dataset file format
     */
    DatasetFileFormat getFileFormat();
    
    /**
     * Get the data type e.g. RAW, RECON, etc.
     * 
     * @return the data type
     */
    DatasetDataType getDataType();
    
    /**
     * Get the creation date.
     * 
     * @return the creation date
     */
    Date getCreated();
    
    /**
     * Get the dataset's metadata.
     * 
     * @return the dataset's metadata
     */
    DatasetMetadata getMetadata();
}
