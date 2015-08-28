package org.hps.rundb;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.hps.record.scalers.ScalerData;

/**
 * Implementation of database API for {@link org.hps.record.scalers.ScalerData} in the run database.
 *
 * @author Jeremy McCormick, SLAC
 */
final class ScalerDataDaoImpl implements ScalerDataDao {

    /**
     * SQL query strings.
     */
    private static final class ScalerDataQuery {

        /**
         * Delete by run.
         */
        private static final String DELETE_RUN = "DELETE FROM run_scalers WHERE run = ?";
        /**
         * Insert a record.
         */
        private static final String INSERT = "INSERT INTO run_scalers (run, event, idx, value) VALUES (?, ?, ?, ?)";
        /**
         * Select by run.
         */
        private static final String SELECT_RUN = "SELECT event, idx, value FROM run_scalers WHERE run = ? ORDER BY event, idx";
    }

    /**
     * The database connection.
     */
    private final Connection connection;

    /**
     * Create object for managing scaler data in the run database.
     *
     * @param connection the database connection
     */
    public ScalerDataDaoImpl(final Connection connection) {
        if (connection == null) {
            throw new IllegalArgumentException("The connection is null.");
        }
        this.connection = connection;
    }

    /**
     * Delete scaler data for the run.
     *
     * @param run the run number
     */
    @Override
    public void deleteScalerData(final int run) {
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = connection.prepareStatement(ScalerDataQuery.DELETE_RUN);
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
     * Get scaler data for a run.
     *
     * @param run the run number
     * @return the scaler data for the run
     */
    @Override
    public List<ScalerData> getScalerData(final int run) {
        PreparedStatement preparedStatement = null;
        final List<ScalerData> scalerDataList = new ArrayList<ScalerData>();
        try {
            preparedStatement = this.connection.prepareStatement(ScalerDataQuery.SELECT_RUN);
            preparedStatement.setInt(1, run);
            final ResultSet resultSet = preparedStatement.executeQuery();

            int[] scalerArray = new int[ScalerData.ARRAY_SIZE];
            int event = 0;

            while (resultSet.next()) {

                // Get record data.
                event = resultSet.getInt("event");
                final int idx = resultSet.getInt("idx");
                final int value = resultSet.getInt("value");

                // Is this the start of a new scaler data set and not the first one?
                if (idx == 0 && resultSet.getRow() > 1) {
                    // Create new scaler data object and add to list.
                    final ScalerData scalerData = new ScalerData(scalerArray, event);
                    scalerDataList.add(scalerData);

                    // Reset the data array for next object.
                    scalerArray = new int[ScalerData.ARRAY_SIZE];
                }

                // Set value by index.
                scalerArray[idx] = value;
            }

            // Add the last object which will not happen inside the loop.
            if (scalerArray != null) {
                scalerDataList.add(new ScalerData(scalerArray, event));
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
        return scalerDataList;
    }

    /**
     * Insert scaler data for a run.
     *
     * @param scalerData the list of scaler data
     * @param run the run number
     */
    @Override
    public void insertScalerData(final List<ScalerData> scalerDataList, final int run) {
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = this.connection.prepareStatement(ScalerDataQuery.INSERT);
            for (final ScalerData scalerData : scalerDataList) {
                final int size = scalerData.size();
                final Integer event = scalerData.getEventId();
                if (event == null) {
                    throw new IllegalStateException("The scaler data is missing the event ID.");
                }
                for (int i = 0; i < size; i++) {
                    preparedStatement.setInt(1, run);
                    preparedStatement.setInt(2, event);
                    preparedStatement.setInt(3, i);
                    preparedStatement.setInt(4, scalerData.getValue(i));
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
}
