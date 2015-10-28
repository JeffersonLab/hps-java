package org.hps.datacat.client;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Implementation of {@link DatacatClient} interface for working with SRS datacat REST API.
 *
 * @author Jeremy McCormick, SLAC
 */
final class DatacatClientImpl implements DatacatClient {

    /**
     * Setup class logging.
     */
    private static Logger LOGGER = Logger.getLogger(DatacatClientImpl.class.getPackage().getName());

    /**
     * The root directory (e.g. should be 'HPS').
     */
    private final String rootDir;

    /**
     * The site (SLAC or JLAB).
     */
    private final DatasetSite site;

    /**
     * The base URL of the datacat server.
     */
    private URL url;

    /**
     * Create client with default parameters.
     */
    DatacatClientImpl() {
        this(DatacatConstants.BASE_URL, DatasetSite.SLAC, DatacatConstants.ROOT_DIR);
    }

    /**
     * Create client.
     *
     * @param baseUrl the base URL of the data catalog application
     * @param site the default site where physical files are located
     * @param rootDir the root directory in the data catalog
     */
    DatacatClientImpl(final String url, final DatasetSite site, final String rootDir) {
        try {
            this.url = new URL(url);
        } catch (final MalformedURLException e) {
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
        LOGGER.config("url: " + url + "; site: " + site + "; rootDir: " + rootDir);
    }

    /**
     * Add a dataset to the data catalog.
     *
     * @param folder the logical folder in the datacat, which must already exist
     * @param dataType the data type
     * @param resource the resource (path on the file system)
     * @param size the size of the file in bytes
     * @param site the site of the file
     * @param fileFormat the file format
     * @param name the name of the dataset
     * @param metadata metadata to assign to the dataset
     * @return the HTTP status code from the request
     */
    @Override
    public int addDataset(
            final String folder, 
            final DatasetDataType dataType, 
            final String resource,
            final long size, 
            final DatasetSite site, 
            final DatasetFileFormat fileFormat, 
            final String name,
            final Map<String, Object> metadata) {
        final Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("dataType", dataType.toString());
        parameters.put("resource", resource);
        parameters.put("site", DatasetSite.SLAC.name());
        parameters.put("fileFormat", fileFormat.toString());
        parameters.put("name", name);
        parameters.put("size", size);
        final JSONObject jsonDataset = JSONUtilities.createJSONDataset(parameters, metadata);
        final String urlLocation = url + "/datasets.json/" + this.rootDir + "/" + folder;
        LOGGER.info("addDataset: " + urlLocation);
        LOGGER.info("dataset JSON: " + jsonDataset.toString());
        return HttpUtilities.doPost(urlLocation, jsonDataset.toString());
    }

    /**
     * Add metadata to an existing dataset.
     *
     * @param folder the folder
     * @param datasetName the name of the dataset
     * @param metaData the map of metadata where values can be <code>String</code>, <code>Integer</code> or
     *            <code>Float</code>
     * @return the HTTP status code from the request
     */
    @Override
    public int addMetadata(final String folder, final String name, final Map<String, Object> metaData) {
        final JSONObject object = new JSONObject();
        object.put("versionMetadata", JSONUtilities.createJSONMetadataArray(metaData));
        final String patchUrl = this.url.toString() + "/datasets.json/" + this.rootDir + "/" + folder + "/" + name
                + ";v=current;s=" + this.site;
        LOGGER.info("addMetadata: " + patchUrl);
        return HttpUtilities.doPatch(patchUrl, object.toString());
    }

    /**
     * Delete a dataset from the catalog.
     * <p>
     * This has no affect on the underlying resource (file on disk).
     *
     * @param path the path of the dataset
     * @return the HTTP status code from the reqest
     */
    @Override
    public int deleteDataset(final String path) {
        final String urlLocation = url.toString() + "/datasets.json/" + this.rootDir + path;
        LOGGER.info("deleting dataset: " + urlLocation);
        return HttpUtilities.doDelete(urlLocation);
    }

    /**
     * Return <code>true</code> if the path exists.
     *
     * @param path the path in the data catalog
     */
    @Override
    public boolean exists(final String path) {
        if (path == null) {
            throw new IllegalArgumentException("The path is null.");
        }
        if (path.length() == 0) {
            throw new IllegalArgumentException("The path is a blank string.");
        }
        final String urlLocation = this.url + "/path.json/" + this.rootDir + "/" + path;
        final StringBuffer output = new StringBuffer();
        final int status = HttpUtilities.doGet(urlLocation, output);
        if (status > 400) {
            throw new RuntimeException("HTTP GET returned error status: " + status);
        }
        final JSONObject jsonObject = new JSONObject(output.toString());
        return jsonObject.has("_type");
    }

    /**
     * Find datasets in the catalog.
     * <p>
     * See <a href="http://docs.datacatalog.apiary.io/#search">Search Doc</a> for more details
     * on search syntax.
     *
     * @param folder the folder path
     * @param query the query statement to execute
     * @return the HTTP status code from the request
     */
    @Override
    public List<Dataset> findDatasets(final String directory, final String query, final Set<String> showMetadata) {

        String urlLocation = this.url.toString() + "/search.json/" + this.rootDir + "/";
        if (directory != null) {
            urlLocation += directory;
        }
        urlLocation += ";s=" + this.site.name() + "?";
        
        // Encode query string so it will be a valid URL.
        if (query != null) {
            String encoded = null;
            try {
                encoded = URLEncoder.encode(query, "UTF-8");
            } catch (final UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
            urlLocation += "filter=" + encoded;
        }
        
        // Metadata fields to show.
        if (showMetadata != null) {
            for (final String metadataField : showMetadata) {
                urlLocation += "&show=" + metadataField;
            }
        }
        
        LOGGER.info("findDatasets: " + urlLocation);
        final StringBuffer outputBuffer = new StringBuffer();
        final int response = HttpUtilities.doGet(urlLocation, outputBuffer);
        if (response >= 400) {
            throw new RuntimeException("HTTP GET returned error code: " + response);
        }

        // Build and return dataset list
        final JSONObject searchResults = new JSONObject(outputBuffer.toString());
        LOGGER.info("returning search results: " + searchResults.toString());
        return createDatasetsFromSearch(searchResults);
    }

    /**
     * Get a dataset from its path.
     *
     * @param path the path in the data catalog
     * @return the dataset
     */
    @Override
    public Dataset getDataSet(final String path, final DatasetSite site) {
        if (path == null) {
            throw new IllegalArgumentException("The path is null.");
        }
        if (path.length() == 0) {
            throw new IllegalArgumentException("The path is a blank string.");
        }
        if (site == null) {
            throw new IllegalArgumentException("The site is null.");
        }
        String urlLocation = this.url + "/path.json/" + this.rootDir;
        if (!path.startsWith("/")) {
            urlLocation += "/";
        }
        urlLocation += path + ";s=" + site.name();
        final StringBuffer output = new StringBuffer();
        HttpUtilities.doGet(urlLocation, output);
        return new DatasetImpl(new JSONObject(output.toString()));
    }

    /**
     * Return <code>true</code> if path is a folder in the data catalog.
     *
     * @return <code>true</code> if path is a folder in the data catalog
     */
    @Override
    public boolean isFolder(final String path) {
        if (path == null) {
            throw new IllegalArgumentException("The path is null.");
        }
        if (path.length() == 0) {
            throw new IllegalArgumentException("The path is a blank string.");
        }
        final String urlLocation = this.url + "/path.json/" + this.rootDir + "/" + path;
        final StringBuffer output = new StringBuffer();
        final int status = HttpUtilities.doGet(urlLocation, output);
        if (status > 400) {
            throw new RuntimeException("HTTP GET query returned error status: " + status);
        }
        final JSONObject jsonObject = new JSONObject(output.toString());
        return jsonObject.has("_type") ? "folder".equals(jsonObject.getString("_type")) : false;
    }

    /**
     * Create a folder in the data catalog.
     *
     * @param folder the folder's path
     * @return the HTTP status code from the request
     */
    @Override
    public int makeFolder(final String path) {
        final Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("path", "/" + DatacatConstants.ROOT_DIR + "/" + path);
        final String name = new File(path).getName();
        parameters.put("name", name);
        parameters.put("_type", "folder");
        final JSONObject object = JSONUtilities.createJSONFromMap(parameters);
        final String urlLocation = url + "/folders.json/" + this.rootDir;
        LOGGER.info("making folder: " + urlLocation);
        LOGGER.info("folder JSON: " + object.toString());
        return HttpUtilities.doPost(urlLocation, object.toString());
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
    public int removeFolder(final String folder) {
        final String fullUrl = url.toString() + "/folders.json/" + this.rootDir + folder;
        LOGGER.info("removing folder: " + fullUrl);
        return HttpUtilities.doDelete(fullUrl);
    }
    
    /**
     * Create {@link Dataset} objects from JSON search results.
     * 
     * @param searchResults the JSON search results
     * @return the list of {@link Dataset} objects
     */
    static List<Dataset> createDatasetsFromSearch(JSONObject searchResults) {
        List<Dataset> datasets = new ArrayList<Dataset>();
        JSONArray resultsArray = searchResults.getJSONArray("results");
        for (int i = 0; i < resultsArray.length(); i++) {
            JSONObject jsonObject = resultsArray.getJSONObject(i);
            System.out.println("result[" + i + "]: " + jsonObject.toString());
            datasets.add(new DatasetImpl(resultsArray.getJSONObject(i)));
        }
        return datasets;
    }
}
