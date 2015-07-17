package org.hps.record.run;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.hps.record.epics.EpicsData;

/**
 * Convert run database records from the <i>run_epics</i> table in to a {@link EpicsData} object.
 *
 * @author Jeremy McCormick, SLAC
 */
final class EpicsDataReader extends AbstractRunDatabaseReader<EpicsData> {

    /**
     * The SQL SELECT query string.
     */
    private final String SELECT_SQL = "SELECT variable_name, value FROM run_epics WHERE run = ?";

    /**
     * Read data from the database and convert to a {@link EpicsData} object.
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

            final EpicsData epicsData = new EpicsData();

            while (resultSet.next()) {
                epicsData.setValue(resultSet.getString("variable_name"), resultSet.getDouble("value"));
            }

            this.setData(epicsData);

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
