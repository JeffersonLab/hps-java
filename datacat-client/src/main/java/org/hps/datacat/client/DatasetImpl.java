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
 * 
 * @author Jeremy McCormick, SLAC
 *
 */
final class DatasetImpl implements Dataset {
    
    private String name;
    private String path;
    private DatasetDataType dataType;
    private DatasetFileFormat fileFormat;
    private List<DatasetLocation> locations = new ArrayList<DatasetLocation>();
    private Date created;
    
    private static final SimpleDateFormat DATE_PARSER = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
    
    DatasetImpl(JSONObject jsonObject) {
        parse(jsonObject);
    }
        
    private void parse(JSONObject jsonObject) {
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
    }
    
    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String getPath() {
        return this.path;
    }

    @Override
    public List<DatasetLocation> getLocations() {
        return Collections.unmodifiableList(this.locations);
    }

    @Override
    public DatasetFileFormat getFileFormat() {
        return this.fileFormat;
    }

    @Override
    public DatasetDataType getDataType() {
        return this.dataType;
    }

    @Override
    public Date getCreated() {
        return this.created;
    }
}
