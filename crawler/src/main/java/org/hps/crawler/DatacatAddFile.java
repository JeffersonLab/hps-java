package org.hps.crawler;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.hps.datacat.DatacatConstants;
import org.hps.datacat.DatacatUtilities;
import org.hps.datacat.Site;
import org.srs.datacat.model.DatasetModel;

/**
 * Command line tool for adding files to the data catalog.
 *
 * @author Jeremy McCormick, SLAC
 */
public final class DatacatAddFile {

    private static final Logger LOGGER = Logger.getLogger(DatacatCrawler.class.getPackage().getName());
    
    private List<File> paths = new ArrayList<File>();
    
    private String folder = null;
    private Site site = Site.JLAB;
    private String datacatUrl = DatacatConstants.DATACAT_URL;
    private boolean dryRun = false;
    
    /**
     * Command line options.
     */
    private static final Options OPTIONS = new Options();

    /**
     * Statically define the command options.
     */
    static {
        OPTIONS.addOption("h", "help", false, "Print help and exit (overrides all other arguments).");
        OPTIONS.addOption("f", "folder", true, "Datacat folder (required)");
        OPTIONS.addOption("s", "site", true, "Datacat site (default is JLAB)");
        OPTIONS.addOption("u", "url", true, "Set the URL of a datacat server (default is JLAB prod server)");
        OPTIONS.addOption("D", "dry-run", false, "Dry run mode which will not update the datacat");
        OPTIONS.addOption("p", "patch", false, "Allow patching of existing records in the datacat");
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
     * The options parser.
     */
    private final PosixParser parser = new PosixParser();
    
    private boolean patch = false;
    
    /**
     * Parse command line options.
     *
     * @param args the command line arguments
     * @return this object (for method chaining)
     */
    private DatacatAddFile parse(final String[] args) {
        
        LOGGER.config("parsing command line options");

        try {
            final CommandLine cl = this.parser.parse(OPTIONS, args);

            // Print help.
            if (cl.hasOption("h") || args.length == 0) {
                this.printUsage();
            }

            // Datacat folder.
            if (cl.hasOption("f")) {
                folder = cl.getOptionValue("f");
                LOGGER.config("set datacat folder to " + folder);
            } else {
                throw new RuntimeException("The -f argument with the datacat folder is required.");
            }

            // Dry run.
            if (cl.hasOption("D")) {
                this.dryRun = true;
            }
                        
            // List of paths.
            if (!cl.getArgList().isEmpty()) {
                for (String arg : cl.getArgList()) {                    
                    paths.add(new File(arg));
                }
            }
            
            if (this.paths.isEmpty()) {
                throw new RuntimeException("Missing at least one file to process.");
            }
            
            // Dataset site (defaults to JLAB).
            if (cl.hasOption("s")) {
                this.site = Site.valueOf(cl.getOptionValue("s"));
            }
            LOGGER.config("datacat site: " + site);
                        
            // Data catalog URL.
            if (cl.hasOption("u")) {
                datacatUrl = cl.getOptionValue("u");
            }
            LOGGER.config("datacat url: " + datacatUrl);

            if (cl.hasOption("p")) {
                this.patch = true;
            }
            
        } catch (final ParseException e) {
            throw new RuntimeException("Error parsing the command line options.", e);
        }

        LOGGER.info("Done parsing command line options.");

        return this;
    }

    /**
     * Print the usage statement and then exit.
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
        List<DatasetModel> datasets = DatacatHelper.createDatasets(paths, folder, site.toString());
        DatacatUtilities util = new DatacatUtilities();
        if (!dryRun) {
            util.updateDatasets(datasets, folder, patch);
            //LOGGER.info("Added " + datasets.size() + " datasets to datacat.");
        } else {
            LOGGER.info("Dry run is enabled; skipped adding dataset.");
        }
     }
}
