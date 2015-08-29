package org.hps.run.database;

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
        PreparedStatement deleteTriggerConfig = null;
        try {
            deleteTriggerConfig = connection.prepareStatement("DELETE FROM trigger_config WHERE run = ?");
            deleteTriggerConfig.setInt(1, run);
            deleteTriggerConfig.executeUpdate();
        } catch (final SQLException e) {
            throw new RuntimeException(e);
        } finally {
            if (deleteTriggerConfig != null) {
                try {
                    deleteTriggerConfig.close();
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
        PreparedStatement selectTriggerConfig = null;
        final TriggerConfig triggerConfig = new TriggerConfig();
        try {
            selectTriggerConfig = connection.prepareStatement("SELECT * FROM trigger_config WHERE run = ?");
            selectTriggerConfig.setInt(1, run);
            final ResultSet resultSet = selectTriggerConfig.executeQuery();
            if (resultSet.next()) {
                triggerConfig.put(TriggerConfigVariable.TI_TIME_OFFSET,
                        resultSet.getLong(TriggerConfigVariable.TI_TIME_OFFSET.getColumnName()));
            }
        } catch (final SQLException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                selectTriggerConfig.close();
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

        PreparedStatement insertTriggerConfig = null;
        try {
            insertTriggerConfig = connection.prepareStatement("INSERT INTO trigger_config ( run, "
                    + TriggerConfigVariable.TI_TIME_OFFSET.getColumnName() + " ) VALUES (?, ?)");
            insertTriggerConfig.setInt(1, run);
            insertTriggerConfig.setLong(2, triggerConfig.getTiTimeOffset());
            insertTriggerConfig.executeUpdate();
        } catch (final SQLException e) {
            throw new RuntimeException(e);
        } finally {
            if (insertTriggerConfig != null) {
                try {
                    insertTriggerConfig.close();
                } catch (final SQLException e) {
                    e.printStackTrace();
                }
            }
        }

    }
}
