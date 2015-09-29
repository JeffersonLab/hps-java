package org.hps.datacat.client;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Implementation of the {@link Dataset} interface. 
 * 
 * @author Jeremy McCormick, SLAC
 */
final class DatasetImpl implements Dataset {
    
    /**
     * The name of the dataset.
     */
    private String name;
    
    /**
     * The path in the datacatalog which is folder + name.
     */
    private String path;
    
    /**
     * The data type of the file.
     */
    private DatasetDataType dataType;
    
    /**
     * The format of the file.
     */
    private DatasetFileFormat fileFormat;
    
    /**
     * The list of file locations.
     */
    private List<DatasetLocation> locations = new ArrayList<DatasetLocation>();
    
    /**
     * The creation date.
     */
    private Date created;
    
    /**
     * The dataset's metadata.
     */
    private DatasetMetadata metadata;
    
    /**
     * Parser for reading in dates from JSON.
     */
    private static final SimpleDateFormat DATE_PARSER = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    
    /**
     * Create a new dataset from JSON.
     * 
     * @param jsonObject the JSON data
     */
    DatasetImpl(JSONObject jsonObject) {
        if (!jsonObject.has("_type")) {
            throw new IllegalArgumentException("JSON object is missing _type field.");
        }
        if (!jsonObject.getString("_type").startsWith("dataset")) {
            throw new IllegalArgumentException("JSON _type field is not a dataset: " + jsonObject.getString("_type"));
        }
        
        if (jsonObject.getString("_type").equals("dataset#flat")) {            
            parseFlat(jsonObject);
        } else if (jsonObject.getString("_type").equals("dataset#full")) { 
            parseFull(jsonObject);
        } else {
            throw new IllegalArgumentException("Unknown dataset type: " + jsonObject.getString("_type"));
        }
    }
        
    /**
     * Parse the flat JSON representation.
     * 
     * @param jsonObject the JSON object
     */
    private void parseFlat(JSONObject jsonObject) {
        name = jsonObject.getString("name");
        path = jsonObject.getString("path");
        DatasetLocationImpl location = new DatasetLocationImpl(
                DatasetSite.valueOf(jsonObject.getString("site")), 
                jsonObject.getString("resource"),                
                jsonObject.getInt("size"),
                ScanStatus.valueOf(jsonObject.getString("scanStatus")));
        location.setEventCount(jsonObject.getInt("eventCount"));
        location.setRunMin(jsonObject.getInt("runMin"));
        location.setRunMax(jsonObject.getInt("runMax"));
        locations.add(location);                       
        dataType = DatasetDataType.valueOf(jsonObject.getString("dataType"));
        fileFormat = DatasetFileFormat.valueOf(jsonObject.getString("fileFormat"));
        try {
            created = DATE_PARSER.parse(jsonObject.getString("created"));
        } catch (ParseException e) {
            throw new IllegalArgumentException("Bad created value: " + jsonObject.getString("created"), e);
        }    
    }
        
    /**
     * Parse the full JSON representation.
     * 
     * @param jsonObject the JSON object
     */
    private void parseFull(JSONObject jsonObject) {
        if (!jsonObject.getString("_type").equals("dataset#full")) {
            throw new IllegalArgumentException("Wrong _type in JSON data: " + jsonObject.getString("_type"));
        }
        name = jsonObject.getString("name");
        path = jsonObject.getString("path");
        dataType = DatasetDataType.valueOf(jsonObject.getString("dataType"));
        fileFormat = DatasetFileFormat.valueOf(jsonObject.getString("fileFormat"));
        try {
            created = DATE_PARSER.parse(jsonObject.getString("created"));
        } catch (ParseException e) {
            throw new IllegalArgumentException("Bad created value: " + jsonObject.getString("created"), e);
        }        
        JSONArray locationsArray = jsonObject.getJSONArray("locations");
        for (int i = 0; i < locationsArray.length(); i++) {
            this.locations.add(new DatasetLocationImpl(locationsArray.getJSONObject(i)));
        }
        if (jsonObject.has("metadata")) {
            metadata = new DatasetMetadataImpl(jsonObject.getJSONArray("metadata"));
        }
    }
    
    /**
     * Get the name of the dataset without the path component e.g. "dataset01".
     * 
     * @return the name of the dataset
     */
    @Override
    public String getName() {
        return this.name;
    }

    /**
     * Get the logical path of the dataset e.g. "/HPS/folder/dataset01".
     * 
     * @return the path of the dataset
     */
    @Override
    public String getPath() {
        return this.path;
    }

    /**
     * Get the dataset locations.
     * 
     * @return the dataset locations
     */
    @Override
    public List<DatasetLocation> getLocations() {
        return Collections.unmodifiableList(this.locations);
    }

    /**
     * Get the file format e.g. EVIO, LCIO, etc.
     * 
     * @return the dataset file format
     */
    @Override
    public DatasetFileFormat getFileFormat() {
        return this.fileFormat;
    }

    /**
     * Get the data type e.g. RAW, RECON, etc.
     * 
     * @return the data type
     */
    @Override
    public DatasetDataType getDataType() {
        return this.dataType;
    }

    /**
     * Get the creation date.
     * 
     * @return the creation date
     */
    @Override
    public Date getCreated() {
        return this.created;
    }
    
    /**
     * Get the dataset's metadata.
     * 
     * @return the dataset's metadata
     */
    @Override
    public DatasetMetadata getMetadata() {
        return this.metadata;
    }
    
    /**
     * Convert this object to a string.
     * 
     * @return this object converted to a string
     */
    public String toString() {
        return "Dataset { name: " + name + ", path:" + path + ", " + "dataType: " + dataType.name() + "fileFormat: " + fileFormat.name() + ", created: " + created + " }"; 
    }    
}
