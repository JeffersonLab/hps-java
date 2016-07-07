package org.hps.rundb;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.hps.record.scalers.ScalerData;
import org.hps.record.scalers.ScalerDataIndex;

/**
 * Implementation of database API for {@link org.hps.record.scalers.ScalerData} in the run database.
 *
 * @author jeremym
 */
final class ScalerDataDaoImpl implements ScalerDataDao {

    /**
     * Insert a record.
     */
    private static final String INSERT = createInsertSql();    

    /**
     * Create insert SQL for scaler data.
     *
     * @return the SQL insert string
     */
    private static String createInsertSql() {
        final StringBuffer sb = new StringBuffer();
        sb.append("INSERT INTO scalers ( run, event, timestamp, ");
        for (final ScalerDataIndex index : ScalerDataIndex.values()) {
            sb.append(index.name().toLowerCase() + ", ");
        }
        sb.setLength(sb.length() - 2);
        sb.append(" ) VALUES ( ?, ?, ?, ");
        for (int i = 0; i < ScalerDataIndex.values().length; i++) {
            sb.append("?, ");
        }
        sb.setLength(sb.length() - 2);
        sb.append(" )");
        return sb.toString();
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
            preparedStatement = connection.prepareStatement("DELETE FROM scalers WHERE run = ?");
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
        PreparedStatement selectScalers = null;
        final List<ScalerData> scalerDataList = new ArrayList<ScalerData>();
        try {
            selectScalers = this.connection.prepareStatement("SELECT * FROM sc"
                    + "alers WHERE run = ? ORDER BY event");
            selectScalers.setInt(1, run);
            final ResultSet resultSet = selectScalers.executeQuery();
            while (resultSet.next()) {
                final int[] data = new int[ScalerData.ARRAY_SIZE];
                for (final ScalerDataIndex index : ScalerDataIndex.values()) {
                    data[index.index()] = resultSet.getInt(index.name().toLowerCase());
                }
                final int event = resultSet.getInt("event");
                final int timestamp = resultSet.getInt("timestamp");
                final ScalerData scalerData = new ScalerData(data, event, timestamp);
                scalerDataList.add(scalerData);
            }
        } catch (final SQLException e) {
            throw new RuntimeException(e);
        } finally {
            if (selectScalers != null) {
                try {
                    selectScalers.close();
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
        PreparedStatement insertScalers = null;
        try {
            insertScalers = this.connection.prepareStatement(INSERT);
            for (final ScalerData scalerData : scalerDataList) {
                insertScalers.setInt(1, run);
                insertScalers.setInt(2, scalerData.getEventId());
                insertScalers.setInt(3, scalerData.getTimestamp());
                int parameterIndex = 4;
                for (final ScalerDataIndex index : ScalerDataIndex.values()) {
                    insertScalers.setInt(parameterIndex, scalerData.getValue(index));
                    ++parameterIndex;
                }
                final int rowsAffected = insertScalers.executeUpdate();
                if (rowsAffected == 0) {
                    throw new SQLException("Creation of scalers failed; no rows affected.");
                }
            }
        } catch (final SQLException e) {
            throw new RuntimeException(e);
        } finally {
            if (insertScalers != null) {
                try {
                    insertScalers.close();
                } catch (final SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
