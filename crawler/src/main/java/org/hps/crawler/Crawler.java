package org.hps.crawler;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.hps.conditions.database.ConnectionParameters;
import org.hps.datacat.client.DatacatClient;
import org.hps.datacat.client.DatacatClientFactory;
import org.hps.datacat.client.DatasetFileFormat;
import org.hps.run.database.RunManager;
import org.hps.run.database.RunProcessor;
import org.hps.run.database.RunSummary;
import org.hps.run.database.RunSummaryImpl;
import org.lcsim.util.log.DefaultLogFormatter;
import org.lcsim.util.log.LogUtil;

/**
 * Crawls a directory tree for data files and performs tasks related to this information.
 * <p>
 * The crawler can find EVIO, LCIO, or ROOT files in a directory tree and then perform various tasks based on
 * information extracted from them.
 *
 * @author Jeremy McCormick, SLAC
 */
public final class Crawler {

    /**
     * Make a list of available features for printing help.
     */
    private static String AVAILABLE_FEATURES;

    /**
     * Make a list of available features for printing help.
     */
    private static String AVAILABLE_FORMATS;

    /**
     * Setup the logger.
     */
    private static final Logger LOGGER = LogUtil.create(Crawler.class, new DefaultLogFormatter(), Level.CONFIG);

    /**
     * Constant for milliseconds conversion.
     */
    private static final long MILLISECONDS = 1000L;
    /**
     * Command line options for the crawler.
     */
    private static final Options OPTIONS = new Options();

    static {
        final StringBuffer buffer = new StringBuffer();
        for (final CrawlerFeature feature : CrawlerFeature.values()) {
            buffer.append(feature.name() + " ");
        }
        buffer.setLength(buffer.length() - 1);
        AVAILABLE_FEATURES = buffer.toString();
    }
    static {
        final StringBuffer buffer = new StringBuffer();
        for (final DatasetFileFormat format : DatasetFileFormat.values()) {
            buffer.append(format.name() + " ");
        }
        buffer.setLength(buffer.length() - 1);
        AVAILABLE_FORMATS = buffer.toString();
    }

    /**
     * Statically define the command options.
     */
    static {
        OPTIONS.addOption("b", "min-date", true, "min date for a file (example \"2015-03-26 11:28:59\")");
        OPTIONS.addOption("c", "datacat", true, "use the specified datacat folder");
        OPTIONS.addOption("C", "cache", false, "cache files from MSS (JLAB only and not for batch farm use!)");
        OPTIONS.addOption("e", "enable", true, "enable a feature: " + AVAILABLE_FEATURES);
        OPTIONS.addOption("D", "default-features", false, "enable default features");
        OPTIONS.addOption("F", "default-formats", false, "enable default file filters");
        OPTIONS.addOption("f", "format", true, "add a file format for filtering: " + AVAILABLE_FORMATS);
        OPTIONS.addOption("p", "connection-properties", true, "database connection properties file (required)");
        OPTIONS.addOption("d", "directory", true, "root directory to start crawling (default is current dir)");
        OPTIONS.addOption("E", "evio-processor", true, "class name of an EvioEventProcessor to execute");
        OPTIONS.addOption("h", "help", false, "print help and exit (overrides all other arguments)");
        OPTIONS.addOption("L", "log-level", true, "set the log level (INFO, FINE, etc.)");
        OPTIONS.addOption("r", "run", true, "add a run number to accept (others will be excluded)");
        OPTIONS.addOption("t", "timestamp-file", true, "existing or new timestamp file name");
        OPTIONS.addOption("w", "max-cache-wait", true, "time per run allowed for file caching in seconds");
        OPTIONS.addOption("x", "max-depth", true, "max depth to crawl");
    }

