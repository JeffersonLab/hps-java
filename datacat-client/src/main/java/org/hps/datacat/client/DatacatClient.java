package org.hps.datacat.client;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Interface to the SLAC SRS datacat system via HTTP/REST calls.
 *
 * @author Jeremy McCormick, SLAC
 */
// TODO: add method for adding a location to an existing dataset
public interface DatacatClient {

    /**
     * Add a dataset to the data catalog.
     *
     * @param folder the folder which must already exist
     * @param dataType the data type
     * @param resource the resource (path)
     * @param size the size of the file in bytes
     * @param site the site of the file
     * @param fileFormat the file format
     * @param name the name of the dataset
     * @param metadata metadata to assign to the dataset
     * @return the HTTP status code from the request
     */
    public int addDataset(final String folder, final DatasetDataType dataType, final String resource,
            final long size, final DatasetSite site, final DatasetFileFormat fileFormat, final String name,
            final Map<String, Object> metadata);

    /**
     * Add metadata to an existing dataset.
     *
     * @param folder the folder
     * @param datasetName the name of the dataset
     * @param metaData the map of metadata where values can be <code>String</code>, <code>Integer</code> or
     *            <code>Float</code>
     * @return the HTTP status code from the request
     */
    int addMetadata(String folder, String datasetName, Map<String, Object> metaData);

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
     * Return <code>true</code> if the path exists in the datacat.
     * 
     * @param path the path in the datacat
     * @return <code>true</code> if the path exists
     */
    boolean exists(String path);

    /**
     * Find datasets in the catalog.
     * <p>
     * See <a href="http://docs.datacatalog.apiary.io/#search">Search Doc</a> for more details.
     *
     * @param folder the folder path
     * @param query the query to execute
     * @return the HTTP status code from the request
     */
    List<Dataset> findDatasets(String folder, String query, Set<String> showMetadata);

    /**
     * Get a dataset from its path.
     * <p>
     * Example URL:
     * 
     * <pre>
     * http://localhost:8080/datacat-v0.4-SNAPSHOT/r/path.json/HPS/data/hps_005772.evio.0;s=SLAC
     * </pre>
     *
     * @param path the path in the data catalog
     * @return the dataset
     */
    Dataset getDataSet(String path, DatasetSite site);

    /**
     * Return <code>true</code> if path is a folder in the data catalog.
     *
     * @return <code>true</code> if path is a folder in the data catalog
     */
    boolean isFolder(String path);

    /**
     * Create a folder in the data catalog.
     *
     * @param folder the folder's path
     * @return the HTTP status code from the request
     */
    int makeFolder(String folder);

    /**
     * Remove a folder from the catalog.
     * <p>
     * It must be empty or an error will occur.
     *
     * @param folder the folder path
     * @return the HHTP status code from the request
     */
    int removeFolder(String folder);
}
