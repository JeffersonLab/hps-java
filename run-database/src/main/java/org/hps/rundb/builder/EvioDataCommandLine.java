package org.hps.rundb.builder;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.hps.conditions.database.ConnectionParameters;
import org.hps.record.evio.EvioFileUtilities;
import org.hps.rundb.RunManager;

/**
 * Extracts information from EVIO files and inserts into the run database.
 * 
 * @author jeremym
 */
public class EvioDataCommandLine {

    private Logger LOGGER = Logger.getLogger(EvioDataCommandLine.class.getPackage().getName());

    private boolean dryRun = false;
    private ConnectionParameters connectionParameters = null;
    private List<File> evioFiles = new ArrayList<File>();

    /**
     * Command line options for the crawler.
     */
    private static final Options OPTIONS = new Options();

    static {
        OPTIONS.addOption("h", "help", false, "print help and exit (overrides all other arguments)");
        OPTIONS.addOption("p", "connection-properties", true, "database connection properties file (required)");
        OPTIONS.getOption("p").setRequired(true);
        OPTIONS.addOption("D", "dry-run", false, "enable dry run with no db update (optional)");
    }
    
    public static void main(String[] args) {
        new EvioDataCommandLine().parse(args).run();
    }

    private EvioDataCommandLine parse(String[] args) {
        try {
            final CommandLine cl = new PosixParser().parse(OPTIONS, args);
            
            if (cl.hasOption("h") || args.length == 0) {
                final HelpFormatter help = new HelpFormatter();
                help.printHelp("EvioDataCommandLine [options] file1 file2 ...", "", OPTIONS, "");
                System.exit(0);
            }

            if (cl.hasOption("D")) {
                dryRun = true;
                LOGGER.config("Dry run enabled; database will not be updated.");
            }

            if (cl.hasOption("p")) {
                final String dbPropPath = cl.getOptionValue("p");
                final File dbPropFile = new File(dbPropPath);
                if (!dbPropFile.exists()) {
                    throw new IllegalArgumentException("Connection properties file " + dbPropFile.getPath()
                            + " does not exist.");
                }
                connectionParameters = ConnectionParameters.fromProperties(dbPropFile);
                LOGGER.config("connection props set from " + dbPropFile.getPath());
            } else {
                // Database connection properties file is required.
                throw new RuntimeException("Connection properties are a required argument.");
            }

            for (String arg : cl.getArgList()) {
                evioFiles.add(new File(arg));
                LOGGER.config("adding file " + arg + " to job");
            }

            if (evioFiles.isEmpty()) {
                throw new RuntimeException("No EVIO files were provided from the command line.");
            }

        } catch (ParseException e) {
            throw new RuntimeException("Error parsing command line arguments.", e);
        }
        return this;
    }

    private void run() {
        
        RunManager runManager = new RunManager(this.connectionParameters.createConnection());
        Connection connection = runManager.getConnection();
        try {
            connection.setAutoCommit(false);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        
        for (File evioFile : evioFiles) {

            LOGGER.info("Processing file " + evioFile.getPath() + " ...");
            
            int run = EvioFileUtilities.getRunFromName(evioFile);
            
            EvioDataBuilder builder = new EvioDataBuilder();
            builder.setEvioFile(evioFile);
            builder.build();
                       
            LOGGER.info("Found " + builder.getEpicsData().size() + " EPICS records.");
            LOGGER.info("Found " + builder.getScalerData().size() + " scaler records.");            
            
            LOGGER.info("Set run " + run + " from file " + evioFile);
            
            runManager.setRun(run);
                                    
            if (!dryRun) {
                
                try {
                    runManager.updateEpicsData(builder.getEpicsData());
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Problem updating EPICS data in db.", e);
                }

                try {
                    runManager.updateScalerData(builder.getScalerData());
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Problem updating scaler data in db.", e);
                }

                try {
                    if (builder.getTriggerConfig() != null) {
                        runManager.updateTriggerConfig(builder.getTriggerConfig(), false /* do not replace existing config */);
                    } else {
                        LOGGER.info("No valid trigger config data was found.");
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Problem updating trigger config data in db.", e);
                }
            } else {
                LOGGER.info("Dry run is enabled; database will not be updated.");
            }
            
            LOGGER.info("Done processing " + evioFile.getPath());
        }
        
        // Commit the transaction.
        try {
            connection.commit();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to commit db transaction.", e);
        }
        
        try {
            connection.close();
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Error closing db connection.", e);
        }        
    }
}
