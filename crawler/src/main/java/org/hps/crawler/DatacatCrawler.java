package org.hps.crawler;

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
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.DefaultParser;
import org.hps.datacat.client.DatacatClient;
import org.hps.datacat.client.DatacatClientFactory;
import org.hps.datacat.client.DatasetFileFormat;
import org.hps.datacat.client.DatasetSite;

/**
 * Command line file crawler for populating the data catalog.
 *
 * @author Jeremy McCormick, SLAC
 */
public class DatacatCrawler {

    /**
     * Visitor which creates a {@link FileSet} from walking a directory tree.
     * <p>
     * Any number of {@link java.io.FileFilter} objects can be registered with this visitor to restrict which files are
     * accepted.
     *
     * @author Jeremy McCormick, SLAC
     */
    final class DatacatFileVisitor extends SimpleFileVisitor<Path> {

        /**
         * The run log containing information about files from each run.
         */
        private final FileSet fileSet = new FileSet();

        /**
         * A list of file filters to apply.
         */
        private final List<FileFilter> filters = new ArrayList<FileFilter>();

        /**
         * Run the filters on the file to tell whether it should be accepted or not.
         *
         * @param file the EVIO file
         * @return <code>true</code> if file should be accepted
         */
        private boolean accept(final File file) {
            boolean accept = true;
            for (final FileFilter filter : this.filters) {
                accept = filter.accept(file);
                if (!accept) {
                    break;
                }
            }
            return accept;
        }

        /**
         * Add a file filter.
         *
         * @param filter the file filter
         */
        void addFilter(final FileFilter filter) {
            this.filters.add(filter);
        }

        /**
         * Get the file set created by visiting the directory tree.
         *
         * @return the file set from visiting the directory tree
         */
        FileSet getFileSet() {
            return this.fileSet;
        }

        /**
         * Visit a single file.
         *
         * @param path the file to visit
         * @param attrs the file attributes
         */
        @Override
        public FileVisitResult visitFile(final Path path, final BasicFileAttributes attrs) {
            final File file = path.toFile();
            if (this.accept(file)) {
                final DatasetFileFormat format = DatacatUtilities.getFileFormat(file);
                fileSet.addFile(format, file);
            }
            return FileVisitResult.CONTINUE;
        }
    }

    /**
     * Make a list of available file formats for printing help.
     */
    private static String AVAILABLE_FORMATS;

    /**
     * Setup the logger.
     */
    private static final Logger LOGGER = Logger.getLogger(DatacatCrawler.class.getPackage().getName());

