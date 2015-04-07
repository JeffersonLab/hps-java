package org.hps.conditions.database;

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
 * This class encapsulates the parameters for connecting to a database, including host name, port, user and password.
 *
 * @author <a href="mailto:jeremym@slac.stanford.edu">Jeremy McCormick</a>
 */
public final class ConnectionParameters {

    /**
     * The default port number.
     */
    public static final int DEFAULT_PORT = 3306;

    /**
     * The user name.
     */
    private String user;

    /**
     * The user's password.
     */
    private String password;

    /**
     * The port.
     */
    private int port;

    /**
     * The host name.
     */
    private String hostname;

    /**
     * The database name.
     */
    private String database;

    /**
     * Protected constructor for sub-classes.
     */
    protected ConnectionParameters() {
    }

    /**
     * Fully qualified constructor.
     *
     * @param user the user name
     * @param password the password
     * @param hostname the hostname
     * @param port the port number
     * @param database the database name
     */
    public ConnectionParameters(final String user, final String password, final String database, final String hostname,
            final int port) {
        this.user = user;
        this.password = password;
        this.database = database;
        this.hostname = hostname;
        this.port = port;
    }

    /**
     * Get Properties object for this connection.
     *
     * @return the Properties for this connection
     */
    public Properties getConnectionProperties() {
        final Properties p = new Properties();
        p.put("user", user);
        p.put("password", password);
        return p;
    }

    /**
     * Get the hostname.
     *
     * @return the hostname
     */
    String getHostname() {
        return hostname;
    }

    /**
     * Get the port number.
     *
     * @return the port number
     */
    int getPort() {
        return port;
    }

    /**
     * Get the name of the database.
     *
     * @return the name of the database
     */
    String getDatabase() {
        return database;
    }

    /**
     * Get the user name.
     *
     * @return the user name
     */
    String getUser() {
        return user;
    }

    /**
     * Get the password.
     *
     * @return the password
     */
    String getPassword() {
        return password;
    }

    /**
     * Get the connection string for these parameters.
     * <p>
     * This is public because the DQM database manager is using it.
     *
     * @return the connection string
     */
    public String getConnectionString() {
        return "jdbc:mysql://" + hostname + ":" + port + "/";
    }

    /**
     * Create a database connection from these parameters. The caller becomes the "owner" and is responsible for closing
     * it when finished.
     *
     * @return the new <code>Connection</code> object
     */
    public Connection createConnection() {
        final Properties connectionProperties = getConnectionProperties();
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
     *
     * @param file the properties file
     * @return the connection parameters
     */
    public static ConnectionParameters fromProperties(final File file) {
        FileInputStream fin = null;
        try {
            fin = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            throw new IllegalArgumentException(file.getPath() + " does not exist.", e);
        }
        return fromProperties(fin);
    }

    /**
     * Configure the connection parameters from an embedded classpath resource which should be a properties file.
     *
     * @param resource the resource path
     * @return the connection parameters
     */
    public static ConnectionParameters fromResource(final String resource) {
        return fromProperties(ConnectionParameters.class.getResourceAsStream(resource));
    }

    /**
     * Configure the connection parameters from an <code>InputStream</code> of properties.
     *
     * @param in the InputStream of the properties
     * @return the connection parameters
     * @throws RuntimeException if the InputStream is invalid
     */
    private static ConnectionParameters fromProperties(final InputStream in) {
        final Properties properties = new Properties();
        try {
            properties.load(in);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        final String user = properties.getProperty("user");
        final String password = properties.getProperty("password");
        final String database = properties.getProperty("database");
        final String hostname = properties.getProperty("hostname");
        int port = DEFAULT_PORT;
        if (properties.containsKey("port")) {
            port = Integer.parseInt(properties.getProperty("port"));
        }
        return new ConnectionParameters(user, password, database, hostname, port);
    }
}
