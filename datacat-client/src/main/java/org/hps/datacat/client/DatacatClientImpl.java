package org.hps.datacat.client;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONObject;
import org.lcsim.util.log.LogUtil;
import org.lcsim.util.log.MessageOnlyLogFormatter;

/**
 * Implementation of {@link DatacatClient} interface for working with SRS datacat REST API.
 * 
 * @author Jeremy McCormick, SLAC
 */
final class DatacatClientImpl implements DatacatClient {
    
    /**
     * Setup class logging.
     */
    private static Logger LOGGER = LogUtil.create(DatacatClientImpl.class, new MessageOnlyLogFormatter(), Level.ALL);

    /**
     * The base URL of the datacat server.
     */
    private URL url;
    
    /**
     * The site (SLAC or JLAB).
     */
    private DatasetSite site;
    
    /**
     * The root directory (e.g. should be 'HPS').
     */
    private String rootDir;
    
    /**
     * Create client with default parameters.
     */
    DatacatClientImpl() {        
        this(DatacatConstants.BASE_URL, DatasetSite.SLAC, DatacatConstants.ROOT_DIR);
    }
    
    /**
     * Create client.
     * 
     * @param baseUrl
     * @param site
     * @param rootDir
     */
    DatacatClientImpl(String url, DatasetSite site, String rootDir) {
        try {
            this.url = new URL(url);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("The URL is bad.", e);
        }
        if (site == null) {
            throw new IllegalArgumentException("The site argument is null.");
        }
        this.site = site;
        if (rootDir == null) {
            throw new IllegalArgumentException("The root dir argument is null.");
        }
        this.rootDir = rootDir;
        LOGGER.config("url: "  + url);
        LOGGER.config("site: " + site);
        LOGGER.config("rootDir: " + rootDir);
    }
    
    /**
     * Remove a folder from the catalog.
     * <p>
     * It must be empty or an error will occur. 
     * 
     * @param folder the folder path
     * @return the HHTP status code from the request
     */
    @Override
    public int removeFolder(String folder) {
        String fullUrl = url.toString() + "/r/folders.json/" + this.rootDir + folder;
        LOGGER.info("removing folder: " + fullUrl);
        return HttpUtilities.doDelete(fullUrl);
    }

    /**
     * Delete a dataset from the catalog.
     * <p>
     * This has no affect on the underlying resource (file).
     * 
     * @param path the path of the dataset
     * @return the HTTP status code from the reqest
     */
    @Override
    public int deleteDataset(String path) {
        String urlLocation = url.toString() + "/r/datasets.json/" + this.rootDir + path;
        LOGGER.info("deleting dataset: " + urlLocation);
        return HttpUtilities.doDelete(urlLocation);
    }
     
    /**
     * Create a folder in the data catalog.
     * 
     * @param folder the folder's path
     * @return the HTTP status code from the request
     */
    @Override
    public int makeFolder(String path) {
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("path", "/" + DatacatConstants.ROOT_DIR + "/" + path);
        String name = new File(path).getName();       
        parameters.put("name", name);
        parameters.put("_type", "folder");
        JSONObject object = JSONUtilities.createJSONFromMap(parameters);        
        String urlLocation = url + "/r/folders.json/" + this.rootDir;
        LOGGER.info("making folder: " + urlLocation);
        LOGGER.info("folder JSON: " + object.toString());
        return HttpUtilities.doPost(urlLocation, object.toString());
    }
    
    /**
     * Add metadata to an existing dataset.
     * 
     * @param folder the folder
     * @param datasetName the name of the dataset
     * @param metaData the map of metadata where values can be <code>String</code>, <code>Integer</code> or <code>Float</code>
     * @return the HTTP status code from the request
     */
    @Override
    public int addMetadata(String folder, String name, Map<String, Object> metaData) {
        JSONObject object = new JSONObject();
        object.put("versionMetadata", JSONUtilities.createJSONMetadataArray(metaData));
        String patchUrl = this.url.toString() + "/r/datasets.json/" + this.rootDir + "/" + folder + "/" + name + ";v=current;s=" + this.site;
        LOGGER.info("addMetadata: " + patchUrl);
        return HttpUtilities.doPatch(patchUrl, object.toString());
    }      

    /**
     * Find datasets in the catalog.
     * <p>
     * See <a href="http://docs.datacatalog.apiary.io/#search">Search Doc</a> for more details.
     * 
     * @param folder the folder path
     * @param query the query to execute
     * @return the HTTP status code from the request
     */
    @Override
    public List<Dataset> findDatasets(String directory, String query, Set<String> showMetadata) {
        
        String urlLocation = this.url.toString() + "/r/search.json/" + this.rootDir + "/";
        if (directory != null) {
            urlLocation += directory;
        }
        urlLocation += ";s=" + this.site.name() + "?";        
        if (query != null) {
            String encoded = null;        
            try {
                encoded = URLEncoder.encode(query, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
            urlLocation += "filter=" + encoded;
        }
        if (showMetadata != null) {
            for (String metadataField : showMetadata) {
                urlLocation += "&show=" + metadataField;
            }
        }
        LOGGER.info("findDatasets: " + urlLocation);
        StringBuffer outputBuffer = new StringBuffer();
        int response = HttpUtilities.doGet(urlLocation, outputBuffer);
        if (response >= 400) {
            throw new RuntimeException("HTTP GET failed with code: " + response);
        }
        
        // Build and return dataset list
        JSONObject searchResults = new JSONObject(outputBuffer.toString());
        LOGGER.info("returning search results: " + searchResults.toString());
        return DatasetUtilities.getDatasetsFromSearch(searchResults);
    }    

    /**
     * Add a dataset to the data catalog.
     * 
     * @param folder the folder which must already exist
     * @param dataType the data type
     * @param resource the resource (path)
     * @param site the site of the file 
     * @param fileFormat the file format
     * @param name the name of the dataset
     * @param metadata metadata to assign to the dataset
     * @return the HTTP status code from the request
     */
    @Override
    public int addDataset(String folder, DatasetDataType dataType, String resource, DatasetSite site, 
            DatasetFileFormat fileFormat, String name, Map<String, Object> metadata) {
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("dataType", dataType.toString());
        parameters.put("resource", resource);
        parameters.put("site", DatasetSite.SLAC.name());
        parameters.put("fileFormat", fileFormat.toString());        
        parameters.put("name", name);
        JSONObject jsonDataset = JSONUtilities.createJSONDataset(parameters, metadata);
        String urlLocation = url + "/r/datasets.json/" + this.rootDir + "/" + folder;
        LOGGER.info("addDataset: " + urlLocation);
        LOGGER.info("dataset JSON: " + jsonDataset.toString());
        return HttpUtilities.doPost(urlLocation, jsonDataset.toString());
    }
}
