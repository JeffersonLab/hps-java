package org.hps.crawler;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.hps.datacat.DatacatUtilities;
import org.hps.datacat.FileFormat;
import org.hps.datacat.Site;
import org.srs.datacat.model.DatasetModel;

/**
 * Command line file crawler for populating the data catalog.
 *
 * @author Jeremy McCormick, SLAC
 */
public final class DatacatCrawler {

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
        for (final FileFormat format : FileFormat.values()) {
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
        OPTIONS.addOption("d", "directory", true, "root directory to crawl");
        OPTIONS.addOption("f", "folder", true, "datacat folder");
        OPTIONS.addOption("h", "help", false, "print help and exit (overrides all other arguments)");
        OPTIONS.addOption("o", "format", true, "add a file format for filtering: " + AVAILABLE_FORMATS);
        OPTIONS.addOption("r", "run", true, "add a run number to accept");
        OPTIONS.addOption("s", "site", true, "datacat site");
        OPTIONS.addOption("t", "timestamp-file", true, "existing or new timestamp file name");
        OPTIONS.addOption("x", "max-depth", true, "max depth to crawl");
        OPTIONS.addOption("D", "dry-run", false, "dry run which will not update the datacat");
        OPTIONS.addOption("u", "url", true, "provide a base URL of the datacat server");
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
    private final PosixParser parser = new PosixParser();
    
    /**
     * Parse command line options.
     *
     * @param args the command line arguments
     * @return this object (for method chaining)
     */
    private DatacatCrawler parse(final String[] args) {
        
        LOGGER.config("parsing command line options");

        this.config = new CrawlerConfig();

        try {
            final CommandLine cl = this.parser.parse(OPTIONS, args);

            // Print help.
            if (cl.hasOption("h") || args.length == 0) {
                this.printUsage();
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
                    FileFormat format = null;
                    try {
                        format = FileFormat.valueOf(arg);
                    } catch (IllegalArgumentException | NullPointerException e) {
                        throw new IllegalArgumentException("The format " + arg + " is not valid.", e);
                    }
                    LOGGER.config("adding format " + format.name());
                    this.config.addFileFormat(format);
                }
            } else {
                for (FileFormat format : FileFormat.values()) {
                    this.config.addFileFormat(format);
                    LOGGER.config("adding default format " + format);
                }
            }
            
            // Enable the default set of file formats.
            if (this.config.getFileFormats().isEmpty()) {
                LOGGER.config("enabling default file formats");
                this.config.addDefaultFileFormats();
            }

            // Datacat folder.
            if (cl.hasOption("f")) {
                config.setDatacatFolder(cl.getOptionValue("f"));
                LOGGER.config("set datacat folder to " + config.folder());
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
                                    
            // Dry run.
            if (cl.hasOption("D")) {
                config.setDryRun(true);
            }
                        
            // List of paths.
            if (!cl.getArgList().isEmpty()) {
                for (String arg : cl.getArgList()) {
                    config.addPath(arg);
                }
            }
            
            // Dataset site (defaults to JLAB).
            Site site = Site.JLAB;
            if (cl.hasOption("s")) {
                site = Site.valueOf(cl.getOptionValue("s"));
            }
            LOGGER.config("dataset site " + site);
            config.setSite(site);
            
            // Data catalog URL.
            if (cl.hasOption("u")) {
                config.setDatacatUrl(cl.getOptionValue("u"));
                LOGGER.config("datacat URL " + config.datacatUrl());
            }

        } catch (final ParseException e) {
            throw new RuntimeException("Error parsing options.", e);
        }

        // Check that there is at least one file format enabled for filtering.
        if (this.config.getFileFormats().isEmpty()) {
            throw new IllegalStateException("At least one file format must be provided with the -o switch.");
        }

        LOGGER.info("Done parsing command line options.");

        return this;
    }

    /**
     * Print the usage statement for this tool to the console and then exit the program.
     */
    private void printUsage() {
        final HelpFormatter help = new HelpFormatter();
        help.printHelp(70, "DatacatCrawler [options] path ...", "", OPTIONS, "");
        System.exit(0);
    }

    /**
     * Run the crawler job.
     */
    private void run() {
                
        // Create the file visitor for crawling the root directory with the given date filter.
        final CrawlerFileVisitor visitor = new CrawlerFileVisitor();

        // Add date filter if timestamp is supplied.
        if (config.timestamp() != null) {
            visitor.addFilter(new DateFileFilter(config.timestamp()));
            LOGGER.config("added timestamp filter " + config.timestamp());
        }
        
        // Add path filter.
        if (!config.paths().isEmpty()) {
            visitor.addFilter(new PathFilter(config.paths()));
            StringBuffer sb = new StringBuffer();
            for (String path : config.paths()) {
                sb.append(path + ":");
            }
            sb.setLength(sb.length() - 1);
            LOGGER.config("added paths " + sb.toString());
        }

        // Add file format filter.
        visitor.addFilter(new FileFormatFilter(config.getFileFormats()));

        // Add run number filter.
        if (!config.acceptRuns().isEmpty()) {
            visitor.addFilter(new RunFilter(config.acceptRuns()));
        }

        // Walk the file tree and get list of files.
        this.walk(visitor);
                
        // Insert datasets if files were found.
        if (!visitor.getFiles().isEmpty()) {
            List<DatasetModel> datasets = DatacatHelper.createDatasets(visitor.getFiles(), config.folder(), config.site().toString());
            LOGGER.info("built " + datasets.size() + " datasets");
            DatacatUtilities.updateDatasets(datasets, config.folder(), config.datacatUrl(), false);
            LOGGER.info("added datasets to datacat");
        } else {
            LOGGER.warning("No files were found by the crawler.");
        }
        
        LOGGER.info("Done!");
    }
                         
    /**
     * Walk the directory tree to find files for the runs that are being processed in the job.
     *
     * @param visitor the file visitor
     */
    private void walk(final CrawlerFileVisitor visitor) {
        try {
            // Walk the file tree from the root directory.
            final EnumSet<FileVisitOption> options = EnumSet.noneOf(FileVisitOption.class);
            Files.walkFileTree(config.rootDir().toPath(), options, config.maxDepth(), visitor);
        } catch (final IOException e) {
            throw new RuntimeException("Error while walking the directory tree.", e);
        }
    }
}
