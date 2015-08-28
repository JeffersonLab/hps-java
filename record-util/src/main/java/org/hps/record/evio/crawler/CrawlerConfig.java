package org.hps.record.evio.crawler;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.hps.conditions.database.ConnectionParameters;
import org.hps.record.evio.EvioEventProcessor;

/**
 * Full configuration information for the {@link Crawler} class.
 * <p>
 * Method chaining of setters is supported.
 *
 * @author Jeremy McCormick, SLAC
 */
final class CrawlerConfig {

    /**
     * The format for input timestamps used for file filtering.
     */
    private static final SimpleDateFormat TIMESTAMP_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /**
     * A list of run numbers to accept in the job; this default will probably get overridden but it is here to avoid
     * null pointer exceptions. An empty list is assumed to mean "accept all runs" e.g. no run number filtering.
     */
    private Set<Integer> acceptRuns = new LinkedHashSet<Integer>();

    /**
     * <code>true</code> if database updates are allowed meaning existing records can be deleted and replaced.
     */
    private boolean allowUpdates = false;

    /**
     * The database connection parameters which must be provided by a command line argument.
     */
    private ConnectionParameters connectionParameters;

    /**
     * The maximum depth to crawl.
     */
    private Integer maxDepth = Integer.MAX_VALUE;

    /**
     * The maximum number of files to accept (just used for debugging purposes).
     */
    private int maxFiles = -1;

    /**
     * A list of extra {@link org.hps.record.evio.EvioEventProcessor}s to run with the job.
     */
    private final List<EvioEventProcessor> processors = new ArrayList<EvioEventProcessor>();

    /**
     * The root directory to search for files, which defaults to the current directory.
     */
    private File rootDir = new File(System.getProperty("user.dir"));

    /**
     * A timestamp to use for filtering input files on their creation date.
     */
    private Date timestamp = null;

    /**
     * A file to use for getting the timestamp date.
     */
    private File timestampFile = null;

    /**
     * <code>true</code> if the run database should be updated from results of the job.
     */
    private boolean updateRunLog = false;

    /**
     * <code>true</code> if file caching should be used to move files to the cache disk from tape at JLAB.
     */
    private boolean useFileCache = false;

    /**
     * The maximum wait time in milliseconds to allow for file caching operations.
     */
    private Long waitTime;

    /**
     * Get the set of runs that will be accepted for the job.
     *
     * @return the list of runs that will be accepted
     */
    Set<Integer> acceptRuns() {
        return acceptRuns;
    }

    /**
     * Add an {@link org.hps.record.evio.EvioEventProcessor} to the job.
     *
     * @param processor
     * @return this object
     */
    CrawlerConfig addProcessor(final EvioEventProcessor processor) {
        this.processors.add(processor);
        return this;
    }

    /**
     * Add an {@link org.hps.record.evio.EvioEventProcessor} to the job by its class name.
     *
     * @param processor the <code>EvioEventProcessor</code> to instantiate
     * @return this object
     */
    CrawlerConfig addProcessor(final String className) {
        try {
            this.processors.add(EvioEventProcessor.class.cast(Class.forName(className).newInstance()));
        } catch (final Exception e) {
            throw new RuntimeException("Error creating EvioEventProcessor with type: " + className, e);
        }
        return this;
    }

    /**
     * Return <code>true</code> if updates/deletions of existing records in the database is allowed.
     *
     * @return <code>true</code> if updating/deleting records in the database is allowed
     */
    boolean allowUpdates() {
        return allowUpdates;
    }

    /**
     * Get the database connection parameters.
     *
     * @return the database connection parameters
     */
    ConnectionParameters connectionParameters() {
        return connectionParameters;
    }

    /**
     * Get the max depth in the directory tree to crawl.
     *
     * @return the max depth
     */
    Integer maxDepth() {
        return maxDepth;
    }

    /**
     * Get the maximum number of files that the job can process.
     *
     * @return the maximum number of files
     */
    int maxFiles() {
        return maxFiles;
    }

    /**
     * Get the list of extra event processors that will run with the job.
     * <p>
     * Required (default) processors for the job are not included here.
     *
     * @return the list of extra event processors
     */
    List<EvioEventProcessor> processors() {
        return processors;
    }

    /**
     * Get the root directory for the file search.
     *
     * @return the root directory for the file search
     */
    File rootDir() {
        return rootDir;
    }

    /**
     * Set the list of run numbers that should be accepted.
     *
     * @param acceptRuns the list of acceptable run numbers
     * @return this object
     */
    CrawlerConfig setAcceptRuns(final Set<Integer> acceptRuns) {
        this.acceptRuns = acceptRuns;
        return this;
    }

