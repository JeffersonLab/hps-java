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
     * Name of system property that can be used to specify custom database connection parameters.
     */
    private static final String CONNECTION_PROPERTY = "org.hps.conditions.connection.file";

    /**
     * The default XML config.
     */
    private static final String DEFAULT_CONFIG = "/org/hps/conditions/config/conditions_database_prod.xml";

    /**
     * The connection properties resource for connecting to the default JLAB database.
     */
    private static final String DEFAULT_CONNECTION_PROPERTIES_RESOURCE = "/org/hps/conditions/config/jlab_connection.prop";

    /**
     * The Eng Run XML config.
     */
    private static final String ENGRUN_CONFIG = "/org/hps/conditions/config/conditions_database_engrun.xml";

    /**
     * Initialize the logger.
     */
    private static Logger logger = LogUtil.create(DatabaseConditionsManager.class.getName(), new DefaultLogFormatter(),
            Level.FINE);

    /**
     * The Test Run XML config.
     */
    private static final String TEST_RUN_CONFIG = "/org/hps/conditions/config/conditions_database_testrun_2012.xml";

    /**
     * The max value for a run to be considered Test Run.
     */
    private static final int TEST_RUN_MAX_RUN = 1365;

    static {
        // Set default login timeout of 5 seconds.
        DriverManager.setLoginTimeout(5);
    }

    /**
     * Get the static instance of this class.
     *
     * @return the static instance of the manager
     */
    public static synchronized DatabaseConditionsManager getInstance() {

        logger.finest("getting conditions manager instance");

        // Is there no manager installed yet?
        if (!ConditionsManager.isSetup() || !(ConditionsManager.defaultInstance() instanceof DatabaseConditionsManager)) {
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
     * Get the Logger for this class, which can be used by related sub-classes if they do not have their own logger.
     *
     * @return the Logger for this class
     */
    public static Logger getLogger() {
        return logger;
    }

    /**
     * Utility method to determine if a run number is from the 2012 Test Run.
     *
     * @param runNumber the run number
     * @return <code>true</code> if run number is from the Test Run
     */
    public static boolean isTestRun(final int runNumber) {
        return runNumber > 0 && runNumber <= TEST_RUN_MAX_RUN;
    }

    /**
     * Reset the global static instance of the conditions manager to a new object.
     */
    public static synchronized void resetInstance() {
        logger.finest("DatabaseConditionsManager instance is being reset");
        new DatabaseConditionsManager();
    }

    /**
     * True to cache all known conditions sets (from keys) during initialization.
     */
    private boolean cacheAllConditions = false;

    /**
     * True to close the connection after initialization.
     */
    private boolean closeConnectionAfterInitialize = true;

    /**
     * The current database connection.
     */
    private Connection connection;

    /**
     * The current connection parameters.
     */
    private ConnectionParameters connectionParameters;

    /**
     * The connection properties file, if one is being used from the command line.
     */
    private File connectionPropertiesFile;

    /**
     * Create the global registry of conditions object converters.
     */
    private final ConverterRegistry converters = ConverterRegistry.create();

    /**
     * The converter for creating the combined ECAL conditions object.
     */
    private ConditionsConverter ecalConverter;

    /**
     * The default ECAL detector name in the detector geometry.
     */
    private String ecalName = "Ecal";

    /**
     * True to freeze the system after initialization.
     */
    private boolean freezeAfterInitialize = false;

    /**
     * True if the conditions manager was configured from an XML configuration resource or file.
     */
    private boolean isConfigured = false;

    /**
     * True if manager is connected to the database.
     */
    private boolean isConnected = false;

    /**
     * True if the conditions system has been frozen and will ignore updates after it is initialized.
     */
    private boolean isFrozen = false;

    /**
     * True if the manager has been initialized, e.g. the {@link #setDetector(String, int)} method was called.
     */
    private boolean isInitialized = false;

    /**
     * True if current run number is from Test Run.
     */
    private boolean isTestRun = false;

    /**
     * Flag used to print connection parameters one time.
     */
    private boolean loggedConnectionParameters = false;

    /**
     * True to setup the SVT detector model with conditions.
     */
    private boolean setupSvtDetector = true;

    /**
     * The converter for creating the combined SVT conditions object.
     */
    private ConditionsConverter svtConverter;

    /**
     * The default SVT name in the detector geometry.
     */
    private String svtName = "Tracker";

    /**
     * The helper for setting up the SVT detector with its conditions information.
     */
    private final SvtDetectorSetup svtSetup = new SvtDetectorSetup(this.svtName);

    /**
     * Create the global registry of table meta data.
     */
    private final TableRegistry tableRegistry = TableRegistry.create();

    /**
     * The currently active conditions tag.
     */
    private String tag = null;

    /**
     * Class constructor. Calling this will automatically register this manager as the global default.
     */
    private DatabaseConditionsManager() {
        this.registerConditionsConverter(new DetectorConditionsConverter());
        this.setupConnectionFromSystemProperty();
        ConditionsManager.setDefaultConditionsManager(this);
        this.setRun(-1);
        for (final AbstractConditionsObjectConverter converter : this.converters.values()) {
            // logger.fine("registering converter for " + converter.getType());
            this.registerConditionsConverter(converter);
        }
        this.addConditionsListener(this.svtSetup);
    }

    /**
     * Add a row for a new collection and return the new collection ID assigned to it.
     *
     * @param tableName the name of the table
     * @param comment an optional comment about this new collection
     * @return the collection's ID
     * @throws SQLException
     */
    public synchronized int addCollection(final String tableName, final String log, final String description)
            throws SQLException {
        if (tableName == null) {
            throw new IllegalArgumentException("The tableName argument is null.");
        }
        final boolean opened = this.openConnection();
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        int collectionId = -1;
        try {
            statement = this.connection.prepareStatement(
                    "INSERT INTO collections (table_name, log, description, created) VALUES (?, ?, ?, NOW())",
                    Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, tableName);
            if (log == null) {
                statement.setNull(2, java.sql.Types.VARCHAR);
            } else {
                statement.setString(2, log);
            }
            if (description == null) {
                statement.setNull(3, java.sql.Types.VARCHAR);
            } else {
                statement.setString(3, description);
            }
            statement.execute();
            resultSet = statement.getGeneratedKeys();
            resultSet.next();
            collectionId = resultSet.getInt(1);
        } finally {
            if (resultSet != null) {
                resultSet.close();
            }
            if (statement != null) {
                statement.close();
            }
            this.closeConnection(opened);
        }
        return collectionId;
    }

    /**
     * Cache conditions sets for all known tables.
     */
    private void cacheConditionsSets() {
        for (final TableMetaData meta : this.tableRegistry.values()) {
            try {
                logger.fine("caching conditions " + meta.getKey() + " with type "
                        + meta.getCollectionClass().getCanonicalName());
                this.getCachedConditions(meta.getCollectionClass(), meta.getKey());
            } catch (final Exception e) {
                logger.warning("could not cache conditions " + meta.getKey());
            }
        }
    }

    /**
     * Close the database connection.
     */
    public synchronized void closeConnection() {
        logger.fine("closing connection");
        if (this.connection != null) {
            try {
                if (!this.connection.isClosed()) {
                    this.connection.close();
                }
            } catch (final SQLException e) {
                throw new RuntimeException(e);
            }
        }
        this.connection = null;
        this.isConnected = false;
        logger.info("connection closed");
    }

    /**
     * Close the database connection but only if there was a connection opened based on the flag. Otherwise, it should
     * be left open. Used in conjunction with return value of {@link #openConnection()}.
     *
     * @param connectionOpened <code>true</code> to close the connection; <code>false</code> to leave it open
     */
    public synchronized void closeConnection(final boolean connectionOpened) {
        if (connectionOpened) {
            this.closeConnection();
        }
    }

    /**
     * This method will return <code>true</code> if the given collection ID already exists in the table.
     *
     * @param tableName the name of the table
     * @param collectionID the collection ID value
     * @return <code>true</code> if collection exists
     */
    public boolean collectionExists(final String tableName, final int collectionID) {
        final String sql = "SELECT * FROM " + tableName + " where collection_id = " + collectionID;
        final ResultSet resultSet = this.selectQuery(sql);
        try {
            resultSet.last();
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        int rowCount = 0;
        try {
            rowCount = resultSet.getRow();
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        return rowCount != 0;
    }

    /**
     * Configure this class from an <code>InputStream</code> which should point to an XML document.
     *
     * @param in the InputStream which should be in XML format
     */
    private void configure(final InputStream in) {
        if (!this.isConfigured) {
            final SAXBuilder builder = new SAXBuilder();
            Document config = null;
            try {
                config = builder.build(in);
            } catch (JDOMException | IOException e) {
                throw new RuntimeException(e);
            }
            this.loadConfiguration(config);
            try {
                in.close();
            } catch (final IOException e) {
                logger.warning(e.getMessage());
            }
            this.isConfigured = true;
        } else {
            logger.warning("System is already configured, so call to configure is ignored!");
        }
    }

    /**
     * Find a collection of conditions validity records by key name. The key name is distinct from the table name, but
     * they are usually set to the same value.
     *
     * @param name the conditions key name
     * @return the set of matching conditions records
     */
    public ConditionsRecordCollection findConditionsRecords(final String name) {
        final ConditionsRecordCollection runConditionsRecords = this.getCachedConditions(
                ConditionsRecordCollection.class, "conditions").getCachedData();
        logger.fine("searching for conditions with name " + name + " in " + runConditionsRecords.size() + " records");
        final ConditionsRecordCollection foundConditionsRecords = new ConditionsRecordCollection();
        for (final ConditionsRecord record : runConditionsRecords) {
            if (record.getName().equals(name)) {
                if (this.matchesTag(record)) {
                    foundConditionsRecords.add(record);
                    logger.finer("found matching conditions record " + record.getRowId());
                } else {
                    logger.finer("conditions record " + record.getRowId() + " rejected from non-matching tag "
                            + record.getTag());
                }
            }
        }
        logger.fine("found " + foundConditionsRecords.size() + " conditions records matching tag " + this.tag);
        return foundConditionsRecords;
    }

    /**
     * Find table information from the collection type.
     *
     * @param type the collection type
     * @return the table information or <code>null</code> if does not exist
     */
    public List<TableMetaData> findTableMetaData(final Class<?> type) {
        return this.tableRegistry.findByCollectionType(type);
    }

    /**
     * Find table information from the name.
     *
     * @param name the name of the table
     * @return the table information or <code>null</code> if does not exist
     */
    public TableMetaData findTableMetaData(final String name) {
        return this.tableRegistry.findByTableName(name);
    }

    /**
     * This method can be called to "freeze" the conditions system so that any subsequent updates to run number or
     * detector name will be ignored.
     */
    public synchronized void freeze() {
        if (this.getDetector() != null && this.getRun() != -1) {
            this.isFrozen = true;
            logger.config("conditions system is frozen");
        } else {
            logger.warning("conditions system cannot be frozen because it is not initialized yet");
        }
    }

    /**
     * Get a list of all the {@link ConditionsRecord} objects.
     *
     * @return the list of all the {@link ConditionsRecord} objects
     */
    // FIXME: This should use a cache that is created during initialization, rather than look these up every time.
    public ConditionsRecordCollection getConditionsRecords() {
        logger.finer("getting conditions records ...");
        final ConditionsRecordCollection conditionsRecords = new ConditionsRecordCollection();
        for (final TableMetaData tableMetaData : this.tableRegistry.values()) {
            try {
                final ConditionsRecordCollection foundConditionsRecords = this.findConditionsRecords(tableMetaData
                        .getKey());
                logger.finer("found " + foundConditionsRecords.size() + " collections with name "
                        + tableMetaData.getKey());
                conditionsRecords.addAll(foundConditionsRecords);
            } catch (final Exception e) {
                e.printStackTrace();
                logger.warning(e.getMessage());
            }
        }
        logger.finer("found " + conditionsRecords + " conditions records");
        logger.getHandlers()[0].flush();
        return conditionsRecords;
    }

    /**
     * Get a conditions series with one or more collections.
     *
     * @param collectionType the type of the collection
     * @param tableName the name of the data table
     * @param <ObjectType> the type of the conditions object
     * @param <CollectionType> the type of the conditions collection
     * @return the conditions series
     */
    @SuppressWarnings("unchecked")
    public <ObjectType extends ConditionsObject, CollectionType extends ConditionsObjectCollection<ObjectType>> ConditionsSeries<ObjectType, CollectionType> getConditionsSeries(
            final Class<CollectionType> collectionType, final String tableName) {

        final TableMetaData metaData = this.tableRegistry.get(tableName);
        if (metaData == null) {
            throw new IllegalArgumentException("No table metadata found for type " + collectionType.getName());
        }
        if (!metaData.getCollectionClass().equals(collectionType)) {
            throw new IllegalArgumentException("The type " + collectionType.getName() + " does not match the class "
                    + metaData.getCollectionClass().getName() + " from the meta data");
        }
        final Class<? extends ConditionsObject> objectType = metaData.getObjectClass();
        final ConditionsSeriesConverter<ObjectType, CollectionType> converter = new ConditionsSeriesConverter(
                objectType, collectionType);
        return converter.createSeries(tableName);
    }

    /**
     * Get the current LCSim compact <code>Detector</code> object with the geometry and detector model.
     *
     * @return the detector object
     */
    public Detector getDetectorObject() {
        return this.getCachedConditions(Detector.class, "compact.xml").getCachedData();
    }

    /**
     * Get the combined ECAL conditions for this run.
     *
     * @return the combined ECAL conditions
     */
    public EcalConditions getEcalConditions() {
        return this.getCachedConditions(EcalConditions.class, "ecal_conditions").getCachedData();
    }

    /**
     * Get the name of the ECAL in the detector geometry.
     *
     * @return the name of the ECAL
     */
    public String getEcalName() {
        return this.ecalName;
    }

    /**
     * Get the subdetector object of the ECAL.
     *
     * @return the ECAL subdetector
     */
    public Subdetector getEcalSubdetector() {
        return this.getDetectorObject().getSubdetector(this.ecalName);
    }

    /**
     * Get the combined SVT conditions for this run.
     *
     * @return the combined SVT conditions
     */
    public SvtConditions getSvtConditions() {
        return this.getCachedConditions(SvtConditions.class, "svt_conditions").getCachedData();
    }

    /**
     * Get the set of available conditions tags from the conditions table
     *
     * @return the set of available conditions tags
     */
    public Set<String> getTags() {
        logger.fine("getting list of available conditions tags");
        final boolean openedConnection = this.openConnection();
        final Set<String> tags = new LinkedHashSet<String>();
        final ResultSet rs = this
                .selectQuery("select distinct(tag) from conditions where tag is not null order by tag");
        try {
            while (rs.next()) {
                tags.add(rs.getString(1));
            }
        } catch (final SQLException e) {
            throw new RuntimeException(e);
        }
        try {
            rs.close();
        } catch (final SQLException e) {
            logger.log(Level.WARNING, "error closing ResultSet", e);
        }
        final StringBuffer sb = new StringBuffer();
        sb.append("found unique conditions tags: ");
        for (final String tag : tags) {
            sb.append(tag + " ");
        }
        sb.setLength(sb.length() - 1);
        logger.fine(sb.toString());
        this.closeConnection(openedConnection);
        return tags;
    }

    /**
     * True if there is a conditions record with the given name.
     *
     * @param name the conditions record name (usually will match to table name)
     * @return <code>true</code> if a conditions record exists with the given name
     */
    public boolean hasConditionsRecord(final String name) {
        return !this.findConditionsRecords(name).isEmpty();
    }

    /**
     * Perform all necessary initialization, including setup of the XML configuration and loading of conditions onto the
     * Detector. This is called from the {@link #setDetector(String, int)} method to setup the manager for a new run or
     * detector.
     *
     * @param detectorName the name of the detector model
     * @param runNumber the run number
     * @throws ConditionsNotFoundException if there is a conditions system error
     */
    private void initialize(final String detectorName, final int runNumber) throws ConditionsNotFoundException {

        logger.config("initializing with detector " + detectorName + " and run " + runNumber);

        // Is not configured yet?
        if (!this.isConfigured) {
            if (isTestRun(runNumber)) {
                // This looks like the Test Run so use the custom configuration for it.
                this.setXmlConfig(DatabaseConditionsManager.TEST_RUN_CONFIG);
            } else if (runNumber > TEST_RUN_MAX_RUN) {
                // Run numbers greater than max of Test Run assumed to be Eng Run (for now!).
                this.setXmlConfig(DatabaseConditionsManager.ENGRUN_CONFIG);
            } else if (runNumber == 0) {
                // Use the default configuration because the run number is basically meaningless.
                this.setXmlConfig(DatabaseConditionsManager.DEFAULT_CONFIG);
            }
        }

        // Register the converters for this initialization.
        logger.fine("registering converters");
        this.registerConverters();

        // Enable or disable the setup of the SVT detector.
        logger.fine("enabling SVT setup: " + this.setupSvtDetector);
        this.svtSetup.setEnabled(this.setupSvtDetector);

        // Open the database connection.
        this.openConnection();

        // Call the super class's setDetector method to construct the detector object and activate conditions listeners.
        logger.fine("activating default conditions manager");
        super.setDetector(detectorName, runNumber);

        // Should all conditions sets be cached?
        if (this.cacheAllConditions) {
            // Cache the conditions sets of all registered converters.
            logger.fine("caching all conditions sets ...");
            this.cacheConditionsSets();
        }

        if (this.closeConnectionAfterInitialize) {
            logger.fine("closing connection after initialization");
            // Close the connection.
            this.closeConnection();
        }

        // Should the conditions system be frozen now?
        if (this.freezeAfterInitialize) {
            // Freeze the conditions system so subsequent updates will be ignored.
            this.freeze();
            logger.config("system was frozen after initialization");
        }

        this.isInitialized = true;

        logger.info("conditions system initialized successfully");

        // Flush logger after initialization.
        logger.getHandlers()[0].flush();
    }

    /**
     * Insert a collection of ConditionsObjects into the database.
     *
     * @param collection the collection to insert
     * @param <ObjectType> the type of the conditions object
     * @throws SQLException if there is a database or SQL error
     * @throws ConditionsObjectException if there is a problem inserting the object
     */
    public <ObjectType extends ConditionsObject> void insertCollection(
            final ConditionsObjectCollection<ObjectType> collection) throws SQLException, ConditionsObjectException {

        if (collection == null) {
            throw new IllegalArgumentException("The collection is null.");
        }
        if (collection.size() == 0) {
            throw new IllegalArgumentException("The collection is empty.");
        }

        TableMetaData tableMetaData = collection.getTableMetaData();
        if (tableMetaData == null) {
            final List<TableMetaData> metaDataList = this.tableRegistry.findByCollectionType(collection.getClass());
            if (metaDataList == null) {
                // This is a fatal error because no meta data is available for the type.
                throw new ConditionsObjectException("Failed to find meta data for type: " + collection.getClass());
            }
            tableMetaData = metaDataList.get(0);
        }
        if (collection.getCollectionId() == -1) {
            try {
                collection.setCollectionId(this.addCollection(tableMetaData.getTableName(),
                        "DatabaseConditionsManager created collection by " + System.getProperty("user.name"), null));
            } catch (final ConditionsObjectException e) {
                throw new RuntimeException(e);
            }
        }
        // FIXME: If collection ID is already set this should be an error!

        logger.info("inserting collection with ID " + collection.getCollectionId() + " and key "
                + tableMetaData.getKey() + " into table " + tableMetaData.getTableName());

        final boolean openedConnection = this.openConnection();

        PreparedStatement preparedStatement = null;

        try {
            this.connection.setAutoCommit(false);
            logger.fine("starting insert transaction");
            final String sql = QueryBuilder.buildPreparedInsert(tableMetaData.getTableName(), collection.iterator()
                    .next());
            preparedStatement = this.connection.prepareStatement(sql);
            logger.fine("using prepared statement: " + sql);
            final int collectionId = collection.getCollectionId();
            for (final ConditionsObject object : collection) {
                preparedStatement.setObject(1, collectionId);
                int parameterIndex = 2;
                if (object instanceof ConditionsRecord) {
                    parameterIndex = 1;
                }
                for (final Entry<String, Object> entry : object.getFieldValues().entrySet()) {
                    preparedStatement.setObject(parameterIndex, entry.getValue());
                    ++parameterIndex;
                }
                preparedStatement.executeUpdate();
            }
            this.connection.commit();
            logger.fine("committed transaction");
        } catch (final Exception e) {
            e.printStackTrace();
            logger.warning(e.getMessage());
            logger.warning("rolling back transaction");
            this.connection.rollback();
            logger.warning("transaction was rolled back");
        } finally {
            this.connection.setAutoCommit(true);
        }

        try {
            preparedStatement.close();
        } catch (final Exception e) {
        }

        this.closeConnection(openedConnection);
    }

    /**
     * Check if connected to the database.
     *
     * @return <code>true</code> if connected
     */
    public boolean isConnected() {
        return this.isConnected;
    }

    /**
     * True if conditions system is frozen
     *
     * @return <code>true</code> if conditions system is currently frozen
     */
    public boolean isFrozen() {
        return this.isFrozen;
    }

    /**
     * True if conditions manager is properly initialized.
     *
     * @return <code>true</code> if the manager is initialized
     */
    public boolean isInitialized() {
        return this.isInitialized;
    }

    /**
     * Return <code>true</code> if Test Run configuration is active
     *
     * @return <code>true</code> if Test Run configuration is active
     */
    public boolean isTestRun() {
        return this.isTestRun;
    }

    /**
     * Load configuration information from an XML document.
     *
     * @param document the XML document
     */
    private void loadConfiguration(final Document document) {

        final Element node = document.getRootElement().getChild("configuration");

        if (node == null) {
            return;
        }

        Element element = node.getChild("setupSvtDetector");
        if (element != null) {
            this.setupSvtDetector = Boolean.parseBoolean(element.getText());
            logger.config("setupSvtDetector = " + this.setupSvtDetector);
        }

        element = node.getChild("ecalName");
        if (element != null) {
            this.setEcalName(element.getText());
        }

        element = node.getChild("svtName");
        if (element != null) {
            this.setSvtName(element.getText());
        }

        element = node.getChild("freezeAfterInitialize");
        if (element != null) {
            this.freezeAfterInitialize = Boolean.parseBoolean(element.getText());
            logger.config("freezeAfterInitialize = " + this.freezeAfterInitialize);
        }

        element = node.getChild("cacheAllCondition");
        if (element != null) {
            this.cacheAllConditions = Boolean.parseBoolean(element.getText());
            logger.config("cacheAllConditions = " + this.cacheAllConditions);
        }

        element = node.getChild("isTestRun");
        if (element != null) {
            this.isTestRun = Boolean.parseBoolean(element.getText());
            logger.config("isTestRun = " + this.isTestRun);
        }

        element = node.getChild("logLevel");
        if (element != null) {
            this.setLogLevel(Level.parse(element.getText()));
        }

        element = node.getChild("closeConnectionAfterInitialize");
        if (element != null) {
            this.closeConnectionAfterInitialize = Boolean.parseBoolean(element.getText());
            logger.config("closeConnectionAfterInitialize = " + this.closeConnectionAfterInitialize);
        }

        element = node.getChild("loginTimeout");
        if (element != null) {
            final Integer timeout = Integer.parseInt(element.getText());
            DriverManager.setLoginTimeout(timeout);
            logger.config("loginTimeout = " + timeout);
        }
    }

    /**
     * Return <code>true</code> if the conditions record matches the current tag
     *
     * @param record the conditions record
     * @return <code>true</code> if conditions record matches the currently used tag
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
        return this.tag.equals(recordTag);
    }

    /**
     * Open the database connection.
     *
     * @return <code>true</code> if a connection was opened; <code>false</code> if using an existing connection.
     */
    public synchronized boolean openConnection() {
        boolean openedConnection = false;
        if (!this.isConnected) {
            // Do the connection parameters need to be figured out automatically?
            if (this.connectionParameters == null) {
                // Setup the default read-only connection, which will choose a SLAC or JLab database.
                this.connectionParameters = ConnectionParameters.fromResource(DEFAULT_CONNECTION_PROPERTIES_RESOURCE);
            }

            if (!this.loggedConnectionParameters) {
                // Print out detailed info to the log on first connection within the job.
                logger.info("opening connection ... " + '\n' + "connection: "
                        + this.connectionParameters.getConnectionString() + '\n' + "host: "
                        + this.connectionParameters.getHostname() + '\n' + "port: "
                        + this.connectionParameters.getPort() + '\n' + "user: " + this.connectionParameters.getUser()
                        + '\n' + "database: " + this.connectionParameters.getDatabase());
                this.loggedConnectionParameters = true;
            }

            // Create the connection using the parameters.
            this.connection = this.connectionParameters.createConnection();
            this.isConnected = true;
            openedConnection = true;
        }
        logger.info("connection opened successfully");

        // Flag to indicate whether an existing connection was used or not.
        return openedConnection;
    }

    /**
     * Register the conditions converters with the manager.
     */
    private void registerConverters() {
        if (this.svtConverter != null) {
            // Remove old SVT converter.
            this.removeConditionsConverter(this.svtConverter);
        }

        if (this.ecalConverter != null) {
            // Remove old ECAL converter.
            this.registerConditionsConverter(this.ecalConverter);
        }

        // Is configured for TestRun?
        if (this.isTestRun()) {
            // Load Test Run specific converters.
            this.svtConverter = new TestRunSvtConditionsConverter();
            this.ecalConverter = new TestRunEcalConditionsConverter();
            logger.config("registering Test Run conditions converters");
        } else {
            // Load the default converters.
            this.svtConverter = new SvtConditionsConverter();
            this.ecalConverter = new EcalConditionsConverter();
            logger.config("registering default conditions converters");
        }
        this.registerConditionsConverter(this.svtConverter);
        this.registerConditionsConverter(this.ecalConverter);
    }

    /**
     * This method can be used to perform a database SELECT query.
     *
     * @param query the SQL query string
     * @return the <code>ResultSet</code> from the query
     * @throws RuntimeException if there is a query error
     */
    ResultSet selectQuery(final String query) {
        logger.fine("executing SQL select query ..." + '\n' + query);
        ResultSet result = null;
        Statement statement = null;
        try {
            statement = this.connection.createStatement();
            result = statement.executeQuery(query);
        } catch (final SQLException x) {
            throw new RuntimeException("Error in query: " + query, x);
        }
        return result;
    }

    /**
     * Set the connection parameters of the conditions database.
     *
     * @param connectionParameters the connection parameters
     */
    public void setConnectionParameters(final ConnectionParameters connectionParameters) {
        this.connectionParameters = connectionParameters;
    }

    /**
     * Set the path to a properties file containing connection settings.
     *
     * @param file the properties file
     */
    public void setConnectionProperties(final File file) {
        logger.config("setting connection properties file " + file.getPath());
        if (!file.exists()) {
            throw new IllegalArgumentException("The connection properties file does not exist: "
                    + this.connectionPropertiesFile.getPath());
        }
        this.connectionParameters = ConnectionParameters.fromProperties(file);
    }

    /**
     * Set the connection parameters from an embedded resource location.
     *
     * @param resource the classpath resource location
     */
    public void setConnectionResource(final String resource) {
        logger.config("setting connection resource " + resource);
        this.connectionParameters = ConnectionParameters.fromResource(resource);
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

        if (!this.isInitialized || !detectorName.equals(this.getDetector()) || runNumber != this.getRun()) {
            if (!this.isFrozen) {
                logger.info("new detector " + detectorName + " and run #" + runNumber);
                this.initialize(detectorName, runNumber);
            } else {
                logger.finest("Conditions changed but will be ignored because manager is frozen.");
            }
        }
    }

    /**
     * Set the name of the ECAL sub-detector.
     *
     * @param ecalName the name of the ECAL subdetector
     */
    private void setEcalName(final String ecalName) {
        if (ecalName == null) {
            throw new IllegalArgumentException("The ecalName is null");
        }
        this.ecalName = ecalName;
        logger.info("ECAL name set to " + ecalName);
    }

    /**
     * Set the log level.
     *
     * @param level the new log level
     */
    public void setLogLevel(final Level level) {
        logger.config("setting log level to " + level);
        logger.setLevel(level);
        logger.getHandlers()[0].setLevel(level);
        this.svtSetup.setLogLevel(level);
    }

    /**
     * Set the name of the SVT subdetector.
     *
     * @param svtName the name of the SVT subdetector
     */
    private void setSvtName(final String svtName) {
        if (svtName == null) {
            throw new IllegalArgumentException("The svtName is null");
        }
        this.svtName = svtName;
        logger.info("SVT name set to " + this.ecalName);
    }

    /**
     * Set a tag used to filter the accessible conditions records
     *
     * @param tag the tag value used to filter returned conditions records
     */
    public void setTag(final String tag) {
        this.tag = tag;
        logger.info("using conditions tag: " + tag);
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
            this.setConnectionProperties(f);
            logger.info("connection setup from system property " + CONNECTION_PROPERTY + " = "
                    + systemPropertiesConnectionPath);
        }
    }

    /**
     * Configure some properties of this object from an XML file
     *
     * @param file the XML file
     */
    public void setXmlConfig(final File file) {
        logger.config("setting XML config from file " + file.getPath());
        if (!file.exists()) {
            throw new IllegalArgumentException("The config file does not exist: " + file.getPath());
        }
        try {
            this.configure(new FileInputStream(file));
        } catch (final FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Configure this object from an embedded XML resource.
     *
     * @param resource the embedded XML resource
     */
    public void setXmlConfig(final String resource) {
        logger.config("setting XML config from resource " + resource);
        final InputStream is = this.getClass().getResourceAsStream(resource);
        this.configure(is);
    }

    /**
     * Un-freeze the conditions system so that updates will be received again.
     */
    public synchronized void unfreeze() {
        this.isFrozen = false;
        logger.info("conditions system unfrozen");
    }

    /**
     * Perform a SQL query with an update command like INSERT, DELETE or UPDATE.
     *
     * @param query the SQL query string
     * @return the keys of the rows affected
     */
    public List<Integer> updateQuery(final String query) {
        final boolean openedConnection = this.openConnection();
        logger.fine("executing SQL update query ..." + '\n' + query);
        final List<Integer> keys = new ArrayList<Integer>();
        Statement statement = null;
        ResultSet resultSet = null;
        try {
            statement = this.connection.createStatement();
            statement.executeUpdate(query, Statement.RETURN_GENERATED_KEYS);
            resultSet = statement.getGeneratedKeys();
            while (resultSet.next()) {
                final int key = resultSet.getInt(1);
                keys.add(key);
            }
        } catch (final SQLException x) {
            throw new RuntimeException("Error in SQL query: " + query, x);
        }
        DatabaseUtilities.cleanup(resultSet);
        this.closeConnection(openedConnection);
        return keys;
    }
}
