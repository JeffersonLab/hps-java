package org.hps.datacat.client;

import java.util.List;
import java.util.Map;

/**
 * Interface to the SLAC SRS datacat system via HTTP/REST calls.
 * 
 * @author Jeremy McCormick, SLAC
 */
public interface DatacatClient {

    /**
     * Add a dataset to the data catalog.
     * 
     * @param folder the folder which must already exist
     * @param dataType the data type
     * @param resource the resource (path)
     * @param site the site of the file 
     * @param fileFormat the file format
     * @param name the name of the dataset
     * @return the HTTP status code from the request
     */
    int addDataset(String folder, DatasetDataType dataType, String resource, DatasetSite site, DatasetFileFormat fileFormat, String name);
            
    /**
     * Create a folder in the data catalog.
     * 
     * @param folder the folder's path
     * @return the HTTP status code from the request
     */
    int makeFolder(String folder);
    
    /**
     * Add metadata to an existing dataset.
     * 
     * @param folder the folder
     * @param datasetName the name of the dataset
     * @param metaData the map of metadata where values can be <code>String</code>, <code>Integer</code> or <code>Float</code>
     * @return the HTTP status code from the request
     */
    int addMetadata(String folder, String datasetName, Map<String, Object> metaData);
    
    /**
     * Remove a folder from the catalog.
     * <p>
     * It must be empty or an error will occur. 
     * 
     * @param folder the folder path
     * @return the HHTP status code from the request
     */
    int removeFolder(String folder);
    
    /**
     * Delete a dataset from the catalog.
     * <p>
     * This has no affect on the underlying resource (file).
     * 
     * @param path the path of the dataset
     * @return the HTTP status code from the reqest
     */
    int deleteDataset(String path);
            
    /**
     * Find datasets in the catalog.
     * <p>
     * See <a href="http://docs.datacatalog.apiary.io/#search">Search Doc</a> for more details.
     * 
     * @param folder the folder path
     * @param query the query to execute
     * @return the HTTP status code from the request
     */
    List<Dataset> findDatasets(String folder, String query);
    
    // TODO: method to get dataset from path
    // to get all metadata need site
    // http://localhost:8080/datacat-v0.4-SNAPSHOT/r/path.json/HPS/derp/herp01;s=SLAC 
    // use HTTP GET 
    // Dataset getDataSet(String path);
    
    // TODO: method to determine if folder or dataset exists
    // http://localhost:8080/datacat-v0.4-SNAPSHOT/r/path.json/HPS/derp/derp
    // will return 
    // {"message":"File doesn't exist","type":"NoSuchFileException","cause":"Unable to resolve /HPS/derp/derp in parent Name: derp\tPath: /HPS/derp\t"}
    // boolean exists(String path);
}
