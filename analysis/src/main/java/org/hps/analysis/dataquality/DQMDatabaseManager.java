package org.hps.analysis.dataquality;

import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.hps.conditions.ConnectionParameters;
import org.hps.conditions.TableMetaData;

/**
 * Manages the DQM database connection and access
 * re-uses ConnectionParameters and TableMetaData classes from conditionsDB 
 * as they do exactly what we want here. 
 * @author Matt Graham <mgraham@slac.stanford.edu>
 */ 

@SuppressWarnings("rawtypes")
public class DQMDatabaseManager{

    int runNumber = -1;
    String detectorName;
    List<TableMetaData> tableMetaData;    
    File connectionPropertiesFile;
 
    static Logger logger = null;
    ConnectionParameters connectionParameters;
    Connection connection;
    String dqmTableName;
    boolean wasConfigured = false;
    boolean isConnected = false;
    
    // FIXME: Prefer using the ConditionsManager's instance if possible.
    static private DQMDatabaseManager instance; 

    /**
     * Class constructor, which is only package accessible.
     */
    DQMDatabaseManager() {
            
    }

    /**
     * Simple log formatter for this class.
     */
    private static final class LogFormatter extends Formatter {

        public String format(LogRecord record) {
            StringBuilder sb = new StringBuilder();
            sb.append(record.getLoggerName() + " [ " + record.getLevel() + " ] " + record.getMessage() + '\n');
            return sb.toString();
        }
    }

    /**
     * Setup the logger for this class, with initial level of ALL.
     */
    static {
        logger = Logger.getLogger(DQMDatabaseManager.class.getSimpleName());
        logger.setUseParentHandlers(false);
        logger.setLevel(Level.ALL);
        ConsoleHandler handler = new ConsoleHandler();
        handler.setLevel(Level.ALL);
        handler.setFormatter(new LogFormatter());
        logger.addHandler(handler);
        logger.config("logger initialized with level " + handler.getLevel());
    }
    
    
  /**
     * Register this conditions manager as the global default. 
     */
    void register() {     
        instance = this;
    }
    
    
    /**
     * Get the static instance of this class, which must have been
     * registered first from a call to {@link #register()}.
     * @return The static instance of the manager.
     */
    public static DQMDatabaseManager getInstance() {
        return instance;
    }
    
 
    
    public void setup() {
        if (!isConnected())
            openConnection();
        else
            logger.log(Level.CONFIG, "using existing connection {0}", connectionParameters.getConnectionString());        
    }

 
    
    /**
     * Set the path to a properties file containing connection settings.
     * @param file The properties file
     */
    public void setConnectionProperties(File file) {
        logger.config("setting connection prop file " + file.getPath());
        if (!file.exists())
            throw new IllegalArgumentException("The connection properties file does not exist: " + connectionPropertiesFile.getPath());
        connectionParameters = ConnectionParameters.fromProperties(file);        
    }
    
    /**
     * Set the connection parameters from an embedded resource.
     * @param resource The classpath resource
     */
    public void setConnectionResource(String resource) {
        logger.config("setting connection resource " + resource);
        connectionParameters = ConnectionParameters.fromResource(resource);        
    }

  

