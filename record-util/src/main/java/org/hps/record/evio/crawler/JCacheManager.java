package org.hps.record.evio.crawler;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.xml.sax.InputSource;

// sample path => /mss/hallb/hps/data/cosmic_002713.evio.0
public class JCacheManager {

    Map<File, FileInfo> fileInfos = new HashMap<File, FileInfo>();

    static class FileInfo {

        private Integer requestId = null;
        private File file = null;

        FileInfo(File file, Integer requestId) {
            this.requestId = requestId;
        }

        File getCachedFile() {
            return new File("/cache" + file.getPath());
        }

        Integer getRequestId() {
            return requestId;
        }

        String getStatus(File file) {
            Process process = null;
            try {
                process = new ProcessBuilder("jcache", "request", requestId.toString()).start();
            } catch (final IOException e) {
                throw new RuntimeException(e);
            }
            String xmlString = null;
            try {
                xmlString = readFully(process.getInputStream(), "US-ASCII");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            Document xml = buildDocument(xmlString);
            Element root = xml.getRootElement();
            String status = root.getChild("request").getChildText("status");
            return status;
        }

    }
    
    void cache(List<File> files) {
        for (File file : files) {
            cache(file);
        }
    }   

    void cache(File file) {
        // LOGGER.info("running cache commands ...");
        if (!EvioFileUtilities.isCachedFile(file)) {
            throw new IllegalArgumentException("Only files on /mss can be cached.");
        }
        Process process = null;
        try {
            process = new ProcessBuilder("jcache", "submit", "default", file.getPath()).start();
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
        String output = null;
        try {
            output = readFully(process.getInputStream(), "US-ASCII");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Integer requestId = Integer.parseInt(output.substring(output.indexOf("'") + 1, output.lastIndexOf("'")));
        FileInfo fileInfo = new FileInfo(file, requestId);
        fileInfos.put(file, fileInfo);
    }

    // <?xml version="1.0"?><jcache><request
    // id="5123929"><user>jeremym</user><family>default</family><status>active</status><file
    // id="1"><path>/cache/mss/hallb/hps/production/slic/tritrig/2pt2/tritrigv1_s2d6_10.slcio</path><status>pending</status></file></request></jcache>

    private static Document buildDocument(String xmlString) {
        SAXBuilder builder = new SAXBuilder();
        Document document = null;
        try {
            builder.build(new InputSource(new StringReader(xmlString)));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return document;
    }

    boolean waitForAll(int maxWaitMillis) {
        boolean allCached = false;
        long elapsed = 0;
        while (!allCached) {
            boolean cacheCheck = true;
            for (Entry<File, FileInfo> entry : fileInfos.entrySet()) {
                // TODO: Should also check the status here.
                if (!entry.getValue().getCachedFile().exists()) {
                    cacheCheck = false;
                    break;
                }
            }
            if (cacheCheck) {
                allCached = true;
                break;
            }
            elapsed = System.currentTimeMillis();
            if (elapsed > maxWaitMillis) {
                break;
            }
            Object lock = new Object();
            synchronized(lock) {
                try {
                    lock.wait(5000); // Wait 5 seconds before re-polling the files.
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        return allCached;
    }

    private static String readFully(InputStream inputStream, String encoding) throws IOException {
        return new String(readFully(inputStream), encoding);
    }

    private static byte[] readFully(InputStream inputStream) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length = 0;
        while ((length = inputStream.read(buffer)) != -1) {
            baos.write(buffer, 0, length);
        }
        return baos.toByteArray();
    }
}
