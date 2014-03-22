package org.hps.conditions;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * This class provides various database utilities for the conditions system, primarily the
 * converter classes.
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class ConnectionManager {

    private ConnectionParameters _connectionParameters;
    private static ConnectionManager _instance = null;
    private Connection _connection = null;

    /**
     * Class constructor.  Override at your own risk!
     */
    protected ConnectionManager() {
    }
    
    /**
     * Get the singleton instance of this class.
     * @return The instance of this class.
     */
    public static ConnectionManager getConnectionManager() {
        if (_instance == null) {
            _instance = new ConnectionManager();
        }
        return _instance;
    }

    /**
     * Set the connection parameters.
     * @param connectionParameters The connection parameters.
     */
    void setConnectionParameters(ConnectionParameters connectionParameters) {
        this._connectionParameters = connectionParameters;
    }

    /**
     * Get the connection parameters.
     * @return The connection parameters.
     */
    public ConnectionParameters getConnectionParameters() {
        return _connectionParameters;
    }

    /**
     * Create a connection to the database.
     * @return The database connection.
     */
    Connection createConnection() {
        if (_connectionParameters == null)
            throw new RuntimeException("Connection parameters have not been set.");
        Connection newConnection = _connectionParameters.createConnection();
        try {
            System.out.println("USE " + _connectionParameters.getDatabase());
            newConnection.createStatement().execute("USE " + _connectionParameters.getDatabase());
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
        
        System.out.println(query);
        
        if (_connection == null)
            _connection = createConnection();
        ResultSet result = null;
        try {
            Statement statement = _connection.createStatement();
            result = statement.executeQuery(query);
        } catch (SQLException x) {
            throw new RuntimeException("Error in query: " + query, x);
        }
        return result;
    }
    
    /**
     * Perform a query with an update SQL command like INSERT, DELETE or UPDATE.
     * @return query The SQL query string.
     * @return The number of rows affected.
     */
    public int update(String query) {
        
        System.out.println(query);
        
        if (_connection == null)
            _connection = createConnection();
        int key = -1;
        try {
            // NOTE: Assumes only one row is updated!
            Statement statement = _connection.createStatement();
            statement.executeUpdate(query, Statement.RETURN_GENERATED_KEYS); 
            ResultSet resultSet = statement.getGeneratedKeys();
            if (resultSet.next()) {
                key = resultSet.getInt(1);
            }
        } catch (SQLException x) {
            throw new RuntimeException("Error in query: " + query, x);
        }
        return key;
    }
        
    public void disconnect() {
        cleanup(_connection);
    }  
}
