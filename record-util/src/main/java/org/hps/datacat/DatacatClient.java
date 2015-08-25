package org.hps.datacat;

import java.net.URL;
import java.util.Map;

/**
 * Interface to the SLAC SRS datacat system via HTTP/REST calls.
 * 
 * @author Jeremy McCormick, SLAC
 */
public interface DatacatClient {
    
    int addDataset(String folder, Map<String, Object> parameters);
    
    int makeFolder(String folder);
    
    int patchDataset(String folder, String datasetName, Map<String, Object> metaData);
    
    int removeFolder(String folder);
    
    int deleteDataset(String path);
    
    // TODO: get full info on dataset
    
    // TODO: query for datasets on meta data or other info
    
    String getRootDir();
    
    URL getBaseUrl();
    
    String getSite();
}
