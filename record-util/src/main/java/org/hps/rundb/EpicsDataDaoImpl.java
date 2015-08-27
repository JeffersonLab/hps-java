package org.hps.rundb;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.hps.record.epics.EpicsData;
import org.hps.record.epics.EpicsHeader;

/**
 * Implementation of database operations for EPICS data.
 *
 * @author Jeremy McCormick, SLAC
 */
public class EpicsDataDaoImpl implements EpicsDataDao {

    /**
     * SQL data query strings.
     */
    private static class EpicsDataQuery {

        /**
         * Delete by run number.
         */
        private static final String DELETE_BY_RUN = "DELETE FROM run_epics WHERE run = ?";
        /**
         * Delete by run and sequence number.
         */
        private static final String DELETE_RUN_AND_SEQUENCE = "DELETE FROM run_epics WHERE run = ? and sequence = ?";
        /**
         * Insert a record.
         */
        private static final String INSERT = "INSERT INTO run_epics (run, sequence, timestamp, variable_name, value) VALUES (?, ?, ?, ?, ?)";
        /**
         * Select all records.
         */
        private static final String SELECT_ALL = "SELECT * FROM run_epics ORDER BY run, sequence";
        /**
         * Select by run number.
         */
        private static final String SELECT_RUN = "SELECT * FROM run_epics WHERE run = ? ORDER BY `sequence`";
        /**
         * Select unique variable names.
         */
        private static final String SELECT_VARIABLE_NAMES = "SELECT DISTINCT(variable_name) FROM run_epics ORDER BY variable_name";
        /**
         * Update a record.
         */
        private static final String UPDATE = "UPDATE run_epics SET run = ?, sequence = ?, timestamp = ?, variable_name = ?, value = ? WHERE run = ? and sequence = ? and variable_name = ?";
    }

    /**
     * The database connection.
     */
    private final Connection connection;

    /**
     * Create a new DAO implementation for EPICS data.
     *
     * @param connection the database connection
     */
    public EpicsDataDaoImpl(final Connection connection) {
        if (connection == null) {
            throw new IllegalArgumentException("The connection is null.");
        }
        this.connection = connection;
    }

