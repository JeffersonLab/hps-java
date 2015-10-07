package org.hps.users.jeremym;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.hps.conditions.database.ConnectionParameters;
import org.hps.record.evio.EvioEventConstants;
import org.hps.record.evio.EvioEventUtilities;
import org.jlab.coda.jevio.BaseStructure;
import org.jlab.coda.jevio.EvioEvent;
import org.jlab.coda.jevio.EvioException;
import org.jlab.coda.jevio.EvioReader;

/**
 * A utility for scanning EVIO files by run.
 *
 * @author <a href="mailto:jeremym@slac.stanford.edu">Jeremy McCormick</a>
 */
// TODO:
//
// tasks per run:
//
// -get list of files in run
// -cache all files from run to /cache/mss
// -get start and end dates
// -get total number of events
// -dump trigger config
// -dump SVT config
// -check for missing or corrupt files in run
// -update run database
public class EvioFileScanner {

    static class DateFilter implements FileFilter {

        Date date;

        DateFilter(final Date date) {
            this.date = date;
        }

        @Override
        public boolean accept(final File pathname) {
            BasicFileAttributes attr = null;
            try {
                attr = Files.readAttributes(pathname.toPath(), BasicFileAttributes.class);
            } catch (final IOException e) {
                throw new RuntimeException("Error getting file attributes.", e);
            }
            return attr.creationTime().toMillis() > this.date.getTime();
        }
    }

    static class EvioFileList extends ArrayList<File> {

        int totalEvents = 0;

