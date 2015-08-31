package org.hps.datacat.client;

import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * 
 * @author Jeremy McCormick, SLAC
 */
final class JSONUtilities {
    
    static JSONObject createJSONDataset(Map<String, Object> parameters) {
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
        return dataset;
    }
    
    static JSONObject createJSONFromMap(Map<String, Object> parameters) {
        JSONObject object = new JSONObject();
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            object.put(entry.getKey(), entry.getValue());
        }
        return object;
    }
    
    static JSONObject createJSONMetaData(Map<String, Object> metaData) {
        JSONObject object = new JSONObject();
        JSONArray array = new JSONArray();
        for (Map.Entry<String, Object> entry : metaData.entrySet()) {
            JSONObject value = new JSONObject();
            value.put(entry.getKey(), entry.getValue());
            array.put(value);
        }                
        object.put("versionMetadata", array);
        return object;
    }
}
