package org.hps.users.jeremym.crawler;

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
 * based on the run summary information, including printing a summary, caching the files from JLAB MSS, and updating a
 * run database.
 *
 * @author <a href="mailto:jeremym@slac.stanford.edu">Jeremy McCormick</a>
 */
public class EvioFileCrawler {

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
        OPTIONS.addOption("c", "cache", false, "cache files to /cache/mss from MSS (only works at JLAB)");
        OPTIONS.addOption("p", "print", false, "print run summary at end of job");
        OPTIONS.addOption("L", "log-level", true, "set log level (INFO, FINE, etc.)");
        OPTIONS.addOption("u", "update", false, "update the run database");
    }

    public static void main(final String[] args) {
        new EvioFileCrawler().parse(args).run();
    }

    final Set<Integer> acceptRuns = new HashSet<Integer>();

    boolean cache = false;

    final PosixParser parser = new PosixParser();

    boolean printSummary = false;

    File rootDir = new File(System.getProperty("user.dir"));

    Date timestamp = null;

    File timestampFile = null;

    boolean update = false;

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

            if (cl.hasOption("p")) {
                this.printSummary = true;
            }

            if (cl.hasOption("u")) {
                this.update = true;
            }

            if (cl.hasOption("c")) {
                this.cache = true;
            }

            if (this.cache && (this.printSummary || this.update)) {
                // If file caching is selected, then printing run summary or updating the database won't work.
                throw new IllegalArgumentException("File caching cannot be activated with the -p or -u options.");
            }

        } catch (final ParseException e) {
            throw new RuntimeException("Error parsing options.", e);
        }

        return this;
    }

    public void run() {
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

        if (this.cache) {
            // Cache files from MSS.
            runs.cache();
        } else {

            // Print the run summaries.
            if (this.printSummary) {
                runs.printRunSummaries();
            }

            // Insert run summary into run_log table.
            if (this.update) {
                runs.update();
            }
        }

        // Update timestamp file.
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
