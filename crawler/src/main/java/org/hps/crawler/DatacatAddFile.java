package org.hps.crawler;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.srs.datacat.model.DatasetModel;

/**
 * Command line file crawler for populating the data catalog.
 *
 * @author Jeremy McCormick, SLAC
 */
public final class DatacatAddFile {

    /**
     * Setup the logger.
     */
    private static final Logger LOGGER = Logger.getLogger(DatacatCrawler.class.getPackage().getName());
    
    private List<File> paths;
    
    /**
     * Command line options for the crawler.
     */
    private static final Options OPTIONS = new Options();

    /**
     * Statically define the command options.
     */
    static {
        OPTIONS.addOption("h", "help", false, "print help and exit (overrides all other arguments)");
        OPTIONS.addOption("f", "folder", true, "datacat folder");
        OPTIONS.addOption("s", "site", true, "datacat site");
        OPTIONS.addOption("u", "base-url", true, "provide a base URL of the datacat server");
    }

    /**
     * Main method.
     *
     * @param args the command line arguments
     */
    public static void main(final String[] args) {
        new DatacatAddFile().parse(args).run();
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
     * Parse command line options.
     *
     * @param args the command line arguments
     * @return this object (for method chaining)
     */
    private DatacatAddFile parse(final String[] args) {
        
        LOGGER.config("parsing command line options");

        this.config = new CrawlerConfig();

        try {
            final CommandLine cl = this.parser.parse(OPTIONS, args);

            // Print help.
            if (cl.hasOption("h") || args.length == 0) {
                this.printUsage();
            }

            // Datacat folder.
            if (cl.hasOption("f")) {
                config.setDatacatFolder(cl.getOptionValue("f"));
                LOGGER.config("set datacat folder to " + config.folder());
            } else {
                throw new RuntimeException("The -f argument with the datacat folder is required.");
            }

            // Dry run.
            if (cl.hasOption("D")) {
                config.setDryRun(true);
            }
                        
            // List of paths.
            if (!cl.getArgList().isEmpty()) {
                paths = new ArrayList<File>();
                for (String arg : cl.getArgList()) {                    
                    paths.add(new File(arg));
                }
            }
            
            if (this.paths.isEmpty()) {
                throw new RuntimeException("Missing at least one file to process.");
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

        LOGGER.info("Done parsing command line options.");

        return this;
    }

    /**
     * Print the usage statement for this tool to the console and then exit the program.
     */
    private void printUsage() {
        final HelpFormatter help = new HelpFormatter();
        help.printHelp(70, "DatacatAddFile [options] path1 path2 ...", "", OPTIONS, "");
        System.exit(0);
    }

    /**
     * Run the job.
     */
    private void run() {
        List<DatasetModel> datasets = DatacatHelper.createDatasets(paths, config.folder(), config.site().toString());
        DatacatHelper.addDatasets(datasets, config.folder(), config.datacatUrl());
        LOGGER.info("added " + datasets.size() + " datasets");
    }
}
