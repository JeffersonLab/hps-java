package org.hps.record.evio.crawler;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
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
import org.lcsim.util.log.DefaultLogFormatter;
import org.lcsim.util.log.LogUtil;

/**
 * Crawls EVIO files in a directory tree, groups the files that are found by run, and optionally performs various tasks based on the run summary
 * information that is accumulated, including printing a summary, caching the files from JLAB MSS, and updating a run database.
 *
 * @author <a href="mailto:jeremym@slac.stanford.edu">Jeremy McCormick</a>
 */
public final class EvioFileCrawler {

    /**
     * Setup logger.
     */
    private static final Logger LOGGER = LogUtil.create(EvioFileCrawler.class, new DefaultLogFormatter(), Level.ALL);

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
        OPTIONS.addOption("h", "help", false, "print help and exit");
        OPTIONS.addOption("t", "timestamp-file", true, "timestamp file for date filtering; modified time will be set at end of job");
        OPTIONS.addOption("d", "directory", true, "starting directory");
        OPTIONS.addOption("r", "runs", true, "list of runs to accept (others will be excluded)");
        OPTIONS.addOption("s", "summary", false, "print run summary at end of job");
        OPTIONS.addOption("L", "log-level", true, "set log level (INFO, FINE, etc.)");
        OPTIONS.addOption("u", "update", false, "update the run database");
        OPTIONS.addOption("e", "epics", false, "process EPICS data");
        OPTIONS.addOption("c", "cache", false, "automatically cache all files from MSS");
        OPTIONS.addOption("w", "wait", true, "total time in seconds to allow for file caching");
        OPTIONS.addOption("m", "max-files", true, "maximum number of files to accept per run (for debugging)");
        OPTIONS.addOption("p", "print", true, "set event printing interval when running EVIO processors");
    }

    /**
     * Support running the crawler from the command line.
     *
     * @param args the command line arguments
     */
    public static void main(final String[] args) {
        try {
            new EvioFileCrawler().parse(args).run();
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * A list of run numbers to accept in the job.
     */
    private final Set<Integer> acceptRuns = new HashSet<Integer>();

    /**
     * The class for managing the file caching using the jcache command.
     */
    private final JCacheManager cacheManager = new JCacheManager();

    /**
     * Default event print interval.
     */
    private final int DEFAULT_EVENT_PRINT_INTERVAL = 1000;

    /**
     * Flag indicating whether EPICS data banks should be processed.
     */
    private boolean epics = false;

    /**
     * Interval for printing out event number while running EVIO processors.
     */
    private int eventPrintInterval = DEFAULT_EVENT_PRINT_INTERVAL;

    /**
     * The maximum number of files to
     */
    private int maxFiles = -1;

    /**
     * The options parser.
     */
    private final PosixParser parser = new PosixParser();

    /**
     * Flag indicating whether the run summaries should be printed (may result in some extra file processing).
     */
    private boolean printSummary = false;

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
    private boolean update = false;

    /**
     * Flag indicating if the file cache should be used (e.g. jcache automatically executed to move files to the cache disk from tape).
     */
    private boolean useFileCache = false;

    /**
     * The maximum wait time in milliseconds to allow for file caching operations.
     */
    private Long waitTime;

    /**
     * Create the processor for a single run.
     *
     * @param runSummary the run summary for the run
     * @return the run processor
     */
    private RunProcessor createRunProcessor(final RunSummary runSummary) {
        final RunProcessor processor = new RunProcessor(runSummary, this.cacheManager);
        if (this.epics) {
            processor.addProcessor(new EpicsLog(runSummary));
        }
        if (this.printSummary) {
            processor.addProcessor(new EventTypeLog(runSummary));
        }
        if (this.maxFiles != -1) {
            processor.setMaxFiles(this.maxFiles);
        }
        processor.useFileCache(this.useFileCache);
        processor.setEventPrintInterval(this.eventPrintInterval);
        return processor;
    }

    /**
     * Parse command line options, but do not start the job.
     *
     * @param args the command line arguments
     * @return the configured crawler object
     */
    private EvioFileCrawler parse(final String args[]) {
        try {
            final CommandLine cl = this.parser.parse(OPTIONS, args);

            if (cl.hasOption("h")) {
                this.printUsage();
            }

            if (cl.hasOption("L")) {
                final Level level = Level.parse(cl.getOptionValue("L"));
                LOGGER.info("setting log level to " + level);
                LOGGER.setLevel(level);
            }

            if (cl.hasOption("d")) {
                this.rootDir = new File(cl.getOptionValue("d"));
                if (!this.rootDir.exists()) {
                    throw new IllegalArgumentException("The directory does not exist.");
                }
                if (!this.rootDir.isDirectory()) {
                    throw new IllegalArgumentException("The specified path is not a directory.");
                }
            }

            if (cl.hasOption("t")) {
                this.timestampFile = new File(cl.getOptionValue("t"));
                if (!this.timestampFile.exists()) {
                    throw new IllegalArgumentException("The timestamp file does not exist: " + this.timestampFile.getPath());
                }
                try {
                    this.timestamp = new Date(Files.readAttributes(this.timestampFile.toPath(), BasicFileAttributes.class).lastModifiedTime()
                            .toMillis());
                } catch (final IOException e) {
                    throw new RuntimeException("Error getting attributes of timestamp file.", e);
                }
            }

            if (cl.hasOption("r")) {
                for (final String runString : cl.getOptionValues("r")) {
                    final Integer acceptRun = Integer.parseInt(runString);
                    this.acceptRuns.add(acceptRun);
                    LOGGER.config("added accept run " + acceptRun);
                }
            }

            if (cl.hasOption("s")) {
                this.printSummary = true;
            }

            if (cl.hasOption("u")) {
                this.update = true;
            }

            if (cl.hasOption("e")) {
                this.epics = true;
            }

            if (cl.hasOption("c")) {
                this.useFileCache = true;
            }

            if (cl.hasOption("w")) {
                this.waitTime = Long.parseLong(cl.getOptionValue("w")) * MILLISECONDS;
                if (this.waitTime > 0L) {
                    this.cacheManager.setWaitTime(this.waitTime);
                }
            }

            if (cl.hasOption("m")) {
                this.maxFiles = Integer.parseInt(cl.getOptionValue("m"));
            }

            if (cl.hasOption("p")) {
                this.eventPrintInterval = Integer.parseInt(cl.getOptionValue("p"));
            }

        } catch (final ParseException e) {
            throw new RuntimeException("Error parsing options.", e);
        }

        return this;
    }

    /**
     * Print the usage statement for this tool to the console.
     */
    private void printUsage() {
        final HelpFormatter help = new HelpFormatter();
        help.printHelp("EvioFileCrawler", "", OPTIONS, "");
        System.exit(0);
    }

    /**
     * Process a single run.
     *
     * @param runSummary the run summary information
     * @throws Exception if there is some error while running the file processing
     */
    private void processRun(final RunSummary runSummary) throws Exception {

        // Clear the cache manager.
        this.cacheManager.clear();

        // Create a processor to process all the EVIO events in the run.
        final RunProcessor processor = this.createRunProcessor(runSummary);

        // Process all of the runs files.
        processor.process();
    }

    /**
     * Process all runs that were found.
     *
     * @param runs the run log containing the list of run summaries
     * @throws Exception if there is an error processing one of the runs
     */
    private void processRuns(final RunLog runs) throws Exception {
        // Process all of the runs that were found.
        for (final int run : runs.getSortedRunNumbers()) {
            this.processRun(runs.getRunSummary(run));
        }
    }

    /**
     * Run the crawler job (generally will take a long time!).
     *
     * @throws Exception if there is some error during the job
     */
    public void run() throws Exception {
        final EnumSet<FileVisitOption> options = EnumSet.noneOf(FileVisitOption.class);
        final EvioFileVisitor visitor = new EvioFileVisitor(this.timestamp);
        if (this.timestamp != null) {
            visitor.addFilter(new DateFileFilter(this.timestamp));
            LOGGER.config("added date filter with timestamp " + this.timestamp);
        }
        if (!this.acceptRuns.isEmpty()) {
            visitor.addFilter(new RunFilter(this.acceptRuns));
            LOGGER.config("added run filter");
        }
        try {
            // Walk the file tree from the root directory.
            Files.walkFileTree(this.rootDir.toPath(), options, Integer.MAX_VALUE, visitor);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }

        final RunLog runs = visitor.getRunLog();

        // Print run numbers that were found.
        final StringBuffer sb = new StringBuffer();
        for (final Integer run : runs.getSortedRunNumbers()) {
            sb.append(run + " ");
        }
        LOGGER.info("found files from runs: " + sb.toString());

        // Sort files on their sequence numbers.
        LOGGER.fine("sorting files by sequence ...");
        runs.sortAllFiles();

        // Process all the files in all of runs. This will perform caching from MSS if necessary.
        this.processRuns(runs);

        // Print the run summary information.
        if (this.printSummary) {
            runs.printRunSummaries();
        }

        // Insert run information into the database.
        if (this.update) {
            // Update run log.
            runs.insert();
        }

        // Update the timestamp file which can be used to tell which files have been processed.
        if (this.timestampFile == null) {
            this.timestampFile = new File("timestamp");
            try {
                this.timestampFile.createNewFile();
            } catch (final IOException e) {
                throw new RuntimeException(e);
            }
            LOGGER.info("created new timestamp file: " + this.timestampFile.getPath());
        }
        this.timestampFile.setLastModified(System.currentTimeMillis());
        LOGGER.info("set modified on timestamp file: " + new Date(this.timestampFile.lastModified()));
    }
}
