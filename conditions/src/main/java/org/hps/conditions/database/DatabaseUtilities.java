package org.hps.conditions.database;

import java.sql.ResultSet;
import java.sql.Statement;

public final class DatabaseUtilities {
        
    static void cleanup(ResultSet resultSet) {
        Statement statement = null;
        try {
            statement = resultSet.getStatement();
        } catch (Exception e) {
        }
        try {
            if (resultSet != null) {
                resultSet.close();
            }
        } catch (Exception e) {
        }
        try {
            if (statement != null) {
                statement.close();
            } 
        } catch (Exception e) {
        }        
    }

}
