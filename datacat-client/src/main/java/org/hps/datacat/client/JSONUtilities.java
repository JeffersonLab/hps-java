package org.hps.datacat.client;

import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * JSON datacat utilities.
 *  
 * @author Jeremy McCormick, SLAC
 */
final class JSONUtilities {
    
    /**
     * Create a full JSON dataset object.
     * 
     * @param parameters the parameters of the object as a map
     * @param metadata the metadata of the object as a map
     * @return the created JSON dataset object
     */
    static JSONObject createJSONDataset(Map<String, Object> parameters, Map<String, Object> metadata) {
        JSONObject dataset = new JSONObject();
        dataset.put("dataType", parameters.get("dataType"));
        dataset.put("versionId", "new");
        JSONObject location = new JSONObject();
        location.put("resource", parameters.get("resource"));
        location.put("site", parameters.get("site"));
        JSONArray array = new JSONArray();
        array.put(location);
        dataset.put("locations", array);                
        dataset.put("fileFormat", parameters.get("fileFormat"));
        dataset.put("name", parameters.get("name"));
        if (metadata != null) {
            JSONArray jsonMetadata = createJSONMetadataArray(metadata);
            dataset.put("versionMetadata", jsonMetadata);
        }
        return dataset;
    }
    
    /**
     * Create a flat JSON object from a map of keys and values.
     * 
     * @param parameters the parameter map
     * @return the JSON object
     */
    static JSONObject createJSONFromMap(Map<String, Object> parameters) {
        JSONObject object = new JSONObject();
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            object.put(entry.getKey(), entry.getValue());
        }
        return object;
    }
    
    /**
     * Create a JSON array of metadata from the map of keys and values.
     * 
     * @param metadata the metadata map 
     * @return the JSON array
     */
    static JSONArray createJSONMetadataArray(Map<String, Object> metadata) {
        JSONArray array = new JSONArray();
        for (Map.Entry<String, Object> entry : metadata.entrySet()) {
            JSONObject metadataObject = new JSONObject();
            metadataObject.put(entry.getKey(), entry.getValue());
            metadataObject.put("key", entry.getKey());
            Object rawValue = entry.getValue();
            if (rawValue instanceof String) {
                metadataObject.put("type", "string");
            } else if (rawValue instanceof Integer | rawValue instanceof Long) {
                metadataObject.put("type", "integer");
            } else if (rawValue instanceof Float | rawValue instanceof Double) {
                metadataObject.put("type", "decimal");
            } else {
                throw new IllegalArgumentException("Do not know how to handle type: " + rawValue.getClass().getName());
            }
            metadataObject.put("value", entry.getValue());                      
            array.put(metadataObject);
        }                
        return array;        
    }
}
