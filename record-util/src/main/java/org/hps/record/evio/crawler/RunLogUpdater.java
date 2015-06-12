package org.hps.record.evio.crawler;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hps.conditions.database.ConnectionParameters;
import org.lcsim.util.log.LogUtil;

/**
 * Updates the run database with information from the crawler job.
 * <p>
 * The {@link RunSummaryUpdater} is used to insert rows for each run.
 * 
 * @author Jeremy McCormick, SLAC
 */
public class RunLogUpdater {
    
    /**
     * Setup logging.
     */
    private static final Logger LOGGER = LogUtil.create(RunLogUpdater.class);
    
    /**
     * The run log with information for all runs.
     */
    private RunLog runLog;
    
    /**
     * The database connection.
     */
    private final Connection connection;
    
    /**
     * <code>true</code> if updates should be allowed in database or only inserts.
     */
    private boolean allowUpdates = false;
    
    /**
     * <code>true</code> if EPICS data should be put into the database (skipped if not).
     */
    private boolean insertEpicsData = true;

    /**
     * Create a new updater.
     * 
     * @param runLog the run information
     * @param allowUpdates <code>true</code> if updates should be allowed
     */
    RunLogUpdater(RunLog runLog, boolean allowUpdates) {
        this.runLog = runLog;
        
        // Create database connection to use in this session.
        final ConnectionParameters cp = new ConnectionParameters("root", "derp", "hps_run_db", "localhost");
        connection = cp.createConnection();
    }
    
    /**
     * Set to <code>true</code> if EPICS data should be inserted.
     * 
     * @param insertEpicsData <code>true</code> 
     */
    void setInsertEpicsData(boolean insertEpicsData) {
        this.insertEpicsData = insertEpicsData;
    }
    
    /**
     * Close the database connection.
     */
    void close() {
        try {
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Insert the run summary information into the database, including updating the run_log_files
     * and run_log_epics tables.
     *
     * @param connection the database connection
     * @throws SQLException if there is an error querying the database
     */
    void insert() throws SQLException {
        
        LOGGER.info("inserting run data into database ...");
        try {
            connection.setAutoCommit(false);
            
            // Loop over all runs found while crawling.
            for (final Integer run : runLog.getSortedRunNumbers()) {
                
                LOGGER.info("beginning transaction for run " + run);
                
                // Get the RunSummary data for the run.
                RunSummary runSummary = runLog.getRunSummary(run);
                
                LOGGER.info("updating " + runSummary);
                                
                // Create the db updater for the RunSummary.
                RunSummaryUpdater runUpdater = new RunSummaryUpdater(connection, runSummary);      
                                
                // Does a row already exist for the run?
                if (runUpdater.runExists()) {
                    LOGGER.info("record for " + run + " exists already");
                    // Are updates allowed?
                    if (allowUpdates) {
                        LOGGER.info("existing row for " + run + " will be updated");
                        // Update existing row.
                        runUpdater.updateRun();
                    } else {
                        // Row exists and updates not allowed which is an error.
                        throw new RuntimeException("Row already exists for run " + run + " and allowUpdates is false.");
                    }
                } else {                
                                        
                    LOGGER.info("inserting new row in runs for run " + run + " ...");
                    
                    // Insert new record into run_log.
                    runUpdater.insertRun();
                }
                
                // Do records exist in the run_log_files table?
                if (runUpdater.filesExist()) {
                    // Are updates disallowed?
                    if (!allowUpdates) { 
                        // File records exist for the run but updating is allowed so throw an exception.
                        throw new RuntimeException("Cannot delete existing records in run_log_files because allowUpdates is false.");
                    } else {
                        // Delete the file log.
                        runUpdater.deleteFiles();
                    }
                }
                                
                // Insert records into run_log_files now that existing records were deleted, if necessary.
                runUpdater.insertFiles();
                
                // Is EPICS data processing enabled?
                if (insertEpicsData) {
                    // Does the EPICS data already exist?
                    if (runUpdater.epicsExists()) {
                        // Is replacing data disallowed?
                        if (!allowUpdates) {
                            // EPICS data exists but updating is not allowed so throw exception.
                            throw new RuntimeException("EPICS run log already exists and allowUpdates is false.");
                        } else {
                            // Delete existing EPICS data.
                            runUpdater.deleteEpics();
                        }
                    }
                    
                    // Insert EPICS data processed in the job for this run.
                    runUpdater.insertEpics();
                }
                
                // Commit the transactions for this run.
                LOGGER.info("committing transaction for run " + run);
                connection.commit();
            }
            
        } catch (final SQLException e) {
            LOGGER.log(Level.SEVERE, "rolling back transaction", e);
            try {
                connection.rollback();
            } catch (final SQLException e2) {
                LOGGER.log(Level.SEVERE, "error rolling back transaction", e2);
                throw new RuntimeException(e);
            }
        } finally {
            try {
                connection.close();
            } catch (final SQLException e) {
                throw new RuntimeException(e);
            }
        }
        
        LOGGER.info("done inserting run data");
    }             
}
