package org.hps.run.database;

import java.sql.Clob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.hps.record.svt.SvtConfigData;
import org.hps.record.svt.SvtConfigData.RocTag;

/**
 * Implementation of SVT configuration database operations.
 * 
 * @author Jeremy McCormick, SLAC
 */
public class SvtConfigDaoImpl implements SvtConfigDao {

    private Connection connection = null;
    
    private static final String INSERT = 
            "INSERT INTO svt_configs (run, timestamp, config1, status1, config2, status2) VALUES (?, ?, ?, ?, ?, ?)"; 
    
    private static final String SELECT = 
            "SELECT * FROM svt_configs WHERE run = ?";
    
    SvtConfigDaoImpl(Connection connection) {
        if (connection == null) {
            throw new IllegalArgumentException("The connection is null.");
        }
        this.connection = connection;
    }
    
    @Override
    public void insertSvtConfigs(List<SvtConfigData> svtConfigs, int run) {
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = connection.prepareStatement(INSERT);
            for (SvtConfigData config : svtConfigs) {
                preparedStatement.setInt(1, run);
                preparedStatement.setInt(2, config.getTimestamp());
                if (config.getConfigData(RocTag.DATA) != null) {
                    preparedStatement.setBytes(3, config.getConfigData(RocTag.DATA).getBytes());
                } else {
                    preparedStatement.setBytes(3, null);
                }
                if (config.getStatusData(RocTag.DATA) != null) {
                    preparedStatement.setBytes(4, config.getStatusData(RocTag.DATA).getBytes());
                } else {
                    preparedStatement.setBytes(4, null);
                }
                if (config.getConfigData(RocTag.CONTROL) != null) {
                    preparedStatement.setBytes(5, config.getConfigData(RocTag.CONTROL).getBytes());
                } else {
                    preparedStatement.setBytes(6, null);
                }
                if (config.getStatusData(RocTag.CONTROL) != null) {
                    preparedStatement.setBytes(7, config.getConfigData(RocTag.CONTROL).getBytes());
                } else {
                    preparedStatement.setBytes(8, null);
                }
                preparedStatement.executeUpdate();
            }
        } catch (SQLException e) {
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
    public List<SvtConfigData> getSvtConfigs(int run) {
        List<SvtConfigData> svtConfigList = new ArrayList<SvtConfigData>();
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = connection.prepareStatement(SELECT);
            preparedStatement.setInt(1, run);
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                
                SvtConfigData config = new SvtConfigData(resultSet.getInt("timestamp"));
                                
                Clob clob = resultSet.getClob("config1");
                if (clob != null) {
                    config.setConfigData(RocTag.DATA, clob.getSubString(1, (int) clob.length()));
                }
                
                clob = resultSet.getClob("status1");
                if (clob != null) {
                    config.setStatusData(RocTag.DATA, clob.getSubString(1, (int) clob.length()));
                }
                
                clob = resultSet.getClob("config2");
                if (clob != null) { 
                    config.setConfigData(RocTag.CONTROL, clob.getSubString(1, (int) clob.length()));
                }
                
                clob = resultSet.getClob("status2");
                if (clob != null) {
                    config.setStatusData(RocTag.CONTROL, clob.getSubString(1, (int) clob.length()));
                }                
                
                svtConfigList.add(config);
            }
        } catch (SQLException e) {
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
        return svtConfigList;
    }
    
    @Override
    public void deleteSvtConfigs(int run) {
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = connection.prepareStatement("DELETE FROM svt_configs WHERE run = ?");
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
