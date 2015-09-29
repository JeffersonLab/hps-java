package org.hps.crawler;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.hps.conditions.database.ConnectionParameters;
import org.hps.datacat.client.DatasetFileFormat;
import org.hps.datacat.client.DatasetSite;

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
     * The database connection parameters which must be provided by a command line argument.
     */
    private ConnectionParameters connectionParameters;

    /**
     * The name of the folder in the data catalog for inserting data (under "/HPS" root folder).
     * <p>
     * Default provided for Eng Run 2015 data.
     */
    private String datacatFolder = null;

    /**
     * Set whether extraction of metadata from files is enabled.
     */
    private boolean enableMetadata;

    /**
     * Set of file formats for filtering files.
     */
    Set<DatasetFileFormat> formats = new HashSet<DatasetFileFormat>();

    /**
     * The maximum depth to crawl.
     */
    private Integer maxDepth = Integer.MAX_VALUE;

    /**
     * The root directory to search for files, which defaults to the current directory.
     */
    private File rootDir = new File(System.getProperty("user.dir"));

    /**
     * The dataset site for the datacat.
     */
    private DatasetSite site;

    /**
     * A timestamp to use for filtering input files on their creation date.
     */
    private Date timestamp = null;

    /**
     * A file to use for getting the timestamp date.
     */
    private File timestampFile = null;

    /**
     * Get the set of runs that will be accepted for the job.
     *
     * @return the list of runs that will be accepted
     */
    Set<Integer> acceptRuns() {
        return acceptRuns;
    }

    /**
     * Add the default file formats.
     */
    CrawlerConfig addDefaultFileFormats() {
        final List<DatasetFileFormat> defaultFormats = Arrays.asList(DatasetFileFormat.values());
        this.formats.addAll(defaultFormats);
        return this;
    }

    /**
     * Add a file format for filtering.
     *
     * @param format the file format
     */
    CrawlerConfig addFileFormat(final DatasetFileFormat format) {
        this.formats.add(format);
        return this;
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
     * Get the data catalog folder.
     *
     * @return the data catalog folder
     */
    String datacatFolder() {
        return this.datacatFolder;
    }

    /**
     * Get the dataset site.
     *
     * @return the dataset site
     */
    DatasetSite datasetSite() {
        return this.site;
    }

    /**
     * Return <code>true</code> if metadata extraction from files is enabled.
     *
     * @return <code>true</code> if metadata extraction is enabled
     */
    boolean enableMetaData() {
        return this.enableMetadata;
    }

    /**
     * Get the file formats for filtering.
     *
     * @return the file formats for filtering
     */
    Set<DatasetFileFormat> getFileFormats() {
        return this.formats;
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
     * Set the data catalog folder.
     *
     * @param datacatFolder the data catalog folder
     */
    CrawlerConfig setDatacatFolder(final String datacatFolder) {
        this.datacatFolder = datacatFolder;
        return this;
    }

    /**
     * Set the dataset site.
     *
     * @return this object
     */
    void setDatasetSite(final DatasetSite site) {
        this.site = site;
    }

    /**
     * Set whether metadata extraction is enabled.
     *
     * @param enableMetadata <code>true</code> to enable metadata
     * @return this object
     */
    CrawlerConfig setEnableMetadata(final boolean enableMetadata) {
        this.enableMetadata = enableMetadata;
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
}
