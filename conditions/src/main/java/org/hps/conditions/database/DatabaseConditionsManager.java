package org.hps.conditions.database;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hps.conditions.api.ConditionsObject;
import org.hps.conditions.api.ConditionsObjectCollection;
import org.hps.conditions.api.ConditionsObjectException;
import org.hps.conditions.api.ConditionsRecord;
import org.hps.conditions.api.ConditionsRecord.ConditionsRecordCollection;
import org.hps.conditions.api.ConditionsSeries;
import org.hps.conditions.ecal.EcalConditions;
import org.hps.conditions.ecal.EcalConditionsConverter;
import org.hps.conditions.ecal.TestRunEcalConditionsConverter;
import org.hps.conditions.svt.SvtConditions;
import org.hps.conditions.svt.SvtConditionsConverter;
import org.hps.conditions.svt.SvtDetectorSetup;
import org.hps.conditions.svt.TestRunSvtConditionsConverter;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.lcsim.conditions.ConditionsConverter;
import org.lcsim.conditions.ConditionsManager;
import org.lcsim.conditions.ConditionsManagerImplementation;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.Subdetector;
import org.lcsim.util.log.DefaultLogFormatter;
import org.lcsim.util.log.LogUtil;
import org.lcsim.util.loop.DetectorConditionsConverter;

/**
 * <p>
 * This class provides the top-level API for accessing database conditions, as well as configuring the database
 * connection, initializing all required components, and loading required converters and table meta data. It is
 * registered as the global <code>ConditionsManager</code> in the constructor.
 * <p>
 * Differences between Test Run and Engineering Run configurations are handled automatically.
 *
 * @see org.lcsim.conditions.ConditionsManager
 * @author <a href="mailto:jeremym@slac.stanford.edu">Jeremy McCormick</a>
 */
@SuppressWarnings("rawtypes")
public final class DatabaseConditionsManager extends ConditionsManagerImplementation {

    /**
     * Initialize the logger.
     */
    private static Logger logger = LogUtil.create(DatabaseConditionsManager.class.getName(), new DefaultLogFormatter(),
            Level.FINE);

    /**
     * Create the global registry of conditions object converters.
     */
    private ConverterRegistry converters = ConverterRegistry.create();

    /**
     * Create the global registry of table meta data.
     */
    private TableRegistry tableRegistry = TableRegistry.create();

    /**
     * Name of system property that can be used to specify custom database connection parameters.
     */
    private static final String CONNECTION_PROPERTY = "org.hps.conditions.connection.file";

    /**
     * The connection properties file, if one is being used from the command line.
     */
    private File connectionPropertiesFile;

    /**
     * The current connection parameters.
     */
    private ConnectionParameters connectionParameters;

    /**
     * The current database connection.
     */
    private Connection connection;

    /**
     * True if manager is connected to the database.
     */
    private boolean isConnected = false;

    /**
     * Flag used to print connection parameters one time.
     */
    private boolean loggedConnectionParameters = false;

    /**
     * The default XML config.
     */
    private static final String DEFAULT_CONFIG = "/org/hps/conditions/config/conditions_database_prod.xml";

    /**
     * The Test Run XML config.
     */
    private static final String TEST_RUN_CONFIG = "/org/hps/conditions/config/conditions_database_testrun_2012.xml";

    /**
     * The Eng Run XML config.
     */
    private static final String ENGRUN_CONFIG = "/org/hps/conditions/config/conditions_database_engrun.xml";

    /**
     * The connection properties resource for connecting to the default JLAB database.
     */
    private static final String DEFAULT_CONNECTION_PROPERTIES_RESOURCE = "/org/hps/conditions/config/jlab_connection.prop";

    /**
     * The max value for a run to be considered Test Run.
     */
    private static final int TEST_RUN_MAX_RUN = 1365;

    /**
     * The default ECAL detector name in the detector geometry.
     */
    private String ecalName = "Ecal";

    /**
     * The default SVT name in the detector geometry.
     */
    private String svtName = "Tracker";

    /**
     * The converter for creating the combined SVT conditions object.
     */
    private ConditionsConverter svtConverter;

