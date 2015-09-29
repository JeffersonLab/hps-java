package org.hps.datacat.client;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Implementation of {@link DatasetMetadata).
 * 
 * @author Jeremy McCormick, SLAC
 */
final class DatasetMetadataImpl implements DatasetMetadata {

    /**
     * The mapping of keys to values.
     */
    private final Map<String, Object> metadataMap = new HashMap<String, Object>();
    
    /**
     * Create a new metadata object.
     * 
     * @param jsonArray the JSON array with the metadata
     */
    DatasetMetadataImpl(JSONArray jsonArray) {
        parse(jsonArray);
    }
    
    /**
     * Parse metadata from JSON array.
     * 
     * @param jsonArray the JSON array
     */
    private void parse(JSONArray jsonArray) {
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject jsonObject = jsonArray.getJSONObject(i);
            String type = jsonObject.getString("type");
            Object value = null;
            if (type.equals("string")) {
                value = jsonObject.getString("value");
            } else if (type.equals("decimal")) {
                value = jsonObject.getDouble("value");
            } else if (type.equals("integer")) {
                value = jsonObject.getLong("value");
            } else {
                throw new IllegalArgumentException("Unknown type: " + type);
            }
            String key = jsonObject.getString("key");
            metadataMap.put(key, value);
        }
    }
    
    /**
     * Get a double value.
     * 
     * @param key the key name
     * @return the double value
     */
    @Override
    public Double getDouble(String key) {
        return Double.class.cast(metadataMap.get(key));
    }

    /**
     * Get a long value. 
     * 
     * @param key the key name
     * @return the long value
     */
    @Override
    public Long getLong(String key) {
        return Long.class.cast(metadataMap.get(key));
    }
      
    /**
     * Get a string value.
     * 
     * @param key the key name
     * @return the 
     */
    @Override
    public String getString(String key) {
        return String.class.cast(metadataMap.get(key));
    }

    /**
     * Return <code>true</code> if key exists in metadata.
     * 
     * @param key the key name
     * @return <code>true</code> if key exists in metadata
     */
    @Override
    public boolean hasKey(String key) {
        return metadataMap.containsKey(key);
    }
    
    /**
     * Convert this object to a string.
     * 
     * @return this object converted to a string
     */
    @Override
    public String toString() {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append(" DatasetMetadata { ");
        for (Entry<String, Object> entry : metadataMap.entrySet()) {
            stringBuffer.append(entry.getKey() + ": " + entry.getValue() + ", ");
        }
        stringBuffer.setLength(stringBuffer.length() - 2);
        stringBuffer.append(" }");
        return stringBuffer.toString();
    }
}
