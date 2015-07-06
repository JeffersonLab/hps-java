package org.hps.record.evio.crawler;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.hps.record.scalers.ScalerData;

/**
 * Database interface for inserting scaler data into the run log.
 * 
 * @author Jeremy McCormick, SLAC
 */
public class ScalerDataUpdater {

    private boolean allowDeleteExisting = false;

    private final Connection connection;

    private final ScalerData data;

    private final int run;

    private final String SQL_DELETE = "DELETE FROM run_scalers WHERE run = ?";

    private final String SQL_INSERT = "INSERT INTO run_scalers (run, idx, value) VALUES (?, ?, ?)";

    private final String SQL_SELECT = "SELECT run FROM run_scalers WHERE run = ?";

    ScalerDataUpdater(final Connection connection, final ScalerData data, final int run) {
        this.connection = connection;
        this.data = data;
        this.run = run;
    }

    private void delete() throws SQLException {
        final PreparedStatement s = connection.prepareStatement(SQL_DELETE);
        s.setInt(1, run);
        s.executeUpdate();
    }

    private boolean exists() throws SQLException {
        final PreparedStatement s = connection.prepareStatement(SQL_SELECT);
        s.setInt(1, run);
        final ResultSet rs = s.executeQuery();
        return rs.first();
    }

    void insert() throws SQLException {
        if (this.exists()) {
            if (allowDeleteExisting) {
                this.delete();
            } else {
                throw new RuntimeException("Scaler data already exists and updates are not allowed.");
            }
        }
        try {
            this.insertScalerData();
        } catch (final SQLException e) {
            connection.rollback();
        }
    }

    private void insertScalerData() throws SQLException {
        final PreparedStatement statement;
        try {
            statement = connection.prepareStatement(SQL_INSERT);
            for (int idx = 0; idx < data.size(); idx++) {
                statement.setInt(1, run);
                statement.setInt(2, idx);
                statement.setInt(3, data.getValue(idx));
                statement.executeUpdate();
            }
        } finally {
            connection.commit();
        }
    }

    void setAllowDeleteExisting(final boolean allowDeleteExisting) {
        this.allowDeleteExisting = allowDeleteExisting;
    }
}
