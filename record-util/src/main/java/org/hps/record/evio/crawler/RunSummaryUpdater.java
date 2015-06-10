package org.hps.record.evio.crawler;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hps.record.epics.EpicsData;
import org.lcsim.util.log.LogUtil;

/**
 * Updates the run database tables with information from a single run.
 * 
 * @author Jeremy McCormick, SLAC
 */
public class RunSummaryUpdater {

    /**
     * Setup logging.
     */
    private static final Logger LOGGER = LogUtil.create(RunSummaryUpdater.class);
    
    /**
     * The run summary to update or insert.
     */
    private RunSummary runSummary;   
    
    /**
     * The database connection.
     */
    private Connection connection;
    
    /**
     * The run number (read from the summary in the constructor for convenience).
     */
    private int run = -1;
        
    /**
     * Create a <code>RunSummaryUpdater</code> for the given <code>RunSummary</code>.
     * 
     * @param connection the database connection
     * @param runSummary the run summary to update or insert
     */
    RunSummaryUpdater(Connection connection, RunSummary runSummary) {

        if (connection == null) {
            throw new IllegalArgumentException("connection is null");
        }
        this.connection = connection;
        
        if (runSummary == null) {
            throw new IllegalArgumentException("runSummary is null");
        }
        this.runSummary = runSummary;
        
        this.run = this.runSummary.getRun();
    }
        
    /**
     * Execute a SQL update to modify an existing row in the database.
     * 
     * @throws SQLException if there is an error executing the SQL statement
     */
    void updateRun() throws SQLException {
        
        PreparedStatement runLogStatement = null;
        runLogStatement = 
                connection.prepareStatement("UPDATE runs SET start_date = ?, end_date = ?, nevents = ?, nfiles = ?, end_ok = ? where run = ?");        
        LOGGER.info("preparing to update run " + run + " in runs table ..");
        runLogStatement.setTimestamp(1, new java.sql.Timestamp(runSummary.getStartDate().getTime()));
        runLogStatement.setTimestamp(2, new java.sql.Timestamp(runSummary.getEndDate().getTime()));
        runLogStatement.setInt(3, runSummary.getTotalEvents());
        runLogStatement.setInt(4, runSummary.getEvioFileList().size());
        runLogStatement.setBoolean(5, runSummary.isEndOkay());
        runLogStatement.setInt(6, run);
        runLogStatement.executeUpdate();
        LOGGER.info("run " + run + " was updated");
    }
    
    /**
     * Insert a new row in the <i>runs</i> table.
     *
     * @param connection the database connection
     * @throws SQLException if there is an error querying the database
     */
    void insertRun() throws SQLException {
        PreparedStatement statement = 
                connection.prepareStatement("INSERT INTO runs (run, start_date, end_date, nevents, nfiles, end_ok) VALUES(?, ?, ?, ?, ?, ?)");
        LOGGER.info("preparing to insert run " + run + " into runs table ..");
        statement.setInt(1, run);
        statement.setTimestamp(2, new java.sql.Timestamp(runSummary.getStartDate().getTime()));
        statement.setTimestamp(3, new java.sql.Timestamp(runSummary.getEndDate().getTime()));
        statement.setInt(4, runSummary.getTotalEvents());
        statement.setInt(5, runSummary.getEvioFileList().size());
        statement.setBoolean(6, runSummary.isEndOkay());
        statement.executeUpdate();
        LOGGER.info("inserted run " + run + " to runs table");
    }
    
    /**
     * Return <code>true</code> if there is an existing row for this run summary.
     * 
     * @return <code>true</code> if there is an existing row for this run summary.
     * @throws SQLException if there is an error executing the SQL query
     */
    boolean runExists() throws SQLException {
        PreparedStatement s = connection.prepareStatement("SELECT run FROM runs where run = ?");
        s.setInt(1, run);        
        ResultSet rs = s.executeQuery();
        return rs.first();
    }
    
