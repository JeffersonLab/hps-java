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
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.lcsim.util.log.DefaultLogFormatter;
import org.lcsim.util.log.LogUtil;
import org.xml.sax.InputSource;

/**
 * Utility class for using the <i>jcache</i> command at JLAB.
 */
public class JCacheManager {

    private static Logger LOGGER = LogUtil.create(JCacheManager.class, new DefaultLogFormatter(), Level.ALL);
    
    private Map<File, FileInfo> fileInfos = new HashMap<File, FileInfo>();
    
    private static long DEFAULT_MAX_WAIT_TIME = 300000;
    
    private long maxWaitTime = DEFAULT_MAX_WAIT_TIME;
    
    private static final long POLL_WAIT_TIME = 10000;
            
    static class FileInfo {

        private Integer requestId = null;
        private File file = null;
        private boolean cached = false;
        private Process process = null;

        FileInfo(File file, Integer requestId, Process process) {
            this.requestId = requestId;
        }

        File getCachedFile() {
            return new File("/cache" + file.getPath());
        }

        Integer getRequestId() {
            return requestId;
        }
        
        Process getProcess() {
            return process;
        }
        
        boolean isCached() {
            return cached;
        }
                
        void update() {
            if (!isCached()) {
                if (!isPending() && getCachedFile().exists()) {
                    LOGGER.info("file " + file.getPath() + " is cached");
                    this.cached = true;
                }
            }
        }

        String getStatus() {
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
        
        boolean isPending() {
            return !"pending".equals(getStatus());
        }
    }
    
    void setWaitTime(long maxWaitTime) {
        this.maxWaitTime = maxWaitTime;
    }
    
    void cache(List<File> files) {
        for (File file : files) {
            cache(file);
        }
    }   

    void cache(File file) {
        if (!EvioFileUtilities.isMssFile(file)) {
            LOGGER.severe("file " + file.getPath() + " is not on the MSS");
            throw new IllegalArgumentException("Only files on the MSS can be cached.");
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
        FileInfo fileInfo = new FileInfo(file, requestId, process);
        fileInfos.put(file, fileInfo);
        LOGGER.info("jcache submitted for " + file.getPath() + " with req ID '" + requestId + "' and process " + process);
    }

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

    boolean waitForAll() {
        if (this.fileInfos.isEmpty()) {
            throw new IllegalStateException("There are no files being cached.");
        }
        LOGGER.info("waiting for files to be cached ...");
        long elapsed = 0;
        boolean cached = false;
        while (!cached) {
            boolean check = true;    
            INFO_LOOP: for (Entry<File, FileInfo> entry : fileInfos.entrySet()) {
                FileInfo info = entry.getValue();
                info.update();                
                if (!info.isCached()) {
                    LOGGER.info(entry.getKey() + " is not cached yet");
                    check = false;
                    break INFO_LOOP;
                }
            }
            if (check) {
                cached = true;
                break;
            }
            
            elapsed = System.currentTimeMillis();
            LOGGER.info("elapsed time: " + elapsed + " ms");
            if (elapsed > maxWaitTime) {
                break;
            }
            Object lock = new Object();
            synchronized(lock) {
                try {
                    LOGGER.info("waiting " + POLL_WAIT_TIME + " ms before checking again ...");
                    lock.wait(POLL_WAIT_TIME);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        LOGGER.info("files were cached: " + cached);
        return cached;
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