    /**
     * Command line options for the crawler.
     */
    private static final Options OPTIONS = new Options();
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
        OPTIONS.addOption("L", "log-level", true, "set the log level (INFO, FINE, etc.)");
        OPTIONS.addOption("b", "min-date", true, "min date for a file (example \"2015-03-26 11:28:59\")");
        OPTIONS.addOption("d", "directory", true, "root directory to crawl");
        OPTIONS.addOption("f", "folder", true, "datacat folder");
        OPTIONS.addOption("h", "help", false, "print help and exit (overrides all other arguments)");
        OPTIONS.addOption("o", "format", true, "add a file format for filtering: " + AVAILABLE_FORMATS);
        OPTIONS.addOption("m", "metadata", false, "create metadata for datasets");
        OPTIONS.addOption("r", "run", true, "add a run number to accept");
        OPTIONS.addOption("s", "site", true, "datacat site");
        OPTIONS.addOption("t", "timestamp-file", true, "existing or new timestamp file name");
        OPTIONS.addOption("x", "max-depth", true, "max depth to crawl");
        OPTIONS.addOption("D", "dry-run", false, "dry run which will not update the datacat");
    }

    /**
     * Main method.
     *
     * @param args the command line arguments
     */
    public static void main(final String[] args) {
        new DatacatCrawler().parse(args).run();
    }

    /**
     * The crawler configuration.
     */
    private CrawlerConfig config;

    /**
     * The options parser.
     */
    private final DefaultParser parser = new DefaultParser();

    /**
     * Throw an exception if the path doesn't exist in the data catalog or it is not a folder.
     *
     * @param folder the folder in the datacat
     * @throws RuntimeException if the given path does not exist or it is not a folder
     */
    void checkFolder(final String folder) {
        final DatacatClient datacatClient = new DatacatClientFactory().createClient();
        if (!datacatClient.exists(folder)) {
            throw new RuntimeException("The folder " + folder + " does not exist in the data catalog.");
        }
        if (!datacatClient.isFolder(folder)) {
            throw new RuntimeException("The path " + folder + " is not a folder.");
        }
    }

    /**
     * Parse command line options.
     *
     * @param args the command line arguments
     * @return this object (for method chaining)
     */
    public DatacatCrawler parse(final String[] args) {
        config = new CrawlerConfig();

        LOGGER.config("parsing command line options");

        this.config = new CrawlerConfig();

        try {
            final CommandLine cl = this.parser.parse(OPTIONS, args);

            // Print help.
            if (cl.hasOption("h") || args.length == 0) {
                this.printUsage();
            }

            // Log level (only used for this class's logger).
            if (cl.hasOption("L")) {
                final Level level = Level.parse(cl.getOptionValue("L"));
                LOGGER.config("log level " + level);
                LOGGER.setLevel(level);
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
                LOGGER.config("root dir " + config.rootDir());
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

            // Max depth to crawl.
            if (cl.hasOption("x")) {
                final Integer maxDepth = Integer.parseInt(cl.getOptionValue("x"));
                if (maxDepth < 1) {
                    throw new IllegalArgumentException("invalid -x argument for maxDepth: " + maxDepth);
                }
                config.setMaxDepth(maxDepth);
                LOGGER.config("set max depth to " + maxDepth);
            }

            // Configure enabled file formats.
            if (cl.hasOption("o")) {
                for (final String arg : cl.getOptionValues("o")) {
                    DatasetFileFormat format = null;
                    try {
                        format = DatasetFileFormat.valueOf(arg);
                    } catch (IllegalArgumentException | NullPointerException e) {
                        throw new IllegalArgumentException("The format " + arg + " is not valid.", e);
                    }
                    LOGGER.config("adding format " + format.name());
                    this.config.addFileFormat(format);
                }
            } else {
                throw new RuntimeException("The -o argument with data format must be supplied at least once.");
            }

            // Enable metadata extraction from files.
            if (cl.hasOption("m")) {
                config.setEnableMetadata(true);
                LOGGER.config("metadata extraction enabled");
            }

            // Datacat folder.
            if (cl.hasOption("f")) {
                config.setDatacatFolder(cl.getOptionValue("f"));
                LOGGER.config("set datacat folder to " + config.datacatFolder());
            } else {
                throw new RuntimeException("The -f argument with the datacat folder is required.");
            }

            // List of run numbers.
            if (cl.hasOption("r")) {
                final Set<Integer> acceptRuns = new HashSet<Integer>();
                for (final String arg : cl.getOptionValues("r")) {
                    acceptRuns.add(Integer.parseInt(arg));
                }
                config.setAcceptRuns(acceptRuns);
            }
            
            // Dataset site (defaults to JLAB).
            DatasetSite site = DatasetSite.JLAB;
            if (cl.hasOption("s")) {
                site = DatasetSite.valueOf(cl.getOptionValue("s"));
            }
            LOGGER.config("dataset site " + site);
            config.setDatasetSite(site);

        } catch (final ParseException e) {
            throw new RuntimeException("Error parsing options.", e);
        }

        // Check the datacat folder which must already exist.
        this.checkFolder(config.datacatFolder());

        // Check that there is at least one file format enabled for filtering.
        if (this.config.getFileFormats().isEmpty()) {
            throw new IllegalStateException("At least one file format must be provided with the -f switch.");
        }

        LOGGER.info("done parsing command line options");

        return this;
    }

    /**
     * Print the usage statement for this tool to the console and then exit the program.
     */
    private void printUsage() {
        final HelpFormatter help = new HelpFormatter();
        help.printHelp(70, "DatacatCrawler [options]", "", OPTIONS, "");
        System.exit(0);
    }

    /**
     * Run the crawler job.
     */
    void run() {

        // Create the file visitor for crawling the root directory with the given date filter.
        final DatacatFileVisitor visitor = new DatacatFileVisitor();

        // Add date filter if timestamp is supplied.
        if (config.timestamp() != null) {
            visitor.addFilter(new DateFileFilter(config.timestamp()));
        }

        // Add file format filter.
        for (final DatasetFileFormat fileFormat : config.getFileFormats()) {
            LOGGER.info("adding file format filter for " + fileFormat.name());
        }
        visitor.addFilter(new FileFormatFilter(config.getFileFormats()));

        // Run number filter.
        if (!config.acceptRuns().isEmpty()) {
            visitor.addFilter(new RunFilter(config.acceptRuns()));
        }

        // Walk the file tree using the visitor.
        this.walk(visitor);

        // Update the data catalog.
        this.updateDatacat(visitor.getFileSet());
    }

    /**
     * Update the data catalog.
     *
     * @param runMap the map of run information including the EVIO file list
     */
    private void updateDatacat(final FileSet fileSet) {
        final DatacatClient datacatClient = new DatacatClientFactory().createClient();
        for (final DatasetFileFormat fileFormat : config.getFileFormats()) {
            List<File> formatFiles = fileSet.get(fileFormat);
            LOGGER.info("adding " + formatFiles.size() + " files with format " + fileFormat.name());
            for (final File file : formatFiles) {

                LOGGER.info("adding file " + file.getAbsolutePath());

                // Create metadata if this is enabled (will take awhile).
                Map<String, Object> metadata = new HashMap<String, Object>();
                if (config.enableMetaData()) {
                    LOGGER.info("creating metadata for " + file.getPath());
                    metadata = DatacatUtilities.createMetadata(file);
                }

                // Register file in the catalog.
                if (!config.dryRun()) {
                    int response = DatacatUtilities.addFile(datacatClient, config.datacatFolder(), file, config.datasetSite(), metadata);
                    LOGGER.info("HTTP response " + response);
                    if (response >= 400) {
                        // Throw exception if response from server indicates an error occurred.
                        throw new RuntimeException("HTTP error code " + response + " received from server.");
                    }
                } else {
                    LOGGER.info("update on " + file.getPath() + " skipped from dry run");
                }
            }
            LOGGER.info("successfully added " + formatFiles.size() + " " + fileFormat + " files");
        }
        LOGGER.info("done updating datacat");
    }
       
    /**
     * Walk the directory tree to find files for the runs that are being processed in the job.
     *
     * @param visitor the file visitor
     */
    private void walk(final DatacatFileVisitor visitor) {
        try {
            // Walk the file tree from the root directory.
            final EnumSet<FileVisitOption> options = EnumSet.noneOf(FileVisitOption.class);
            Files.walkFileTree(config.rootDir().toPath(), options, config.maxDepth(), visitor);
        } catch (final IOException e) {
            throw new RuntimeException("Error while walking the directory tree.", e);
        }
    }
}
