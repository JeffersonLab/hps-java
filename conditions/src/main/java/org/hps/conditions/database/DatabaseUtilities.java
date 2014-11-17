package org.hps.conditions.database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public final class DatabaseUtilities {
    
    /**
     * Close a JDBC <code>Statement</code>.
     * @param statement the Statement to close
     */
    public static void close(Statement statement) {
        if (statement != null) {
            try {
                if (!statement.isClosed()) {
                    statement.close();
                }
            } catch (SQLException x) {
                throw new RuntimeException("Failed to close statement.", x);
            }
        }
    }

    /**
     * Close the JDBC the <code>Statement</code> connected to the <code>ResultSet</code>.
     * @param resultSet the ResultSet to close
     */
    public static void close(ResultSet resultSet) {
        if (resultSet != null) {
            try {
                Statement statement = resultSet.getStatement();
                if (!statement.isClosed()) {
                    statement.close();
                }
            } catch (SQLException x) {
                throw new RuntimeException("Failed to close statement.", x);
            }
        }
    }

}
