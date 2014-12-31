package org.hps.conditions.database;

import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Database utility methods.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 *
 */
public final class DatabaseUtilities {
    
    private DatabaseUtilities() {        
    }
        
    /**
     * Cleanup a JDBC <code>ResultSet</code> by closing it and its <code>Statement</code>
     * @param resultSet The database ResultSet.
     */
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
