package org.hps.datacat.client;

import java.util.HashMap;
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
        dataset.put("resource", parameters.get("resource"));
        dataset.put("site", parameters.get("site"));
        dataset.put("fileFormat", parameters.get("fileFormat"));
        dataset.put("name", parameters.get("name"));
        if (parameters.containsKey("size")) {
            dataset.put("size", parameters.get("size"));
        }
        
        Map<String, Object> metadataCopy = new HashMap<String, Object>();
        metadataCopy.putAll(metadata);
        
        // This metadata needs to be set at the top-level and not in versionMetadata.
        if (metadataCopy.containsKey("runMin")) {
            dataset.put("runMin", metadataCopy.get("runMin"));
            metadataCopy.remove("runMin");
        }
        if (metadataCopy.containsKey("runMax")) {
            dataset.put("runMax", metadataCopy.get("runMax"));
            metadataCopy.remove("runMax");
        }
        if (metadataCopy.containsKey("eventCount")) {
            dataset.put("eventCount", metadataCopy.get("eventCount"));
            metadataCopy.remove("eventCount");
        }
        
        if (metadata != null && metadata.size() != 0) {
            JSONArray jsonMetadata = createJSONMetadataArray(metadataCopy);
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
