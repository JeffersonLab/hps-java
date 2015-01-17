package org.hps.conditions.database;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hps.conditions.api.ConditionsObject;
import org.hps.conditions.api.ConditionsObjectCollection;
import org.hps.conditions.api.ConditionsObjectException;
import org.hps.conditions.api.ConditionsRecord;
import org.hps.conditions.api.ConditionsRecord.ConditionsRecordCollection;
import org.hps.conditions.api.ConditionsSeries;
import org.hps.conditions.ecal.EcalConditionsConverter;
import org.hps.conditions.ecal.EcalDetectorSetup;
import org.hps.conditions.ecal.TestRunEcalConditionsConverter;
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
import org.lcsim.util.log.LogUtil;
import org.lcsim.util.loop.DetectorConditionsConverter;

/**
 * <p>
 * This class provides the top-level API for accessing database conditions,
 * as well as configuring the database connection, initializing all
 * required components, and loading required converters and table meta data.
 * It is registered as the global <code>ConditionsManager</code> in the 
 * constructor.
 * <p>
 * Differences between Test Run and Engineering Run configurations are handled
 * automatically.
 * 
 * @see org.lcsim.conditions.ConditionsManager
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
@SuppressWarnings("rawtypes")
public class DatabaseConditionsManager extends ConditionsManagerImplementation {

    protected static Logger logger = LogUtil.create(DatabaseConditionsManager.class);

    // Registry of conditions converters.
    protected ConverterRegistry converters = ConverterRegistry.create();
    
    // Registry of table meta data.
    protected TableRegistry tableRegistry = TableRegistry.create();
    
    // Connection configuration.
    protected static final String CONNECTION_PROPERTY = "org.hps.conditions.connection.file";
    protected File connectionPropertiesFile;
    protected ConnectionParameters connectionParameters;
    protected Connection connection;
    protected boolean isConnected = false;
    protected boolean loggedConnectionParameters = false;
    
    // Default configuration resources.
    protected static final String DEFAULT_CONFIG = "/org/hps/conditions/config/conditions_database_prod.xml";
    protected static final String TEST_RUN_CONFIG = "/org/hps/conditions/config/conditions_database_testrun_2012.xml";
    protected static final String ENGRUN_CONFIG = "/org/hps/conditions/config/conditions_database_engrun.xml";
    
    // Max run number for the Test Run.
    protected static final int TEST_RUN_MAX_RUN = 1365;
    
    // The default Test Run detector.
    private static final String DEFAULT_TEST_RUN_DETECTOR = "HPS-TestRun-v8-5";

    // The default Engineering Run detector.
    private static final String DEFAULT_ENG_RUN_DETECTOR = "HPS-Proposal2014-v8-6pt6";
    
    // Detector setup.
    protected String detectorName;
    protected String ecalName = "Ecal";
    protected String svtName = "Tracker";
    protected ConditionsConverter svtConverter;
    protected ConditionsConverter ecalConverter;
    protected EcalDetectorSetup ecalSetup = new EcalDetectorSetup(ecalName);
    protected SvtDetectorSetup svtSetup = new SvtDetectorSetup(svtName);
    
    // Active conditions tag.
    protected String tag = null;

    // State of manager.
    protected boolean isInitialized = false;
    protected boolean isFrozen = false;
    protected boolean isConfigured = false;
    
    // Configuration from XML settings.  These are the defaults.
    protected boolean setupSvtDetector = true;
    protected boolean setupEcalDetector = true;
    protected boolean freezeAfterInitialize = false;
    protected boolean closeConnectionAfterInitialize = true;
    protected boolean cacheAllConditions = false;
    protected boolean isTestRun = false;
    
    // Default login timeout of 5 seconds.
    static {
        DriverManager.setLoginTimeout(5);
    }
    
    /**
     * Class constructor.
     * Calling this will automatically register this
     * manager as the global default.
     */
    public DatabaseConditionsManager() {
        logger.setLevel(Level.INFO);
        registerConditionsConverter(new DetectorConditionsConverter());
        setupConnectionFromSystemProperty();
        ConditionsManager.setDefaultConditionsManager(this);
        setRun(-1);
        for (ConditionsObjectConverter converter : converters.values()) {
            //logger.config("registering converter for " + converter.getType());
            registerConditionsConverter(converter);
        }
        addConditionsListener(ecalSetup);
        addConditionsListener(svtSetup);
    }
    
    /**
     * Get the static instance of this class.
     * @return The static instance of the manager.
     */
    public static DatabaseConditionsManager getInstance() {

        // Is there no manager installed yet?
        if (!ConditionsManager.isSetup()) {
            // Perform default setup if necessary.
            new DatabaseConditionsManager();
        }

        // Get the instance of the manager from the conditions system and check that the type is valid.
        ConditionsManager manager = ConditionsManager.defaultInstance();
        if (!(manager instanceof DatabaseConditionsManager)) {
            throw new RuntimeException("The default ConditionsManager has the wrong type.");
        }

        return (DatabaseConditionsManager) manager;
    }
    
    /**
     * Open the database connection.
     * @return True if a connection was opened; false if using an existing connection.
     */
    public boolean openConnection() {
        boolean openedConnection = false;
        if (!isConnected) {
            // Do the connection parameters need to be figured out automatically?
            if (connectionParameters == null) {
                // Setup the default read-only connection, which will choose a SLAC or JLab database.
                connectionParameters = ConnectionParameters.fromResource(chooseConnectionPropertiesResource());
            }
            
            if (!loggedConnectionParameters) {
                // Print out detailed info to the log on first connection.
                logger.info("opening connection to " + connectionParameters.getConnectionString());
                logger.info("host " + connectionParameters.getHostname());
                logger.info("port " + connectionParameters.getPort());
                logger.info("user " + connectionParameters.getUser());
                logger.info("database " + connectionParameters.getDatabase());
                loggedConnectionParameters = true;
            }

            // Create the connection using the parameters.
            connection = connectionParameters.createConnection();
            isConnected = true;                        
            openedConnection = true;
        } 
        logger.info("connection opened");
        
        // Flag to indicate whether an existing connection was used or not.
        return openedConnection;
    }

    /**
     * Close the database connection.
     */
    public void closeConnection() {
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
     * Close the database connection but only if there was a connection opened
     * based on the flag.  Otherwise, it should be left open.
     * @param connectionOpened True to close the connection; false to leave it open.
     */
    public void closeConnection(boolean connectionOpened) {
        if (connectionOpened) {
            closeConnection();
        }
    }

    /**
     * Get a conditions series with one or more collections.
     * @param collectionType The type of the collection.
     * @param tableName The name of the data table.
     * @return The conditions series.
     */
    public 
    <ObjectType extends ConditionsObject, CollectionType extends ConditionsObjectCollection<ObjectType>> 
    ConditionsSeries<ObjectType, CollectionType> getConditionsSeries(Class<CollectionType> collectionType, String tableName) {
        TableMetaData tableInfo = tableRegistry.findByCollectionType(collectionType);
        if (tableInfo == null) {
            throw new IllegalArgumentException("No table meta data found for collection type: " + collectionType);
        }
        Class<? extends ConditionsObject> objectType = tableInfo.getObjectClass();
        ConditionsSeriesConverter<ObjectType, CollectionType> converter = 
                new ConditionsSeriesConverter(objectType, collectionType);
        return converter.createSeries(tableName);
    }
    
    /**
     * Get a conditions series using the default table name for the data.
     * @param collectionType The type of collection.
     * @return The conditions series.
     */
    public 
    <ObjectType extends ConditionsObject, CollectionType extends ConditionsObjectCollection<ObjectType>> 
    ConditionsSeries<ObjectType, CollectionType> getConditionsSeries(Class<CollectionType> collectionType) {
        TableMetaData tableInfo = findTableMetaData(collectionType); 
        return getConditionsSeries((Class<CollectionType>)tableInfo.getCollectionClass(), tableInfo.getTableName());
    }
    
    /**
     * Get a given collection of the given type from the conditions database
     * using the default table name.
     * @param type The type of the conditions data.
     * @return A collection of objects of the given type from the conditions database
     */
    public <CollectionType extends ConditionsObjectCollection> CollectionType getCollection(Class<CollectionType> type) {
        TableMetaData metaData = tableRegistry.findByCollectionType(type);
        if (metaData == null) {
            throw new RuntimeException("Table name data for condition of type " + type.getSimpleName() + " was not found.");
        }
        String tableName = metaData.getTableName();
        CollectionType conditionsCollection = getCachedConditions(type, tableName).getCachedData();
        return conditionsCollection;
    }

    /**
     * This method handles changes to the detector name and run number.
     * It is called every time an LCSim event is created, and so it has 
     * internal logic to figure out if the conditions system actually
     * needs to be updated.
     */
    @Override
    public void setDetector(String detectorName, int runNumber) throws ConditionsNotFoundException {

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
     * @param runNumber The run number.
     * @return True if run number is from the Test Run.
     */
    public static boolean isTestRun(int runNumber) {
        return runNumber > 0 && runNumber <= TEST_RUN_MAX_RUN;
    }
    
    /**
     * True if Test Run configuration is selected.
     * @return True if current run is from the Test Run.
     */
    public boolean isTestRun() {
        return isTestRun;
    }
    
    /**
     * Perform all necessary initialization, including setup of the XML
     * configuration and loading of conditions onto the Detector.
     */
    protected void initialize(String detectorName, int runNumber) throws ConditionsNotFoundException {
        
        logger.info("initializing detector " + detectorName + " and run " + runNumber);
        
        // Is not configured yet?
        if (!isConfigured) {
            if (isTestRun(runNumber)) {
                logger.config("using Test Run configuration");
                // This looks like the Test Run so use the custom configuration for it.
                setXmlConfig(DatabaseConditionsManager.TEST_RUN_CONFIG);
            } else if (runNumber > TEST_RUN_MAX_RUN) {
                // Run numbers greater than max of Test Run assumed to be Eng Run (for now!).
                setXmlConfig(DatabaseConditionsManager.ENGRUN_CONFIG);
            } 
        }

        registerConverters();
    
        ecalSetup.setEnabled(setupEcalDetector);
        svtSetup.setEnabled(setupSvtDetector);

        // Open the database connection.
        openConnection();
        
        // Call the super class's setDetector method to construct the detector object.
        super.setDetector(detectorName, runNumber);
                        
        // Should all conditions sets be cached?
        if (cacheAllConditions) {
            // Cache the conditions sets of all registered converters.
            logger.info("caching all conditions sets ...");
            cacheConditionsSets();
        }
                
        if (closeConnectionAfterInitialize) {
            // Close the connection. 
            closeConnection();
        }
        
        // Should the conditions system be frozen now?
        if (freezeAfterInitialize) {
            // Freeze the conditions system so subsequent updates will be ignored.
            freeze();
            logger.info("frozen after initialize");
        }
        
        isInitialized = true;
        
        logger.info("conditions system initialized successfully");
    }

    /**
     * 
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
     * Get the current LCSim compact <code>Detector</code> object.
     * @return The detector object.
     */
    public Detector getDetectorObject() {
        return getCachedConditions(Detector.class, "compact.xml").getCachedData();
    }

    /**
     * Get conditions data by class and name.
     * @param type The class of the conditions.
     * @param name The name of the conditions set.
     * @return The conditions or null if does not exist.
     */
    public <T> T getConditionsData(Class<T> type, String name) {
        logger.fine("getting conditions " + name + " of type " + type.getSimpleName());
        return getCachedConditions(type, name).getCachedData();
    }

    /**
     * Configure this object from an XML file.
     * @param file The XML file.
     */
    public void setXmlConfig(File fileConfig) {       
        logger.config("setting XML config from file " + fileConfig.getPath());
        if (!fileConfig.exists()) {
            throw new IllegalArgumentException("The config file does not exist: " + fileConfig.getPath());
        }
        try {
            configure(new FileInputStream(fileConfig));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Configure this object from an embedded XML resource.
     * @param resource The embedded XML resource.
     */
    public void setXmlConfig(String resourceConfig) {
        logger.config("setting XML config from resource " + resourceConfig);
        InputStream is = getClass().getResourceAsStream(resourceConfig);
        configure(is);
    }

    /**
     * Set the path to a properties file containing connection settings.
     * @param file The properties file
     */
    public void setConnectionProperties(File file) {
        logger.config("setting connection properties file " + file.getPath());
        if (!file.exists())
            throw new IllegalArgumentException("The connection properties file does not exist: " + connectionPropertiesFile.getPath());
        connectionParameters = ConnectionParameters.fromProperties(file);
    }
    
    /**
     * Set the connection parameters of the conditions database.
     * @param connectionParameters The connection parameters.
     */
    public void setConnectionParameters(ConnectionParameters connectionParameters) {
        this.connectionParameters = connectionParameters;
    }

    /**
     * Set the connection parameters from an embedded resource location.
     * @param resource The classpath resource location.
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
    public int getNextCollectionID(String tableName) {
        boolean openedConnection = openConnection();
        TableMetaData tableData = tableRegistry.findByTableName(tableName);
        if (tableData == null)
            throw new IllegalArgumentException("There is no meta data for table " + tableName);
        ResultSet resultSet = selectQuery("SELECT MAX(collection_id)+1 FROM " + tableName);
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
     * This method will return true if the given collection ID already exists in
     * the table.
     * @param tableName The name of the table.
     * @param collectionID The collection ID value.
     * @return True if collection exists.
     */
    public boolean collectionExists(String tableName, int collectionID) {
        String sql = "SELECT * FROM " + tableName + " where collection_id = " + collectionID;
        ResultSet resultSet = selectQuery(sql);
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
        if (rowCount != 0) {
            return true;
        } else {
            return false;
        }
    }
   
    /**
     * This method can be used to perform a database SELECT query.
     * @param query The SQL query string.
     * @return The ResultSet from the query or null.
     */
    ResultSet selectQuery(String query) {
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
        boolean openedConnection = openConnection();
        logger.fine(query);
        List<Integer> keys = new ArrayList<Integer>();
        Statement statement = null;
        ResultSet resultSet = null;
        try {
            statement = connection.createStatement();
            statement.executeUpdate(query, Statement.RETURN_GENERATED_KEYS);
            resultSet = statement.getGeneratedKeys();
            while (resultSet.next()) {
                int key = resultSet.getInt(1);
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
     * Set the log level.
     * @param level The log level.
     */
    public void setLogLevel(Level level) {
        logger.config("setting log level to " + level);
        logger.setLevel(level);
        logger.getHandlers()[0].setLevel(level);
        
        ecalSetup.setLogLevel(level);
        svtSetup.setLogLevel(level);
    }

    /**
     * Find a collection of conditions validity records by key name. The key
     * name is distinct from the table name, but they are usually set to the
     * same value in the XML configuration.
     * @param name The conditions key name.
     * @return The set of matching conditions records.
     */
    public ConditionsRecordCollection findConditionsRecords(String name) {
        ConditionsRecordCollection runConditionsRecords = getCollection(ConditionsRecordCollection.class);
        logger.fine("searching for condition " + name + " in " + runConditionsRecords.size() + " records");
        ConditionsRecordCollection foundConditionsRecords = new ConditionsRecordCollection();
        for (ConditionsRecord record : runConditionsRecords) {
            if (record.getName().equals(name)) {
                if (tag == null || (tag != null && record.getTag().equals(tag))) {
                    foundConditionsRecords.add(record);
                } else {
                    logger.fine("rejected ConditionsRecord " + record.getRowId() + " because of non-matching tag " + record.getTag());
                }
            }
        }
        if (foundConditionsRecords.size() > 0) {
            for (ConditionsRecord record : foundConditionsRecords) {
                logger.fine("found ConditionsRecord with key " + name + '\n' + record.toString());
            }
        }
        return foundConditionsRecords;
    }

    /**
     * Get a list of all the ConditionsRecord objects.
     * @return The list of all the ConditionsRecord objects.
     */
    // FIXME: This should use a cache created in initialize rather than do a find every time.
    public ConditionsRecordCollection getConditionsRecords() {
        ConditionsRecordCollection conditionsRecords = new ConditionsRecordCollection();
        for (TableMetaData tableMetaData : tableRegistry.values()) {
            try {
                ConditionsRecordCollection foundConditionsRecords = findConditionsRecords(tableMetaData.getKey());
                conditionsRecords.addAll(foundConditionsRecords); 
            } catch (Exception e) {
                logger.warning(e.getMessage());
            }
        }        
        return conditionsRecords;
    }
    
    /**
     * This method can be called to "freeze" the conditions system so that
     * any subsequent updates to run number or detector name will be ignored.
     */
    public void freeze() {
        if (getDetector() != null && getRun() != -1) {
            isFrozen = true;
            logger.config("The conditions system has been frozen and will ignore subsequent updates.");
        } else {
            logger.warning("The conditions system cannot be frozen now because it is not initialized yet.");
        }
    }
    
    /**
     * Un-freeze the conditions system so that updates will be received again.
     */
    public void unfreeze() {
        isFrozen = false;
    }
    
    /**
     * True if conditions system is frozen.
     * @return True if conditions system is frozen.
     */
    public boolean isFrozen() {
        return isFrozen;
    }
    
    /**
     * Set the name of the ECAL sub-detector.
     * @param ecalName The name of the ECAL.
     */
    protected void setEcalName(String ecalName) {
        if (ecalName == null) {
            throw new IllegalArgumentException("The ecalName is null");
        }
        this.ecalName = ecalName;
        ecalSetup.setEcalName(ecalName);
    }
    
    /**
     * Set the name of the SVT sub-detector.
     * @param svtName The name of the SVT.
     */
    protected void setSvtName(String svtName) {
        if (svtName == null) {
            throw new IllegalArgumentException("The svtName is null");
        }
        this.svtName = svtName;
    }
    
    /**
     * Set a tag used to filter ConditionsRecords.
     * @param tag The tag value used to filter ConditionsRecords.
     */
    public void setTag(String tag) {
        this.tag = tag;
    }

    /**
     * Insert a collection of ConditionsObjects into the database.
     * @param collection The collection to insert.
     * @throws SQLException If there is a database error.
     * @throws ConditionsObjectException If there is a problem with the ConditionsObjects.
     */
    public <ObjectType extends ConditionsObject> void insertCollection(ConditionsObjectCollection<ObjectType> collection) throws SQLException, ConditionsObjectException {
                
        if (collection == null) {
            throw new IllegalArgumentException("The collection is null.");
        }
        if (collection.size() == 0) {
            throw new IllegalArgumentException("The collection is empty.");
        }

        TableMetaData tableMetaData = collection.getTableMetaData();
        if (tableMetaData == null) {            
            tableMetaData = tableRegistry.findByCollectionType(collection.getClass()); 
            logger.fine("using default table meta data with table " + tableMetaData.getTableName() + " for collection of type " + collection.getClass().getCanonicalName());            
        }
        if (collection.getCollectionId() == -1) {
            try {
                collection.setCollectionId(getNextCollectionID(tableMetaData.getTableName()));
            } catch (ConditionsObjectException e) {
                throw new RuntimeException(e);
            }
        }
        logger.info("inserting collection with ID " + collection.getCollectionId() 
                + " and key " + tableMetaData.getKey() + " into table " + tableMetaData.getTableName());

        boolean openedConnection = false;
        if (!isConnected()) {
            openConnection();
            openedConnection = true;
        }
        
        PreparedStatement preparedStatement = null;
        
        try {
            connection.setAutoCommit(false);
            logger.finest("starting insert transaction");
            String sql = QueryBuilder.buildPreparedInsert(tableMetaData.getTableName(), collection.iterator().next());
            preparedStatement = connection.prepareStatement(sql);
            logger.finest("using prepared statement: " + sql);
            logger.finest("preparing updates ...");
            int collectionId = collection.getCollectionId();
            for (ConditionsObject object : collection) {
                preparedStatement.setObject(1, collectionId);
                int parameterIndex = 2;
                for (Entry<String,Object> entry : object.getFieldValues().entrySet()) {
                    preparedStatement.setObject(parameterIndex, entry.getValue());
                    ++parameterIndex;
                }
                preparedStatement.executeUpdate();
            }
            logger.finest("done preparing updates");
            connection.commit();
            logger.finest("committed transaction");
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
     * @return true if connected
     */
    public boolean isConnected() {
        return isConnected;
    }
    
    /**
     * Get the default detector name for the Test Run.
     * @return The default detector name for the Test Run.
     */
    public static String getDefaultTestRunDetectorName() {
        return DEFAULT_TEST_RUN_DETECTOR;
    }
    
    /**
     * Get the default detector name for the Engineering Run.
     * @return The default detector name for the Engineering Run.
     */
    public static String getDefaultEngRunDetectorName() {
        return DEFAULT_ENG_RUN_DETECTOR;
    }
        
    /**
     * Get the Logger for this class, which can be used by related sub-classes
     * if they do not have their own logger.
     * @return The Logger for this class.
     */
    public Logger getLogger() {
        return logger;
    }
    
    /**
     * Find table information from the name.
     * @param name The name of the table.
     * @return The table information or null if does not exist.
     */
    public TableMetaData findTableMetaData(String name) {
        return tableRegistry.findByTableName(name);
    }
    
    /**
     * Find table information from the collection type.
     * @param type The collection type.
     * @return The table information or null if does not exist.
     */
    public TableMetaData findTableMetaData(Class<?> type) {
        return tableRegistry.findByCollectionType(type);
    }

    /*
     *******************************
     * Private methods below here. *
     *******************************
     */
                          
    /**
     * Cache conditions sets for all known tables.
     */
    private void cacheConditionsSets() {
        for (TableMetaData meta : tableRegistry.values()) {
            try {
                logger.fine("caching conditions " + meta.key + " with type "+ meta.collectionClass.getCanonicalName());
                getCachedConditions(meta.collectionClass, meta.key);
            } catch (Exception e) {
                logger.warning("could not cache conditions " + meta.key);
            }
        }
     }
        
    /**
     * Choose whether to use the JLAB or SLAC external database
     * and return a connection properties resource with the appropriate
     * connection information.
     * @return The connection properties resource.
     */
    private String chooseConnectionPropertiesResource() {
        String connectionName = "slac";
        try {
            // Is the JLAB database reachable?
            if (InetAddress.getByName("jmysql.jlab.org").isReachable(5000)) {
                connectionName = "jlab";
            } 
        } catch (UnknownHostException e) {
            // This will actually print a warning if the JLAB server is unreachable.
            //logger.log(Level.WARNING, e.getMessage(), e);
        } catch (IOException e) {
            logger.severe(e.getMessage());
            throw new RuntimeException(e);            
        }
        logger.config("connection " + connectionName + " will be used");
        return "/org/hps/conditions/config/" + connectionName + "_connection.prop";
    }

    /**
     * Configure this class from an <code>InputStream</code> which should point to an XML document.
     * @param in the InputStream which should be in XML format
     */
    private void configure(InputStream in) {
        if (!isConfigured) {
            SAXBuilder builder = new SAXBuilder();
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
     * Setup the database connection from a file specified by a Java system
     * property setting. This is possibly overridden by subsequent, direct API calls to
     * {@link #setConnectionProperties(File)} or {@link #setConnectionResource(String)}.
     */
    private void setupConnectionFromSystemProperty() {
        String systemPropertiesConnectionPath = (String) System.getProperties().get(CONNECTION_PROPERTY);
        if (systemPropertiesConnectionPath != null) {
            File f = new File(systemPropertiesConnectionPath);
            if (!f.exists()) {
                throw new RuntimeException("Connection properties file from " + CONNECTION_PROPERTY + " does not exist.");
            }
            setConnectionProperties(f);
        }
    }

    /**
     * Load configuration information from an XML document.
     * @param document The XML document.
     */
    // TODO: Add detectorName and runNumber settings.
    private void loadConfiguration(Document document) {
        
        Element node = document.getRootElement().getChild("configuration");
        
        if (node == null)
            return;
        
        Element element = node.getChild("setupSvtDetector");
        if (element != null) {
            setupSvtDetector = Boolean.parseBoolean(element.getText());
            logger.config("setupSvtDetector = " + setupSvtDetector);
        }
        
        element = node.getChild("setupEcalDetector");
        if (element != null) {
            setupEcalDetector = Boolean.parseBoolean(element.getText());
            logger.config("setupEcalDetector = " + setupEcalDetector);
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
        }
        
        element = node.getChild("loginTimeout");
        if (element != null) {
            DriverManager.setLoginTimeout(Integer.parseInt(element.getText()));
        }
    }
}