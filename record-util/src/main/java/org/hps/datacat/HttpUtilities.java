package org.hps.datacat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

/**
 * 
 * @author Jeremy McCormick, SLAC
 */
final class HttpUtilities {

    static int doPost(String urlLocation, String data) {
        int responseCode = 0;
        try {
            URL url = new URL(urlLocation);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);
            if (data != null) {
                OutputStreamWriter out = new OutputStreamWriter(connection.getOutputStream());
                out.write(data);
                out.close();
            }
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
    
    static int doGet(String urlLocation, StringBuffer stringBuffer) {
        HttpURLConnection connection = null;
        int response = 0;
        try {
            //System.out.println("doGet: " + urlLocation);
            connection = (HttpURLConnection) new URL(urlLocation).openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");
            connection.setDoInput(true);
            connection.connect();
            if (stringBuffer != null) {
                String output = IOUtils.toString(connection.getInputStream(), "UTF-8");            
                stringBuffer.append(output);
            }
            response = connection.getResponseCode();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            connection.disconnect();
        }
        return response;
    }    
    
    
    static int doPatch(String urlLocation, String data) {
        int responseCode = 0;        
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpPatch httpPatch = null;
        try {
            httpPatch = new HttpPatch(new URI(urlLocation));            
            InputStreamEntity entity = 
                    new InputStreamEntity(
                            new ByteArrayInputStream(
                                    data.getBytes("UTF-8")), 
                                    -1, 
                                    ContentType.APPLICATION_JSON);
            httpPatch.setEntity(entity);            
            CloseableHttpResponse response = httpClient.execute(httpPatch);
            System.out.println("status: " + response.getStatusLine());
            try {
                EntityUtils.consume(response.getEntity());
            } finally {
                response.close();
            }            
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        } catch(IOException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                httpClient.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }                   
        return responseCode;
    }    
    
    
    static int doDelete(String fullUrl) {
        int responseCode = 0;
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
    
    static URL createURL(String... chunks) {
        if (chunks.length == 0) {
            throw new IllegalArgumentException("No arguments provided.");
        }
        String urlString = "";
        for (String chunk : chunks) {
            urlString += chunk;
        }
        try {
            return new URL(urlString);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Bad URL string: " + urlString);
        }
    }
}
