package org.hps.rundb.builder;

import java.io.File;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Logger;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.hps.conditions.database.ConnectionParameters;
import org.hps.rundb.DaoProvider;
import org.hps.rundb.RunManager;
import org.hps.rundb.RunSummaryDao;
import org.hps.rundb.RunSummaryImpl;
import org.srs.datacat.client.ClientBuilder;

import org.hps.datacat.DatacatConstants;
import org.hps.datacat.Site;

/**
 * Creates a basic run database record from information in the data catalog 
 * as well as (optionally) a CSV dump of the run spreadsheet from Google Docs.
 * 
 * @author jeremym
 */
public class BuilderCommandLine {
    
    private static final Logger LOGGER = 
            Logger.getLogger(BuilderCommandLine.class.getPackage().getName());
       
    /**
     * Command line options for the crawler.
     */
    private static final Options OPTIONS = new Options();
    
    /**
     * Statically define the command options.
     */
    static {
        OPTIONS.addOption("h", "help", false, "print help and exit (overrides all other arguments)");
        OPTIONS.addOption("r", "run", true, "run to insert or update (required)");
        OPTIONS.getOption("r").setRequired(true);
        OPTIONS.addOption("p", "connection-properties", true, "database connection properties file (required)");
        OPTIONS.getOption("p").setRequired(true);
        OPTIONS.addOption("s", "spreadsheet", true, "path to run database spreadsheet CSV file (optional)");
        OPTIONS.addOption("u", "url", true, "data catalog URL (optional)");
        OPTIONS.addOption("S", "site", true, "data catalog site e.g. SLAC or JLAB (optional)");
        OPTIONS.addOption("f", "folder", true, "folder in datacat for dataset search (optional)");
        OPTIONS.addOption("D", "dry-run", false, "enable dry run with no db update (optional)");
    }

    /**
     * Run the program from the command line.
     *
     * @param args the command line arguments
     */
    public static void main(final String args[]) {
        new BuilderCommandLine().parse(args).run();
    }
        
    /**
     * Run number.
     */
    private int run;
    
    /**
     * Path to spreadsheet CSV file.
     */
    private File spreadsheetFile = null;
        
    /**
     * Data catalog site.
     */
    private String site = Site.JLAB.toString();
    
    /**
     * Data catalog URL.
     */
    private String url = DatacatConstants.DATACAT_URL;  
    
    /**
     * Default folder for file search.
     */
    private String folder = DatacatConstants.RAW_DATA_FOLDER;
    
    /**
     * Database connection parameters.
     */
    private ConnectionParameters connectionParameters = null;
    
    /**
     * <code>true</code> if database should not be updated.
     */
    private boolean dryRun = false;
    
    /**
     * Parse command line options and return reference to <code>this</code> object.
     *
     * @param args the command line arguments
     * @return reference to this object
     */
    private BuilderCommandLine parse(final String args[]) {
        try {
            final CommandLine cl = new PosixParser().parse(OPTIONS, args);

            // Print help and exit.
            if (cl.hasOption("h") || args.length == 0) {
                final HelpFormatter help = new HelpFormatter();
                help.printHelp("RunDatabaseCommandLine [options]", "", OPTIONS, "");
                System.exit(0);
            }
            
            // Run number.
            if (cl.hasOption("r")) {
                run = Integer.parseInt(cl.getOptionValue("r"));
            } else {
                throw new RuntimeException("The run number is required.");
            }
                        
            // Run spreadsheet.
            if (cl.hasOption("s")) {
                this.spreadsheetFile = new File(cl.getOptionValue("s"));
                if (!this.spreadsheetFile.exists()) {
                    throw new RuntimeException("The run spreadsheet " + this.spreadsheetFile.getPath() + " is inaccessible or does not exist.");
                }
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
            
            // Database connection properties file.
            if (cl.hasOption("p")) {
                final String dbPropPath = cl.getOptionValue("p");
                final File dbPropFile = new File(dbPropPath);
                if (!dbPropFile.exists()) {
                    throw new IllegalArgumentException("Connection properties file " + dbPropFile.getPath() + " does not exist.");
                }
                connectionParameters = ConnectionParameters.fromProperties(dbPropFile);
            } else {
                // Database connection properties file is required.
                throw new RuntimeException("Connection properties are a required argument.");
            }
            
            if (cl.hasOption("D")) {
                this.dryRun = true;
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
        
        System.out.println("connecting to " + this.connectionParameters.getConnectionString() + " ...");
                        
        RunManager mgr = new RunManager(this.connectionParameters.createConnection());
        Connection connection = mgr.getConnection();
        try {
            connection.setAutoCommit(true);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        
        mgr.setRun(run);
        
        RunSummaryImpl runSummary = new RunSummaryImpl(run);
        
        // build info from datacat
        DatacatBuilder datacatBuilder = new DatacatBuilder();
        try {
            datacatBuilder.setDatacatClient(new ClientBuilder().setUrl(url).build());
        } catch (URISyntaxException e) {
            throw new RuntimeException("Datacat URL " + url + " is invalid.", e);
        }
        datacatBuilder.setFolder(folder);
        datacatBuilder.setSite(site);
        datacatBuilder.setRunSummary(runSummary);
        datacatBuilder.build();
                
        // build info from run spreadsheet
        if (spreadsheetFile != null) {
            SpreadsheetBuilder spreadsheetBuilder = new SpreadsheetBuilder();
            spreadsheetBuilder.setSpreadsheetFile(spreadsheetFile);
            spreadsheetBuilder.setRunSummary(datacatBuilder.getRunSummary());
            spreadsheetBuilder.build();
        } else {
            LOGGER.warning("No run spreadsheet provided with command line option!");
        }
        
        LOGGER.info(runSummary.toString());
                
        // insert run summary
        if (!dryRun) {
            RunSummaryDao runSummaryDao = new DaoProvider(connection).getRunSummaryDao();
            if (mgr.runExists()) {
                System.out.println("updating existing run summary ...");
                runSummaryDao.updateRunSummary(runSummary);
            } else {
                System.out.println("inserting new run summary ...");
                runSummaryDao.insertRunSummary(runSummary);
            }        
        
            try {
                System.out.println("closing db connection ...");
                connection.close();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        } else {
            LOGGER.info("Dry run enabled.  Database was not updated!");
        }
        
        System.out.println("DONE!");
    }        
}