    /**
     * Delete the record for this EPICS data object using its run and sequence number.
     *
     * @param run the run number
     * @throws IllegalArgumentException if the EPICS data is missing a header object
     */
    @Override
    public void deleteEpicsData(final EpicsData epicsData) {
        PreparedStatement preparedStatement = null;
        try {
            final EpicsHeader epicsHeader = epicsData.getEpicsHeader();
            if (epicsHeader == null) {
                throw new IllegalArgumentException("The EPICS data is missing header information.");
            }
            preparedStatement = connection.prepareStatement(EpicsDataQuery.DELETE_RUN_AND_SEQUENCE);
            preparedStatement.setInt(1, epicsHeader.getRun());
            preparedStatement.setInt(2, epicsHeader.getSequence());
            preparedStatement.executeUpdate();
        } catch (final SQLException e) {
            throw new RuntimeException(e);
        } finally {
            if (preparedStatement != null) {
                try {
                    preparedStatement.close();
                } catch (final SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Delete all EPICS data for a run from the database.
     *
     * @param run the run number
     */
    @Override
    public void deleteEpicsData(final int run) {
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = connection.prepareStatement(EpicsDataQuery.DELETE_BY_RUN);
            preparedStatement.setInt(1, run);
            preparedStatement.executeUpdate();
        } catch (final SQLException e) {
            throw new RuntimeException(e);
        } finally {
            if (preparedStatement != null) {
                try {
                    preparedStatement.close();
                } catch (final SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Get all the EPICS data in the database.
     *
     * @return the list of EPICS data
     */
    @Override
    public List<EpicsData> getAllEpicsData() {
        PreparedStatement preparedStatement = null;
        final List<EpicsData> epicsDataList = new ArrayList<EpicsData>();
        try {
            preparedStatement = connection.prepareStatement(EpicsDataQuery.SELECT_ALL);
            final ResultSet resultSet = preparedStatement.executeQuery();
            Integer currentRun = null;
            Integer currentSequence = null;
            EpicsData epicsData = new EpicsData();
            while (resultSet.next()) {
                if (currentRun == null) {
                    currentRun = resultSet.getInt("run");
                }
                if (currentSequence == null) {
                    currentSequence = resultSet.getInt("sequence");
                }
                final int run = resultSet.getInt("run");
                final int sequence = resultSet.getInt("sequence");
                final int timestamp = resultSet.getInt("timestamp");
                final String variableName = resultSet.getString("variable_name");
                final double value = resultSet.getDouble("value");
                if (currentRun != run || currentSequence != sequence) {
                    epicsDataList.add(epicsData);
                    epicsData = new EpicsData();
                    final EpicsHeader epicsHeader = new EpicsHeader(new int[] {run, sequence, timestamp});
                    epicsData.setEpicsHeader(epicsHeader);
                }
                epicsData.setValue(variableName, value);
            }
            epicsDataList.add(epicsData);
        } catch (final SQLException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                preparedStatement.close();
            } catch (final SQLException e) {
                e.printStackTrace();
            }
        }
        return epicsDataList;
    }

    /**
     * Get EPICS data by run.
     *
     * @param run the run number
     * @return the EPICS data
     */
    @Override
    public List<EpicsData> getEpicsData(final int run) {
        PreparedStatement preparedStatement = null;
        final List<EpicsData> epicsDataList = new ArrayList<EpicsData>();
        try {
            preparedStatement = connection.prepareStatement(EpicsDataQuery.SELECT_RUN);
            preparedStatement.setInt(1, run);
            final ResultSet resultSet = preparedStatement.executeQuery();
            Integer currentSequence = null;
            EpicsData epicsData = new EpicsData();
            EpicsHeader epicsHeader = null;
            while (resultSet.next()) {

                // Get record data.
                final int sequence = resultSet.getInt("sequence");
                final int timestamp = resultSet.getInt("timestamp");
                final String variableName = resultSet.getString("variable_name");
                final double value = resultSet.getDouble("value");

                // Get sequence first time.
                if (currentSequence == null) {
                    currentSequence = resultSet.getInt("sequence");
                }

                // Create EPICS header.
                epicsHeader = new EpicsHeader(new int[] {run, sequence, timestamp});

                // First time need to set header here.
                if (epicsData.getEpicsHeader() == null) {
                    epicsData.setEpicsHeader(epicsHeader);
                }

                // New sequence number occurred.
                if (currentSequence != sequence) {

                    // Add the EPICS data to the list.
                    epicsDataList.add(epicsData);

                    // Use the new sequence number.
                    currentSequence = sequence;

                    // Create new EPICS data.
                    epicsData = new EpicsData();

                    // Set header from current record.
                    epicsData.setEpicsHeader(epicsHeader);
                }

                // Set the value of the variable from the current record.
                epicsData.setValue(variableName, value);
            }

            // Add the last object which will not happen inside the loop.
            epicsDataList.add(epicsData);

        } catch (final SQLException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                preparedStatement.close();
            } catch (final SQLException e) {
                e.printStackTrace();
            }
        }
        return epicsDataList;
    }

    /**
     * Get the list of unique variables names used in the database records.
     *
     * @return the list of unique variable names
     */
    @Override
    public List<String> getVariableNames() {
        final List<String> variableNames = new ArrayList<String>();
        Statement statement = null;
        try {
            statement = connection.createStatement();
            final ResultSet resultSet = statement.executeQuery(EpicsDataQuery.SELECT_VARIABLE_NAMES);
            while (resultSet.next()) {
                variableNames.add(resultSet.getString(1));
            }
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
        return variableNames;
    }

    /**
     * Insert a list of EPICS data into the database.
     * <p>
     * The run number comes from the header information.
     *
     * @param epicsDataList the list of EPICS data
     */
    @Override
    public void insertEpicsData(final List<EpicsData> epicsDataList) {
        if (epicsDataList.isEmpty()) {
            throw new IllegalStateException("The EPICS data list is empty.");
        }
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = connection.prepareStatement(EpicsDataQuery.INSERT);
            for (final EpicsData epicsData : epicsDataList) {
                final EpicsHeader epicsHeader = epicsData.getEpicsHeader();
                if (epicsHeader == null) {
                    throw new IllegalArgumentException("The EPICS data is missing header information.");
                }
                for (final String variableName : epicsData.getKeys()) {
                    preparedStatement.setInt(1, epicsData.getEpicsHeader().getRun());
                    preparedStatement.setInt(2, epicsData.getEpicsHeader().getSequence());
                    preparedStatement.setInt(3, epicsData.getEpicsHeader().getTimestamp());
                    preparedStatement.setString(4, variableName);
                    preparedStatement.setDouble(5, epicsData.getValue(variableName));
                    preparedStatement.executeUpdate();
                }
            }
        } catch (final SQLException e) {
            throw new RuntimeException(e);
        } finally {
            if (preparedStatement != null) {
                try {
                    preparedStatement.close();
                } catch (final SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Updates EPICS data in the database.
     *
     * @param epicsData the EPICS data to update
     */
    @Override
    public void updateEpicsData(final EpicsData epicsData) {
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = connection.prepareStatement(EpicsDataQuery.UPDATE);
            final int run = epicsData.getEpicsHeader().getRun();
            final int sequence = epicsData.getEpicsHeader().getSequence();
            final int timestamp = epicsData.getEpicsHeader().getTimestamp();
            for (final String variableName : epicsData.getKeys()) {
                preparedStatement.setInt(1, run);
                preparedStatement.setInt(2, sequence);
                preparedStatement.setInt(3, timestamp);
                preparedStatement.setString(4, variableName);
                preparedStatement.setDouble(5, epicsData.getValue(variableName));
                preparedStatement.setInt(6, run);
                preparedStatement.setInt(7, sequence);
                preparedStatement.setString(8, variableName);
                preparedStatement.executeUpdate();
            }
        } catch (final SQLException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                if (preparedStatement != null) {
                    preparedStatement.close();
                }
            } catch (final SQLException e) {
                e.printStackTrace();
            }
            try {
                connection.setAutoCommit(true);
            } catch (final SQLException e) {
                e.printStackTrace();
            }
        }
    }
}
