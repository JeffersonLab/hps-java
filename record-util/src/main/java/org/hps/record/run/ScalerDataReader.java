package org.hps.record.run;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.hps.record.scalers.ScalerData;

/**
 * 
 * @author Jeremy McCormick, SLAC
 */
public class ScalerDataReader extends AbstractRunDatabaseReader<ScalerData> {
    
    private String SELECT_SQL = "SELECT idx, value FROM run_scalers WHERE run = ? ORDER BY idx";
    
    @Override
    void read() {
        if (getRun() == -1) {
            throw new IllegalStateException("run number is invalid: " + getRun());
        }
        if (getConnection() == null) {
            throw new IllegalStateException("Connection is not set.");
        }

        PreparedStatement statement = null;
        try {
            statement = getConnection().prepareStatement(SELECT_SQL);
            statement.setInt(1, getRun());
            ResultSet resultSet = statement.executeQuery();
            
            List<Integer> scalerValues = new ArrayList<Integer>();
            while (resultSet.next()) {
                scalerValues.add(resultSet.getInt("value"));
            }
            
            int[] scalerArray = new int[scalerValues.size()];
            for (int i = 0; i < scalerArray.length; i++) {
                scalerArray[i] = scalerValues.get(i);
            }
                                                                        
            setData(new ScalerData(scalerArray));
            
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            if (statement != null) {
                try {
                    statement.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }        
    }
}