        void computeTotalEvents() {
            this.totalEvents = 0;
            for (final File file : this) {
                EvioReader reader = null;
                try {
                    reader = open(file);
                    this.totalEvents += reader.getEventCount();
                } catch (EvioException | IOException e) {
                    throw new RuntimeException(e);
                } finally {
                    if (reader != null) {
                        try {
                            reader.close();
                        } catch (final IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }

        File first() {
            return this.get(0);
        }

        int getTotalEvents() {
            return this.totalEvents;
        }

        File last() {
            return this.get(this.size() - 1);
        }

        void sort() {
            final List<File> fileList = new ArrayList<File>(this);
            Collections.sort(fileList, new EvioFileSequenceComparator());
            this.clear();
            this.addAll(fileList);
        }
    }

    static class EvioFileSequenceComparator implements Comparator<File> {

        @Override
        public int compare(final File o1, final File o2) {
            final Integer sequenceNumber1 = getSequenceNumber(o1);
            final Integer sequenceNumber2 = getSequenceNumber(o2);
            return sequenceNumber1.compareTo(sequenceNumber2);
        }
    }

    static class EvioFileVisitor extends SimpleFileVisitor<Path> {

        boolean cache = false;

        List<FileFilter> filters = new ArrayList<FileFilter>();

        RunLog runs = new RunLog();

        EvioFileVisitor() {
            addFilter(new EvioFilter());
        }

        boolean accept(final File file) {
            boolean accept = true;
            for (final FileFilter filter : this.filters) {
                accept = filter.accept(file);
                if (accept == false) {
                    LOGGER.fine(filter.getClass().getSimpleName() + " rejected file: " + file.getPath());
                    break;
                }
            }
            return accept;
        }

        void addFilter(final FileFilter filter) {
            this.filters.add(filter);
            LOGGER.config("added filter: " + filter.getClass().getSimpleName());
        }

        RunLog getRunLog() {
            return this.runs;
        }

        void setCache(final boolean cache) {
            this.cache = cache;
        }

        @Override
        public FileVisitResult visitFile(final Path path, final BasicFileAttributes attrs) {

            final File file = path.toFile();
            if (accept(file)) {
                LOGGER.info("accepted EVIO file: " + file.getPath());

                final Integer run = getRunFromName(file);
                final Integer sequence = getSequenceNumber(file);

                LOGGER.info("run: " + run);
                LOGGER.info("sequence: " + sequence);

                this.runs.get(run).addFile(file);
            } else {
                LOGGER.fine("rejected file: " + file.getPath());
            }
            return FileVisitResult.CONTINUE;
        }
    }

    static class EvioFilter implements FileFilter {

        @Override
        public boolean accept(final File pathname) {
            return pathname.getName().contains(".evio");
        }
    }

    static class RunFilter implements FileFilter {
        Set<Integer> acceptRuns;

        RunFilter(final Set<Integer> acceptRuns) {
            this.acceptRuns = acceptRuns;
        }

        @Override
        public boolean accept(final File file) {
            return this.acceptRuns.contains(getRunFromName(file));
        }
    }

    static class RunLog extends HashMap<Integer, RunSummary> {

        void computeTotalEvents() {
            for (final RunSummary runSummary : this.values()) {
                runSummary.computeTotalEvents();
            }
        }

        @Override
        public RunSummary get(final Object key) {
            if (!this.containsKey(key)) {
                if (!(key instanceof Integer)) {
                    throw new IllegalArgumentException("The key argument has bad type.");
                }
                if (super.get(key) == null) {
                    final int run = Integer.class.cast(key);
                    this.put(Integer.class.cast(key), new RunSummary(run));
                }
            }
            return super.get(key);
        }

        List<Integer> getSortedRunNumbers() {
            final List<Integer> runList = new ArrayList<Integer>(this.keySet());
            Collections.sort(runList);
            return runList;
        }

        void printRunSummaries() {
            for (final int run : this.keySet()) {
                this.get(run).printRunSummary(System.out);
            }
        }

        void sortAllFiles() {
            for (final Integer run : this.keySet()) {
                this.get(run).sortFiles();
            }
        }
    }

    static class RunSummary {

        EvioFileList files = new EvioFileList();
        int run;

        RunSummary(final int run) {
            this.run = run;
        }

        void addFile(final File file) {
            this.files.add(file);
        }

        void computeTotalEvents() {
            this.files.computeTotalEvents();
        }

        EvioFileList getFiles() {
            return this.files;
        }

        Date getRunEnd() {
            return EvioFileScanner.getRunEnd(this.files.last());
        }

        Date getRunStart() {
            return EvioFileScanner.getRunStart(this.files.first());
        }

        int getTotalEvents() {
            return this.files.getTotalEvents();
        }

        boolean isEndOkay() {
            LOGGER.info("checking is END okay ...");
            boolean isEndOkay = false;
            final File lastFile = this.files.last();
            EvioReader reader = null;
            try {
                reader = open(lastFile);
                reader.gotoEventNumber(reader.getEventCount() - 5);
                EvioEvent event = null;
                while ((event = reader.parseNextEvent()) != null) {
                    if (event.getHeader().getTag() == EvioEventConstants.END_EVENT_TAG) {
                        isEndOkay = true;
                        break;
                    }
                }
            } catch (EvioException | IOException e) {
                throw new RuntimeException(e);
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            return isEndOkay;
        }

        void printRunSummary(final PrintStream ps) {
            ps.println("--------------------------------------------");
            ps.println("run: " + this.run);
            ps.println("first file: " + this.files.first());
            ps.println("last file: " + this.files.last());
            ps.println("started: " + getRunStart());
            ps.println("ended: " + getRunEnd());
            ps.println("total events: " + this.files.getTotalEvents());
            ps.println("files: " + this.files.size());
            for (final File file : this.files) {
                ps.println(file.getPath());
            }
        }

        void sortFiles() {
            this.files.sort();
        }
    }

    private static final Logger LOGGER = Logger.getLogger(EvioFileScanner.class.getName());
    private static final long MILLISECONDS = 1000L;

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

    static Date getDate(final File file, final int eventTag, final int gotoEvent) {
        Date date = null;
        EvioReader reader = null;
        try {
            reader = open(file);
            EvioEvent event;
            if (gotoEvent > 0) {
                reader.gotoEventNumber(gotoEvent);
            } else if (gotoEvent < 0) {
                reader.gotoEventNumber(reader.getEventCount() + gotoEvent);
            }
            while ((event = reader.parseNextEvent()) != null) {
                if (event.getHeader().getTag() == eventTag) {
                    final int[] data = EvioEventUtilities.getControlEventData(event);
                    final long seconds = data[0];
                    date = new Date(seconds * MILLISECONDS);
                    break;
                }
            }
        } catch (EvioException | IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (final IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return date;
    }

    static Date getHeadBankDate(final EvioEvent event) {
        Date date = null;
        final BaseStructure headBank = EvioEventUtilities.getHeadBank(event);
        if (headBank != null) {
            final int[] data = headBank.getIntData();
            final long time = data[3];
            if (time != 0L) {
                date = new Date(time * MILLISECONDS);
            }
        }
        return date;
    }

    static Date getRunEnd(final File file) {
        Date date = getDate(file, EvioEventConstants.END_EVENT_TAG, -10);
        if (date == null) {
            EvioReader reader = null;
            try {
                reader = open(file);
                reader.gotoEventNumber(reader.getEventCount() - 11);
                EvioEvent event = null;
                while ((event = reader.parseNextEvent()) != null) {
                    if (EvioEventUtilities.isPhysicsEvent(event)) {
                        if ((date = getHeadBankDate(event)) != null) {
                            break;
                        }
                    }
                }
            } catch (EvioException | IOException e) {
                throw new RuntimeException(e);
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return date;
    }

    static Integer getRunFromName(final File file) {
        final String name = file.getName();
        final int startIndex = name.lastIndexOf("_") + 1;
        final int endIndex = name.indexOf(".");
        return Integer.parseInt(name.substring(startIndex, endIndex));
    }

    static Date getRunStart(final File file) {
        Date date = getDate(file, EvioEventConstants.PRESTART_EVENT_TAG, 0);
        if (date == null) {
            EvioReader reader = null;
            try {
                reader = open(file);
                EvioEvent event = null;
                while ((event = reader.parseNextEvent()) != null) {
                    if (EvioEventUtilities.isPhysicsEvent(event)) {
                        if ((date = getHeadBankDate(event)) != null) {
                            break;
                        }
                    }
                }
            } catch (EvioException | IOException e) {
                throw new RuntimeException(e);
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return date;
    }

    static Integer getSequenceNumber(final File file) {
        final String name = file.getName();
        return Integer.parseInt(name.substring(name.lastIndexOf(".") + 1));
    }

    public static void main(final String[] args) {
        new EvioFileScanner().parse(args).run();
    }

    static EvioReader open(final File file) throws IOException, EvioException {
        final long start = System.currentTimeMillis();
        // final EvioReader reader = new EvioReader(file, false, true);
        final EvioReader reader = new EvioReader(file, false, false);
        final long end = System.currentTimeMillis() - start;
        LOGGER.info("opened " + file.getPath() + " in " + end / MILLISECONDS + " seconds");
        return reader;
    }

    final Set<Integer> acceptRuns = new HashSet<Integer>();

    final PosixParser parser = new PosixParser();

    boolean printSummary = false;

    File rootDir = new File(System.getProperty("user.dir"));

    Date timestamp = null;

    File timestampFile = null;

    boolean update = false;

    void cache(final File file) {
        if (!file.getPath().startsWith("/mss")) {
            throw new IllegalArgumentException("Only files on /mss can be cached.");
        }
        try {
            new ProcessBuilder("jcache", "submit", "default", file.getPath()).start();
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
        LOGGER.info("process started to cache " + file.getPath());
    }

    EvioFileScanner parse(final String args[]) {
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

        } catch (final ParseException e) {
            throw new RuntimeException("Error parsing options.", e);
        }

        return this;
    }

    void run() {
        final EnumSet<FileVisitOption> options = EnumSet.noneOf(FileVisitOption.class);
        final EvioFileVisitor visitor = new EvioFileVisitor();
        if (this.timestamp != null) {
            visitor.addFilter(new DateFilter(this.timestamp));
            LOGGER.config("added date filter with timestamp " + this.timestamp);
        }
        if (!this.acceptRuns.isEmpty()) {
            visitor.addFilter(new RunFilter(this.acceptRuns));
            LOGGER.config("added run filter");
        }
        try {
            Files.walkFileTree(this.rootDir.toPath(), options, Integer.MAX_VALUE, visitor);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }

        final RunLog runs = visitor.getRunLog();

        LOGGER.fine("sorting files ...");
        runs.sortAllFiles();
        LOGGER.fine("compute total events ...");
        runs.computeTotalEvents();

        if (this.printSummary) {
            runs.printRunSummaries();
        }

        if (this.update) {
            update(runs);
        }

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

    void update(final RunLog runLog) {
        LOGGER.info("updating database from run log ...");
        final ConnectionParameters cp = new ConnectionParameters("root", "derp", "hps_run_db", "localhost");
        Connection connection = null;
        PreparedStatement runLogStatement = null;
        try {
            connection = cp.createConnection();
            connection.setAutoCommit(false);
            runLogStatement = connection
                    .prepareStatement("INSERT INTO run_log (run, start_date, end_date, nevents, nfiles, end_ok, last_updated) VALUES(?, ?, ?, ?, ?, ?, NOW())");
            for (final Integer run : runLog.getSortedRunNumbers()) {
                LOGGER.info("inserting run " + run + " into database");
                final RunSummary runSummary = runLog.get(run);
                runLogStatement.setInt(1, run);
                runLogStatement.setTimestamp(2, new java.sql.Timestamp(runSummary.getRunStart().getTime()));
                runLogStatement.setTimestamp(3, new java.sql.Timestamp(runSummary.getRunEnd().getTime()));
                runLogStatement.setInt(4, runSummary.getTotalEvents());
                runLogStatement.setInt(5, runSummary.getFiles().size());
                runLogStatement.setBoolean(6, runSummary.isEndOkay());
                runLogStatement.executeUpdate();
                connection.commit();
            }
        } catch (final SQLException e) {
            LOGGER.log(Level.SEVERE, "rolling back transaction", e);
            try {
                connection.rollback();
            } catch (final SQLException e2) {
                throw new RuntimeException(e);
            }
        } finally {
            if (connection != null) {
                try {
                    connection.setAutoCommit(true);
                    if (!connection.isClosed()) {
                        connection.close();
                    }
                } catch (final SQLException e) {
                    e.printStackTrace();
                }
            }
        }
        LOGGER.info("database was updated!");
    }
}
