package org.hps.datacat;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;

/**
 * 
 * @author Jeremy McCormick, SLAC
 */
final class DatacatClientImpl implements DatacatClient {

    private URL url;
    private DatasetSite site;
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
        System.out.println("url: " + url);
        System.out.println("site: " + site);
        System.out.println("rootDir: " + rootDir);
    }
    
    @Override
    public int removeFolder(String folder) {
        String fullUrl = url.toString() + "/r/folders.json/" + this.rootDir + folder;
        return HttpUtilities.doDelete(fullUrl);
    }

    @Override
    public int deleteDataset(String path) {
        String fullUrl = url.toString() + "/r/datasets.json/" + this.rootDir + path;
        return HttpUtilities.doDelete(fullUrl);
    }
        
    @Override
    public int makeFolder(String path) {
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("path", "/" + DatacatConstants.ROOT_DIR + "/" + path);
        String name = new File(path).getName();       
        parameters.put("name", name);
        parameters.put("_type", "folder");
        JSONObject object = JSONUtilities.createJSONFromMap(parameters);
        return HttpUtilities.doPost(url + "/r/folders.json/" + this.rootDir, object.toString());
    }
    
    @Override
    public int addMetadata(String folder, String name, Map<String, Object> metaData) {
        JSONObject object = JSONUtilities.createJSONMetaData(metaData);
        String patchUrl = this.url.toString() + "/r/datasets.json/" + this.rootDir + "/" + folder + "/" + name + ";v=current;s=" + this.site;
                
        return HttpUtilities.doPatch(patchUrl, object.toString());
    }      

    // example
    // http://localhost:8080/datacat-v0.4-SNAPSHOT/r/search.json/HPS/derp?filter=run+%3E+1000
    @Override
    public List<Dataset> findDatasets(String directory, String query) {
        
        String fullUrl = this.url.toString() + "/r/search.json/" + this.rootDir + "/";
        if (directory != null) {
            fullUrl += directory;
        }
        fullUrl += ";s=" + this.site.name();
        if (query != null) {
            String encoded = null;        
            try {
                encoded = URLEncoder.encode(query, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
            fullUrl += "?filter=" + encoded;
        }
        System.out.println("query: " + fullUrl);
        StringBuffer outputBuffer = new StringBuffer();
        int response = HttpUtilities.doGet(fullUrl, outputBuffer);
        System.out.println("response: " + response);
        System.out.println("output: " + outputBuffer.toString());
        
        // Build and return dataset list
        JSONObject searchResults = new JSONObject(outputBuffer.toString());
        return DatasetUtilities.getDatasetsFromSearch(searchResults);
    }    
    
    @Override
    public int addDataset(String folder, DatasetDataType dataType, String resource, DatasetSite site, DatasetFileFormat fileFormat, String name) {
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("dataType", dataType.toString());
        parameters.put("resource", resource);
        parameters.put("site", DatasetSite.SLAC.name());
        parameters.put("fileFormat", fileFormat.toString());        
        parameters.put("name", name);           
        JSONObject dataset = JSONUtilities.createJSONDataset(parameters);
        return HttpUtilities.doPost(url + "/r/datasets.json/" + this.rootDir + "/" + folder, dataset.toString());
    }
}
