package org.hps.record.evio.crawler;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hps.conditions.database.ConnectionParameters;
import org.lcsim.util.log.LogUtil;

/**
 * This class contains summary information about a series of runs that are themselves modeled with the {@link RunSummary} class. These can be looked
 * up by their run number.
 * <p>
 * This class is able to update the run database using the <code>insert</code> methods.
 *
 * @author Jeremy McCormick
 */
final class RunLog {

    /**
     * Setup logging.
     */
    private static final Logger LOGGER = LogUtil.create(RunLog.class);

    /**
     * A map between run numbers and the run summary information.
     */
    private final Map<Integer, RunSummary> runs = new HashMap<Integer, RunSummary>();

    /**
     * Get a run summary by run number.
     * <p>
     * It will be created if it does not exist.
     *
     * @param run the run number
     * @return the <code>RunSummary</code> for the run number
     */
    public RunSummary getRunSummary(final int run) {
        if (!this.runs.containsKey(run)) {
            LOGGER.info("creating new RunSummary for run " + run);
            this.runs.put(run, new RunSummary(run));
        }
        return this.runs.get(run);
    }

    /**
     * Get a list of sorted run numbers in this run log.
     * <p>
     * This is a copy of the keys from the map so modifying it will have no effect on this class.
     *
     * @return the list of sorted run numbers
     */
    List<Integer> getSortedRunNumbers() {
        final List<Integer> runList = new ArrayList<Integer>(this.runs.keySet());
        Collections.sort(runList);
        return runList;
    }

    /**
     * Insert all the information from the run log into the run database.
     */
    void insert() {

        LOGGER.info("inserting runs into run_log ...");
        final ConnectionParameters cp = new ConnectionParameters("root", "derp", "hps_run_db", "localhost");
        final Connection connection = cp.createConnection();
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
            if (connection != null) {
                try {
                    connection.setAutoCommit(true);
                    connection.close();
                } catch (final SQLException e) {
                    throw new RuntimeException(e);
                }
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
        for (final int run : this.getSortedRunNumbers()) {
            this.getRunSummary(run).getEvioFileList().insert(connection, run);
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
        for (final Integer run : this.getSortedRunNumbers()) {
            LOGGER.info("preparing to insert run " + run + " into database ..");
            final RunSummary runSummary = this.runs.get(run);
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
     * Print out the run summaries to <code>System.out</code>.
     */
    void printRunSummaries() {
        for (final int run : this.runs.keySet()) {
            this.runs.get(run).printRunSummary(System.out);
        }
    }

    /**
     * Sort all the file lists in place (by sequence number).
     */
    void sortAllFiles() {
        for (final Integer run : this.runs.keySet()) {
            this.runs.get(run).sortFiles();
        }
    }
}
