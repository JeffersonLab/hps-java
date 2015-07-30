package org.hps.record.evio.crawler;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hps.record.epics.EpicsData;
import org.hps.record.run.RunSummary;
import org.hps.record.scalers.ScalerData;
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
     * Flag to allow deletion/replacement of existing records; disallowed by default.
     */
    private boolean allowDeleteExisting = false;

    /**
     * The database connection.
     */
    private final Connection connection;

    /**
     * The run number (read from the summary in the constructor for convenience).
     */
    private int run = -1;

    /**
     * The run summary to update or insert.
     */
    private final RunSummary runSummary;

    /**
     * Create a <code>RunSummaryUpdater</code> for the given <code>RunSummary</code>.
     *
     * @param connection the database connection
     * @param runSummary the run summary to update or insert
     */
    RunSummaryUpdater(final Connection connection, final RunSummary runSummary) {

        if (connection == null) {
            throw new IllegalArgumentException("connection is null");
        }
        this.connection = connection;

        if (runSummary == null) {
            throw new IllegalArgumentException("runSummary is null");
        }
        this.runSummary = runSummary;

        // Cache run number.
        this.run = this.runSummary.getRun();
    }

    /**
     * Delete all information for this run from all tables in the database.
     *
     * @throws SQLException if there is a SQL query error
     */
    private void delete() throws SQLException {

        LOGGER.info("deleting existing information for run " + runSummary.getRun());

        // Delete EPICS log.
        this.deleteEpics();

        // Delete scaler data.
        this.deleteScalerData();

        // Delete file list.
        this.deleteFiles();

        // Delete run log.
        this.deleteRun();

        LOGGER.info("deleted run " + runSummary.getRun() + " info successfully");
    }

    /**
     * Delete existing EPICS data from the run_log_epics table.
     *
     * @throws SQLException if there is an error performing the db query
     */
    private void deleteEpics() throws SQLException {
        final PreparedStatement statement = connection.prepareStatement("DELETE FROM run_epics WHERE run = ?");
        statement.setInt(1, this.run);
        statement.executeUpdate();
    }

    /**
     * Delete the records of the files associated to this run.
     *
     * @param files the list of files
     * @throws SQLException if there is an error executing the SQL query
     */
    private void deleteFiles() throws SQLException {
        LOGGER.info("deleting rows from run_files for " + run + " ...");
        final PreparedStatement s = connection.prepareStatement("DELETE FROM run_files where run = ?");
        s.setInt(1, run);
        s.executeUpdate();
        LOGGER.info("done deleting rows from run_files for " + run);
    }

    /**
     * Delete the row for this run from the <i>runs</i> table.
     *
     * @throws SQLException if there is an error executing the SQL query
     */
    private void deleteRun() throws SQLException {
        LOGGER.info("deleting record from runs for " + run + " ...");
        final PreparedStatement s = connection.prepareStatement("DELETE FROM runs where run = ?");
        s.setInt(1, run);
        s.executeUpdate();
        LOGGER.info("deleted rows from runs for " + run);
    }

    /**
     * Delete existing EPICS data from the run_log_epics table.
     *
     * @throws SQLException if there is an error performing the db query
     */
    private void deleteScalerData() throws SQLException {
        final PreparedStatement statement = connection.prepareStatement("DELETE FROM run_scalers WHERE run = ?");
        statement.setInt(1, this.run);
        statement.executeUpdate();
    }

    /**
     * Insert the current {@link RunSummary} into the run database.
     *
     * @throws SQLException if there is a SQL query error
     */
    void insert() throws SQLException {

        LOGGER.info("performing db insert for " + runSummary);

        // Turn auto-commit off as this whole method is a single transaction.
        connection.setAutoCommit(false);

        // Does the run exist in the database already?
        if (this.runExists()) {
            // Is deleting existing rows allowed?
            if (this.allowDeleteExisting) {
                // Delete the existing rows.
                this.delete();
            } else {
                // Rows exist but updating is disallowed which is a fatal error.
                final RuntimeException x = new RuntimeException("Run " + runSummary.getRun()
                        + " already exists and deleting is not allowed.");
                LOGGER.log(Level.SEVERE, x.getMessage(), x);
                throw x;
            }
        }

        // Insert basic run log info.
        this.insertRun();

        // Insert list of files.
        this.insertFiles();

        // Insert EPICS data.
        this.insertEpics();

        // Insert scaler data.
        this.insertScalarData();

        // Commit the transactions for this run.
        LOGGER.info("committing transaction for run " + run);
        connection.commit();

        // Turn auto-commit back on.
        connection.setAutoCommit(true);
    }

    /**
     * Insert EPICS data into the run_log_epics table.
     *
     * @throws SQLException if there is an error performing the db query
     */
    private void insertEpics() throws SQLException {
        final PreparedStatement statement = connection
                .prepareStatement("INSERT INTO run_epics (run, variable_name, value) values (?, ?, ?)");
        final EpicsData data = runSummary.getEpicsData();
        if (data != null) {
            for (final String variableName : data.getKeys()) {
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
     * Insert the file names into the run database.
     *
     * @param connection the database connection
     * @param run the run number
     * @throws SQLException if there is a problem executing one of the database queries
     */
    private void insertFiles() throws SQLException {
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
     * Insert a new row in the <i>runs</i> table.
     *
     * @param connection the database connection
     * @throws SQLException if there is an error querying the database
     */
    private void insertRun() throws SQLException {
        final PreparedStatement statement = connection
                .prepareStatement("INSERT INTO runs (run, start_date, end_date, nevents, nfiles, end_ok, created) VALUES(?, ?, ?, ?, ?, ?, NOW())");
        LOGGER.info("preparing to insert run " + run + " into runs table ..");
        statement.setInt(1, run);
        statement.setTimestamp(2, new java.sql.Timestamp(runSummary.getStartDate().getTime()));
        statement.setTimestamp(3, new java.sql.Timestamp(runSummary.getEndDate().getTime()));
        statement.setInt(4, runSummary.getTotalEvents());
        statement.setInt(5, runSummary.getEvioFileList().size());
        statement.setBoolean(6, runSummary.getEndOkay());
        statement.executeUpdate();
        LOGGER.info("inserted run " + run + " to runs table");
    }

    /**
     * Insert scaler data into the database.
     * 
     * @throws SQLException if there is a SQL query error
     */
    private void insertScalarData() throws SQLException {
        final PreparedStatement statement;
        final ScalerData scalerData = this.runSummary.getScalerData();
        if (scalerData == null) {
            throw new RuntimeException("scaler data is missing");
        }
        try {
            statement = connection.prepareStatement("INSERT INTO run_scalers (run, idx, value) VALUES (?, ?, ?)");
            for (int idx = 0; idx < scalerData.size(); idx++) {
                statement.setInt(1, run);
                statement.setInt(2, idx);
                statement.setInt(3, scalerData.getValue(idx));
                statement.executeUpdate();
            }
        } finally {
            connection.commit();
        }
    }

    /**
     * Return <code>true</code> if there is an existing row for this run summary.
     *
     * @return <code>true</code> if there is an existing row for this run summary.
     * @throws SQLException if there is an error executing the SQL query
     */
    private boolean runExists() throws SQLException {
        final PreparedStatement s = connection.prepareStatement("SELECT run FROM runs where run = ?");
        s.setInt(1, run);
        final ResultSet rs = s.executeQuery();
        return rs.first();
    }

    /**
     * Set whether replacement of existing rows in the database is allowed.
     *
     * @param allowDeleteExisting <code>true</code> to allow replacement of existing rows
     */
    void setAllowDeleteExisting(final boolean allowDeleteExisting) {
        this.allowDeleteExisting = allowDeleteExisting;
    }
}
