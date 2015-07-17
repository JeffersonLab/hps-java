package org.hps.record.run;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Convert run database records from the <i>runs</i> table into a {@link RunSummary} object.
 * <p>
 * This class will not create the sub-objects for the {@link RunSummary} which must be read using their own
 * {@link AbstractRunDatabaseReader} implementation classes. Then these objects should be set on the {@link RunSummary}
 * e.g. using {@link RunSummary#setEpicsData(org.hps.record.epics.EpicsData)}, etc.
 *
 * @author Jeremy McCormick, SLAC
 */
public class RunSummaryReader extends AbstractRunDatabaseReader<RunSummary> {

    /**
     * The SQL SELECT query string.
     */
    private final String SELECT_SQL = "SELECT run, start_date, end_date, nevents, nfiles, end_ok, run_ok, updated, created FROM runs WHERE run = ?";

    /**
     * Read data from the database and convert to a {@link RunSummary} object.
     */
    @Override
    void read() {
        if (this.getRun() == -1) {
            throw new IllegalStateException("run number is invalid: " + this.getRun());
        }
        if (this.getConnection() == null) {
            throw new IllegalStateException("Connection is not set.");
        }

        PreparedStatement statement = null;
        try {
            statement = this.getConnection().prepareStatement(SELECT_SQL);
            statement.setInt(1, this.getRun());
            final ResultSet resultSet = statement.executeQuery();
            if (!resultSet.next()) {
                throw new RuntimeException("No record exists for run " + this.getRun() + " in database.");
            }

            final RunSummary runSummary = new RunSummary(this.getRun());
            runSummary.setStartDate(resultSet.getTimestamp("start_date"));
            runSummary.setEndDate(resultSet.getTimestamp("end_date"));
            runSummary.setTotalEvents(resultSet.getInt("nevents"));
            runSummary.setTotalFiles(resultSet.getInt("nfiles"));
            runSummary.setEndOkay(resultSet.getBoolean("end_ok"));
            runSummary.setRunOkay(resultSet.getBoolean("run_ok"));
            runSummary.setUpdated(resultSet.getTimestamp("updated"));
            runSummary.setCreated(resultSet.getTimestamp("created"));

            this.setData(runSummary);

        } catch (final SQLException e) {
            throw new RuntimeException(e);
        } finally {
            if (statement != null) {
                try {
                    statement.close();
                } catch (final SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
