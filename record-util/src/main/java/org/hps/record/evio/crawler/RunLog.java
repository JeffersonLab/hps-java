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

final class RunLog {

    private static final Logger LOGGER = LogUtil.create(RunLog.class);

    Map<Integer, RunSummary> runs = new HashMap<Integer, RunSummary>();

    public RunSummary getRunSummary(final int run) {
        if (!this.runs.containsKey(run)) {
            LOGGER.info("creating new RunSummary for run " + run);
            this.runs.put(run, new RunSummary(run));
        }
        return this.runs.get(run);
    }

    List<Integer> getSortedRunNumbers() {
        final List<Integer> runList = new ArrayList<Integer>(this.runs.keySet());
        Collections.sort(runList);
        return runList;
    }

    void insert() {

        LOGGER.info("inserting runs into run_log ...");
        final ConnectionParameters cp = new ConnectionParameters("root", "derp", "hps_run_db", "localhost");
        final Connection connection = cp.createConnection();
        try {
            connection.setAutoCommit(false);

            insertRunLog(connection);

            insertFiles(connection);

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

    void insertFiles(final Connection connection) throws SQLException {
        for (final int run : getSortedRunNumbers()) {
            getRunSummary(run).getFiles().insert(connection, run);
        }
    }

    void insertRunLog(final Connection connection) throws SQLException {
        PreparedStatement runLogStatement = null;
        runLogStatement = connection
                .prepareStatement("INSERT INTO run_log (run, start_date, end_date, nevents, nfiles, end_ok, last_updated) VALUES(?, ?, ?, ?, ?, ?, NOW())");
        for (final Integer run : getSortedRunNumbers()) {
            LOGGER.info("preparing to insert run " + run + " into database ..");
            final RunSummary runSummary = this.runs.get(run);
            runLogStatement.setInt(1, run);
            runLogStatement.setTimestamp(2, new java.sql.Timestamp(runSummary.getStartDate().getTime()));
            runLogStatement.setTimestamp(3, new java.sql.Timestamp(runSummary.getEndDate().getTime()));
            runLogStatement.setInt(4, runSummary.getTotalEvents());
            runLogStatement.setInt(5, runSummary.getFiles().size());
            runLogStatement.setBoolean(6, runSummary.isEndOkay());
            runLogStatement.executeUpdate();
            LOGGER.info("committed run " + run + " to run_log");
        }
        LOGGER.info("run_log was updated!");
    }

    void printRunSummaries() {
        for (final int run : this.runs.keySet()) {
            this.runs.get(run).printRunSummary(System.out);
        }
    }

    void sortAllFiles() {
        for (final Integer run : this.runs.keySet()) {
            this.runs.get(run).sortFiles();
        }
    }
}
