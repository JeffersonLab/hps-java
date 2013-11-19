package org.lcsim.hps.conditions;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

/**
 * This class provides various database utilities for the conditions system, primarily the
 * converter classes.
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class ConnectionManager {

    private ConnectionParameters connectionParameters = new ConnectionParameters();
    private static ConnectionManager instance = null;
    private Connection connection = null;

    /**
     * Class constructor which is private so singleton must be used.
     */
    private ConnectionManager() {
        setupFromProperties();
    }

    /**
     * Get the singleton instance of this class.
     * @return The instance of this class.
     */
    public static ConnectionManager getConnectionManager() {
        if (instance == null) {
            instance = new ConnectionManager();
        }
        return instance;
    }

    /**
     * Set the connection parameters.
     * @param connectionParameters The connection parameters.
     */
    void setConnectionParameters(ConnectionParameters connectionParameters) {
        this.connectionParameters = connectionParameters;
    }

    /**
     * Get the connection parameters.
     * @return The connection parameters.
     */
    public ConnectionParameters getConnectionParameters() {
        return connectionParameters;
    }

    /**
     * Create a connection to the database.
     * @return The database connection.
     */
    Connection createConnection() {
        Connection newConnection = connectionParameters.createConnection();
        try {
            newConnection.createStatement().execute("USE " + connectionParameters.getDatabase());
        } catch (SQLException e) {
            throw new RuntimeException("Failed to connect to database.", e);
        }
        return newConnection;
    }

    /**
     * Cleanup a connection.
     * @param connection The Connection to cleanup.
     */
    public void cleanup(Connection connection) {
        if (connection != null) {
            try {
                if (!connection.isClosed())
                    connection.close();
                else
                    System.err.println("Connection already closed!");
            } catch (SQLException x) {
                throw new RuntimeException("Failed to close connection.", x);
            }
        }
    }

    /**
     * Cleanup a result set, or the Statement connected to it.
     * @param resultSet The ResultSet to cleanup.
     */
    public void cleanup(ResultSet resultSet) {
        if (resultSet != null) {
            try {
                // This should close the ResultSet itself, too.
                Statement statement = resultSet.getStatement();
                if (statement != null)
                    if (!statement.isClosed())
                        statement.close();
                    else
                        System.err.println("Statement already closed!");
            } catch (SQLException x) {
                throw new RuntimeException("Failed to close statement.", x);
            }
        }
    }

    /**
     * This method can be used to perform a database query. 
     * @param query The SQL query string.
     * @return The ResultSet from the query or null.
     */
    public ResultSet query(String query) {
        if (connection == null)
            connection = createConnection();
        ResultSet result = null;
        try {
            Statement statement = connection.createStatement();
            result = statement.executeQuery(query);
        } catch (SQLException x) {
            throw new RuntimeException("Error in query: " + query, x);
        }
        return result;
    }
    
    public void disconnect() {
        cleanup(connection);
    }

    /**
     * Setup the object from a properties file.
     */
    private void setupFromProperties() {
        Object obj = System.getProperties().get("hps.conditions.db.configuration");
        if (obj != null) {
            String config = obj.toString();
            Properties p = new Properties();
            try {
                p.load(new FileInputStream(new File(config)));
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            connectionParameters = ConnectionParameters.fromProperties(p);
        }
    }
}
