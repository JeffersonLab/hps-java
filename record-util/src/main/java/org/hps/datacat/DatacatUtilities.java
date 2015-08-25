package org.hps.datacat;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

public final class DatacatUtilities {

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
        
    static int doPost(String urlLocation, String data) {
        int responseCode = 520;
        try {
            URL url = new URL(urlLocation);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);
            OutputStreamWriter out = new OutputStreamWriter(connection.getOutputStream());
            out.write(data);
            out.close();
            System.out.println("url: " + urlLocation);
            System.out.println("data: " + data);
            System.out.println("response: " + connection.getResponseCode());
            System.out.println("message: " + connection.getResponseMessage());
            responseCode = connection.getResponseCode();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return responseCode;
    }    
    
    static int doDelete(String fullUrl) {
        int responseCode = 520;
        try {
            URL url = new URL(fullUrl);
            System.out.println("deleting url: " + fullUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoOutput(true);
            connection.setRequestMethod("DELETE");
            connection.connect();
            responseCode = connection.getResponseCode();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return responseCode;
    }
}