    /**
     * Run the crawler from the command line.
     *
     * @param args the command line arguments
     */
    public static void main(final String[] args) {
        try {
            new Crawler().parse(args).run();
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * The class for managing the file caching using the 'jcache' command.
     */
    private final JCacheManager cacheManager = new JCacheManager();

    /**
     * Configuration options from the command line.
     */
    private CrawlerConfig config;

    /**
     * The options parser.
     */
    private final PosixParser parser = new PosixParser();

    /**
     * Cache all files and wait for the operation to complete.
     * <p>
     * Potentially, this operation can take a very long time. This can be managed using the
     * {@link JCacheManager#setWaitTime(long)} method to set a timeout.
     */
    private void cacheFiles(final RunSummary runSummary) {
        if (this.config.useFileCache()) {

            LOGGER.info("caching files for run " + runSummary.getRun());

            // Cache all the files and wait for the operation to complete.
            this.cacheManager.cache(runSummary.getEvioFiles());
            final boolean cached = this.cacheManager.waitForCache();

            // If the files weren't cached then die.
            if (!cached) {
                throw new RuntimeException("The cache process did not complete in time.");
            }

            LOGGER.info("done caching files from run " + runSummary.getRun());

        } else {
            LOGGER.info("file caching disabled");
        }

        LOGGER.getHandlers()[0].flush();
    }

    /**
     * Create a run processor from the current configuration.
     *
     * @return the run processor
     */
    private RunProcessor createEvioRunProcessor(final RunSummaryImpl runSummary) {

        final RunProcessor runProcessor = new RunProcessor(runSummary);

        final Set<CrawlerFeature> features = config.getFeatures();

        if (features.contains(CrawlerFeature.EPICS)) {
            runProcessor.addEpicsProcessor();
        }
        if (features.contains(CrawlerFeature.SCALERS)) {
            runProcessor.addScalerProcessor();
        }
        if (features.contains(CrawlerFeature.TRIGGER)) {
            runProcessor.addTriggerTimeProcessor();
        }

        return runProcessor;
    }

    /**
     * Parse command line options and create a new {@link Crawler} object from the configuration.
     *
     * @param args the command line arguments
     * @return the configured crawler object
     */
    private Crawler parse(final String args[]) {

        LOGGER.config("parsing command line options");

        this.config = new CrawlerConfig();

        try {
            final CommandLine cl = this.parser.parse(OPTIONS, args);

            // Print help.
            if (cl.hasOption("h")) {
                this.printUsage();
            }

            // Log level.
            if (cl.hasOption("L")) {
                final Level level = Level.parse(cl.getOptionValue("L"));
                LOGGER.config("setting log level to " + level);
                LOGGER.setLevel(level);
            }

            // Enable default features.
            if (cl.hasOption("D")) {
                LOGGER.config("enabling default features");
                this.config.addDefaultFeatures();
            }

            // Enable default file formats.
            if (cl.hasOption("F")) {
                LOGGER.config("enabling default file formats");
                this.config.addDefaultFileFormats();
            }

            // Root directory for file crawling.
            if (cl.hasOption("d")) {
                final File rootDir = new File(cl.getOptionValue("d"));
                if (!rootDir.exists()) {
                    throw new IllegalArgumentException("The directory does not exist.");
                }
                if (!rootDir.isDirectory()) {
                    throw new IllegalArgumentException("The specified path is not a directory.");
                }
                config.setRootDir(rootDir);
                LOGGER.config("root dir set to " + config.rootDir());
            }

            // Database connection properties file (this is not optional).
            // FIXME: This only needs to be set for updating the run database.
            if (cl.hasOption("p")) {
                final String dbPropPath = cl.getOptionValue("p");
                final File dbPropFile = new File(dbPropPath);
                if (!dbPropFile.exists()) {
                    throw new IllegalArgumentException("Connection properties file " + dbPropFile.getPath()
                            + " does not exist.");
                }
                final ConnectionParameters connectionParameters = ConnectionParameters.fromProperties(dbPropFile);
                config.setConnection(connectionParameters);
                LOGGER.config("using " + dbPropPath + " for db connection properties");
            } else {
                throw new RuntimeException(
                        "The -p switch providing the database connection properties file is a required argument.");
            }

            // Timestamp file for date filtering.
            if (cl.hasOption("t")) {
                final File timestampFile = new File(cl.getOptionValue("t"));
                config.setTimestampFile(timestampFile);
                if (!timestampFile.exists()) {
                    try {
                        // Create new time stamp file which will have its date updated at the end of the job.
                        LOGGER.config("creating new timestamp file " + timestampFile.getPath());
                        timestampFile.createNewFile();
                    } catch (final IOException e) {
                        throw new IllegalArgumentException("Error creating timestamp file: " + timestampFile.getPath());
                    }
                } else {
                    try {
                        // Get the date filter for files from an existing time stamp file provided by the user.
                        final Date timestamp = new Date(Files
                                .readAttributes(config.timestampFile().toPath(), BasicFileAttributes.class)
                                .lastModifiedTime().toMillis());
                        config.setTimestamp(timestamp);
                        LOGGER.config("got timestamp " + timestamp + " from existing file "
                                + config.timestampFile().getPath());
                    } catch (final IOException e) {
                        throw new RuntimeException("Error getting attributes of timestamp file.", e);
                    }
                }
            }

            // List of one or more runs to accept in the job.
            if (cl.hasOption("r")) {
                final Set<Integer> acceptRuns = new HashSet<Integer>();
                for (final String runString : cl.getOptionValues("r")) {
                    final Integer acceptRun = Integer.parseInt(runString);
                    acceptRuns.add(acceptRun);
                    LOGGER.config("added run filter " + acceptRun);
                }
                config.setAcceptRuns(acceptRuns);
            }

            // Enable file cache usage for running at JLAB.
            if (cl.hasOption("C")) {
                config.setUseFileCache(true);
                LOGGER.config("file cache is enabled");
            }

            // Max wait time for file caching.
            if (cl.hasOption("w")) {
                final Long waitTime = Long.parseLong(cl.getOptionValue("w")) * MILLISECONDS;
                config.setWaitTime(waitTime);
                LOGGER.config("max time for file caching set to " + config.waitTime());
            }

            // User supplied timestamp string that is converted to a date for file filtering.
            if (cl.hasOption("b")) {
                try {
                    if (config.timestamp() != null) {
                        LOGGER.warning("existing timestamp from file " + config.timestamp()
                                + " will be overridden by date from -b argument");
                    }
                    config.setTimestamp(cl.getOptionValue("b"));
                    LOGGER.config("set timestamp to " + config.timestamp() + " from -b argument");
                } catch (final java.text.ParseException e) {
                    throw new RuntimeException(e);
                }
            }

            // User supplied EvioEventProcessor classes to run in the run processing step.
            if (cl.hasOption("E")) {
                final String[] classNames = cl.getOptionValues("E");
                for (final String className : classNames) {
                    try {
                        LOGGER.config("adding extra EVIO processor " + className);
                        config.addProcessor(className);
                    } catch (final Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            }

            // Max depth to crawl.
            if (cl.hasOption("x")) {
                final Integer maxDepth = Integer.parseInt(cl.getOptionValue("x"));
                if (maxDepth < 1) {
                    throw new IllegalArgumentException("invalid -x argument for maxDepth: " + maxDepth);
                }
                config.setMaxDepth(maxDepth);
                LOGGER.config("set max depth to " + maxDepth);
            }

            // Update data catalog.
            if (cl.hasOption("c")) {
                final String datacatFolder = cl.getOptionValue("c");
                if (datacatFolder == null) {
                    throw new IllegalArgumentException("missing -c argument with data catalog folder");
                }

                // Set datacat folder.
                LOGGER.config("using data catalog folder " + datacatFolder);
                config.setDatacatFolder(datacatFolder);

                // Assume datacat should be enabled if folder name was given.
                config.getFeatures().add(CrawlerFeature.DATACAT);
                LOGGER.config(CrawlerFeature.DATACAT + " is enabled");
            }

            // Configure enabled features.
            if (cl.hasOption("e")) {
                for (final String arg : cl.getOptionValues("e")) {
                    CrawlerFeature feature = null;
                    try {
                        feature = CrawlerFeature.valueOf(arg);
                    } catch (IllegalArgumentException | NullPointerException e) {
                        throw new IllegalArgumentException("The feature " + arg + " is not valid.", e);
                    }
                    this.config.addFeature(feature);
                }
            }

            // Configure enabled file formats.
            if (cl.hasOption("f")) {
                for (final String arg : cl.getOptionValues("f")) {
                    DatasetFileFormat format = null;
                    try {
                        format = DatasetFileFormat.valueOf(arg);
                    } catch (IllegalArgumentException | NullPointerException e) {
                        throw new IllegalArgumentException("The feature " + arg + " is not valid.", e);
                    }
                    LOGGER.config("adding format " + format.name());
                    this.config.addFileFormat(format);
                }
            }
        } catch (final ParseException e) {
            throw new RuntimeException("Error parsing options.", e);
        }

        // Check that there is at least one file format enabled for filtering.
        if (this.config.getFileFormats().isEmpty()) {
            throw new IllegalStateException(
                    "There are no file formats enabled.  Enable defaults using -F or add a format using the -f switch.");
        }

        // Print a message if no features are enabled; this is not a fatal error but the job won't update anything.
        if (this.config.getFeatures().isEmpty()) {
            LOGGER.warning("no features are enabled");
        }

        // Check that EVIO file filter is active if run database is being updated.
        // Don't add by default because the user may have made a mistake in the options they provided.
        if (this.config.getFeatures().contains(CrawlerFeature.RUNDB_INSERT)
                && !this.config.getFileFormats().contains(DatasetFileFormat.EVIO)) {
            throw new IllegalStateException("Run database is enabled without EVIO file filter active.");
        }

        // Configure the max wait time for file caching operations.
        if (config.waitTime() != null && config.waitTime() > 0L) {
            cacheManager.setWaitTime(config.waitTime());
        }

        LOGGER.info("done parsing command line options");
        LOGGER.getHandlers()[0].flush();

        return this;
    }

    /**
     * Print the usage statement for this tool to the console and then exit the program.
     */
    private void printUsage() {
        final HelpFormatter help = new HelpFormatter();
        // FIXME: include more info here and improve the way this looks (line width should be increased)
        help.printHelp("Crawler [options]", "", OPTIONS, "");
        System.exit(0);
    }

    /**
     * Process a run using its run summary.
     *
     * @param runs the run log containing the list of run summaries
     * @throws Exception if there is an error processing one of the runs
     */
    private RunProcessor processRun(final RunSummary runSummary) throws Exception {

        LOGGER.info("processing run" + runSummary.getRun());

        // Create a processor to process all the EVIO events in the run.
        LOGGER.info("creating run processor for " + runSummary.getRun());
        final RunProcessor runProcessor = this.createEvioRunProcessor((RunSummaryImpl) runSummary);

        // Process all of the files from the run.
        LOGGER.info("processing run " + runSummary.getRun());
        runProcessor.processRun();

        LOGGER.getHandlers()[0].flush();

        return runProcessor;
    }

    /**
     * Run the full crawler job.
     *
     * @throws Exception if there is some error during the job
     */
    public void run() throws Exception {

        LOGGER.info("starting Crawler job");

        // Create the file visitor for crawling the root directory with the given date filter.
        final CrawlerFileVisitor visitor = new CrawlerFileVisitor();

        if (config.timestamp() != null) {
            // Add date filter if timestamp is supplied.
            visitor.addFilter(new DateFileFilter(config.timestamp()));
        }

        // Add file format filter.
        for (final DatasetFileFormat fileFormat : config.getFileFormats()) {
            LOGGER.info("adding file format filter for " + fileFormat.name());
        }
        visitor.addFilter(new FileFormatFilter(config.getFileFormats()));

        // Walk the file tree using the visitor.
        this.walk(visitor);

        // Get the list of run data created by the visitor.
        final RunSummaryMap runMap = visitor.getRunMap();

        LOGGER.info("found " + runMap.size() + " runs from crawl job");

        // Process all runs that were found.
        for (final RunSummary runSummary : runMap.getRunSummaries()) {

            if (runSummary == null) {
                throw new IllegalArgumentException("The run summary is null for some weird reason.");
            }

            LOGGER.info("starting processing of run " + runSummary.getRun());

            // Cache files from MSS.
            this.cacheFiles(runSummary);

            // Process the run's EVIO files.
            if (!runSummary.getFiles(DatasetFileFormat.EVIO).isEmpty()) {
                final RunProcessor runProcessor = this.processRun(runSummary);
            }

            if (config.getFeatures().contains(CrawlerFeature.RUNDB_INSERT)) {
                // Execute the run database update.
                this.updateRunDatabase(runSummary);
            } else {
                LOGGER.info("updating run database is not enabled");
            }

            // Update the data catalog.
            if (this.config.getFeatures().contains(CrawlerFeature.DATACAT)) {
                this.updateDatacat(runSummary);
            }

            LOGGER.info("completed full processing of run " + runSummary);
        }

        // Update the timestamp output file.
        this.updateTimestamp();

        LOGGER.getHandlers()[0].flush();

        LOGGER.info("Crawler job is done!");
    }

    /**
     * Update the data catalog.
     *
     * @param runMap the map of run information including the EVIO file list
     */
    private void updateDatacat(final RunSummary runSummary) {
        final DatacatClient datacatClient = new DatacatClientFactory().createClient();
        for (final DatasetFileFormat fileFormat : config.getFileFormats()) {
            LOGGER.info("adding files to datacat with format " + fileFormat.name());
            for (final File file : runSummary.getFiles(fileFormat)) {

                LOGGER.info("adding file " + file.getPath() + " to datacat");

                // Get folder for file by stripping out root directory.
                final String folder = DatacatUtilities.getFolder(config.rootDir().getPath(), file);

                // Register file in the catalog.
                // DatacatUtilities.addFile(datacatClient, folder, file);
            }
        }
    }

    /**
     * Update the database with information found from crawling the files.
     *
     * @param runs the list of runs to update
     * @throws SQLException if there is a database query error
     */
    private void updateRunDatabase(final RunSummary runSummary) throws SQLException {

        LOGGER.info("updating run database for run " + runSummary.getRun());

        // Open a DB connection.
        final Connection connection = config.connectionParameters().createConnection();

        // Create factory for interfacing to run database.
        final RunManager runManager = new RunManager(connection);
        runManager.setRun(runSummary.getRun());

        // Delete existing run summary if necessary.
        if (runManager.runExists()) {
            if (this.config.features.contains(CrawlerFeature.RUNDB_UPDATE)) {
                LOGGER.info("deleting existing information for run " + runSummary.getRun());
                runManager.deleteRun();
            }
        } else {
            throw new RuntimeException("Run " + runSummary.getRun()
                    + " exists in database and deletion is not enabled.");
        }
    
        runManager.closeConnection();

        LOGGER.info("done updating run database");

        LOGGER.getHandlers()[0].flush();
    }

    /**
     * Update the timestamp file's modification date to the time when this job ended.
     * <p>
     * This can be then be used in subsequent crawl jobs to filter out files that have already been processed.
     */
    private void updateTimestamp() {
        if (config.timestampFile() == null) {

            LOGGER.info("creating default timestamp file");

            config.setTimestampFile(new File("timestamp"));
            try {
                config.timestampFile().createNewFile();
            } catch (final IOException e) {
                throw new RuntimeException(e);
            }
        }
        config.timestampFile().setLastModified(System.currentTimeMillis());
        LOGGER.info("touching timestamp file " + config.timestampFile().getPath());
        LOGGER.info("set modified on timestamp file: " + new Date(config.timestampFile().lastModified()));
    }

    /**
     * Walk the directory tree to find EVIO files for the runs that are being processed in the job.
     *
     * @param visitor the file visitor
     */
    private void walk(final CrawlerFileVisitor visitor) {
        if (config.timestamp() != null) {
            // Date filter from timestamp.
            visitor.addFilter(new DateFileFilter(config.timestamp()));
            LOGGER.config("added date filter with time stamp " + config.timestamp());
        }

        // Is the accept run list not empty? (Empty means accept all runs.)
        if (!config.acceptRuns().isEmpty()) {
            // List of run numbers to accept.
            visitor.addFilter(new RunFilter(config.acceptRuns()));
            LOGGER.config("added run number filter");
        } else {
            LOGGER.config("no run number filter will be used");
        }

        try {
            // Walk the file tree from the root directory.
            final EnumSet<FileVisitOption> options = EnumSet.noneOf(FileVisitOption.class);
            Files.walkFileTree(config.rootDir().toPath(), options, config.maxDepth(), visitor);
        } catch (final IOException e) {
            throw new RuntimeException("Error while walking the directory tree.", e);
        }
    }
}
