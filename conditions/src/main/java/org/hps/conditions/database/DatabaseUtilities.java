package org.hps.conditions.database;

import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Database utility methods.
 *
 * @author Jeremy McCormick, SLAC
 */
// TODO: Merge this single method into the manager class or a connection utilities class.
public final class DatabaseUtilities {

    /**
     * Cleanup a JDBC <code>ResultSet</code> by closing it and its <code>Statement</code>
     *
     * @param resultSet the database <code>ResultSet</code>
     */
    public static void cleanup(final ResultSet resultSet) {
        Statement statement = null;
        try {
            statement = resultSet.getStatement();
        } catch (final Exception e) {
        }
        try {
            if (resultSet != null) {
                resultSet.close();
            }
        } catch (final Exception e) {
            e.printStackTrace();
        }
        try {
            if (statement != null) {
                statement.close();
            }
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Do not allow instantiation.
     */
    private DatabaseUtilities() {
    }

}
