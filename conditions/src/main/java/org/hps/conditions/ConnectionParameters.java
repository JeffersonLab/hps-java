package org.hps.conditions;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * This class encapsulates the parameters for connecting to a database, including
 * hostname, port, user and password. It can also create and return a Connection object
 * based on these parameters.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 * @version $Id: ConnectionParameters.java,v 1.8 2013/10/04 01:54:16 jeremy Exp $
 */
public class ConnectionParameters {

    protected String user;
    protected String password;
    protected int port;
    protected String hostname;
    protected String database;

    /**
     * Protected constructor for sub-classes.
     */	
    protected ConnectionParameters() {
    }
    
    /**
     * Fully qualified constructor.
     * @param user The user name.
     * @param password The password.
     * @param hostname The hostname.
     * @param port The port number.
     * @param conditionsTable The table containing conditions validity data.
     */
   public  ConnectionParameters(String user, String password, String database, String hostname, int port) {
        this.user = user;
        this.password = password;
        this.database = database;
        this.hostname = hostname;
        this.port = port;
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
    String getHostname() {
        return hostname;
    }

    /**
     * Get the port number.
     * @return The port number.
     */
    int getPort() {
        return port;
    }

    /**
     * Get the name of the database.
     * @return The name of the database.
     */
    String getDatabase() {
        return database;
    }

    /**
     * Get the user name.
     */
    String getUser() {
        return user;
    }

    /**
     * Get the password.
     */
    String getPassword() {
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
     * Create a database connection from these parameters. The caller becomes the "owner"
     * and is responsible for closing it when finished.
     * @return The Connection object.
     */
    public Connection createConnection() {               
        Properties connectionProperties = getConnectionProperties();
        Connection connection = null;
        try {
            connection = DriverManager.getConnection(getConnectionString(), connectionProperties);
            connection.createStatement().execute("USE " + getDatabase());
        } catch (SQLException x) {
            throw new RuntimeException("Failed to connect to database: " + getConnectionString(), x);
        }
        return connection;
    }

    /**
     * Configure the connection parameters from a properties file.
     * @param file The properties file.
     * @return The connection parameters.
     */
    public static final ConnectionParameters fromProperties(File file) {
        FileInputStream fin = null;
        try {
            fin = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            throw new IllegalArgumentException(file.getPath() + " does not exist.", e);
        }
        return fromProperties(fin);
    }

    /**
     * Configure the connection parameters from an embedded classpath resource which
     * should be a properties file.
     * @param String The resource path.
     * @return The connection parameters.
     */
    public static final ConnectionParameters fromResource(String resource) {
        return fromProperties(ConnectionParameters.class.getResourceAsStream(resource));
    }

    /**
     * Configure the connection parameters from an <code>InputStream</code> of properties.
     * @param in The InputStream of the properties.
     * @return The connection parameters.
     * @throws RuntimeException if the InputStream is invalid
     */
    private static final ConnectionParameters fromProperties(InputStream in) {
        Properties properties = new Properties();
        try {
            properties.load(in);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        String user = properties.getProperty("user");
        String password = properties.getProperty("password");
        String database = properties.getProperty("database");
        String hostname = properties.getProperty("hostname");
        int port = Integer.parseInt(properties.getProperty("port"));
        return new ConnectionParameters(user, password, database, hostname, port);
    }
}
