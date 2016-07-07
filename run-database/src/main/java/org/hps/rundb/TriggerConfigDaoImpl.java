package org.hps.rundb;

import java.sql.Clob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.hps.record.triggerbank.TriggerConfigData;
import org.hps.record.triggerbank.TriggerConfigData.Crate;

/**
 * Implementation of trigger configuration database operations.
 * 
 * @author jeremym
 */
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
    public void insertTriggerConfig(TriggerConfigData config, int run) {
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = connection.prepareStatement(INSERT);
            preparedStatement.setInt(1, run);
            preparedStatement.setInt(2, config.getTimestamp());
            Map<Crate, String> data = config.getData();
            preparedStatement.setBytes(3, data.get(TriggerConfigData.Crate.CONFIG1).getBytes());
            preparedStatement.setBytes(4, data.get(TriggerConfigData.Crate.CONFIG2).getBytes());
            preparedStatement.setBytes(5, data.get(TriggerConfigData.Crate.CONFIG3).getBytes());
            if (data.get(TriggerConfigData.Crate.CONFIG4) != null) {
                preparedStatement.setBytes(6, data.get(TriggerConfigData.Crate.CONFIG4).getBytes());
            } else {
                preparedStatement.setObject(6, null);
            }
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
    public TriggerConfigData getTriggerConfig(int run) {
        PreparedStatement preparedStatement = null;
        TriggerConfigData config = null;
        try {
            preparedStatement = connection.prepareStatement(SELECT);
            preparedStatement.setInt(1, run);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                Map<Crate, String> data = new LinkedHashMap<Crate, String>();
                int timestamp = resultSet.getInt("timestamp");
                Clob clob = resultSet.getClob("config1");
                if (clob != null) {
                    data.put(TriggerConfigData.Crate.CONFIG1, clob.getSubString(1, (int) clob.length()));
                }
                clob = resultSet.getClob("config2");
                if (clob != null) {
                    data.put(TriggerConfigData.Crate.CONFIG2, clob.getSubString(1, (int) clob.length()));
                }
                clob = resultSet.getClob("config3");
                if (clob != null) {
                    data.put(TriggerConfigData.Crate.CONFIG3, clob.getSubString(1, (int) clob.length()));
                }
                clob = resultSet.getClob("config4");
                if (clob != null) {
                    data.put(TriggerConfigData.Crate.CONFIG4, clob.getSubString(1, (int) clob.length()));
                }
                config = new TriggerConfigData(data, timestamp);
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
