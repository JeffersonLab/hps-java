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
import org.hps.datacat.DatacatConstants;
import org.hps.datacat.FileFormat;
import org.hps.datacat.Site;

/**
 * Full configuration information for the {@link Crawler} class.
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
     */
    private String datacatFolder = null;

    /**
     * Set of accepted file formats.
     */
    private Set<FileFormat> formats = new HashSet<FileFormat>();

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
    private Site site = Site.JLAB;

    /**
     * A timestamp to use for filtering input files on their creation date.
     */
    private Date timestamp = null;

    /**
     * A file to use for getting the timestamp date.
     */
    private File timestampFile = null;
    
    /**
     * Dry run for not actually executing updates.
     */
    private boolean dryRun = false;
    
    /**
     * Base URL of datacat client.
     */
    private String baseUrl = DatacatConstants.DATACAT_URL;
        
    /**
     * Set of paths used for filtering files (file's path must match one of these).
     */
    private Set<String> paths = new HashSet<String>();

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
        final List<FileFormat> defaultFormats = Arrays.asList(FileFormat.values());
        this.formats.addAll(defaultFormats);
        return this;
    }

    /**
     * Add a file format for filtering.
     *
     * @param format the file format
     */
    void addFileFormat(final FileFormat format) {
        this.formats.add(format);
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
    String folder() {
        return this.datacatFolder;
    }

    /**
     * Get the dataset site.
     *
     * @return the dataset site
     */
    Site site() {
        return this.site;
    }

    /**
     * Get the file formats for filtering.
     *
     * @return the file formats for filtering
     */
    Set<FileFormat> getFileFormats() {
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
     * Get the root directory in the file catalog.
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
    void setAcceptRuns(final Set<Integer> acceptRuns) {
        this.acceptRuns = acceptRuns;
    }

    /**
     * Set the database connection parameters.
     *
     * @param connectionParameters the database connection parameters
     * @return this object
     */
    void setConnection(final ConnectionParameters connectionParameters) {
        this.connectionParameters = connectionParameters;
    }

    /**
     * Set the data catalog folder.
     *
     * @param datacatFolder the data catalog folder
     */
    void setDatacatFolder(final String datacatFolder) {
        this.datacatFolder = datacatFolder;
    }

    /**
     * Set the dataset site.
     *
     * @return this object
     */
    void setSite(final Site site) {
        this.site = site;
    }
    
    /**
     * Enable dry run.
     * 
     * @param dryRun set to <code>true</code> to enable dry run
     * @return this object
     */
    void setDryRun(boolean dryRun) {
        this.dryRun = dryRun;
    }
    

    /**
     * Set the max depth.
     *
     * @param maxDepth the max depth
     */
    void setMaxDepth(final Integer maxDepth) {
        this.maxDepth = maxDepth;
    }

    /**
     * Set the root directory for the file search.
     *
     * @param rootDir the root directory for the file search
     * @return this object
     */
    void setRootDir(final File rootDir) {
        this.rootDir = rootDir;
    }

    /**
     * Set a date for filtering input files.
     * <p>
     * Those files created before this date will not be processed.
     *
     * @param timestamp the date for filtering files
     * @return this object
     */
    void setTimestamp(final Date timestamp) {
        this.timestamp = timestamp;
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
    void setTimestamp(final String timestampString) throws ParseException {
        TIMESTAMP_FORMAT.parse(timestampString);
    }

    /**
     * Set a date for filtering files based on the modification date of a timestamp file.
     *
     * @param timestampFile the timestamp file for date filtering
     * @return this object
     */
    void setTimestampFile(final File timestampFile) {
        this.timestampFile = timestampFile;
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
     * Returns <code>true</code> if dry run which means no updates will occur.
     * 
     * @return <code>true</code> if dry run
     */
    boolean dryRun() {
        return this.dryRun;
    }
    
    /**
     * Set the data catalog URL.
     * 
     * @param baseUrl the data catalog URL
     */
    void setDatacatUrl(String baseUrl) {
        this.baseUrl = baseUrl;        
    }
    
    /**
     * Get the data catalog URL.
     * 
     * @return the data catalog URL
     */
    String datacatUrl() {
        return this.baseUrl;
    }
        
    /**
     * Add a path for filtering files.
     * 
     * @param path the path for filtering
     */
    void addPath(String path) {
        this.paths.add(path);
    }
    
    /**
     * Get the list of paths for filtering. 
     * 
     * @return the list of paths for filtering
     */
    Set<String> paths() {
        return this.paths;
    }
}
