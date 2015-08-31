package org.hps.datacat.client;

import java.util.Date;
import java.util.List;

/**
 * 
 * @author Jeremy McCormick, SLAC
 *
 */
public interface Dataset {
    
    String getName();
    
    String getPath();
    
    List<DatasetLocation> getLocations();
        
    DatasetFileFormat getFileFormat();
    
    DatasetDataType getDataType();
    
    Date getCreated();
    
    //DatasetMetadata getMetadata();
}