    /**
     * The converter for creating the combined ECAL conditions object.
     */
    private ConditionsConverter ecalConverter;

    /**
     * The helper for setting up the SVT detector with its conditions information.
     */
    private SvtDetectorSetup svtSetup = new SvtDetectorSetup(svtName);

    /**
     * The currently active conditions tag.
     */
    private String tag = null;

    /**
     * True if the manager has been initialized, e.g. the {@link #setDetector(String, int)} method was called.
     */
    private boolean isInitialized = false;

    /**
     * True if the conditions system has been frozen and will ignore updates after it is initialized.
     */
    private boolean isFrozen = false;

    /**
     * True if the conditions manager was configured from an XML configuration resource or file.
     */
    private boolean isConfigured = false;

    /**
     * True to setup the SVT detector model with conditions.
     */
    private boolean setupSvtDetector = true;

    /**
     * True to freeze the system after initialization.
     */
    private boolean freezeAfterInitialize = false;

    /**
     * True to close the connection after initialization.
     */
    private boolean closeConnectionAfterInitialize = true;

    /**
     * True to cache all known conditions sets (from keys) during initialization.
     */
    private boolean cacheAllConditions = false;

    /**
     * True if current run number is from Test Run.
     */
    private boolean isTestRun = false;

    static {
        // Set default login timeout of 5 seconds.
        DriverManager.setLoginTimeout(5);
    }

    /**
     * Class constructor. Calling this will automatically register this manager as the global default.
     */
    private DatabaseConditionsManager() {
        registerConditionsConverter(new DetectorConditionsConverter());
        setupConnectionFromSystemProperty();
        ConditionsManager.setDefaultConditionsManager(this);
        setRun(-1);
        for (AbstractConditionsObjectConverter converter : converters.values()) {
            // logger.fine("registering converter for " + converter.getType());
            registerConditionsConverter(converter);
        }
        addConditionsListener(svtSetup);
    }

    /**
     * Get the static instance of this class.
     *
     * @return The static instance of the manager.
     */
    public static synchronized DatabaseConditionsManager getInstance() {

        logger.finest("getting conditions manager instance");

        // Is there no manager installed yet?
        if (!ConditionsManager.isSetup() ||
                !(ConditionsManager.defaultInstance() instanceof DatabaseConditionsManager)) {
            logger.finest("creating new DatabaseConditionsManager");
            // Create a new instance if necessary, which will install it globally as the default.
            new DatabaseConditionsManager();
        }

        // Get the instance back from the default conditions system and check that the type is correct now.
        final ConditionsManager manager = ConditionsManager.defaultInstance();
        if (!(manager instanceof DatabaseConditionsManager)) {
            logger.severe("default conditions manager has wrong type");
            throw new RuntimeException("Default conditions manager has the wrong type: "
                    + ConditionsManager.defaultInstance().getClass().getName());
        }

        logger.finest("returning conditions manager instance");

        return (DatabaseConditionsManager) manager;
    }

    /**
     * Reset the global static instance of the conditions manager to a new object.
     */
    public static synchronized void resetInstance() {
        logger.finest("DatabaseConditionsManager instance is being reset");
        new DatabaseConditionsManager();
    }

    /**
     * Set the log level.
     *
     * @param level The log level.
     */
    public void setLogLevel(final Level level) {
        logger.config("setting log level to " + level);
        logger.setLevel(level);
        logger.getHandlers()[0].setLevel(level);
        svtSetup.setLogLevel(level);
    }

    /**
     * Open the database connection.
     *
     * @return True if a connection was opened; false if using an existing connection.
     */
    public synchronized boolean openConnection() {
        boolean openedConnection = false;
        if (!isConnected) {
            // Do the connection parameters need to be figured out automatically?
            if (connectionParameters == null) {
                // Setup the default read-only connection, which will choose a SLAC or JLab database.
                connectionParameters = ConnectionParameters.fromResource(DEFAULT_CONNECTION_PROPERTIES_RESOURCE);
            }

            if (!loggedConnectionParameters) {
                // Print out detailed info to the log on first connection within the job.
                logger.info("opening connection ... " + '\n' + "connection: "
                        + connectionParameters.getConnectionString() + '\n' + "host: "
                        + connectionParameters.getHostname() + '\n' + "port: " + connectionParameters.getPort() + '\n'
                        + "user: " + connectionParameters.getUser() + '\n' + "database: "
                        + connectionParameters.getDatabase());
                loggedConnectionParameters = true;
            }

            // Create the connection using the parameters.
            connection = connectionParameters.createConnection();
            isConnected = true;
            openedConnection = true;
        }
        logger.info("connection opened successfully");

        // Flag to indicate whether an existing connection was used or not.
        return openedConnection;
    }

