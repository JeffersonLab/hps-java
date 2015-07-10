package org.hps.record.evio.crawler;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.hps.record.scalers.ScalerData;

/**
 * Inserts scaler data into the run database.
 * 
 * @author Jeremy McCormick, SLAC
 */
public class ScalerDataUpdater {

    /**
     * <code>true</code> if existing data can be replaced and deleted.
     */
    private boolean allowDeleteExisting = false;

    /**
     * The database connection.
     */
    private final Connection connection;

    /**
     * The scaler data to insert into the database.
     */
    private final ScalerData data;

    /**
     * The run number for recording the scaler data.
     */
    private final int run;

    /**
     * SQL delete query string.
     */
    private final String SQL_DELETE = "DELETE FROM run_scalers WHERE run = ?";

    /**
     * SQL insert query string.
     */
    private final String SQL_INSERT = "INSERT INTO run_scalers (run, idx, value) VALUES (?, ?, ?)";

    /**
     * SQL select query string.
     */
    private final String SQL_SELECT = "SELECT run FROM run_scalers WHERE run = ?";

    /**
     * Create an updater for the given scaler data.
     * 
     * @param connection the database connection
     * @param data the scaler data
     * @param run the run number
     */
    ScalerDataUpdater(final Connection connection, final ScalerData data, final int run) {
        this.connection = connection;
        this.data = data;
        this.run = run;
    }

    /**
     * Delete scaler data for the run.
     * 
     * @throws SQLException if there is a SQL query error
     */
    private void delete() throws SQLException {
        final PreparedStatement s = connection.prepareStatement(SQL_DELETE);
        s.setInt(1, run);
        s.executeUpdate();
    }

    /**
     * Return <code>true</code> if scaler data exists for the run number.
     * 
     * @return <code>true</code> if scaler data exists
     * @throws SQLException if there is a SQL query error
     */
    private boolean exists() throws SQLException {
        final PreparedStatement s = connection.prepareStatement(SQL_SELECT);
        s.setInt(1, run);
        final ResultSet rs = s.executeQuery();
        return rs.first();
    }

    /**
     * Insert scaler data for this run number into the database.
     * <p>
     * Includes check on whether data exists and can be replaced or not.
     * 
     * @throws SQLException if there is a SQL query error
     * @throws RuntimeException if data exists and cannot be deleted
     */
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

    /**
     * Insert scaler data into the database.
     *  
     * @throws SQLException if there is a SQL query error
     */
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

    /**
     * Set whether existing data can be deleted and replaced.
     * 
     * @param allowDeleteExisting <code>true</code> if existing data can be replaced     
     */
    void setAllowDeleteExisting(final boolean allowDeleteExisting) {
        this.allowDeleteExisting = allowDeleteExisting;
    }
}
