package org.hps.datacat.client;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
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
 * @author Jeremy McCormick, SLAC
 */
final class HttpUtilities {

    /**
     * Do an HTTP DELETE.
     *
     * @param urlLocation the URL location
     * @return the HTTP response code
     */
    static int doDelete(final String urlLocation) {
        int responseCode = 0;
        try {
            final URL url = new URL(urlLocation);
            final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoOutput(true);
            connection.setRequestMethod("DELETE");
            connection.connect();
            responseCode = connection.getResponseCode();
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
        return responseCode;
    }

    /**
     * Do an HTTP get and return the output from the server in a <code>StringBuffer</code>.
     *
     * @param urlLocation the URL location
     * @param stringBuffer the string buffer with the server output
     * @return the HTTP response
     */
    static int doGet(final String urlLocation, final StringBuffer stringBuffer) {
        HttpURLConnection connection = null;
        int response = 0;
        try {
            connection = (HttpURLConnection) new URL(urlLocation).openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");
            connection.setDoInput(true);
            connection.connect();
            if (stringBuffer != null) {
                final String output = IOUtils.toString(connection.getInputStream(), "UTF-8");
                stringBuffer.append(output);
            }
            response = connection.getResponseCode();
        } catch (final IOException e) {
            throw new RuntimeException(e);
        } finally {
            connection.disconnect();
        }
        return response;
    }

    /**
     * Do an HTTP patch.
     *
     * @param urlLocation the URL location
     * @param data the data to stream to the server
     * @return the HTTP response code
     */
    static int doPatch(final String urlLocation, final String data) {
        int responseCode = 0;
        final CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpPatch httpPatch = null;
        try {
            httpPatch = new HttpPatch(new URI(urlLocation));
            final InputStreamEntity entity = new InputStreamEntity(new ByteArrayInputStream(data.getBytes("UTF-8")),
                    -1, ContentType.APPLICATION_JSON);
            httpPatch.setEntity(entity);
            final CloseableHttpResponse response = httpClient.execute(httpPatch);
            try {
                EntityUtils.consume(response.getEntity());
            } finally {
                response.close();
            }
            responseCode = response.getStatusLine().getStatusCode();
        } catch (final URISyntaxException e) {
            throw new IllegalArgumentException(e);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                httpClient.close();
            } catch (final IOException e) {
                throw new RuntimeException(e);
            }
        }
        return responseCode;
    }

    /**
     * Do an HTTP POST.
     *
     * @param urlLocation the URL location
     * @param data the data to stream to the server
     * @return the HTTP response code
     */
    static int doPost(final String urlLocation, final String data) {
        int responseCode = 0;
        try {
            final URL url = new URL(urlLocation);
            final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);
            connection.setDoInput(true);
            if (data != null) {
                final OutputStreamWriter out = new OutputStreamWriter(connection.getOutputStream());
                out.write(data);
                out.close();
            }
            
            // DEBUG
            /*
            StringBuffer stringBuffer = new StringBuffer();
            if (stringBuffer != null) {
                final String output = IOUtils.toString(connection.getInputStream(), "UTF-8");
                stringBuffer.append(output);
            }
            
            System.out.println("url: " + urlLocation);
            System.out.println("data: " + data);
            System.out.println("response: " + connection.getResponseCode());
            System.out.println("message: " + connection.getResponseMessage());
            System.out.println("output: " + stringBuffer.toString());
            */
            
            responseCode = connection.getResponseCode();
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
        return responseCode;
    }   
}
