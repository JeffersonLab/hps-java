package org.hps.conditions.database;

import static org.hps.conditions.database.TableConstants.ECAL_CONDITIONS;
import static org.hps.conditions.database.TableConstants.SVT_CONDITIONS;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hps.conditions.api.AbstractConditionsObjectCollection;
import org.hps.conditions.api.ConditionsObject;
import org.hps.conditions.api.ConditionsObjectException;
import org.hps.conditions.api.ConditionsRecord;
import org.hps.conditions.api.ConditionsRecord.ConditionsRecordCollection;
import org.hps.conditions.api.ConditionsSeries;
import org.hps.conditions.ecal.EcalConditions;
import org.hps.conditions.ecal.EcalDetectorSetup;
import org.hps.conditions.svt.SvtConditions;
import org.hps.conditions.svt.SvtDetectorSetup;
import org.hps.conditions.svt.TestRunSvtConditions;
import org.hps.conditions.svt.TestRunSvtDetectorSetup;
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
 * This class should be used as the top-level ConditionsManager for database
 * access to conditions data.
 * </p>
 * <p>
 * In general, this will be overriding the
 * <code>LCSimConditionsManagerImplementation</code> which is setup from within
 * <code>LCSimLoop</code>.
 * </p>
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
@SuppressWarnings("rawtypes")
public class DatabaseConditionsManager extends ConditionsManagerImplementation {

    protected static final String CONNECTION_PROPERTY = "org.hps.conditions.connection.file";
    protected static final String DEFAULT_CONFIG = "/org/hps/conditions/config/conditions_dev.xml";
    protected static final String TEST_RUN_CONFIG = "/org/hps/conditions/config/conditions_database_testrun_2012.xml";
    protected static final int TEST_RUN_MAX_RUN = 1365;
    
    // The default Test Run detector.
    private static final String DEFAULT_TEST_RUN_DETECTOR = "HPS-TestRun-v8-5";

    // The default Engineering Run detector.
    private static final String DEFAULT_ENG_RUN_DETECTOR = "HPS-Proposal2014-v8-6pt6";

    protected static Logger logger = LogUtil.create(DatabaseConditionsManager.class);
    
    protected String detectorName;
    protected List<TableMetaData> tableMetaData;
    protected List<ConditionsConverter> converters;
    protected File connectionPropertiesFile;
    protected ConnectionParameters connectionParameters;
    //= new DefaultConnectionParameters();
    protected Connection connection;
    protected boolean isConnected = false;
    protected ConditionsSeriesConverter conditionsSeriesConverter = new ConditionsSeriesConverter(this);
    protected boolean isInitialized = false;
    protected String resourceConfig = null;
    protected File fileConfig = null;
    protected String ecalName = "Ecal";
    protected String svtName = "Tracker";
    protected boolean isFrozen = false;
    protected EcalDetectorSetup ecalLoader = new EcalDetectorSetup();
    protected TestRunSvtDetectorSetup testRunSvtloader = new TestRunSvtDetectorSetup();
    protected SvtDetectorSetup svtLoader = new SvtDetectorSetup();
    protected String tag = null;
        
    /**
     * Class constructor.
     */
    public DatabaseConditionsManager() {
        logger.setLevel(Level.FINER);
        registerConditionsConverter(new DetectorConditionsConverter());
        setupConnectionFromSystemProperty();
        register();
        this.setRun(-1);
    }

    /**
     * Register this conditions manager as the global default.
     */
    public void register() {
        ConditionsManager.setDefaultConditionsManager(this);
    }

