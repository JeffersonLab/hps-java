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
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.lcsim.util.log.LogUtil;

/**
 * Crawls EVIO files in a directory tree, groups the files that are found by run, and optionally performs various tasks
 * based on the run summary information that is accumulated, including printing a summary, caching the files from JLAB
 * MSS, and updating a run database.
 *
 * @author <a href="mailto:jeremym@slac.stanford.edu">Jeremy McCormick</a>
 */
public final class EvioFileCrawler {

    private static final Logger LOGGER = LogUtil.create(EvioFileVisitor.class);

    private static final Options OPTIONS = new Options();

    static {
        LOGGER.setLevel(Level.ALL);
    }

    static {
        OPTIONS.addOption("t", "timestamp-file", true,
                "timestamp file for date filtering; modified time will be set at end of job");
        OPTIONS.addOption("d", "directory", true, "starting directory");
        OPTIONS.addOption("r", "runs", true, "list of runs to accept (others will be excluded)");
        OPTIONS.addOption("s", "summary", false, "print run summary at end of job");
        OPTIONS.addOption("L", "log-level", true, "set log level (INFO, FINE, etc.)");
        OPTIONS.addOption("u", "update", false, "update the run database");
        OPTIONS.addOption("e", "epics", false, "process EPICS data");
        OPTIONS.addOption("c", "cache", false, "cache all files from MSS");
    }

    public static void main(final String[] args) {
        try {
            new EvioFileCrawler().parse(args).run();
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    private final Set<Integer> acceptRuns = new HashSet<Integer>();

    private boolean epics = false;

    private final PosixParser parser = new PosixParser();

    private boolean printSummary = false;

    private File rootDir = new File(System.getProperty("user.dir"));

    private Date timestamp = null;

    private File timestampFile = null;

    private boolean update = false;
    
    private boolean cache = false;

    private RunProcessor createRunProcessor(final RunSummary runSummary) {
        final RunProcessor processor = new RunProcessor(runSummary);
        if (this.epics) {
            processor.addProcessor(new EpicsLog(runSummary));
        }
        if (this.printSummary) {
            processor.addProcessor(new EventTypeLog(runSummary));
        }
        return processor;
    }

    private EvioFileCrawler parse(final String args[]) {
        try {
            final CommandLine cl = this.parser.parse(OPTIONS, args);

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
                    throw new IllegalArgumentException("The timestamp file does not exist: "
                            + this.timestampFile.getPath());
                }
                try {
                    this.timestamp = new Date(Files
                            .readAttributes(this.timestampFile.toPath(), BasicFileAttributes.class).lastModifiedTime()
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
                this.cache = true;
            }

        } catch (final ParseException e) {
            throw new RuntimeException("Error parsing options.", e);
        }

        return this;
    }

    private void cacheFiles(final RunLog runs) {
        JCacheManager cache = new JCacheManager();
        
        // Process all files in the runs.
        for (final int run : runs.getSortedRunNumbers()) {
                        
            // Get the run summary for the run.
            final RunSummary runSummary = runs.getRunSummary(run);
            
            // Cache all the files.
            cache.cache(runSummary.getFiles());
                        
            // Wait for cache operation to complete. (~5 minutes max)
            boolean cached = cache.waitForAll(300000);
            
            if (!cached) {
                throw new RuntimeException("The cache operation did not complete in time.");
            }
        }
    }
    
    private void processRuns(final RunLog runs) throws Exception {
        // Process all files in the runs.
        for (final int run : runs.getSortedRunNumbers()) {
                        
            // Get the run summary for the run.
            final RunSummary runSummary = runs.getRunSummary(run);
                        
            // Create a processor to process all the EVIO records in the run.
            final RunProcessor processor = createRunProcessor(runSummary);
                                   
            // Process the run, updating the run summary.
            processor.process();
        }
    }

    public void run() throws Exception {
        final EnumSet<FileVisitOption> options = EnumSet.noneOf(FileVisitOption.class);
        final EvioFileVisitor visitor = new EvioFileVisitor();
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

        LOGGER.fine("sorting files by sequence ...");
        runs.sortAllFiles();
        
        // Cache all the files to disk before processing them.
        if (this.cache) {
            cacheFiles(runs);
        }

        // Process all the files in the runs.
        processRuns(runs);

        // Print the run summaries.
        if (this.printSummary) {
            runs.printRunSummaries();
        }

        // Insert run information into the database.
        if (this.update) {
            // Update run log.
            runs.insert();
        }

        // Update the timestamp file.
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
