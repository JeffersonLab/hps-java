package org.hps.run.database;

import java.io.File;
import java.net.URISyntaxException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.hps.conditions.database.ConnectionParameters;
import org.srs.datacat.client.Client;
import org.srs.datacat.client.ClientBuilder;

/**
 * Command line tool for inserting records into the run database.
 *
 * @author Jeremy McCormick, SLAC
 */
public final class RunDatabaseCommandLine {
        
    /**
     * Command line options for the crawler.
     */
    private static final Options OPTIONS = new Options();

    /**
     * Statically define the command options.
     */
    static {
        OPTIONS.addOption("h", "help", false, "print help and exit (overrides all other arguments)");
        OPTIONS.addOption("r", "run", true, "run to update");
        OPTIONS.addOption("p", "connection-properties", true, "database connection properties file (required)");       
        OPTIONS.addOption("Y", "dry-run", false, "dry run which will not update the database");
        OPTIONS.addOption("x", "replace", false, "allow deleting and replacing an existing run");
        OPTIONS.addOption("s", "spreadsheet", true, "path to run database spreadsheet (CSV format)");
        OPTIONS.addOption("d", "detector", true, "conditions system detector name");
        OPTIONS.addOption("N", "no-evio-processing", false, "skip processing of all EVIO files");
        OPTIONS.addOption("L", "load", false, "load back run information after inserting (for debugging)");
        OPTIONS.addOption("u", "url", true, "data catalog URL");
        OPTIONS.addOption("S", "site", true, "data catalog site (e.g. SLAC or JLAB)");
        OPTIONS.addOption("f", "folder", true, "folder in datacat for dataset search");
    }

    /**
     * Run the program from the command line.
     *
     * @param args the command line arguments
     */
    public static void main(final String args[]) {
        new RunDatabaseCommandLine().parse(args).run();
    }
    
    /**
     * Enable dry run which will not update the run database.
     */
    private boolean dryRun = false;
    
    /**
     * Run number.
     */
    private int run;
    
    /**
     * Path to spreadsheet CSV file.
     */
    private File spreadsheetFile = null;
    
    /**
     * Name of detector for conditions system (default for Eng Run 2015 provided here).
     */
    private String detectorName = "HPS-EngRun2015-Nominal-v3";
    
    /**
     * Allow replacement of existing records.
     */
    private boolean replace = false;
    
    /**
     * Skip full EVIO file processing.
     */
    private boolean skipEvioProcessing = false;
    
    /**
     * Load back run information after insert (for debugging).
     */
    private boolean reload = false;
    
    /**
     * Database connection parameters.
     */
    private ConnectionParameters connectionParameters = null;
    
    /**
     * Data catalog client interface.
     */
    private Client datacatClient = null;
    
    /**
     * Data catalog site.
     */
    private String site = "JLAB";                             
    
    /**
     * Data catalog URL.
     */
    private String url = "http://hpsweb.jlab.org/datacat/r";  
    
    /**
     * Default folder for file search.
     */
    private String folder = "/HPS/data/raw";
    
    /**
     * Parse command line options and return reference to <code>this</code> object.
     *
     * @param args the command line arguments
     * @return reference to this object
     */
    private RunDatabaseCommandLine parse(final String args[]) {
        try {
            final CommandLine cl = new DefaultParser().parse(OPTIONS, args);

            // Print help and exit.
            if (cl.hasOption("h") || args.length == 0) {
                final HelpFormatter help = new HelpFormatter();
                help.printHelp("RunDatabaseCommandLine [options]", "", OPTIONS, "");
                System.exit(0);
            }

            // Database connection properties file.
            if (cl.hasOption("p")) {
                final String dbPropPath = cl.getOptionValue("p");
                final File dbPropFile = new File(dbPropPath);
                if (!dbPropFile.exists()) {
                    throw new IllegalArgumentException("Connection properties file " + dbPropFile.getPath()
                            + " does not exist.");
                }
                connectionParameters = ConnectionParameters.fromProperties(dbPropFile);
            } else {
                // Database connection properties file is required.
                throw new RuntimeException("Connection properties are a required argument.");
            }

            // Run number.
            if (cl.hasOption("r")) {
                run = Integer.parseInt(cl.getOptionValue("r"));
            } else {
                throw new RuntimeException("The run number is required.");
            }
            
            // Dry run.
            if (cl.hasOption("Y")) {
                this.dryRun = true;
            }
            
            // Run spreadsheet.
            if (cl.hasOption("s")) {
                this.spreadsheetFile = new File(cl.getOptionValue("s"));
                if (!this.spreadsheetFile.exists()) {
                    throw new RuntimeException("The run spreadsheet " + this.spreadsheetFile.getPath() + " is inaccessible or does not exist.");
                }
            }
            
            // Detector name.
            if (cl.hasOption("d")) {
                this.detectorName = cl.getOptionValue("d");
            }
            
            // Replace existing run.
            if (cl.hasOption("x")) {
                this.replace = true;
            }
            
            // Skip full EVIO processing.
            if (cl.hasOption("N")) {
                this.skipEvioProcessing = true;
            }
            
            // Load back run info at end of job.
            if (cl.hasOption("L")) {
                this.reload = true;
            }
            
            // Data catalog URL.
            if (cl.hasOption("u")) {
                url = cl.getOptionValue("u");
            }
            
            // Site in the data catalog.
            if (cl.hasOption("S")) {
                site = cl.getOptionValue("S");
            }
            
            // Set folder for dataset search.
            if (cl.hasOption("f")) {
                folder = cl.getOptionValue("f");
            }
            
            // Initialize the data catalog client.
            try {
                datacatClient = new ClientBuilder().setUrl(url).build();
            } catch (URISyntaxException e) {
                throw new RuntimeException("Bad datacat URL.", e);
            }
            
        } catch (final ParseException e) {
            throw new RuntimeException(e);
        }

        return this;
    }

    /**
     * Configure the builder from command line options and run the job to update the database.
     */
    private void run() {
        new RunDatabaseBuilder()
            .createRunSummary(run)
            .setFolder(folder)
            .setDetectorName(detectorName)
            .setConnectionParameters(connectionParameters)
            .setDatacatClient(datacatClient)
            .setSite(site)
            .setDryRun(dryRun)
            .setReplace(replace)
            .skipEvioProcessing(skipEvioProcessing)
            .setSpreadsheetFile(spreadsheetFile)
            .setReload(reload)
            .run();
    }        
}
