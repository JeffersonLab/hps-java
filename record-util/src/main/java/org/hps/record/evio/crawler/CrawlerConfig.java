package org.hps.record.evio.crawler;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.hps.conditions.database.ConnectionParameters;
import org.hps.record.evio.EvioEventProcessor;

/**
 * Crawls EVIO files in a directory tree, groups the files that are found by run, and optionally performs various tasks based on the run summary
 * information that is accumulated, including printing a summary, caching the files from JLAB MSS, and updating a run database.
 *
 * @author Jeremy McCormick, SLAC
 */
final class CrawlerConfig {

    private static final SimpleDateFormat TIMESTAMP_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    
    /**
     * Default event print interval.
     */
    private final int DEFAULT_EVENT_PRINT_INTERVAL = 1000;

    /**
     * Interval for printing out event number while running EVIO processors.
     */
    private int eventPrintInterval = DEFAULT_EVENT_PRINT_INTERVAL;
   
    /**
     * A list of run numbers to accept in the job.
     */
    private Set<Integer> acceptRuns;

    private boolean allowUpdates = false;

    private ConnectionParameters connectionParameters;

    /**
     * The maximum number of files to accept (just used for debugging purposes).
     */
    private int maxFiles = -1;

    private final List<EvioEventProcessor> processors = new ArrayList<EvioEventProcessor>();

    /**
     * The root directory to crawl which defaults to the current directory.
     */
    private File rootDir = new File(System.getProperty("user.dir"));

    /**
     * A timestamp to use for filtering input files on their creation date.
     */
    private Date timestamp = null;

    /**
     * A file to use for the timestamp date.
     */
    private File timestampFile = null;

    /**
     * Flag indicating if the run database should be updated from results of the job.
     */
    private boolean updateRunLog = false;

    /**
     * Flag indicating if the file cache should be used (e.g. jcache automatically executed to move files to the cache disk from tape).
     */
    private boolean useFileCache = false;

    /**
     * The maximum wait time in milliseconds to allow for file caching operations.
     */
    private Long waitTime;

    CrawlerConfig addProcessor(final EvioEventProcessor processor) {
        this.processors.add(processor);
        return this;
    }
    
    CrawlerConfig addProcessor(final String className) {
        try {
            this.processors.add(EvioEventProcessor.class.cast(Class.forName(className).newInstance()));
        } catch (Exception e) {
            throw new RuntimeException("Error creating EvioEventProcessor with type: " + className, e);
        }
        return this;
    }

    CrawlerConfig setAcceptRuns(final Set<Integer> acceptRuns) {
        this.acceptRuns = acceptRuns;
        return this;
    }

    CrawlerConfig setAllowUpdates(final boolean allowUpdates) {
        this.allowUpdates = allowUpdates;
        return this;
    }

    CrawlerConfig setConnection(final ConnectionParameters connectionParameters) {
        this.connectionParameters = connectionParameters;
        return this;
    }

    CrawlerConfig setMaxFiles(final int maxFiles) {
        this.maxFiles = maxFiles;
        return this;
    }
    
    CrawlerConfig setRootDir(File rootDir) {
        this.rootDir = rootDir;
        return this;
    }
    
    CrawlerConfig setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
        return this;
    }
    
    CrawlerConfig setTimestamp(String timestampString) throws ParseException {
        TIMESTAMP_DATE_FORMAT.parse(timestampString);
        return this;
    }
    
    CrawlerConfig setTimestampFile(File timestampFile) {
        this.timestampFile = timestampFile;
        return this;
    }
    
    CrawlerConfig setUpdateRunLog(boolean updateRunLog) {
        this.updateRunLog = updateRunLog;
        return this;
    }
    
    CrawlerConfig setUseFileCache(boolean useFileCache) {
        this.useFileCache = useFileCache;
        return this;
    }
    
    CrawlerConfig setWaitTime(long waitTime) {
        this.waitTime = waitTime;
        return this;
    }
    
    CrawlerConfig setEventPrintInterval(int eventPrintInterval) {
        this.eventPrintInterval = eventPrintInterval;
        return this;
    }
    
    Set<Integer> acceptRuns() {
        return acceptRuns;
    }
    
    boolean allowUpdates() {
        return allowUpdates;
    }

    ConnectionParameters connectionParameters() {
        return connectionParameters;
    }

    int maxFiles() {
        return maxFiles;
    }

    List<EvioEventProcessor> processors() {
        return processors;
    }

    File rootDir() {
        return rootDir;
    }

    Date timestamp() {
        return timestamp;
    }
    
    File timestampFile() {
        return timestampFile;
    }

    boolean updateRunLog() {
        return updateRunLog;
    }

    boolean useFileCache() {
        return useFileCache;
    }

    Long waitTime() {
        return waitTime;
    }    
    
    int eventPrintInterval() {
        return this.eventPrintInterval;
    }          
}
