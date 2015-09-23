package org.hps.run.database;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Factory for creating database API objects for interacting with the run database.
 * <p>
 * This allows the implementation classes to be package protected as only public interfaces are returned by this class.
 *
 * @author Jeremy McCormick, SLAC
 * @see EpicsDataDao
 * @see EpicsVariableDao
 */
final class RunDatabaseDaoFactory {

    /**
     * The database connection.
     */
    private final Connection connection;

    /**
     * Create a new factory.
     *
     * @param connection the database connection
     */
    RunDatabaseDaoFactory(final Connection connection) {
        if (connection == null) {
            throw new IllegalArgumentException("The connection is null.");
        }
        try {
            if (connection.isClosed()) {
                throw new IllegalStateException("The connection is closed.");
            }
        } catch (final SQLException e) {
            throw new IllegalStateException("Error when checking connection status.", e);
        }
        this.connection = connection;
    }

    /**
     * Get the EPICS DAO.
     *
     * @return the EPICS DAO
     */
    EpicsDataDao createEpicsDataDao() {
        return new EpicsDataDaoImpl(connection);
    }

    /**
     * Get the EPICS variable DAO.
     *
     * @return the EPICS variable DAO
     */
    EpicsVariableDao createEpicsVariableDao() {
        return new EpicsVariableDaoImpl(connection);
    }

    /**
     * Get the run summary DAO.
     *
     * @return the run summary DAO
     */
    RunSummaryDao createRunSummaryDao() {
        return new RunSummaryDaoImpl(connection);
    }

    /**
     * Get the scaler data DAO.
     *
     * @return the scaler data DAO
     */
    ScalerDataDao createScalerDataDao() {
        return new ScalerDataDaoImpl(connection);
    }

    /**
     * Get the trigger config DAO.
     *
     * @return the trigger config DAO
     */
    TriggerConfigDao createTriggerConfigDao() {
        return new TriggerConfigDaoImpl(connection);
    }
}
