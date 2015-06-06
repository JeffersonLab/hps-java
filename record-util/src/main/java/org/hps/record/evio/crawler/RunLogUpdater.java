package org.hps.record.evio.crawler;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hps.conditions.database.ConnectionParameters;
import org.lcsim.util.log.LogUtil;

/**
 * Updates the run database with run log information from crawler job.
 * 
 * @author Jeremy McCormick, SLAC
 */
public class RunLogUpdater {
    
    /**
     * Setup logging.
     */
    private static final Logger LOGGER = LogUtil.create(RunLogUpdater.class);
    
    private RunLog runLog;
    
    private final Connection connection;
    
    private boolean allowUpdates = false;
        
    RunLogUpdater(RunLog runLog, boolean allowUpdates) {
        this.runLog = runLog;
        
        // Create database connection to use in this session.
        final ConnectionParameters cp = new ConnectionParameters("root", "derp", "hps_run_db", "localhost");
        connection = cp.createConnection();
    }
    
    void close() {
        try {
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Insert the run summary information into the database.
     *
     * @param connection the database connection
     * @throws SQLException if there is an error querying the database
     */
    void insert() throws SQLException {
        
        LOGGER.info("inserting runs into run_log ...");
        try {
            connection.setAutoCommit(false);

            // Update or insert a row for every run found.
            for (final Integer run : runLog.getSortedRunNumbers()) {
                
                RunSummary runSummary = runLog.getRunSummary(run);
                
                LOGGER.info("updating " + runSummary);
                                
                RunSummaryUpdater updater = new RunSummaryUpdater(connection, runSummary);      
                                
                // Does a row already exist for run?
                if (updater.runLogExists()) {
                    LOGGER.info("record for " + run + " exists already");
                    // Are updates allowed?
                    if (allowUpdates) {
                        LOGGER.info("updating existing row in run_log for " + run);
                        // Update existing row.
                        updater.updateRunLog();
                    } else {
                        // Row exists and updates not allowed which is an error.
                        throw new RuntimeException("Row already exists for run " + run + " and allowUpdates is false");
                    }
                } else {                
                    
                    LOGGER.info("inserting new row in run_log for " + run);
                    
                    // Insert new record into run_log.
                    updater.insertRunLog();
                }

                boolean fileLogExists = updater.fileLogExists();
                
                // Are updates disallowed and file log exists?
                if (!allowUpdates && fileLogExists) {
                    // File records exist but updates not allowed so this is an error.
                    throw new RuntimeException("Cannot delete existing file records because allowUpdates is false");                    
                }
                
                // Delete existing file log.
                if (fileLogExists) {
                    // Delete the file log.
                    updater.deleteFileLog();
                }
                
                // Insert the file log.
                updater.insertFileLog();
            }
            
        } catch (final SQLException e) {
            LOGGER.log(Level.SEVERE, "rolling back transaction", e);
            try {
                connection.rollback();
            } catch (final SQLException e2) {
                throw new RuntimeException(e);
            }
        } finally {
            try {
                connection.setAutoCommit(true);
                connection.close();
            } catch (final SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }             
}
