package org.hps.record.run;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Implementation of trigger config database interface.
 * 
 * @author Jeremy McCormick, SLAC
 */
public class TriggerConfigDaoImpl implements TriggerConfigDao {

    /**
     * The database connection.
     */
    private Connection connection;
    
    /**
     * SQL query strings.
     */
    static final class TriggerConfigQuery {
        /**
         * Select by run.
         */
        static final String SELECT_RUN = "SELECT * FROM run_trigger_config WHERE run = ?";
        /**
         * Insert by run.
         */
        static final String INSERT_RUN = "INSERT INTO run_trigger_config (run, ti_time_offset) VALUES (?, ?)";
        /**
         * Update by run.
         */
        static final String UPDATE_RUN = "UPDATE run_trigger_config SET ti_time_offset = ? WHERE run = ?";
        /**
         * Delete by run.
         */
        static final String DELETE_RUN = "DELETE FROM run_trigger_config WHERE run = ?";
    }
    
    /**
     * Create a new object.
     * 
     * @param connection the database connection
     */
    TriggerConfigDaoImpl(Connection connection) { 
        this.connection = connection;
    }
    
    /**
     * Get the trigger config by run.
     * 
     * @param run the run number
     * @return the trigger config
     */
    @Override
    public TriggerConfig getTriggerConfig(int run) {
        PreparedStatement preparedStatement = null;
        TriggerConfig triggerConfig = null;
        try {
            preparedStatement = connection.prepareStatement(TriggerConfigQuery.SELECT_RUN);
            preparedStatement.setInt(1, run);
            final ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                triggerConfig = new TriggerConfig();
                triggerConfig.setTiTimeOffset(resultSet.getLong("ti_time_offset"));
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
    public void insertTriggerConfig(int run, TriggerConfig triggerConfig) {
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = connection.prepareStatement(TriggerConfigQuery.INSERT_RUN);            
            preparedStatement.setInt(1, run);
            preparedStatement.setLong(2, triggerConfig.getTiTimeOffset());
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
     * Update a trigger config by run number.
     * 
     * @param run the run number
     * @param triggerConfig the trigger config
     */
    @Override
    public void updateTriggerConfig(int run, TriggerConfig triggerConfig) {
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = connection.prepareStatement(TriggerConfigQuery.UPDATE_RUN);
            preparedStatement.setLong(1, triggerConfig.getTiTimeOffset());
            preparedStatement.setInt(2, run);
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
     * Delete a trigger config by run number.
     * 
     * @param run the run number
     */
    @Override
    public void deleteTriggerConfig(int run) {
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = connection.prepareStatement(TriggerConfigQuery.DELETE_RUN);
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
}
