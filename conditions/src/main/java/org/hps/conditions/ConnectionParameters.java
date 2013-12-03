package org.hps.conditions;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * This class encapsulates the parameters for connecting to a database, 
 * including hostname, port, user and password.  It can also create and 
 * return a Connection object based on these parameters.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 * @version $Id: ConnectionParameters.java,v 1.8 2013/10/04 01:54:16 jeremy Exp $
 */
public final class ConnectionParameters {
    
    /** Default values for the MySQL test database at SLAC. */
    private String user = "rd_hps_cond_ro";
    private String password = "2jumpinphotons.";
    private int port = 3306;
    private String hostname = "mysql-node03.slac.stanford.edu";
    private String database = "rd_hps_cond";
    private String conditionsTable = "conditions_dev";

    /**
     * No argument constructor that uses the defaults.
     */
    ConnectionParameters() {
    }

    /**
     * Fully qualified constructor.
     * @param user The user name.
     * @param password The password.
     * @param hostname The hostname.
     * @param port The port number.
     * @param conditionsTable The table containing conditions validity data.
     */
    ConnectionParameters(String user, String password, String database, String hostname, int port, String conditionsTable) {
        this.user = user;
        this.password = password;
        this.database = database;
        this.hostname = hostname;
        this.port = port;
        this.conditionsTable = conditionsTable;
    }

    /**
     * Get Properties object for this connection.
     * @return The Properties for this connection.
     */
    public Properties getConnectionProperties() {
        Properties p = new Properties();
        p.put("user", user);
        p.put("password", password);
        return p;
    }

    /**
     * Get the hostname.
     * @return The hostname.
     */
    public String getHostname() {
        return hostname;
    }

    /**
     * Get the port number.
     * @return The port number.
     */
    public int getPort() {
        return port;
    }
    
    /**
     * Get the name of the database.
     * @return The name of the database.
     */
    public String getDatabase() {
        return database;
    }

    /**
     * Get the connection string for these parameters.
     * @return The connection string.
     */
    public String getConnectionString() {
        return "jdbc:mysql://" + hostname + ":" + port + "/";
    }       
    
    /**
     * Get the name of the conditions validity data.
     * @return The name of the conditions validity data table. 
     */
    public String getConditionsTable() {
        return conditionsTable;
    }
    
    /**
     * Create a database connection from these parameters.  
     * The caller is responsible for closing it when finished.
     * @return The Connection object.
     */
    public Connection createConnection() {
        Properties connectionProperties = getConnectionProperties();
        Connection connection = null;
        try {
            connection = DriverManager.getConnection(getConnectionString(), connectionProperties);
        } catch (SQLException x) {
            throw new RuntimeException("Failed to connect to database: " + getConnectionString(), x);
        }
        return connection;
    }
    
    /**
     * Create ConnectionParameters from data in a properties file.
     * @param properties The properties file.
     * @return The ConnectionParameters created from the properties file.
     */
    public static final ConnectionParameters fromProperties(Properties properties) {        
        String user = properties.get("user").toString();
        String password = properties.getProperty("password").toString();
        String database = properties.getProperty("database").toString();
        String hostname = properties.getProperty("hostname").toString();
        int port = Integer.parseInt(properties.getProperty("port").toString());
        String conditionsTable = properties.getProperty("conditionsTable").toString();
        return new ConnectionParameters(user, password, database, hostname, port, conditionsTable);
    }    
}