    /**
     * Insert the file names into the run database.    
     *
     * @param connection the database connection
     * @param run the run number
     * @throws SQLException if there is a problem executing one of the database queries
     */
    void insertFiles() throws SQLException {
        LOGGER.info("updating file list ...");
        PreparedStatement filesStatement = null;
        filesStatement = connection.prepareStatement("INSERT INTO run_files (run, directory, name) VALUES(?, ?, ?)");
        LOGGER.info("inserting files from run " + run + " into run_files ...");
        for (final File file : runSummary.getEvioFileList()) {
            LOGGER.info("creating update statement for " + file.getPath());
            filesStatement.setInt(1, run);
            filesStatement.setString(2, file.getParentFile().getPath());
            filesStatement.setString(3, file.getName());
            LOGGER.info("executing statement: " + filesStatement);
            filesStatement.executeUpdate();
        }
        LOGGER.info("run_files was updated");
    }    
    
    /**
     * Delete the records of the files associated to this run.
     * 
     * @param files the list of files
     * @throws SQLException if there is an error executing the SQL query
     */
    void deleteFiles() throws SQLException {        
        LOGGER.info("deleting rows from run_files for " + run + " ...");
        PreparedStatement s = connection.prepareStatement("DELETE FROM run_files where run = ?");
        s.setInt(1, run);
        s.executeUpdate();
        LOGGER.info("done deleting rows from run_files for " + run);
    }
    
    /**
     * Delete the row for this run from the <i>runs</i> table.
     * <p>
     * This doesn't delete the rows from <i>run_epics</i> or <i>run_files</i>.
     * 
     * @throws SQLException if there is an error executing the SQL query
     */
    void deleteRun() throws SQLException {
        LOGGER.info("deleting record from runs for " + run + " ...");
        PreparedStatement s = connection.prepareStatement("DELETE FROM runs where run = ?");
        s.setInt(1, run);
        s.executeUpdate();
        LOGGER.info("deleted rows from runs for " + run);
    }
    
    /**
     * Return <code>true</code> if there is a row for at least one file for the run.
     * @return <code>true</code> if there are file rows for this run
     * @throws SQLException if there is an error executing the SQL query
     */
    boolean filesExist() throws SQLException {
        PreparedStatement s = connection.prepareStatement("SELECT run FROM run_files where run = ?");
        s.setInt(1, run);        
        ResultSet rs = s.executeQuery();
        return rs.first();
    }
    
    /**
     * Insert EPICS data into the run_log_epics table.
     * 
     * @throws SQLException if there is an error performing the db query
     */
    void insertEpics() throws SQLException {
        PreparedStatement statement = connection.prepareStatement("INSERT INTO run_epics (run, variable_name, value) values (?, ?, ?)");
        EpicsData data = runSummary.getEpicsData();
        if (data != null) {
            for (String variableName : data.getUsedNames()) {
                statement.setInt(1, this.run);
                statement.setString(2, variableName);
                statement.setDouble(3, data.getValue(variableName));
                statement.executeUpdate();
            }
        } else {
            LOGGER.warning("skipped inserting EPICS data (none found in RunSummary)");
        }
    }
    
    /**
     * Delete existing EPICS data from the run_log_epics table.
     * 
     * @throws SQLException if there is an error performing the db query
     */
    void deleteEpics() throws SQLException {
        PreparedStatement statement = connection.prepareStatement("DELETE FROM run_epics WHERE run = ?");
        statement.setInt(1, this.run);
        statement.executeUpdate();
    }
    
    /**
     * Return <code>true</code> if there is an existing row for this run summary.
     * 
     * @return <code>true</code> if there is an existing row for this run summary.
     * @throws SQLException if there is an error executing the SQL query
     */
    boolean epicsExists() throws SQLException {
        PreparedStatement s = connection.prepareStatement("SELECT run from run_epics where run = ?");
        s.setInt(1, run);
        ResultSet rs = s.executeQuery();
        return rs.first();
    }
}