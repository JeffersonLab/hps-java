package org.hps.users.jeremym.crawler;

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

class RunLog {

    private static final Logger LOGGER = LogUtil.create(RunLog.class);

    Map<Integer, RunSummary> runs = new HashMap<Integer, RunSummary>();

    void cache() {
        for (final int run : getSortedRunNumbers()) {
            this.runs.get(run).getFiles().cache();
        }
    }

    public RunSummary getRunSummary(final int run) {
        if (!this.runs.containsKey(run)) {
            this.runs.put(run, new RunSummary(run));
        }
        return this.runs.get(run);
    }

    List<Integer> getSortedRunNumbers() {
        final List<Integer> runList = new ArrayList<Integer>(this.runs.keySet());
        Collections.sort(runList);
        return runList;
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

    void update() {
        LOGGER.info("updating database from run log ...");
        final ConnectionParameters cp = new ConnectionParameters("root", "derp", "hps_run_db", "localhost");
        Connection connection = null;
        PreparedStatement runLogStatement = null;
        try {
            connection = cp.createConnection();
            connection.setAutoCommit(false);
            runLogStatement = connection
                    .prepareStatement("INSERT INTO run_log (run, start_date, end_date, nevents, nfiles, end_ok, last_updated) VALUES(?, ?, ?, ?, ?, ?, NOW())");
            for (final Integer run : getSortedRunNumbers()) {
                LOGGER.info("inserting run " + run + " into database");
                final RunSummary runSummary = this.runs.get(run);
                runLogStatement.setInt(1, run);
                runLogStatement.setTimestamp(2, new java.sql.Timestamp(runSummary.getStartDate().getTime()));
                runLogStatement.setTimestamp(3, new java.sql.Timestamp(runSummary.getEndDate().getTime()));
                runLogStatement.setInt(4, runSummary.getTotalEvents());
                runLogStatement.setInt(5, runSummary.getFiles().size());
                runLogStatement.setBoolean(6, runSummary.isEndOkay());
                runLogStatement.executeUpdate();
                connection.commit();
            }
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
                    if (!connection.isClosed()) {
                        connection.close();
                    }
                } catch (final SQLException e) {
                    e.printStackTrace();
                }
            }
        }
        LOGGER.info("database was updated!");
    }

}
