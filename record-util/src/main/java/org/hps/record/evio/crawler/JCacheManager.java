package org.hps.record.evio.crawler;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.text.DecimalFormat;
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
 * Utility class for caching files from the MSS to cache disk at JLAB.
 * <p>
 * This class should <b>not</b> be activated when running the crawler on the Auger batch system as it will take up a lot
 * of job time caching files, which is part of Auger's job staging that doesn't count towards the job time.
 * <p>
 * It is fine to use running on an interactive <i>ifarm</i> node at JLAB.
 *
 * @author Jeremy McCormick, SLAC
 */
final class JCacheManager {

    /**
     * Keeps track of cache status for a single file.
     */
    static class CacheStatus {

        /**
         * Flag indicating if the file is cached yet.
         */
        private boolean cached = false;

        /**
         * Path to the file on the MSS.
         */
        private File file = null;

        /**
         * The request ID from executing the 'jcache submit' command.
         */
        private Integer requestId = null;

        /**
         * The current status from executing the 'jcache request' command.
         */
        private String status;

        /**
         * The xml node with request data.
         */
        private Element xml;

        /**
         * Create a new <code>CacheStatus</code> object.
         *
         * @param file the file which has the MSS path
         * @param requestId the request ID from running the cache command
         */
        CacheStatus(final File file, final Integer requestId) {
            this.file = file;
            this.requestId = requestId;
        }

        /**
         * Get the error message from the XML request.
         *
         * @return the error message from the XML request
         */
        String getErrorMessage() {
            if (this.xml.getChild("request").getChild("file").getChild("error") != null) {
                return this.xml.getChild("request").getChild("file").getChild("error").getText();
            } else {
                return "";
            }
        }

        /**
         * Get the file (path on MSS).
         *
         * @return the file with path on the MSS
         */
        File getFile() {
            return this.file;
        }

        /**
         * Get the request ID.
         *
         * @return the request ID
         */
        Integer getRequestId() {
            return this.requestId;
        }

        /**
         * Get the request XML from running 'jcache request' to get the status.
         *
         * @param is the input stream from the process
         * @return the request status string
         */
        private Element getRequestXml(final InputStream is) {
            String xmlString = null;
            try {
                xmlString = readFully(is, "US-ASCII");
            } catch (final IOException e) {
                throw new RuntimeException(e);
            }
            xmlString = xmlString.substring(xmlString.trim().indexOf("<jcache>"));
            return buildDocument(xmlString).getRootElement();
        }

        /**
         * Get the status string.
         *
         * @param updateStatus <code>true</code> to run the 'jcache status' command
         * @return the status string
         */
        String getStatus(final boolean updateStatus) {
            if (updateStatus) {
                this.update();
            }
            return this.status;
        }

        /**
         * Return <code>true</code> if file is cached.
         *
         * @return <code>true</code> if file is cached
         */
        boolean isCached() {
            return this.cached;
        }

        /**
         * Return <code>true</code> if status is "done".
         *
         * @return <code>true</code> if status is "done"
         */
        boolean isDone() {
            return "done".equals(this.status);
        }

        /**
         * Return <code>true</code> if status is "failed".
         */
        boolean isFailed() {
            return "failed".equals(this.status);
        }

        /**
         * Return <code>true</code> if status is "hit".
         *
         * @return <code>true</code> if status is "hit"
         */
        boolean isHit() {
            return "hit".equals(this.status);
        }

        /**
         * Return <code>true</code> if status is "pending".
         *
         * @return <code>true</code> if status is "pending"
         */
        boolean isPending() {
            return "pending".equals(this.status);
        }

        /**
         * Run the <i>jcache request</i> command for this request ID and return the XML output.
         *
         * @return the XML output from the <i>jcache request</i> command
         */
        private Element request() {
            Process process = null;
            try {
                process = new ProcessBuilder(JCACHE_COMMAND, "request", this.requestId.toString()).start();
            } catch (final IOException e) {
                throw new RuntimeException(e);
            }
            int status = 0;
            try {
                status = process.waitFor();
            } catch (final InterruptedException e) {
                throw new RuntimeException("Cache process was interrupted.", e);
            }
            if (status != 0) {
                throw new RuntimeException("The jcache request returned an error status: " + status);
            }
            return this.getRequestXml(process.getInputStream());
        }

        /**
         * Update the cache status.
         */
        void update() {
            // Request status update and get the XML from that process.
            this.xml = this.request();

            // Update the status from the XML.
            this.status = this.xml.getChild("request").getChild("file").getChildText("status");

            // Is request done or file already in cache?
            if (this.isDone() || this.isHit()) {
                // Flag file as cached.
                this.cached = true;
            }
        }
    }

    /**
     * The default max wait time in milliseconds for all file caching operations to complete (default is ~5 minutes).
     */
    private static long DEFAULT_MAX_WAIT_TIME = 300000;

