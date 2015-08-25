package org.hps.datacat;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;


public class DatacatClientImpl implements DatacatClient {

    private URL url;
    private String site;
    private String rootDir;
    
    /**
     * Create client with default parameters.
     */
    DatacatClientImpl() {        
        this(DatacatConstants.BASE_URL, DatacatConstants.SLAC_SITE, DatacatConstants.ROOT_DIR);
    }
    
    /**
     * Create client.
     * @param baseUrl
     * @param site
     * @param rootDir
     */
    DatacatClientImpl(String baseUrl, String site, String rootDir) {
        try {
            url = new URL(baseUrl);
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
        System.out.println("rootUrl: " + baseUrl);
        System.out.println("site: " + site);
        System.out.println("rootDir: " + rootDir);
    }
    
    @Override
    public int removeFolder(String folder) {
        String fullUrl = url.toString() + "/r/folders.json/" + this.rootDir + folder;
        return DatacatUtilities.doDelete(fullUrl);
    }

    @Override
    public int deleteDataset(String path) {
        String fullUrl = url.toString() + "/r/datasets.json/" + this.rootDir + path;
        return DatacatUtilities.doDelete(fullUrl);
    }

    @Override
    public int addDataset(String folder, Map<String, Object> parameters) {
        JSONObject dataset = DatacatUtilities.createJSONDataset(parameters);
        return DatacatUtilities.doPost(url + "/r/datasets.json/" + this.rootDir + "/" + folder, dataset.toString());
    }
        
    @Override
    public int makeFolder(String path) {
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("path", "/" + DatacatConstants.ROOT_DIR + "/" + path);
        String name = new File(path).getName();       
        parameters.put("name", name);
        parameters.put("_type", "folder");
        JSONObject object = DatacatUtilities.createJSONFromMap(parameters);
        return DatacatUtilities.doPost(url + "/r/folders.json/" + this.rootDir, object.toString());
    }

    @Override
    public int patchDataset(String folder, String name, Map<String, Object> metaData) {
        JSONObject object = DatacatUtilities.createJSONMetaData(metaData);
        String patchUrl = this.url.toString() + "/r/datasets.json/" + this.rootDir + "/" + folder + "/" + name + ";v=current;s=" + this.site;
        return DatacatUtilities.doPost(patchUrl, object.toString());
    }      
    
    public String getRootDir() {
        return this.rootDir;
    }
    
    public URL getBaseUrl() {
        return this.url;
    }
    
    public String getSite() {
        return this.site;
    }
}
