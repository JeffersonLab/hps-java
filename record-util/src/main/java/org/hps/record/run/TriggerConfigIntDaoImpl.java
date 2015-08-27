package org.hps.record.run;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import org.hps.record.triggerbank.TriggerConfigInt;

/**
 * Implementation of trigger config database interface.
 * 
 * @author Jeremy McCormick, SLAC
 */
public class TriggerConfigIntDaoImpl implements TriggerConfigIntDao {

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
        static final String SELECT_RUN = "SELECT * FROM run_trigger_config_int WHERE run = ?";
        /**
         * Insert by run.
         */
        static final String INSERT_VARIABLE = "INSERT INTO run_trigger_config_int (run, variable_name, value) VALUES (?, ?, ?)";
        /**
         * Delete by run.
         */
        static final String DELETE_RUN = "DELETE FROM run_trigger_config_int WHERE run = ?";
    }
    
    /**
     * Create a new object.
     * 
     * @param connection the database connection
     */
    TriggerConfigIntDaoImpl(Connection connection) { 
        this.connection = connection;
    }

    @Override
    public TriggerConfigInt getTriggerConfigInt(int run) {
        PreparedStatement preparedStatement = null;
        TriggerConfigInt triggerConfig = new TriggerConfigInt();
        try {
            preparedStatement = connection.prepareStatement(TriggerConfigQuery.SELECT_RUN);
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                triggerConfig.put(resultSet.getString("variable_name"), resultSet.getLong("value"));
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

    @Override
    public void insertTriggerConfigInt(TriggerConfigInt triggerConfig, int run) {
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = connection.prepareStatement(TriggerConfigQuery.INSERT_VARIABLE);
            for (Map.Entry<String, Long> entry : triggerConfig.entrySet()) {
                preparedStatement.setInt(1, run);
                preparedStatement.setString(2, entry.getKey());
                preparedStatement.setLong(3, entry.getValue());
                preparedStatement.executeUpdate();
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

    @Override
    public void deleteTriggerConfigInt(int run) {
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