    /**
     * Close the database connection.
     */
    public synchronized void closeConnection() {
        logger.fine("closing connection");
        if (connection != null) {
            try {
                if (!connection.isClosed()) {
                    connection.close();
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
        connection = null;
        isConnected = false;
        logger.info("connection closed");
    }

    /**
     * Close the database connection but only if there was a connection opened based on the flag. Otherwise, it should
     * be left open.
     *
     * @param connectionOpened True to close the connection; false to leave it open.
     */
    public synchronized void closeConnection(final boolean connectionOpened) {
        if (connectionOpened) {
            closeConnection();
        }
    }

    /**
     * Get a conditions series with one or more collections.
     *
     * @param collectionType The type of the collection.
     * @param tableName The name of the data table.
     * @param <ObjectType> The type of the conditions object.
     * @param <CollectionType> The type of the conditions collection.
     * @return The conditions series.
     */
    @SuppressWarnings("unchecked")
    public
    <ObjectType extends ConditionsObject, CollectionType extends ConditionsObjectCollection<ObjectType>> 
        ConditionsSeries<ObjectType, CollectionType> getConditionsSeries(
                final Class<CollectionType> collectionType, final String tableName) {

        final TableMetaData metaData = tableRegistry.get(tableName);
        if (metaData == null) {
            throw new IllegalArgumentException("No table metadata found for type " + collectionType.getName());
        }
        if (!metaData.getCollectionClass().equals(collectionType)) {
            throw new IllegalArgumentException("The type " + collectionType.getName() + " does not match the class "
                    + metaData.getCollectionClass().getName() + " from the meta data");
        }
        final Class<? extends ConditionsObject> objectType = metaData.getObjectClass();
        final ConditionsSeriesConverter<ObjectType, CollectionType> converter =
                new ConditionsSeriesConverter(objectType, collectionType);
        return converter.createSeries(tableName);
    }

    /**
     * This method handles changes to the detector name and run number. It is called every time an LCSim event is
     * created, and so it has internal logic to figure out if the conditions system actually needs to be updated.
     */
    @Override
    public synchronized void setDetector(final String detectorName, final int runNumber) 
            throws ConditionsNotFoundException {

        logger.finest("setDetector " + detectorName + " with run number " + runNumber);

        if (detectorName == null) {
            throw new IllegalArgumentException("The detectorName argument is null.");
        }

        if (!isInitialized || !detectorName.equals(getDetector()) || runNumber != getRun()) {
            if (!isFrozen) {
                logger.info("new detector " + detectorName + " and run #" + runNumber);
                initialize(detectorName, runNumber);
            } else {
                logger.finest("Conditions changed but will be ignored because manager is frozen.");
            }
        }
    }

    /**
     * Utility method to determine if a run number is from the 2012 Test Run.
     *
     * @param runNumber The run number.
     * @return True if run number is from the Test Run.
     */
    public static boolean isTestRun(final int runNumber) {
        return runNumber > 0 && runNumber <= TEST_RUN_MAX_RUN;
    }

    /**
     * True if Test Run configuration is selected.
     *
     * @return True if current run is from the Test Run.
     */
    public boolean isTestRun() {
        return isTestRun;
    }

    /**
     * Get the current LCSim compact <code>Detector</code> object.
     *
     * @return The detector object.
     */
    public Detector getDetectorObject() {
        return getCachedConditions(Detector.class, "compact.xml").getCachedData();
    }

    /**
     * Configure this object from an XML file.
     *
     * @param file The XML file.
     */
    public void setXmlConfig(final File file) {
        logger.config("setting XML config from file " + file.getPath());
        if (!file.exists()) {
            throw new IllegalArgumentException("The config file does not exist: " + file.getPath());
        }
        try {
            configure(new FileInputStream(file));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Configure this object from an embedded XML resource.
     *
     * @param resource The embedded XML resource.
     */
    public void setXmlConfig(final String resource) {
        logger.config("setting XML config from resource " + resource);
        final InputStream is = getClass().getResourceAsStream(resource);
        configure(is);
    }

    /**
     * Set the path to a properties file containing connection settings.
     *
     * @param file The properties file
     */
    public void setConnectionProperties(final File file) {
        logger.config("setting connection properties file " + file.getPath());
        if (!file.exists()) {
            throw new IllegalArgumentException("The connection properties file does not exist: "
                    + connectionPropertiesFile.getPath());
        }
        connectionParameters = ConnectionParameters.fromProperties(file);
    }

    /**
     * Set the connection parameters of the conditions database.
     *
     * @param connectionParameters The connection parameters.
     */
    public void setConnectionParameters(final ConnectionParameters connectionParameters) {
        this.connectionParameters = connectionParameters;
    }

    /**
     * Set the connection parameters from an embedded resource location.
     *
     * @param resource The classpath resource location.
     */
    public void setConnectionResource(final String resource) {
        logger.config("setting connection resource " + resource);
        connectionParameters = ConnectionParameters.fromResource(resource);
    }

    /**
     * Get the next collection ID for a database conditions table.
     *
     * @param tableName The name of the table.
     * @return The next collection ID.
     */
    public synchronized int getNextCollectionID(final String tableName) {
        final boolean openedConnection = openConnection();
        final ResultSet resultSet = selectQuery("SELECT MAX(collection_id)+1 FROM " + tableName);
        int collectionId = 1;
        try {
            resultSet.next();
            collectionId = resultSet.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
            logger.warning(e.getMessage());
        }
        logger.fine("new collection ID " + collectionId + " created for table " + tableName);
        closeConnection(openedConnection);
        return collectionId;
    }

    /**
     * This method will return true if the given collection ID already exists in the table.
     *
     * @param tableName The name of the table.
     * @param collectionID The collection ID value.
     * @return True if collection exists.
     */
    public boolean collectionExists(final String tableName, final int collectionID) {
        final String sql = "SELECT * FROM " + tableName + " where collection_id = " + collectionID;
        final ResultSet resultSet = selectQuery(sql);
        try {
            resultSet.last();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        int rowCount = 0;
        try {
            rowCount = resultSet.getRow();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return rowCount != 0;
    }

    /**
     * This method can be used to perform a database SELECT query.
     *
     * @param query The SQL query string.
     * @return The ResultSet from the query or null.
     */
    ResultSet selectQuery(final String query) {
        logger.fine("executing SQL select query ..." + '\n' + query);
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
     *
     * @param query The SQL query string.
     * @return The keys of the rows affected.
     */
    public List<Integer> updateQuery(final String query) {
        final boolean openedConnection = openConnection();
        logger.fine("executing SQL update query ..." + '\n' + query);
        final List<Integer> keys = new ArrayList<Integer>();
        Statement statement = null;
        ResultSet resultSet = null;
        try {
            statement = connection.createStatement();
            statement.executeUpdate(query, Statement.RETURN_GENERATED_KEYS);
            resultSet = statement.getGeneratedKeys();
            while (resultSet.next()) {
                final int key = resultSet.getInt(1);
                keys.add(key);
            }
        } catch (SQLException x) {
            throw new RuntimeException("Error in SQL query: " + query, x);
        }
        DatabaseUtilities.cleanup(resultSet);
        closeConnection(openedConnection);
        return keys;
    }

    /**
     * Find a collection of conditions validity records by key name. The key name is distinct from the table name, but
     * they are usually set to the same value.
     *
     * @param name The conditions key name.
     * @return The set of matching conditions records.
     */
    public ConditionsRecordCollection findConditionsRecords(final String name) {
        final ConditionsRecordCollection runConditionsRecords = getCachedConditions(ConditionsRecordCollection.class,
                "conditions").getCachedData();
        logger.fine("searching for conditions with name " + name + " in " + runConditionsRecords.size() + " records");
        final ConditionsRecordCollection foundConditionsRecords = new ConditionsRecordCollection();
        for (ConditionsRecord record : runConditionsRecords) {
            if (record.getName().equals(name)) {
                if (matchesTag(record)) {
                    foundConditionsRecords.add(record);
                    logger.finer("found matching conditions record " + record.getRowId());
                } else {
                    logger.finer("conditions record " + record.getRowId() + " rejected from non-matching tag "
                            + record.getTag());
                }
            }
        }
        logger.fine("found " + foundConditionsRecords.size() + " conditions records matching tag " + tag);
        return foundConditionsRecords;
    }

    /**
     * True if there is a conditions record with the given name.
     *
     * @param name The conditions name.
     * @return True if a conditions record exists with the given name.
     */
    public boolean hasConditionsRecord(final String name) {
        return !findConditionsRecords(name).isEmpty();
    }

    /**
     * Get a list of all the ConditionsRecord objects.
     *
     * @return The list of all the ConditionsRecord objects.
     */
    // FIXME: This should use a cache that is created during initialization, rather than look these up every time.
    public ConditionsRecordCollection getConditionsRecords() {
        logger.finer("getting conditions records ...");
        final ConditionsRecordCollection conditionsRecords = new ConditionsRecordCollection();
        for (TableMetaData tableMetaData : tableRegistry.values()) {
            try {
                final ConditionsRecordCollection foundConditionsRecords =
                        findConditionsRecords(tableMetaData.getKey());
                logger.finer("found " + foundConditionsRecords.size() + " collections with name "
                        + tableMetaData.getKey());
                conditionsRecords.addAll(foundConditionsRecords);
            } catch (Exception e) {
                e.printStackTrace();
                logger.warning(e.getMessage());
            }
        }
        logger.finer("found " + conditionsRecords + " conditions records");
        logger.getHandlers()[0].flush();
        return conditionsRecords;
    }

    /**
     * Get the combined ECAL conditions for this run.
     *
     * @return The combined ECAL conditions.
     */
    public EcalConditions getEcalConditions() {
        return this.getCachedConditions(EcalConditions.class, "ecal_conditions").getCachedData();
    }

    /**
     * Get the combined SVT conditions for this run.
     *
     * @return The combined SVT conditions.
     */
    public SvtConditions getSvtConditions() {
        return this.getCachedConditions(SvtConditions.class, "svt_conditions").getCachedData();
    }

    /**
     * This method can be called to "freeze" the conditions system so that any subsequent updates to run number or
     * detector name will be ignored.
     */
    public synchronized void freeze() {
        if (getDetector() != null && getRun() != -1) {
            isFrozen = true;
            logger.config("conditions system is frozen");
        } else {
            logger.warning("conditions system cannot be frozen because it is not initialized yet");
        }
    }

    /**
     * Un-freeze the conditions system so that updates will be received again.
     */
    public synchronized void unfreeze() {
        isFrozen = false;
        logger.info("conditions system unfrozen");
    }

    /**
     * True if conditions system is frozen.
     *
     * @return True if conditions system is frozen.
     */
    public boolean isFrozen() {
        return isFrozen;
    }

    /**
     * Set a tag used to filter ConditionsRecords.
     *
     * @param tag The tag value used to filter ConditionsRecords.
     */
    public void setTag(final String tag) {
        this.tag = tag;
        logger.info("using conditions tag: " + tag);
    }

    /**
     * Insert a collection of ConditionsObjects into the database.
     *
     * @param collection The collection to insert.
     * @param <ObjectType> The type of the conditions object.
     * @throws SQLException If there is a database error.
     * @throws ConditionsObjectException If there is a problem with the ConditionsObjects.
     */
    public <ObjectType extends ConditionsObject> void insertCollection(
            final ConditionsObjectCollection<ObjectType> collection)
            throws SQLException, ConditionsObjectException {

        if (collection == null) {
            throw new IllegalArgumentException("The collection is null.");
        }
        if (collection.size() == 0) {
            throw new IllegalArgumentException("The collection is empty.");
        }

        final TableMetaData tableMetaData = collection.getTableMetaData();
        if (tableMetaData == null) {
            final List<TableMetaData> metaDataList = tableRegistry.findByCollectionType(collection.getClass());
            if (metaDataList == null) {
                // This is a fatal error because no meta data is available for the type.
                throw new ConditionsObjectException("Failed to find meta data for type: " + collection.getClass());
            } 
        }
        if (collection.getCollectionId() == -1) {
            try {
                collection.setCollectionId(getNextCollectionID(tableMetaData.getTableName()));
            } catch (ConditionsObjectException e) {
                throw new RuntimeException(e);
            }
        }
        // FIXME: If collection ID is already set this should be an error!

        logger.info("inserting collection with ID " + collection.getCollectionId() + " and key "
                + tableMetaData.getKey() + " into table " + tableMetaData.getTableName());

        final boolean openedConnection = openConnection();

        PreparedStatement preparedStatement = null;

        try {
            connection.setAutoCommit(false);
            logger.fine("starting insert transaction");
            final String sql = QueryBuilder.buildPreparedInsert(
                    tableMetaData.getTableName(), collection.iterator().next());
            preparedStatement = connection.prepareStatement(sql);
            logger.fine("using prepared statement: " + sql);
            final int collectionId = collection.getCollectionId();
            for (ConditionsObject object : collection) {
                preparedStatement.setObject(1, collectionId);
                int parameterIndex = 2;
                for (Entry<String, Object> entry : object.getFieldValues().entrySet()) {
                    preparedStatement.setObject(parameterIndex, entry.getValue());
                    ++parameterIndex;
                }
                preparedStatement.executeUpdate();
            }
            connection.commit();
            logger.fine("committed transaction");
        } catch (Exception e) {
            e.printStackTrace();
            logger.warning(e.getMessage());
            logger.warning("rolling back transaction");
            connection.rollback();
            logger.warning("transaction was rolled back");
        } finally {
            connection.setAutoCommit(true);
        }

        try {
            preparedStatement.close();
        } catch (Exception e) {
        }

        closeConnection(openedConnection);
    }

    /**
     * Check if connected to the database.
     *
     * @return true if connected
     */
    public boolean isConnected() {
        return isConnected;
    }

    /**
     * Get the Logger for this class, which can be used by related sub-classes if they do not have their own logger.
     *
     * @return The Logger for this class.
     */
    public static Logger getLogger() {
        return logger;
    }

    /**
     * Find table information from the name.
     *
     * @param name The name of the table.
     * @return The table information or null if does not exist.
     */
    public TableMetaData findTableMetaData(final String name) {
        return tableRegistry.findByTableName(name);
    }

    /**
     * Find table information from the collection type.
     *
     * @param type The collection type.
     * @return The table information or null if does not exist.
     */
    public List<TableMetaData> findTableMetaData(final Class<?> type) {
        return tableRegistry.findByCollectionType(type);
    }

    /**
     * Get the name of the ECAL in the detector geometry.
     *
     * @return The name of the ECAL.
     */
    public String getEcalName() {
        return ecalName;
    }

    /**
     * Get the subdetector object of the ECAL.
     *
     * @return The ECAL subdetector.
     */
    public Subdetector getEcalSubdetector() {
        return this.getDetectorObject().getSubdetector(ecalName);
    }

    /**
     * True if conditions manager is properly initialized.
     *
     * @return True if the manager is initialized.
     */
    public boolean isInitialized() {
        return isInitialized;
    }

    /**
     * Get the set of unique conditions tags from the conditions table.
     *
     * @return The list of unique conditions tags.
     */
    public Set<String> getTags() {
        logger.fine("getting list of available conditions tags");
        final boolean openedConnection = openConnection();
        final Set<String> tags = new LinkedHashSet<String>();
        final ResultSet rs = selectQuery("select distinct(tag) from conditions where tag is not null order by tag");
        try {
            while (rs.next()) {
                tags.add(rs.getString(1));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        try {
            rs.close();
        } catch (SQLException e) {
            logger.log(Level.WARNING, "error closing ResultSet", e);
        }
        final StringBuffer sb = new StringBuffer();
        sb.append("found unique conditions tags: ");
        for (String tag : tags) {
            sb.append(tag + " ");
        }
        sb.setLength(sb.length() - 1);
        logger.fine(sb.toString());
        closeConnection(openedConnection);
        return tags;
    }

    /**
     * Perform all necessary initialization, including setup of the XML configuration and loading of conditions
     * onto the Detector.
     *
     * @param detectorName The name of the detector model.
     * @param runNumber The run number.
     * @throws ConditionsNotFoundException If there is a conditions system error.
     */
    private void initialize(final String detectorName, final int runNumber) throws ConditionsNotFoundException {

        logger.config("initializing with detector " + detectorName + " and run " + runNumber);

        // Is not configured yet?
        if (!isConfigured) {
            if (isTestRun(runNumber)) {
                // This looks like the Test Run so use the custom configuration for it.
                setXmlConfig(DatabaseConditionsManager.TEST_RUN_CONFIG);
            } else if (runNumber > TEST_RUN_MAX_RUN) {
                // Run numbers greater than max of Test Run assumed to be Eng Run (for now!).
                setXmlConfig(DatabaseConditionsManager.ENGRUN_CONFIG);
            } else if (runNumber == 0) {
                // Use the default configuration because the run number is basically meaningless.
                setXmlConfig(DatabaseConditionsManager.DEFAULT_CONFIG);
            }
        }

        // Register the converters for this initialization.
        logger.fine("registering converters");
        registerConverters();

        // Enable or disable the setup of the SVT detector.
        logger.fine("enabling SVT setup: " + setupSvtDetector);
        svtSetup.setEnabled(setupSvtDetector);

        // Open the database connection.
        openConnection();

        // Call the super class's setDetector method to construct the detector object and activate conditions listeners.
        logger.fine("activating default conditions manager");
        super.setDetector(detectorName, runNumber);

        // Should all conditions sets be cached?
        if (cacheAllConditions) {
            // Cache the conditions sets of all registered converters.
            logger.fine("caching all conditions sets ...");
            cacheConditionsSets();
        }

        if (closeConnectionAfterInitialize) {
            logger.fine("closing connection after initialization");
            // Close the connection.
            closeConnection();
        }

        // Should the conditions system be frozen now?
        if (freezeAfterInitialize) {
            // Freeze the conditions system so subsequent updates will be ignored.
            freeze();
            logger.config("system was frozen after initialization");
        }

        isInitialized = true;

        logger.info("conditions system initialized successfully");

        // Flush logger after initialization.
        logger.getHandlers()[0].flush();
    }

    /**
     * Register the conditions converters with the manager.
     */
    private void registerConverters() {
        if (svtConverter != null) {
            // Remove old SVT converter.
            removeConditionsConverter(svtConverter);
        }

        if (ecalConverter != null) {
            // Remove old ECAL converter.
            registerConditionsConverter(ecalConverter);
        }

        // Is configured for TestRun?
        if (isTestRun()) {
            // Load Test Run specific converters.
            svtConverter = new TestRunSvtConditionsConverter();
            ecalConverter = new TestRunEcalConditionsConverter();
            logger.config("registering Test Run conditions converters");
        } else {
            // Load the default converters.
            svtConverter = new SvtConditionsConverter();
            ecalConverter = new EcalConditionsConverter();
            logger.config("registering default conditions converters");
        }
        registerConditionsConverter(svtConverter);
        registerConditionsConverter(ecalConverter);
    }

    /**
     * Set the name of the ECAL sub-detector.
     *
     * @param ecalName The name of the ECAL.
     */
    private void setEcalName(final String ecalName) {
        if (ecalName == null) {
            throw new IllegalArgumentException("The ecalName is null");
        }
        this.ecalName = ecalName;
        logger.info("ECAL name set to " + ecalName);
    }

    /**
     * Set the name of the SVT sub-detector.
     *
     * @param svtName The name of the SVT.
     */
    private void setSvtName(final String svtName) {
        if (svtName == null) {
            throw new IllegalArgumentException("The svtName is null");
        }
        this.svtName = svtName;
        logger.info("SVT name set to " + ecalName);
    }

    /**
     * True if the conditions record matches the current tag.
     *
     * @param record The conditions record.
     * @return True if conditions record matches the currently used tag.
     */
    private boolean matchesTag(final ConditionsRecord record) {
        if (this.tag == null) {
            // If there is no tag set then all records pass.
            return true;
        }
        final String recordTag = record.getTag();
        if (recordTag == null) {
            // If there is a tag set but the record has no tag, it is rejected.
            return false;
        }
        return tag.equals(recordTag);
    }

    /**
     * Cache conditions sets for all known tables.
     */
    private void cacheConditionsSets() {
        for (TableMetaData meta : tableRegistry.values()) {
            try {
                logger.fine("caching conditions " + meta.key + " with type " + meta.collectionClass.getCanonicalName());
                getCachedConditions(meta.collectionClass, meta.key);
            } catch (Exception e) {
                logger.warning("could not cache conditions " + meta.key);
            }
        }
    }

    /**
     * Configure this class from an <code>InputStream</code> which should point to an XML document.
     *
     * @param in the InputStream which should be in XML format
     */
    private void configure(final InputStream in) {
        if (!isConfigured) {
            final SAXBuilder builder = new SAXBuilder();
            Document config = null;
            try {
                config = builder.build(in);
            } catch (JDOMException | IOException e) {
                throw new RuntimeException(e);
            }
            loadConfiguration(config);
            try {
                in.close();
            } catch (IOException e) {
                logger.warning(e.getMessage());
            }
            isConfigured = true;
        } else {
            logger.warning("System is already configured, so call to configure is ignored!");
        }
    }

    /**
     * Setup the database connection from a file specified by a Java system property setting. This could be overridden
     * by subsequent API calls to {@link #setConnectionProperties(File)} or {@link #setConnectionResource(String)}.
     */
    private void setupConnectionFromSystemProperty() {
        final String systemPropertiesConnectionPath = (String) System.getProperties().get(CONNECTION_PROPERTY);
        if (systemPropertiesConnectionPath != null) {
            final File f = new File(systemPropertiesConnectionPath);
            if (!f.exists()) {
                throw new RuntimeException("Connection properties file from " + CONNECTION_PROPERTY
                        + " does not exist.");
            }
            setConnectionProperties(f);
            logger.info("connection setup from system property " + CONNECTION_PROPERTY + " = "
                    + systemPropertiesConnectionPath);
        }
    }

    /**
     * Load configuration information from an XML document.
     *
     * @param document The XML document.
     */
    private void loadConfiguration(final Document document) {

        final Element node = document.getRootElement().getChild("configuration");

        if (node == null) {
            return;
        }

        Element element = node.getChild("setupSvtDetector");
        if (element != null) {
            setupSvtDetector = Boolean.parseBoolean(element.getText());
            logger.config("setupSvtDetector = " + setupSvtDetector);
        }

        element = node.getChild("ecalName");
        if (element != null) {
            setEcalName(element.getText());
        }

        element = node.getChild("svtName");
        if (element != null) {
            setSvtName(element.getText());
        }

        element = node.getChild("freezeAfterInitialize");
        if (element != null) {
            freezeAfterInitialize = Boolean.parseBoolean(element.getText());
            logger.config("freezeAfterInitialize = " + freezeAfterInitialize);
        }

        element = node.getChild("cacheAllCondition");
        if (element != null) {
            cacheAllConditions = Boolean.parseBoolean(element.getText());
            logger.config("cacheAllConditions = " + cacheAllConditions);
        }

        element = node.getChild("isTestRun");
        if (element != null) {
            isTestRun = Boolean.parseBoolean(element.getText());
            logger.config("isTestRun = " + isTestRun);
        }

        element = node.getChild("logLevel");
        if (element != null) {
            setLogLevel(Level.parse(element.getText()));
        }

        element = node.getChild("closeConnectionAfterInitialize");
        if (element != null) {
            closeConnectionAfterInitialize = Boolean.parseBoolean(element.getText());
            logger.config("closeConnectionAfterInitialize = " + closeConnectionAfterInitialize);
        }

        element = node.getChild("loginTimeout");
        if (element != null) {
            final Integer timeout = Integer.parseInt(element.getText());
            DriverManager.setLoginTimeout(timeout);
            logger.config("loginTimeout = " + timeout);
        }
    }
}
