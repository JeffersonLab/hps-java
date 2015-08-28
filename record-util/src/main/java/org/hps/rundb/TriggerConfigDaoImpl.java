package org.hps.rundb;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.hps.record.triggerbank.TriggerConfig;
import org.hps.record.triggerbank.TriggerConfigVariable;

/**
 * Implementation of trigger config database interface.
 *
 * @author Jeremy McCormick, SLAC
 */
final class TriggerConfigDaoImpl implements TriggerConfigDao {

    /**
     * The database connection.
     */
    private final Connection connection;

    /**
     * Create a new object.
     *
     * @param connection the database connection
     */
    TriggerConfigDaoImpl(final Connection connection) {
        this.connection = connection;
    }

    /**
     * Delete a trigger config by run number.
     *
     * @param run the run number
     */
    @Override
    public void deleteTriggerConfigInt(final int run) {
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = connection.prepareStatement("DELETE FROM trigger_config WHERE run = ?");
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
     * Get the trigger config by run.
     *
     * @param run the run number
     * @return the trigger config
     */
    @Override
    public TriggerConfig getTriggerConfig(final int run) {
        PreparedStatement preparedStatement = null;
        final TriggerConfig triggerConfig = new TriggerConfig();
        try {
            preparedStatement = connection.prepareStatement("SELECT * FROM trigger_config WHERE run = ?");
            final ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                triggerConfig.put(TriggerConfigVariable.TI_TIME_OFFSET,
                        resultSet.getLong(TriggerConfigVariable.TI_TIME_OFFSET.getColumnName()));
            }
        } catch (final SQLException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                preparedStatement.close();
            } catch (final SQLException e) {
                e.printStackTrace();
            }
        }
        return triggerConfig;
    }

    /**
     * Insert a trigger config for a run.
     *
     * @param run the run number
     * @param triggerConfig the trigger config
     */
    @Override
    public void insertTriggerConfig(final TriggerConfig triggerConfig, final int run) {

        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = connection.prepareStatement("INSERT INTO trigger_config ( "
                    + TriggerConfigVariable.TI_TIME_OFFSET.getColumnName() + " ) VALUES (?)");
            preparedStatement.setLong(1, triggerConfig.getTiTimeOffset());
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
}
