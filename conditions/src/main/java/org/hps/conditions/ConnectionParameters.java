package org.hps.conditions;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import org.jdom.Element;

/**
 * This class encapsulates the parameters for connecting to a database, 
 * including hostname, port, user and password.  It can also create and 
 * return a Connection object based on these parameters.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 * @version $Id: ConnectionParameters.java,v 1.8 2013/10/04 01:54:16 jeremy Exp $
 */
public final class ConnectionParameters {
    
    private String user;
    private String password;
    private int port;
    private String hostname;
    private String database;
    private String conditionsTable;

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
     * Get the user name.
     */
    public String getUser() {
        return user;
    }
    
    /**
     * Get the password.
     */
    public String getPassword() {
        return password;
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
     * Create the connection parameters from an XML container node 
     * with the appropriate child elements.    
     * @param element The connection XML element.
     * @return The ConnectionParameters created from XML.
     */
    public static final ConnectionParameters fromXML(Element element) {
        if (element.getChild("user") == null)
            throw new IllegalArgumentException("missing user element");
        String user = element.getChild("user").getText();
        if (element.getChild("password") == null)
            throw new IllegalArgumentException("missing password element");
        String password = element.getChild("password").getText();
        String database = element.getChild("database").getText();
        String hostname = element.getChild("hostname").getText();
        int port = Integer.parseInt(element.getChild("port").getText());
        String conditionsTable = element.getChild("conditions_table").getText();
        return new ConnectionParameters(user, password, database, hostname, port, conditionsTable);        
    }
}
