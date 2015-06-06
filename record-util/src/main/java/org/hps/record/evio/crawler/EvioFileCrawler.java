package org.hps.record.evio.crawler;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.hps.record.evio.EvioEventProcessor;
import org.lcsim.util.log.DefaultLogFormatter;
import org.lcsim.util.log.LogUtil;

/**
 * Crawls EVIO files in a directory tree, groups the files that are found by run, and optionally performs various tasks based on the run summary
 * information that is accumulated, including printing a summary, caching the files from JLAB MSS, and updating a run database.
 *
 * @author Jeremy McCormick, SLAC
 */
// TODO: need options for...
// -database connections prop file
// -writing Auger XML for crawl job (and don't actually execute job)
// -writing out a summary EVIO file containing control events only (PRESTART, EPICS, scalars?, END)
// -get supplementary information from run spreadsheet (including whether run was "JUNK" or not)
// -allow running arbitrary EvioEventProcessor classes by giving fully qualified class names as args on command line
//  e.g. -E org.hps.derp.MyEvioEventProcessor
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
    
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /**
     * Statically define the command options.
     */
    static {
        OPTIONS.addOption("a", "accept-runs", true, "list of run numbers to accept (others will be excluded)");
        OPTIONS.addOption("b", "begin-date", true, "min date for files (example 2015-03-26 11:28:59)");
        OPTIONS.addOption("c", "cache-files", false, "automatically cache files from MSS (JLAB only)");
        OPTIONS.addOption("d", "directory", true, "root directory to start crawling (default is current dir)");
        OPTIONS.addOption("e", "epics", false, "process EPICS data found in EVIO files");
        OPTIONS.addOption("E", "evio-processor", true, "class name of an additional EVIO processor to execute");
        OPTIONS.addOption("h", "help", false, "print help and exit");
        OPTIONS.addOption("m", "max-files", true, "max number of files to process per run (only for debugging)");
        OPTIONS.addOption("p", "print", true, "set event print interval during EVIO processing");
        OPTIONS.addOption("r", "insert-run-log", false, "update the run database (not done by default)");
        OPTIONS.addOption("t", "timestamp-file", true, "existing or new timestamp file name for date cut off");
        OPTIONS.addOption("s", "print-summary", false, "print run summary at the end of the job");
        OPTIONS.addOption("w", "max-cache-wait", true, "total seconds to allow for file caching");
        OPTIONS.addOption("L", "log-level", true, "set the log level (INFO, FINE, etc.)");
        OPTIONS.addOption("u", "update-run-log", false, "allow overriding existing data in the run db (not allowed by default)");
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
     * The class for managing the file caching using the 'jcache' command.
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
    private boolean updateRunLog = false;

    /**
     * Flag indicating if the file cache should be used (e.g. jcache automatically executed to move files to the cache disk from tape).
     */
    private boolean useFileCache = false;

    /**
     * The maximum wait time in milliseconds to allow for file caching operations.
     */
    private Long waitTime;
    
    private boolean allowUpdates = false;
    
    private List<EvioEventProcessor> processors = new ArrayList<EvioEventProcessor>();

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
                    try {
                        // Create new time stamp file.
                        LOGGER.info("creating new timestamp file " + this.timestampFile.getPath());
                        this.timestampFile.createNewFile();
                    } catch (IOException e) {
                        throw new IllegalArgumentException("Error creating timestamp file " + this.timestampFile.getPath());
                    }
                } else { 
                    try {
                        // Get cut-off date for files from existing time stamp file. 
                        this.timestamp = new Date(Files.readAttributes(this.timestampFile.toPath(), BasicFileAttributes.class).lastModifiedTime().toMillis());
                        LOGGER.info("got timestamp " + this.timestamp + " from existing file " + this.timestampFile.getPath());
                    } catch (final IOException e) {
                        throw new RuntimeException("Error getting attributes of timestamp file.", e);
                    }
                }
            }

            if (cl.hasOption("a")) {
                for (final String runString : cl.getOptionValues("a")) {
                    final Integer acceptRun = Integer.parseInt(runString);
                    this.acceptRuns.add(acceptRun);
                    LOGGER.config("added run number filter " + acceptRun);
                }
            }

            if (cl.hasOption("s")) {
                LOGGER.config("print summary enabled");
                this.printSummary = true;
            }

            if (cl.hasOption("r")) {                
                this.updateRunLog = true;
                LOGGER.config("run db will be updated");
            }

            if (cl.hasOption("e")) {                
                this.epics = true;
                LOGGER.config("EPICS processing enabled");
            }

            if (cl.hasOption("c")) {                
                this.useFileCache = true;
                LOGGER.config("using file cache");
            }

            if (cl.hasOption("w")) {
                this.waitTime = Long.parseLong(cl.getOptionValue("w")) * MILLISECONDS;
                if (this.waitTime > 0L) {
                    this.cacheManager.setWaitTime(this.waitTime);
                    LOGGER.config("max wait time for caching set to " + this.waitTime);
                }
            }

            if (cl.hasOption("m")) {
                this.maxFiles = Integer.parseInt(cl.getOptionValue("m"));
                LOGGER.config("max files set to " + this.maxFiles);
            }

            if (cl.hasOption("p")) {
                this.eventPrintInterval = Integer.parseInt(cl.getOptionValue("p"));
                LOGGER.config("event print interval set to " + this.eventPrintInterval);
            }
            
            if (cl.hasOption("u")) {
                this.allowUpdates = true;
                if (!this.updateRunLog) {
                    LOGGER.info("the -u option is ignored because run_log is not being updated");
                }
            }
            
            if (cl.hasOption("b")) {
                try {
                    if (this.timestamp != null) {
                        LOGGER.warning("existing timestamp from file " + this.timestamp + " will be overridden by date from -b argument");
                    }
                    this.timestamp = DATE_FORMAT.parse(cl.getOptionValue("b"));
                    LOGGER.info("set timestamp to " + DATE_FORMAT.format(this.timestamp));
                } catch (java.text.ParseException e) {
                    throw new RuntimeException(e);
                }
            }
            
            
            if (cl.hasOption("E")) {
                String[] classNames = cl.getOptionValues("E");
                for (String className : classNames) {
                    try {
                        processors.add(createEvioEventProcessor(className));
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
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
        final RunProcessor runProcessor = this.createRunProcessor(runSummary);
        
        for (EvioEventProcessor processor : processors) {
            runProcessor.addProcessor(processor);
            LOGGER.config("added extra EVIO processor " + processor.getClass().getName());
        }

        // Process all of the runs files.
        runProcessor.process();
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
        if (this.updateRunLog) {
            // Update run log.
            new RunLogUpdater(runs, allowUpdates).insert();
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
    
    EvioEventProcessor createEvioEventProcessor(String className) throws Exception {
        return EvioEventProcessor.class.cast(Class.forName(className).newInstance());
    }
}
