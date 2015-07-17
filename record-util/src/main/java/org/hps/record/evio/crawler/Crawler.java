package org.hps.record.evio.crawler;

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
import org.lcsim.util.log.DefaultLogFormatter;
import org.lcsim.util.log.LogUtil;

/**
 * Search for EVIO files in a directory tree, group the files that are found by run, extract meta data from these files,
 * and optionally update a run database with the information that was found.
 *
 * @author Jeremy McCormick, SLAC
 */
public final class Crawler {

    /**
     * Setup the logger.
     */
    private static final Logger LOGGER = LogUtil.create(Crawler.class, new DefaultLogFormatter(), Level.ALL);

    /**
     * Constant for milliseconds conversion.
     */
    private static final long MILLISECONDS = 1000L;

    /**
     * Command line options for the crawler.
     */
    private static final Options OPTIONS = new Options();

    /**
     * Statically define the command options.
     */
    static {
        OPTIONS.addOption("a", "runs", true, "list of run numbers to accept (others will be excluded)");
        OPTIONS.addOption("b", "min-date", true, "min date for a file (example \"2015-03-26 11:28:59\")");
        OPTIONS.addOption("c", "cache", false, "automatically cache files from MSS to cache disk (JLAB only)");
        OPTIONS.addOption("C", "connection-properties", true, "database connection properties file (required)");
        OPTIONS.addOption("d", "directory", true, "root directory to start crawling (default is current dir)");
        OPTIONS.addOption("E", "evio-processor", true, "class name of an EvioEventProcessor to execute");
        OPTIONS.addOption("h", "help", false, "print help and exit (overrides all other arguments)");
        OPTIONS.addOption("m", "max-files", true, "max number of files to process per run (mostly for debugging)");
        OPTIONS.addOption("p", "print", true, "set event printing interval during EVIO processing");
        OPTIONS.addOption("r", "insert", false, "insert information into the run database (not done by default)");
        OPTIONS.addOption("t", "timestamp-file", true, "existing or new timestamp file name");
        OPTIONS.addOption("w", "max-cache-wait", true, "total time to allow for file caching (seconds)");
        OPTIONS.addOption("L", "log-level", true, "set the log level (INFO, FINE, etc.)");
        OPTIONS.addOption("u", "update", false, "allow overriding existing data in the run db (not allowed by default)");
        OPTIONS.addOption("x", "max-depth", true, "max depth to crawl in the directory tree");
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
     * Parse command line options and create a new {@link Crawler} object from the configuration.
     *
     * @param args the command line arguments
     * @return the configured crawler object
     */
    private Crawler parse(final String args[]) {

        LOGGER.info("parsing command line options");

        config = new CrawlerConfig();

        try {
            final CommandLine cl = this.parser.parse(OPTIONS, args);

            // Print help.
            if (cl.hasOption("h")) {
                this.printUsage();
            }

            // Log level.
            if (cl.hasOption("L")) {
                final Level level = Level.parse(cl.getOptionValue("L"));
                LOGGER.info("setting log level to " + level);
                LOGGER.setLevel(level);
            }

            // Database connection properties file (this is not optional).
            if (cl.hasOption("C")) {
                final String dbPropPath = cl.getOptionValue("C");
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
                        "The -C switch providing the database connection properties file is a required argument.");
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
                LOGGER.config("root dir for crawling set to " + config.rootDir());
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
            if (cl.hasOption("a")) {
                final Set<Integer> acceptRuns = new HashSet<Integer>();
                for (final String runString : cl.getOptionValues("a")) {
                    final Integer acceptRun = Integer.parseInt(runString);
                    acceptRuns.add(acceptRun);
                    LOGGER.config("added run filter " + acceptRun);
                }
                config.setAcceptRuns(acceptRuns);
            }

            // Enable run log updating (off by default).
            if (cl.hasOption("r")) {
                config.setUpdateRunLog(true);
                LOGGER.config("inserting into run database is enabled");
            }

            // Enable file cache usage for running at JLAB.
            if (cl.hasOption("c")) {
                config.setUseFileCache(true);
                LOGGER.config("file cache is enabled");
            }

            // Max wait time for file caching.
            if (cl.hasOption("w")) {
                final Long waitTime = Long.parseLong(cl.getOptionValue("w")) * MILLISECONDS;
                config.setWaitTime(waitTime);
                LOGGER.config("max time for file caching set to " + config.waitTime());
            }

            // Max files to process per run; mostly just here for debugging purposes.
            if (cl.hasOption("m")) {
                final int maxFiles = Integer.parseInt(cl.getOptionValue("m"));
                config.setMaxFiles(maxFiles);
                LOGGER.config("max files set to " + maxFiles);
            }

            // Event printing interval when doing EVIO event processing.
            if (cl.hasOption("p")) {
                final int eventPrintInterval = Integer.parseInt(cl.getOptionValue("p"));
                config.setEventPrintInterval(eventPrintInterval);
                LOGGER.config("event print interval set to " + eventPrintInterval);
            }

            // Flag to allow replacement of existing records in the database; not allowed by default.
            if (cl.hasOption("u")) {
                config.setAllowUpdates(true);
                LOGGER.config("deletion and replacement of existing runs in the database is enabled");
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
            }

        } catch (final ParseException e) {
            throw new RuntimeException("Error parsing options.", e);
        }

        LOGGER.info("done parsing command line options");

        return this;
    }

    /**
     * Print the usage statement for this tool to the console and then exit the program.
     */
    private void printUsage() {
        final HelpFormatter help = new HelpFormatter();
        help.printHelp("EvioFileCrawler", "", OPTIONS, "");
        System.exit(0);
    }

    /**
     * Run the full crawler job.
     * <p>
     * This might take quite a long time!
     *
     * @throws Exception if there is some error during the job
     */
    public void run() throws Exception {

        LOGGER.info("running Crawler job");

        // Create the file visitor for crawling the root directory with the given date filter.
        final EvioFileVisitor visitor = new EvioFileVisitor(config.timestamp());

        // Walk the file tree using the visitor.
        this.walk(visitor);

        // Get the list of run data created by the visitor.
        final RunLog runs = visitor.getRunLog();

        // Print the run numbers that were found.
        runs.printRunNumbers();

        // Sort the files on their sequence numbers.
        runs.sortFiles();

        // Process all the files, performing caching from the MSS if necessary.
        RunProcessor.processRuns(this.cacheManager, runs, config);

        // Print the summary information after the run processing is done.
        runs.printRunSummaries();

        // Execute the run database update.
        this.updateRunDatabase(runs);

        // Update the timestamp output file.
        this.updateTimestamp();

        LOGGER.info("Crawler job is done!");
    }

    /**
     * Update the database with information found from crawling the files.
     *
     * @param runs the list of runs to update
     * @throws SQLException if there is a database query error
     */
    private void updateRunDatabase(final RunLog runs) throws SQLException {
        // Insert the run information into the database.
        if (config.updateRunLog()) {

            // Open a DB connection.
            final Connection connection = config.connectionParameters().createConnection();

            // Create and configure RunLogUpdater which updates the run log for all runs found in the crawl job.
            final RunLogUpdater runUpdater = new RunLogUpdater(connection, runs, config.allowUpdates());

            // Update the DB.
            runUpdater.insert();

            // Close the DB connection.
            connection.close();
        }
    }

    /**
     * Update the timestamp file's modification date to the time when this job ended.
     * <p>
     * This can be then be used in subsequent crawl jobs to filter out files that have already been seen.
     */
    private void updateTimestamp() {
        // Update the timestamp file which can be used to tell which files have been processed by their creation date.
        if (config.timestampFile() == null) {
            config.setTimestampFile(new File("timestamp"));
            try {
                config.timestampFile().createNewFile();
            } catch (final IOException e) {
                throw new RuntimeException(e);
            }
            LOGGER.config("created new timestamp file: " + config.timestampFile().getPath());
        }
        config.timestampFile().setLastModified(System.currentTimeMillis());
        LOGGER.config("set modified on timestamp file: " + new Date(config.timestampFile().lastModified()));
    }

    /**
     * Walk the directory tree to find EVIO files for the runs that are being processed in the job.
     *
     * @param visitor the file visitor
     */
    private void walk(final EvioFileVisitor visitor) {
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
            LOGGER.config("no run number filter used");
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