    /**
     * Get the static instance of this class, which must have been registered
     * first from a call to {@link #register()}.
     * @return The static instance of the manager.
     */
    public static DatabaseConditionsManager getInstance() {

        // Perform default setup if necessary.
        if (!ConditionsManager.isSetup()) {
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
     */
    public void openConnection() {
        // Do the connection parameters need to be figured out automatically?
        if (connectionParameters == null) {
            // Setup the default read-only connection, which will choose a SLAC or JLab database.
            connectionParameters = ConnectionParameters.fromResource(chooseConnectionPropertiesResource());
        }
        logger.config("opening connection to " + connectionParameters.getConnectionString());
        logger.config("host " + connectionParameters.getHostname());
        logger.config("port " + connectionParameters.getPort());
        logger.config("user " + connectionParameters.getUser());
        logger.config("database " + connectionParameters.getDatabase());
        connection = connectionParameters.createConnection();
        logger.config("successfuly created connection");
        isConnected = true;
    }

    /**
     * Close the database connection.
     */
    public void closeConnection() {
        logger.info("closing connection");
        if (connection != null) {
            try {
                if (!connection.isClosed()) {
                    connection.close();
                } else {
                    logger.info("connection was already closed");
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
        connection = null;
        isConnected = false;
        logger.info("connection closed");
    }

    @Override
    public void finalize() {
        if (isConnected()) {
            closeConnection();
        }
    }

    /**
     * Get multiple <code>ConditionsObjectCollection</code> objects that may
     * have overlapping time validity information.
     * @param conditionsKey The conditions key.
     * @return The <code>ConditionsSeries</code> containing the matching
     *         <code>ConditionsObjectCollection</code>.
     */
    public ConditionsSeries getConditionsSeries(String conditionsKey) {
        return conditionsSeriesConverter.createSeries(conditionsKey);
    }

    /**
     * Get a given collection of the given type from the conditions database.
     * @param type Class type
     * @return A collection of objects of the given type from the conditions
     *         database
     */
    // TODO: This should distinguish among multiple conditions sets of the same type by using the one with the most recent date
    //       in its ConditionsRecord.
    public <CollectionType extends AbstractConditionsObjectCollection> CollectionType getCollection(Class<CollectionType> type) {
        TableMetaData metaData = this.findTableMetaData(type).get(0);
        if (metaData == null) {
            throw new RuntimeException("Table name data for condition of type " + type.getSimpleName() + " was not found.");
        }
        String tableName = metaData.getTableName();
        CollectionType conditionsCollection = this.getCachedConditions(type, tableName).getCachedData();
        return conditionsCollection;
    }

    /**
     * This method catches changes to the detector name and run number. It is
     * actually called every time an lcsim event is created, so it has internal
     * logic to figure out if the conditions system actually needs to be
     * updated.
     */
    @Override
    public void setDetector(String detectorName, int runNumber) throws ConditionsNotFoundException {

        if (detectorName == null) {
            throw new IllegalArgumentException("The detectorName argument is null.");
        }
        
        logger.finest("setDetector detector " + detectorName + " and run #" + runNumber);
        
        if (!isInitialized || !detectorName.equals(this.getDetector()) || runNumber != this.getRun()) {
            if (!isInitialized) {
                logger.fine("first time initialization");
            }
            if (!this.isFrozen) {
                if (!detectorName.equals(this.getDetector())) {
                    logger.finest("detector name is different");
                }
                if (runNumber != this.getRun()) {
                    logger.finest("run number is different");
                }            
                logger.info("setDetector with new detector " + detectorName + " and run #" + runNumber);
                logger.fine("old detector " + this.getDetector() + " and run #" + this.getRun());
                initialize(detectorName, runNumber);
            } else {
                logger.finest("Conditions changed but will be ignored because manager is frozen.");
            }
        }
    }
    
    public static boolean isTestRun(int runNumber) {
        return runNumber > 0 && runNumber <= TEST_RUN_MAX_RUN;
    }
    
    public boolean isTestRun() {
        return isTestRun(this.getRun());
    }
    
    /**
     * Perform all necessary initialization, including setup of the XML
     * configuration and opening a connection to the database.
     */
    protected void initialize(String detectorName, int runNumber) throws ConditionsNotFoundException {

        logger.config("initializing " + getClass().getSimpleName() + " with detector " + detectorName + " and run number " + runNumber);

        // Did the user not specify a config?
        if (resourceConfig == null && fileConfig == null) {
            // We will try to pick a reasonable config based on the run number...
            if (runNumber > 0 && runNumber <= TEST_RUN_MAX_RUN) {
                // This looks like the Test Run so use the custom config for it.
                this.resourceConfig = DatabaseConditionsManager.TEST_RUN_CONFIG;
                logger.config("using test run XML config " + this.resourceConfig);
            } else { 
                // This is probably the Engineering Run or later so use the default config.
                this.resourceConfig = DatabaseConditionsManager.DEFAULT_CONFIG;
                logger.config("using default XML config " + this.resourceConfig);
            }
        }
        
        if (resourceConfig != null && fileConfig != null) {
            throw new RuntimeException("Both resource and file configuration are set.");
        }
                
        if (this.resourceConfig != null) {
            this.configure(getClass().getResourceAsStream(this.resourceConfig));
        } else if (this.fileConfig != null) {
            try {
                this.configure(new FileInputStream(this.fileConfig));
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
        
        // Open the database connection.
        if (!isConnected()) {
            openConnection();
        } else {
            logger.config("using existing connection " + connectionParameters.getConnectionString());
        }
        
        // Call the super class's setDetector method to construct the detector object.
        super.setDetector(detectorName, runNumber);
        
        // Load conditions onto the ECAL subdetector object. 
        try {
            setupEcal();
        } catch (Exception e) {
            e.printStackTrace();
            logger.log(Level.WARNING, "Error loading ECAL conditions onto detector.", e);
        }
        
        // Load conditions onto the SVT subdetector object.
        try {
            setupSvt(runNumber);
        } catch (Exception e) {
            e.printStackTrace();
            logger.log(Level.WARNING, "Error loading SVT conditions onto detector.", e);
        }                       
                       
        this.isInitialized = true;

        logger.config(getClass().getSimpleName() + " is initialized");
    }
    
    /**
     * Get the current lcsim compact <code>Detector</code> object.
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
            throw new IllegalArgumentException("Config file does not exist.");
        }
        this.fileConfig = fileConfig;        
    }

    /**
     * Configure this object from an embedded XML resource.
     * @param resource The embedded XML resource.
     */
    public void setXmlConfig(String resourceConfig) {
        logger.config("setting XML config from resource " + resourceConfig);
        this.resourceConfig = resourceConfig;
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
    
    public void setConnectionParameters(ConnectionParameters connectionParameters) {
        this.connectionParameters = connectionParameters;
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
    // TODO: If there are no records in the table, this method should simply return 1.  (for first collection)
    public int getNextCollectionID(String tableName) {
        TableMetaData tableData = findTableMetaData(tableName);
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
     * Get the list of table meta data.
     * @return The list of table meta data.
     */
    public List<TableMetaData> getTableMetaDataList() {
        return tableMetaData;
    }

    /**
     * Find a table's meta data by key.
     * @param name The name of the table.
     * @return The table's meta data or null if does not exist.
     */
    public TableMetaData findTableMetaData(String name) {
        for (TableMetaData meta : tableMetaData) {
            if (meta.getKey().equals(name)) {
                return meta;
            }
        }
        return null;
    }

    /**
     * Find meta data by collection class type.
     * @param type The collection class.
     * @return The table meta data.
     */
    public List<TableMetaData> findTableMetaData(Class type) {
        List<TableMetaData> metaDataList = new ArrayList<TableMetaData>();
        for (TableMetaData meta : tableMetaData) {
            if (meta.getCollectionClass().equals(type)) {
                metaDataList.add(meta);
            }
        }
        return metaDataList;
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
            DatabaseUtilities.close(statement);
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
        
        this.ecalLoader.setLogLevel(level);
        this.svtLoader.setLogLevel(level);
        this.testRunSvtloader.setLogLevel(level);
    }

    /**
     * Find a collection of conditions validity records by key name. The key
     * name is distinct from the table name, but they are usually set to the
     * same value in the XML configuration.
     * @param name The conditions key name.
     * @return The set of matching conditions records.
     */
    public ConditionsRecordCollection findConditionsRecords(String name) {
        ConditionsRecordCollection runConditionsRecords = getConditionsData(ConditionsRecordCollection.class, TableConstants.CONDITIONS_RECORD);
        logger.fine("searching for condition " + name + " in " + runConditionsRecords.size() + " records");
        ConditionsRecordCollection foundConditionsRecords = new ConditionsRecordCollection();
        for (ConditionsRecord record : runConditionsRecords) {
            if (record.getName().equals(name)) {
                if (tag == null || (tag != null && record.getTag().equals(tag))) {
                    foundConditionsRecords.add(record);
                } else {
                    logger.info("rejected ConditionsRecord " + record.getRowId() + " because of non-matching tag " + record.getTag());
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
    
    public ConditionsRecordCollection getConditionsRecords() {
        ConditionsRecordCollection conditionsRecords = new ConditionsRecordCollection();
        for (TableMetaData tableMetaData : this.getTableMetaDataList()) {
            ConditionsRecordCollection foundConditionsRecords = findConditionsRecords(tableMetaData.getKey());
            conditionsRecords.addAll(foundConditionsRecords); 
        }        
        return conditionsRecords;
    }
    
    public void freeze() {
        if (this.getDetector() != null && this.getRun() != -1) {
            this.isFrozen = true;
            logger.config("The conditions manager has been frozen and will ignore subsequent updates until unfrozen.");
        } else {
            logger.warning("The conditions manager cannot be frozen now because detector or run number are not valid.");
        }
    }
    
    public void unfreeze() {
        this.isFrozen = false;
    }
    
    public boolean isFrozen() {
        return this.isFrozen;
    }
    
    public void setEcalName(String ecalName) {
        this.ecalName = ecalName;
    }
    
    public void setSvtName(String svtName) {
        this.svtName = svtName;
    }
    
    public void setTag(String tag) {
        this.tag = tag;
    }

    public <ObjectType extends ConditionsObject> void insertCollection(AbstractConditionsObjectCollection<ObjectType> collection) throws SQLException, ConditionsObjectException {
        if (collection == null) {
            throw new IllegalArgumentException("The collection is null.");
        }
        if (collection.size() == 0) {
            throw new IllegalArgumentException("The collection is empty.");
        }

        TableMetaData tableMetaData = collection.getTableMetaData();
        if (tableMetaData == null) {            
            List<TableMetaData> tableMetaDataList = this.findTableMetaData(collection.getClass()); 
            if (tableMetaDataList.size() == 0) {
                throw new ConditionsObjectException("The conditions object collection is missing table meta data and none could be found by the conditions manager.");
            } else {
                // Use a default meta data object from the manager.
                tableMetaData = tableMetaDataList.get(0);
                collection.setTableMetaData(tableMetaData);
                logger.fine("using default table meta data with table " + tableMetaData.getTableName() + " for collection of type " + collection.getClass().getCanonicalName());
            }
        }
        if (collection.getCollectionId() == -1) {
            try {
                collection.setCollectionId(this.getNextCollectionID(tableMetaData.getTableName()));
            } catch (ConditionsObjectException e) {
                throw new RuntimeException(e);
            }
        }
        logger.info("inserting collection with ID " + collection.getCollectionId() 
                + " and key " + collection.getTableMetaData().getKey() + " into table " + tableMetaData.getTableName());

        try {
            connection.setAutoCommit(false);
            logger.finest("starting insert transaction");
            String sql = QueryBuilder.buildPreparedInsert(tableMetaData.getTableName(), collection.iterator().next());
            PreparedStatement preparedStatement = 
                connection.prepareStatement(sql);
            logger.finest("using prepared statement: " + sql);
            logger.finest("preparing updates");
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
    }
                      
    private void setupEcal() {
        logger.config("setting up ECAL conditions on detector");
        EcalConditions conditions = getCachedConditions(EcalConditions.class, ECAL_CONDITIONS).getCachedData();
        ecalLoader.load(this.getDetectorObject().getSubdetector(ecalName), conditions);
        logger.fine("done setting up ECAL conditions on detector");
    }
    
    private void setupSvt(int runNumber) {
        if (isTestRun(runNumber)) {
            logger.config("loading Test Run SVT detector conditions");
            TestRunSvtConditions svtConditions = getCachedConditions(TestRunSvtConditions.class, SVT_CONDITIONS).getCachedData();            
            testRunSvtloader.load(getDetectorObject().getSubdetector(svtName), svtConditions);
        } else {
            logger.config("loading default SVT detector conditions");
            SvtConditions svtConditions = getCachedConditions(SvtConditions.class, SVT_CONDITIONS).getCachedData();
            svtLoader.load(getDetectorObject().getSubdetector(svtName), svtConditions);
        }
        logger.config("done loading SVT detector conditions");
    }
    
    /**
     * Check if connected to the database.
     * @return true if connected
     */
    public boolean isConnected() {
        return isConnected;
    }
    
    public static String getDefaultTestRunDetectorName() {
        return DEFAULT_TEST_RUN_DETECTOR;
    }
    
    public static String getDefaultEngRunDetectorName() {
        return DEFAULT_ENG_RUN_DETECTOR;
    }
    
    public void addTableMetaData(TableMetaData tableMetaData) {
        this.tableMetaData.add(tableMetaData);
    }
    
    public Logger getLogger() {
        return logger;
    }
        
    private String chooseConnectionPropertiesResource() {
        String connectionName = "slac";
        try {
            // Is the JLAB database reachable?
            if (InetAddress.getByName("jmysql.jlab.org").isReachable(5000)) {
                logger.config("jmysql.jlab.org is reachable");
                connectionName = "jlab";
            } 
        } catch (UnknownHostException e) {
            // Something wrong with the user's host name, but we will try to continue anyways.
            logger.log(Level.WARNING, e.getMessage(), e);
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

        // Create XML document from stream.
        Document config = createDocument(in);

        // Load the table meta data.
        loadTableMetaData(config);

        // Load the converter classes.
        loadConverters(config);
    }

    /**
     * Create an XML document from an <code>InputStream</code>.
     * @param in The InputStream.
     * @return The XML document.
     */
    private static Document createDocument(InputStream in) {
        // Create an XML document from an InputStream.
        SAXBuilder builder = new SAXBuilder();
        Document config = null;
        try {
            config = builder.build(in);
        } catch (JDOMException | IOException e) {
            throw new RuntimeException(e);
        }
        return config;
    }

    /**
     * Load data converters from an XML document.
     * @param config The XML document.
     */
    private void loadConverters(Document config) {

        if (this.converters != null) {
            this.converters.clear();
        }

        // Load the list of converters from the "converters" section of the config document.
        loadConditionsConverters(config.getRootElement().getChild("converters"));

        // Register the list of converters with this manager.
        for (ConditionsConverter converter : converters) {
            registerConditionsConverter(converter);
            logger.config("registered converter " + converter.getClass().getSimpleName());
        }
    }

    /**
     * Load table meta data configuration from an XML document.
     * @param config The XML document.
     */
    private void loadTableMetaData(Document config) {

        if (this.tableMetaData != null) {
            this.tableMetaData.clear();
        }

        // Load table meta data from the "tables" section of the config document.
        loadTableMetaData(config.getRootElement().getChild("tables"));
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
            this.setConnectionProperties(f);
        }
    }

    /**
     * Load table meta data from the XML list.
     * @param element The XML node containing a list of table elements.
     */
    @SuppressWarnings("unchecked")
    void loadTableMetaData(Element element) {

        tableMetaData = new ArrayList<TableMetaData>();

        for (Iterator<?> iterator = element.getChildren("table").iterator(); iterator.hasNext();) {
            Element tableElement = (Element) iterator.next();
            String tableName = tableElement.getAttributeValue("name");
            String key = tableElement.getAttributeValue("key");

            Element classesElement = tableElement.getChild("classes");
            Element classElement = classesElement.getChild("object");
            Element collectionElement = classesElement.getChild("collection");

            String className = classElement.getAttributeValue("class");
            String collectionName = collectionElement.getAttributeValue("class");

            Class<? extends ConditionsObject> objectClass;
            Class<?> rawObjectClass;
            try {
                rawObjectClass = Class.forName(className);
                if (!ConditionsObject.class.isAssignableFrom(rawObjectClass)) {
                    throw new RuntimeException("The class " + rawObjectClass.getSimpleName() + " does not extend ConditionsObject.");
                }
                objectClass = (Class<? extends ConditionsObject>) rawObjectClass;
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }

            Class<? extends AbstractConditionsObjectCollection<?>> collectionClass;
            Class<?> rawCollectionClass;
            try {
                rawCollectionClass = Class.forName(collectionName);
                if (!AbstractConditionsObjectCollection.class.isAssignableFrom(rawCollectionClass))
                    throw new RuntimeException("The class " + rawCollectionClass.getSimpleName() + " does not extend ConditionsObjectCollection.");
                collectionClass = (Class<? extends AbstractConditionsObjectCollection<?>>) rawCollectionClass;
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }

            TableMetaData tableData = new TableMetaData(key, tableName, objectClass, collectionClass);
            Element fieldsElement = tableElement.getChild("fields");
            for (Iterator<?> fieldsIterator = fieldsElement.getChildren("field").iterator(); fieldsIterator.hasNext();) {
                Element fieldElement = (Element) fieldsIterator.next();
                String fieldName = fieldElement.getAttributeValue("name");
                tableData.addField(fieldName);
            }

            tableMetaData.add(tableData);
        }
    }

    /**
     * Load conditions converters from the XML list.
     * @param element The node with a list of child converter elements.
     */
    private void loadConditionsConverters(Element element) {
        converters = new ArrayList<ConditionsConverter>();
        for (Iterator iterator = element.getChildren("converter").iterator(); iterator.hasNext();) {
            Element converterElement = (Element) iterator.next();
            try {
                Class converterClass = Class.forName(converterElement.getAttributeValue("class"));
                if (ConditionsConverter.class.isAssignableFrom(converterClass)) {
                    try {
                        converters.add((ConditionsConverter) converterClass.newInstance());
                    } catch (InstantiationException | IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    throw new RuntimeException("The converter class " + converterClass.getSimpleName() + " does not extend the correct base type.");
                }
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
