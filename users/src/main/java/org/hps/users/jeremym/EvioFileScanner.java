package org.hps.users.jeremym;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.hps.record.evio.EvioEventUtilities;
import org.jlab.coda.jevio.EvioEvent;
import org.jlab.coda.jevio.EvioException;
import org.jlab.coda.jevio.EvioReader;
import org.lcsim.util.log.LogUtil;

// TODO:
//
// Info from files:
// -run number
// -list of files
// -start date (from PRESTART)
// -end date (from END)
// -total number of events
//
// Command line args:
// -start and end run number filter (outside range will be excluded)
// -list of run numbers (not in list will be excluded)
// -output timestamp file (when dir walk ends)
// -list of "tasks" to execute for each EVIO file (register run data, put into file catalog, etc.)
// -caching to cache first/last files or all files for run
public class EvioFileScanner {

    static class EvioFileList extends ArrayList<File> {

        public File first() {
            return get(0);
        }

        public File last() {
            return get(size() - 1);
        }

        public void sort() {
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

    // hps_005077.evio.20
    static class EvioFileVisitor extends SimpleFileVisitor<Path> {

        static SimpleEvioFileFilter FILTER = new SimpleEvioFileFilter();

        FileRunMap runMap = new FileRunMap();

        FileRunMap getRunMap() {
            return this.runMap;
        }

        @Override
        public FileVisitResult visitFile(final Path path, final BasicFileAttributes attrs) {

            final File file = path.toFile();
            if (FILTER.accept(file)) {
                LOGGER.info("found EVIO file " + file.getPath());

                final Integer runNumber = getRunNumber(file);
                final Integer sequenceNumber = getSequenceNumber(file);

                LOGGER.info("run number " + runNumber);
                LOGGER.info("sequence number " + sequenceNumber);

                this.runMap.get(runNumber).add(file);
            }
            return FileVisitResult.CONTINUE;
        }

    }

    static class FileRunMap extends HashMap<Integer, EvioFileList> {

        @Override
        public EvioFileList get(final Object key) {
            if (!this.containsKey(key)) {
                if (!(key instanceof Integer)) {
                    throw new IllegalArgumentException("The key argument has bad type.");
                }
                if (super.get(key) == null) {
                    this.put(Integer.class.cast(key), new EvioFileList());
                }
            }
            return super.get(key);
        }

        List<Integer> getSortedRunNumbers() {
            final List<Integer> runList = new ArrayList<Integer>(this.keySet());
            Collections.sort(runList);
            return runList;
        }

        void sortFiles() {
            for (final Integer run : keySet()) {
                get(run).sort();
            }
        }
    }

    static class SimpleEvioFileFilter implements FileFilter {

        @Override
        public boolean accept(final File pathname) {
            return pathname.getName().contains(".evio");
        }
    }

    private static final Logger LOGGER = LogUtil.create(EvioFileVisitor.class);

    static final long MILLISECONDS = 1000L;

    private static final Options OPTIONS = new Options();

    static {
        OPTIONS.addOption("t", "timestamp", true, "timestamp file");
        OPTIONS.addOption("d", "dir", true, "starting directory");
    }

    static Integer getRunNumber(final File file) {
        final String name = file.getName();
        return Integer.parseInt(name.substring(0, name.indexOf(".")).replace("hps_", ""));
    }

    static Integer getSequenceNumber(final File file) {
        final String name = file.getName();
        return Integer.parseInt(name.substring(name.lastIndexOf(".") + 1));
    }

    public static void main(final String[] args) {
        new EvioFileScanner().parse(args).run();
    }

    final PosixParser parser = new PosixParser();

    File rootDir = new File(System.getProperty("user.dir"));

    Date getRunEnd(final File file) {
        Date date = null;
        EvioReader reader = null;
        try {
            reader = new EvioReader(file.getPath(), false);
            EvioEvent event;
            while ((event = reader.parseNextEvent()) != null) {
                if (EvioEventUtilities.isEndEvent(event)) {
                    final int[] data = EvioEventUtilities.getControlEventData(event);
                    long seconds = (long)data[0];
                    //System.out.printf("END control: %d %d %d", data[0], data[1], data[2]);
                    date = new Date(seconds * MILLISECONDS);
                    //System.out.println("END date: " + date);
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

    Date getRunStart(final File file) {
        Date date = null;
        EvioReader reader = null;
        try {
            reader = new EvioReader(file.getPath(), false);
            EvioEvent event;
            while ((event = reader.parseNextEvent()) != null) {
                if (EvioEventUtilities.isPreStartEvent(event)) {
                    final int[] data = EvioEventUtilities.getControlEventData(event);
                    //System.out.printf("PRESTART control: %d %d %d%n", data[0], data[1], data[2]);
                    long seconds = (long)data[0];
                    date = new Date(seconds * MILLISECONDS);
                    //System.out.println("PRESTART date: " + date);
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

    public EvioFileScanner parse(final String args[]) {

        try {
            final CommandLine cl = this.parser.parse(OPTIONS, args);

            if (cl.hasOption("d")) {
                this.rootDir = new File(cl.getOptionValue("d"));
                if (!this.rootDir.exists()) {
                    throw new IllegalArgumentException("The directory does not exist.");
                }
                if (!this.rootDir.isDirectory()) {
                    throw new IllegalArgumentException("The specified path is not a directory.");
                }
            }

        } catch (final ParseException e) {
            throw new RuntimeException("Error parsing options.", e);
        }

        return this;
    }

    public void run() {
        final EnumSet<FileVisitOption> options = EnumSet.noneOf(FileVisitOption.class);
        final EvioFileVisitor visitor = new EvioFileVisitor();
        try {
            Files.walkFileTree(this.rootDir.toPath(), options, Integer.MAX_VALUE, visitor);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }

        final FileRunMap runMap = visitor.getRunMap();
        runMap.sortFiles();
        System.out.println("found files...");
        for (final Integer run : runMap.getSortedRunNumbers()) {
            System.out.println();
            System.out.println("run " + run + " has " + runMap.get(run).size() + " files");
            final EvioFileList files = runMap.get(run);
            System.out.println("first file " + files.first());
            System.out.println("last file " + files.last());
            System.out.println("started at " + getRunStart(files.first()));
            System.out.println("ended at " + getRunEnd(files.last()));
        }
    }
}
