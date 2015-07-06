package org.hps.record.evio.crawler;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.lcsim.util.log.LogUtil;

/**
 * Updates the run database with information from the crawler job.
 * <p>
 * The {@link RunSummaryUpdater} is used to insert the data for each run.
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
     * Create a new updater.
     * 
     * @param runLog the run information
     * @param allowUpdates <code>true</code> if updates should be allowed
     */
    RunLogUpdater(Connection connection, RunLog runLog, boolean allowUpdates) {
        
        // Set the DB connection.
        this.connection = connection;
        
        // Set the run log with the run info to update.
        this.runLog = runLog;
        
        // Set whether db updates are allowed (replacement of existing records).
        this.allowUpdates = allowUpdates;
    }
            
    /**
     * Insert the run summary information into the database, including updating the <i>run_log_files</i>
     * and <i>run_log_epics</i> tables.
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
                                                
                // Set whether existing records can be replaced.
                runUpdater.setAllowDeleteExisting(allowUpdates);
                
                LOGGER.info("allow updates: " + allowUpdates);
                
                // Insert the run records.
                runUpdater.insert();
                
                LOGGER.info("run " + runSummary.getRun() + " summary inserted successfully");
            }
            
        } catch (final SQLException e) {
            LOGGER.log(Level.SEVERE, "rolling back transaction", e);
            try {
                connection.rollback();
            } catch (final SQLException e2) {
                LOGGER.log(Level.SEVERE, "error rolling back transaction", e2);
                throw new RuntimeException(e);
            }
        } 
        
        LOGGER.info("done inserting run data");
    }             
}
