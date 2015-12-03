package org.hps.run.database;

import java.sql.Clob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

final class TriggerConfigDaoImpl implements TriggerConfigDao {
      
    private static final String INSERT =
            "INSERT INTO trigger_configs (run, timestamp, config1, config2, config3, config4)"
                + " VALUES (?, ?, ?, ?, ?, ?)";
    
    private static final String SELECT =  "SELECT * FROM trigger_configs WHERE run = ?";
    
    private static final String DELETE = "DELETE FROM trigger_configs WHERE run = ?";
    
    /**
     * The database connection.
     */
    private final Connection connection;

    /**
     * Create object for managing scaler data in the run database.
     *
     * @param connection the database connection
     */
    TriggerConfigDaoImpl(final Connection connection) {
        if (connection == null) {
            throw new IllegalArgumentException("The connection is null.");
        }
        this.connection = connection;
    }
    

    @Override
    public void insertTriggerConfig(TriggerConfig config, int run) {
        if (!config.isValid()) {
            throw new RuntimeException("The trigger config is not valid.");
        }
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = connection.prepareStatement(INSERT);
            preparedStatement.setInt(1, run);
            preparedStatement.setInt(2, config.getTimestamp());
            Map<Integer, String> data = config.getData();
            if (data.size() != TriggerConfig.DATA_LENGTH) {
                throw new IllegalArgumentException("The trigger config data has the wrong length.");
            }
            preparedStatement.setBytes(3, data.get(TriggerConfig.CONFIG1).getBytes());
            preparedStatement.setBytes(4, data.get(TriggerConfig.CONFIG2).getBytes());
            preparedStatement.setBytes(5, data.get(TriggerConfig.CONFIG3).getBytes());
            preparedStatement.setBytes(6, data.get(TriggerConfig.CONFIG4).getBytes());
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                preparedStatement.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void deleteTriggerConfig(int run) {
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = connection.prepareStatement(DELETE);
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
    
    @Override
    public TriggerConfig getTriggerConfig(int run) {
        PreparedStatement preparedStatement = null;
        TriggerConfig config = null;
        try {
            preparedStatement = connection.prepareStatement(SELECT);
            preparedStatement.setInt(1, run);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                Map<Integer, String> data = new LinkedHashMap<Integer, String>();
                int timestamp = resultSet.getInt("timestamp");
                Clob clob = resultSet.getClob("config1");
                if (clob != null) {
                    data.put(TriggerConfig.CONFIG1, clob.getSubString(1, (int) clob.length()));
                }
                clob = resultSet.getClob("config2");
                if (clob != null) {
                    data.put(TriggerConfig.CONFIG2, clob.getSubString(1, (int) clob.length()));
                }
                clob = resultSet.getClob("config3");
                if (clob != null) {
                    data.put(TriggerConfig.CONFIG3, clob.getSubString(1, (int) clob.length()));
                }
                clob = resultSet.getClob("config4");
                if (clob != null) {
                    data.put(TriggerConfig.CONFIG4, clob.getSubString(1, (int) clob.length()));
                }
                config = new TriggerConfig(data, timestamp);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                preparedStatement.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return config;
    }
}