    /**
     * The command for running jcache (accessible from ifarm machines and batch notes).
     */
    private static final String JCACHE_COMMAND = "/site/bin/jcache";

    /**
     * Setup the logger.
     */
    private static Logger LOGGER = LogUtil.create(JCacheManager.class, new DefaultLogFormatter(), Level.FINE);

    /**
     * Time to wait between re-polling all files (30 seconds).
     */
    private static final long POLL_WAIT_TIME = 30000;

    /**
     * Build an XML document from a string.
     *
     * @param xmlString the raw XML string
     * @return the XML document
     */
    private static Document buildDocument(final String xmlString) {
        LOGGER.fine(xmlString);
        final SAXBuilder builder = new SAXBuilder();
        Document document = null;
        try {
            document = builder.build(new InputSource(new StringReader(xmlString)));
        } catch (final Exception e) {
            throw new RuntimeException("Error building XML doc.", e);
        }
        return document;
    }

    /**
     * Read bytes from an input stream into an array.
     *
     * @param inputStream the input stream
     * @return the bytes read
     * @throws IOException if there is a problem reading the stream
     */
    private static byte[] readFully(final InputStream inputStream) throws IOException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final byte[] buffer = new byte[1024];
        int length = 0;
        while ((length = inputStream.read(buffer)) != -1) {
            baos.write(buffer, 0, length);
        }
        return baos.toByteArray();
    }

    /**
     * Read bytes from an input stream into a string with a certain encoding.
     *
     * @param inputStream the input stream
     * @param encoding the encoding
     * @return the output string
     * @throws IOException if there is a problem reading from the stream
     */
    private static String readFully(final InputStream inputStream, final String encoding) throws IOException {
        return new String(readFully(inputStream), encoding);
    }

    /**
     * The current cache statuses mapped by <code>File</code> object.
     */
    private final Map<File, CacheStatus> cacheStatuses = new HashMap<File, CacheStatus>();

    /**
     * The maximum time to wait for all caching operations to complete.
     */
    private long maxWaitTime = DEFAULT_MAX_WAIT_TIME;

    /**
     * The time in milliseconds when the caching operation started.
     */
    long start = 0;

    /**
     * Cache a file by submitting a 'jcache submit' process.
     * <p>
     * The resulting cache request will be registered with this manager.
     *
     * @param file the file to cache which should be a path on the JLAB MSS (e.g. starts with '/mss')
     */
    private void cache(final File file) {
        if (!EvioFileUtilities.isMssFile(file)) {
            LOGGER.severe("file " + file.getPath() + " is not on the MSS");
            throw new IllegalArgumentException("Only files on the MSS can be cached.");
        }

        if (EvioFileUtilities.getCachedFile(file).exists()) {
            // Assume that since the file already exists it will stay in the cache for the duration of the job.
            LOGGER.fine(file.getPath() + " is already on the cache disk so cache request is ignored");
        } else {

            // Execute the submit process.
            final Process process = this.submit(file);

            // Parse out the request ID from the process output.
            final Integer requestId = this.getRequestId(process);

            // Register the request with the manager.
            final CacheStatus cacheStatus = new CacheStatus(file, requestId);
            this.cacheStatuses.put(file, cacheStatus);

            LOGGER.info("jcache submitted for " + file.getPath() + " with req ID '" + requestId + "'");
        }
    }

    /**
     * Submit a cache request for every file in the list.
     *
     * @param files the list of files
     */
    void cache(final List<File> files) {
        for (final File file : files) {
            this.cache(file);
        }
    }

    /**
     * Return <code>true</code> if all files registered with the manager have been cached.
     *
     * @return <code>true</code> if all files registered with the manager are cached
     */
    boolean checkCacheStatus() {

        // Flag which will be changed to false if we find files that are not cached yet.
        boolean allCached = true;

        // Loop over all files, refresh the status, and check that they are cached.
        for (final Entry<File, CacheStatus> entry : this.cacheStatuses.entrySet()) {

            // Get the cache status the file.
            final CacheStatus cacheStatus = entry.getValue();

            LOGGER.info("checking status of " + cacheStatus.getFile().getPath() + " with req ID '"
                    + cacheStatus.getRequestId() + "' ...");

            // Does the file's status indicate it is not cached yet?
            if (!cacheStatus.isCached()) {

                // Update the cache status to see if it has changed.
                LOGGER.info("updating status of " + cacheStatus.getFile().getPath() + " ...");
                cacheStatus.update();

                // Is this file's status still non-cached after the status update?
                if (!cacheStatus.isCached()) {

                    // Set flag which indicates at least one file is not cached yet.
                    allCached = false;

                    LOGGER.info(entry.getKey() + " is NOT cached with status " + cacheStatus.getStatus(false));

                    break;

                } else if (cacheStatus.isFailed()) {
                    // Cache failure is a fatal error.
                    LOGGER.severe("cache request failed with error: " + cacheStatus.getErrorMessage());
                    throw new RuntimeException("Cache request failed.");
                } else {
                    // Log that this file is now cached. It will not be checked next time this method is called.
                    LOGGER.info(cacheStatus.getFile().getPath() + " is now cached with status "
                            + cacheStatus.getStatus(false));
                }
            }
        }
        return allCached;
    }

    /**
     * Clear the cache statuses which means the manager will no longer track any of the files that were registered.
     */
    void clear() {
        this.cacheStatuses.clear();
        this.start = 0;
        LOGGER.info("CacheManager state was cleared.");
    }

    /**
     * Parse out the request ID from the output of a 'jcache request' process.
     *
     * @param process the system process
     * @return the request ID
     */
    private Integer getRequestId(final Process process) {
        String output = null;
        try {
            output = readFully(process.getInputStream(), "US-ASCII");
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
        return Integer.parseInt(output.substring(output.indexOf("'") + 1, output.lastIndexOf("'")));
    }

    /**
     * Get the number of files that are not cached.
     *
     * @return the number of files that are not cached
     */
    int getUncachedCount() {
        int nUncached = 0;
        for (final Entry<File, CacheStatus> entry : this.cacheStatuses.entrySet()) {
            if (!entry.getValue().isCached()) {
                nUncached += 1;
            }
        }
        return nUncached;
    }

    /**
     * Set the maximum wait time for caching to complete.
     *
     * @param maxWaitTime the maximum wait time for caching to complete
     */
    void setWaitTime(final long maxWaitTime) {
        this.maxWaitTime = maxWaitTime;
        LOGGER.config("max wait time set to " + maxWaitTime + " ms");
    }

    /**
     * Sleep after checking cache statuses.
     *
     * @return <code>true</code> if <code>maxWaitTime</code> is exceeded or method is interrupted
     */
    private boolean sleep() {
        final long elapsed = System.currentTimeMillis() - this.start;
        LOGGER.info("elapsed time is " + elapsed + " ms");
        if (elapsed > this.maxWaitTime) {
            LOGGER.warning("max wait time of " + this.maxWaitTime + " ms was exceeded while caching files");
            return true;
        }
        final Object lock = new Object();
        synchronized (lock) {
            try {
                LOGGER.info("waiting " + POLL_WAIT_TIME + " ms before checking cache again ...");
                lock.wait(POLL_WAIT_TIME);
            } catch (final InterruptedException e) {
                e.printStackTrace();
                return true;
            }
        }
        return false;
    }

    /**
     * Submit a cache request for a file.
     *
     * @param file the file
     * @return the system process for the cache request command
     */
    private Process submit(final File file) {
        Process process = null;
        try {
            process = new ProcessBuilder("jcache", "submit", "default", file.getPath()).start();
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
        int status = 0;
        try {
            status = process.waitFor();
        } catch (final InterruptedException e) {
            throw new RuntimeException("Process was interrupted.", e);
        }
        if (status != 0) {
            throw new RuntimeException("The jcache process returned an error status of " + status);
        }
        return process;
    }

    /**
     * Wait for all files registered with the manager to be cached or until a timeout occurs.
     *
     * @return <code>true</code> if all files are successfully cached
     */
    boolean waitForCache() {

        LOGGER.info("waiting for files to be cached ...");

        // This can happen if all the files are already cached.
        if (this.cacheStatuses.isEmpty()) {
            LOGGER.warning("no files to be cached");
            return true;
        }

        // This is the return value which will be changed to true if all files are cached successfully.
        boolean cached = false;

        // Get the start time so we can calculate later if max wait time is exceeded.
        this.start = System.currentTimeMillis();

        // Keep checking files until they are all cached or the max wait time is exceeded.
        while (!cached) {

            // Check cache status of all files. This will return true if all files are cached.
            final boolean allCached = this.checkCacheStatus();

            // If all cache requests have succeeded then break from loop and set cache status to true.
            if (allCached) {
                cached = true;
                break;
            } else {
                LOGGER.info(this.getUncachedCount() + " files are not cached");
            }

            // Sleep for awhile before checking the cache statuses again.
            // This will return true if max wait time is exceeded and the wait should be stopped.
            final boolean waitTimeExceeded = this.sleep();

            // Break from loop if the max wait time was exceeded.
            if (waitTimeExceeded) {
                break;
            }
        }

        final double end = System.currentTimeMillis() - this.start;

        LOGGER.info("caching took " + new DecimalFormat("#.##").format(end / 1000. / 60.) + " minutes");

        if (cached) {
            LOGGER.info("all files cached successfully!");
        } else {
            LOGGER.warning("failed to cache all files!");
        }
        return cached;
    }
}
