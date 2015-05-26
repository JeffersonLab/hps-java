package org.hps.record.evio.crawler;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hps.conditions.database.ConnectionParameters;
import org.lcsim.util.log.LogUtil;

/**
 * Updates the run database with run log information from crawler job.
 * 
 * @author Jeremy McCormick
 */
public class RunLogUpdater {
    
    /**
     * Setup logging.
     */
    private static final Logger LOGGER = LogUtil.create(RunLogUpdater.class);
    
    RunLog runLog;
    
    final Connection connection;
        
    RunLogUpdater(RunLog runLog) {
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
            
    boolean hasRun(int run) {
        boolean hasRun = false;
        PreparedStatement statement = null;
        try {
            statement = connection.prepareStatement("SELECT run from run_log where run = ?");
            statement.setInt(1, run);
            ResultSet rs = statement.executeQuery();
            if (rs.next()) hasRun = true;
        } catch (final SQLException e) {
            throw new RuntimeException(e);
        } 
        return hasRun;
    }
    
    /**
     * Insert all the information from the run log into the run database.
     */
    void insert() {

        LOGGER.info("inserting runs into run_log ...");
        try {
            connection.setAutoCommit(false);

            this.insertRunLog(connection);

            this.insertFiles(connection);

            connection.commit();

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
            } catch (final SQLException e) {
                throw new RuntimeException(e);
            }            
        }
    }

    /**
     * Insert the file lists into the run database.
     *
     * @param connection the database connection
     * @throws SQLException if there is an error executing the SQL query
     */
    private void insertFiles(final Connection connection) throws SQLException {
        for (final int run : runLog.getSortedRunNumbers()) {
            insertFiles(connection, run, runLog.getRunSummary(run).getEvioFileList());
        }
    }

    /**
     * Insert the run summary information into the database.
     *
     * @param connection the database connection
     * @throws SQLException if there is an error querying the database
     */
    private void insertRunLog(final Connection connection) throws SQLException {
        PreparedStatement runLogStatement = null;
        runLogStatement = connection
                .prepareStatement("INSERT INTO run_log (run, start_date, end_date, nevents, nfiles, end_ok, last_updated) VALUES(?, ?, ?, ?, ?, ?, NOW())");
        for (final Integer run : runLog.getSortedRunNumbers()) {
            LOGGER.info("preparing to insert run " + run + " into database ..");
            final RunSummary runSummary = runLog.getRunSummary(run);
            runLogStatement.setInt(1, run);
            runLogStatement.setTimestamp(2, new java.sql.Timestamp(runSummary.getStartDate().getTime()));
            runLogStatement.setTimestamp(3, new java.sql.Timestamp(runSummary.getEndDate().getTime()));
            runLogStatement.setInt(4, runSummary.getTotalEvents());
            runLogStatement.setInt(5, runSummary.getEvioFileList().size());
            runLogStatement.setBoolean(6, runSummary.isEndOkay());
            runLogStatement.executeUpdate();
            LOGGER.info("committed run " + run + " to run_log");
        }
        LOGGER.info("run_log was updated!");
    }
    
    /**
     * Insert the file names into the run database.
     *
     * @param connection the database connection
     * @param run the run number
     * @throws SQLException if there is a problem executing one of the database queries
     */
    void insertFiles(final Connection connection, final int run, List<File> files) throws SQLException {
        LOGGER.info("updating file list ...");
        PreparedStatement filesStatement = null;
        filesStatement = connection.prepareStatement("INSERT INTO run_log_files (run, directory, name) VALUES(?, ?, ?)");
        LOGGER.info("inserting files from run " + run + " into database");
        for (final File file : files) {
            LOGGER.info("creating update statement for " + file.getPath());
            filesStatement.setInt(1, run);
            filesStatement.setString(2, file.getParentFile().getPath());
            filesStatement.setString(3, file.getName());
            LOGGER.info("executing statement: " + filesStatement);
            filesStatement.executeUpdate();
        }
        LOGGER.info("run_log_files was updated!");
    }    
}