    /**
     * Set whether database updates are allowed, i.e. replacement of existing records.
     *
     * @param allowUpdates <code>true</code> to allow database record deletion/updates
     * @return this object
     */
    CrawlerConfig setAllowUpdates(final boolean allowUpdates) {
        this.allowUpdates = allowUpdates;
        return this;
    }

    /**
     * Set the database connection parameters.
     *
     * @param connectionParameters the database connection parameters
     * @return this object
     */
    CrawlerConfig setConnection(final ConnectionParameters connectionParameters) {
        this.connectionParameters = connectionParameters;
        return this;
    }

    /**
     * Set the max depth.
     *
     * @param maxDepth the max depth
     */
    CrawlerConfig setMaxDepth(final Integer maxDepth) {
        this.maxDepth = maxDepth;
        return this;
    }

    /**
     * Set the maximum number of files that will be processed by the job.
     * <p>
     * This should only be used for debugging purposes as it results in incorrect event counts for the run.
     *
     * @param maxFiles the maximum number of files to process or -1 for unlimited
     * @return this object
     */
    CrawlerConfig setMaxFiles(final int maxFiles) {
        this.maxFiles = maxFiles;
        return this;
    }

    /**
     * Set the root directory for the file search.
     *
     * @param rootDir the root directory for the file search
     * @return this object
     */
    CrawlerConfig setRootDir(final File rootDir) {
        this.rootDir = rootDir;
        return this;
    }

    /**
     * Set a date for filtering input files.
     * <p>
     * Those files created before this date will not be processed.
     *
     * @param timestamp the date for filtering files
     * @return this object
     */
    CrawlerConfig setTimestamp(final Date timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    /**
     * Set a date for filtering input files using a string in the default format defined by
     * <code>TIMESTAMP_DATE_FORMAT</code>.
     * <p>
     * Those files created before this date will not be processed.
     *
     * @param timestamp the date string for filtering files
     * @return this object
     */
    CrawlerConfig setTimestamp(final String timestampString) throws ParseException {
        TIMESTAMP_FORMAT.parse(timestampString);
        return this;
    }

    /**
     * Set a date for filtering files based on the modification date of a timestamp file.
     *
     * @param timestampFile the timestamp file for date filtering
     * @return this object
     */
    CrawlerConfig setTimestampFile(final File timestampFile) {
        this.timestampFile = timestampFile;
        return this;
    }

    /**
     * Set whether the run database should be updated in the job.
     * <p>
     * This will not allow replacement of existing run log records. The {@link #allowUpdates()} flag must be on for this
     * be allowed.
     *
     * @param updateRunLog <code>true</code> if the run database should be updated
     * @return this object
     */
    CrawlerConfig setUpdateRunLog(final boolean updateRunLog) {
        this.updateRunLog = updateRunLog;
        return this;
    }

    /**
     * Set whether file caching using the 'jcache' program should be enabled.
     * <p>
     * This is only relevant for jobs run at JLAB.
     *
     * @param useFileCache <code>true</code> to allow file caching
     * @return this object
     */
    CrawlerConfig setUseFileCache(final boolean useFileCache) {
        this.useFileCache = useFileCache;
        return this;
    }

    /**
     * Set the max wait time in seconds for all file caching operations to complete.
     * <p>
     * If this time is exceeded then the job will fail with an error.
     *
     * @param waitTime the max wait time in seconds allowed for file caching to complete
     * @return this object
     */
    CrawlerConfig setWaitTime(final long waitTime) {
        this.waitTime = waitTime;
        return this;
    }

    /**
     * Get the timestamp for file filtering.
     * <p>
     * Files older than this will not be included in the job.
     *
     * @return the timestamp for file filtering
     */
    Date timestamp() {
        return timestamp;
    }

    /**
     * Get the timestamp file using for filtering EVIO files.
     *
     * @return the timestamp file used for filtering EVIO files (can be null)
     */
    File timestampFile() {
        return timestampFile;
    }

    /**
     * Return <code>true</code> if the run database should be updated.
     *
     * @return <code>true</code> if the run database should be updated
     */
    boolean updateRunDatabase() {
        return updateRunLog;
    }

    /**
     * Return <code>true</code> if file caching should be enabled.
     *
     * @return <code>true</code> if file caching should be enabled
     */
    boolean useFileCache() {
        return useFileCache;
    }

    /**
     * Get the max wait time in seconds to allow for file caching operations to complete.
     *
     * @return the max wait time in seconds to allow for file caching operations to complete
     */
    Long waitTime() {
        return waitTime;
    }
}