    /**
     * Get the next collection ID for a database conditions table.
     * @param tableName The name of the table.
     * @return The next collection ID.
     */
    // TODO: If there are no collections that exist, this method should simply return the value '1'
    // or it could throw an exception.
    public int getNextCollectionId(String tableName) {
        TableMetaData tableData = findTableMetaData(tableName);
        if (tableData == null)
            throw new IllegalArgumentException("There is no meta data for table " + tableName);
        ResultSet resultSet = selectQuery("SELECT MAX(collection_id)+1 FROM " + tableName);
        int collectionId = -1;
        try {
            resultSet.next();
            collectionId = resultSet.getInt(1);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        logger.fine("new collection ID " + collectionId + " created for table " + tableName);
        return collectionId;
    }

    /**
     * Get the list of table meta data.
     * @return The list of table meta data.
     */
    public List<TableMetaData> getTableMetaDataList() {
        return tableMetaData;
    }

    /**
     * Find a table's meta data.
     * @param name The name of the table.
     * @return The table's meta data or null if does not exist.
     */
    public TableMetaData findTableMetaData(String name) {
        for (TableMetaData meta : tableMetaData) {
            if (meta.getTableName().equals(name))
                return meta;
        }
        return null;
    }
    
    /**
     * Find meta data by collection class type.
     * @param type The collection class.
     * @return The table meta data.
     */
    public TableMetaData findTableMetaData(Class type) {
        for (TableMetaData meta : tableMetaData) {
            if (meta.getCollectionClass().equals(type)) {
                return meta;
            }
        }
        return null;
    }
    
    /**
     * This method can be used to perform a database SELECT query. 
     * @param query The SQL query string.
     * @return The ResultSet from the query or null.
     */
    public ResultSet selectQuery(String query) {
        logger.fine(query);
        ResultSet result = null;
        Statement statement = null;
        try {
            statement = connection.createStatement();
            result = statement.executeQuery(query);
        } catch (SQLException x) {
            throw new RuntimeException("Error in query: " + query, x);
        } 
        return result;
    }
    
    /**
     * Perform a SQL query with an update command like INSERT, DELETE or UPDATE.
     * @param query The SQL query string.
     * @return The keys of the rows affected.
     */
    public List<Integer> updateQuery(String query) {        
        logger.fine(query);
        List<Integer> keys = new ArrayList<Integer>();
        Statement statement = null;
        try {
            statement = connection.createStatement();
            statement.executeUpdate(query, Statement.RETURN_GENERATED_KEYS); 
            ResultSet resultSet = statement.getGeneratedKeys();            
            while (resultSet.next()) {
                int key = resultSet.getInt(1);
                keys.add(key);
            }
        } catch (SQLException x) {
            throw new RuntimeException("Error in SQL query: " + query, x);            
        } finally {
            close(statement);
        }
        return keys;
    }
                
    /**
     * Set the log level.
     * @param level The log level.
     */
    public void setLogLevel(Level level) {
        logger.config("setting log level to " + level);
        logger.setLevel(level);
        logger.getHandlers()[0].setLevel(level);
    }
    
    /**
     * Get the name of the DQM table 
     */
    public String getDQMTableName() {
        return dqmTableName;
    }

   
    
    /**
     * Return true if the connection parameters are valid, e.g. non-null.
     * @return true if connection parameters are non-null
     */
    public boolean hasConnectionParameters() {
        return connectionParameters != null;
    }
    
    /**
     * Return if the manager was configured e.g. from an XML configuration file.
     * @return true if manager was configured
     */
    public boolean wasConfigured() {
        return wasConfigured;
    }
        
    /**
     * Close a JDBC <code>Statement</code>.
     * @param statement the Statement to close
     */
    static void close(Statement statement) {
        if (statement != null) {
            try {
                if (!statement.isClosed())
                    statement.close();
                else
                    logger.log(Level.WARNING, "Statement is already closed!");
            } catch (SQLException x) {
                throw new RuntimeException("Failed to close statement.", x);
            }
        }
    }
    
    /**
     * Close a JDBC <code>ResultSet</code>, or rather the Statement connected to it.
     * @param resultSet the ResultSet to close
     */
    static void close(ResultSet resultSet) {        
        if (resultSet != null) {
            try {
                Statement statement = resultSet.getStatement();
                if (!statement.isClosed())
                    statement.close();
                else
                    logger.log(Level.WARNING, "Statement is already closed!");
            } catch (SQLException x) {
                throw new RuntimeException("Failed to close statement.", x);
            }
        }
    }
    
    private boolean isConnected() {
        return isConnected;
    }

 
    /**
     * Open the database connection.
     */
    private void openConnection() {
        if (connectionParameters == null)
            throw new RuntimeException("The connection parameters were not configured.");
        connection = connectionParameters.createConnection();
        logger.log(Level.CONFIG, "created connection {0}", connectionParameters.getConnectionString());
        isConnected = true;
    }
    
    /**
     * Close the database connection.
     */
    public void closeConnection() {
        logger.config("closing connection");
        if (connection != null) {
            try {
                if (!connection.isClosed()) {
                    connection.close();
                    logger.config("connection closed");
                } else {
                    logger.config("connection already closed");
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
        connection = null;
        connectionParameters = null;
    }
    
    @Override
    public void finalize() {
        if (isConnected())
            closeConnection();
    }
       
  
   
    
}
