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
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hps.conditions.api.ConditionsObject;
import org.hps.conditions.api.ConditionsObjectCollection;
import org.hps.conditions.api.ConditionsRecord.ConditionsRecordCollection;
import org.hps.conditions.api.ConditionsSeries;
import org.hps.conditions.api.ConditionsTag.ConditionsTagCollection;
import org.hps.conditions.api.TableMetaData;
import org.hps.conditions.api.TableRegistry;
import org.hps.conditions.ecal.EcalConditions;
import org.hps.conditions.ecal.EcalConditionsConverter;
import org.hps.conditions.ecal.TestRunEcalConditionsConverter;
import org.hps.conditions.svt.SvtConditions;
import org.hps.conditions.svt.SvtConditionsConverter;
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
import org.lcsim.util.loop.DetectorConditionsConverter;
import static org.hps.conditions.database.ConnectionParameters.CONNECTION_PROPERTY_FILE;
import static org.hps.conditions.database.ConnectionParameters.CONNECTION_PROPERTY_RESOURCE;

/**
 * <p>
 * This class provides the top-level API for accessing database conditions, as well as configuring the database
 * connection, initializing all required components, and loading required converters and table meta data. It is
 * registered as the global <code>ConditionsManager</code> in the constructor.
 * <p>
 * Differences between Test Run and Engineering Run configurations are handled automatically.
 *
 * @see org.lcsim.conditions.ConditionsManager
 * @author Jeremy McCormick, SLAC
 */
@SuppressWarnings("rawtypes")
public final class DatabaseConditionsManager extends ConditionsManagerImplementation {
   
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
    private static Logger LOGGER = Logger.getLogger(DatabaseConditionsManager.class.getPackage().getName());

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
        DriverManager.setLoginTimeout(30);
    }

    /**
     * Get the static instance of this class.
     *
     * @return the static instance of the manager
     */
    public static synchronized DatabaseConditionsManager getInstance() {

        // Is there no manager installed yet?
        if (!ConditionsManager.isSetup() || !(ConditionsManager.defaultInstance() instanceof DatabaseConditionsManager)) {

            // Create a new instance if necessary, which will install it globally as the default.
            final DatabaseConditionsManager dbManager = new DatabaseConditionsManager();

            // Register default conditions manager.
            ConditionsManager.setDefaultConditionsManager(dbManager);
        }

        // Get the instance back from the default conditions system and check that the type is correct now.
        final ConditionsManager manager = ConditionsManager.defaultInstance();
        if (!(manager instanceof DatabaseConditionsManager)) {
            throw new RuntimeException("Default conditions manager has the wrong type: "
                    + ConditionsManager.defaultInstance().getClass().getName());
        }

        return (DatabaseConditionsManager) manager;
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

        // Create a new instance if necessary, which will install it globally as the default.
        final DatabaseConditionsManager dbManager = new DatabaseConditionsManager();

        // Register default conditions manager.
        ConditionsManager.setDefaultConditionsManager(dbManager);

        LOGGER.info("DatabaseConditionsManager instance is reset");
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
     * The current set of conditions for the run.
     */
    private ConditionsRecordCollection conditionsRecordCollection = null;

    /**
     * The currently active conditions tag (empty collection means no tag is active).
     */
    private final ConditionsTagCollection conditionsTagCollection = new ConditionsTagCollection();

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
     * Create the global registry of table meta data.
     */
    private final TableRegistry tableRegistry = TableRegistry.getTableRegistry();

    /**
     * The currently applied conditions tags.
     */
    private final Set<String> tags = new HashSet<String>();

    /**
     * Class constructor. Calling this will automatically register this manager as the global default.
     */
    protected DatabaseConditionsManager() {

        // Register detector conditions converter.
        this.registerConditionsConverter(new DetectorConditionsConverter());

        // Setup connection from system property pointing to a file, if it was set.
        this.setupConnectionSystemPropertyFile();

        // Setup connection from system property pointing to a resource, if it was set.
        this.setupConnectionSystemPropertyResource();

        // Set run to invalid number.
        this.setRun(-1);

        // Register conditions converters.
        for (final AbstractConditionsObjectConverter converter : this.converters.values()) {
            this.registerConditionsConverter(converter);
        }
    }

    /**
     * Add a tag used to filter the accessible conditions records.
     * <p>
     * Multiple tags are OR'd together.
     *
     * @param tag the tag value used to filter returned conditions records
     */
    public void addTag(final String tag) {
        if (!this.tags.contains(tag)) {
            LOGGER.info("adding tag " + tag);
            final ConditionsTagCollection findConditionsTag = this.getCachedConditions(ConditionsTagCollection.class,
                    tag).getCachedData();
            if (findConditionsTag.size() == 0) {
                throw new IllegalArgumentException("The tag " + tag + " does not exist in the database.");
            }
            LOGGER.info("adding conditions tag " + tag + " with " + conditionsTagCollection.size() + " records");
            this.conditionsTagCollection.addAll(findConditionsTag);
            this.tags.add(tag);
        } else {
            LOGGER.warning("tag " + tag + " is already added");
        }
    }

    /**
     * Add one or more tags for filtering records.
     *
     * @param tags the <code>Set</code> of tags to add
     */
    public void addTags(final Set<String> tags) {
        for (final String tag : tags) {
            this.addTag(tag);
        }
    }

    /**
     * Cache conditions sets for all known tables.
     */
    private void cacheConditionsSets() {
        for (final TableMetaData meta : this.tableRegistry.values()) {
            try {
                LOGGER.fine("caching conditions " + meta.getKey() + " with type "
                        + meta.getCollectionClass().getCanonicalName());
                this.getCachedConditions(meta.getCollectionClass(), meta.getKey());
            } catch (final Exception e) {
                LOGGER.warning("could not cache conditions " + meta.getKey());
            }
        }
    }

    /**
     * Clear the tags used to filter the {@link org.hps.conditions.api.ConditionsRecord}s.
     */
    public void clearTags() {
        this.tags.clear();
        this.conditionsTagCollection.clear();
    }

    /**
     * Close the database connection.
     */
    public synchronized void closeConnection() {
        LOGGER.fine("closing connection");
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
        LOGGER.fine("connection closed");
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
                LOGGER.warning(e.getMessage());
            }
            this.isConfigured = true;
        } else {
            LOGGER.warning("System is already configured, so call to configure is ignored!");
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
        return this.getConditionsRecords().findByKey(name);
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
            LOGGER.config("conditions system is frozen");
        } else {
            LOGGER.warning("conditions system cannot be frozen because it is not initialized yet");
        }
    }

    /**
     * Get the currently active conditions tags.
     *
     * @return the currently active conditions tags
     */
    public Collection<String> getActiveTags() {
        return Collections.unmodifiableCollection(this.tags);
    }

    /**
     * Get the set of available conditions tags from the conditions table
     *
     * @return the set of available conditions tags
     */
    public Set<String> getAvailableTags() {
        LOGGER.fine("getting list of available conditions tags");
        final boolean openedConnection = this.openConnection();
        final Set<String> tags = new LinkedHashSet<String>();
        final ResultSet rs = this
                .selectQuery("select distinct(tag) from conditions_tags where tag is not null order by tag");
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
            LOGGER.log(Level.WARNING, "error closing ResultSet", e);
        }
        final StringBuffer sb = new StringBuffer();
        sb.append("found unique conditions tags: ");
        for (final String tag : tags) {
            sb.append(tag + " ");
        }
        sb.setLength(sb.length() - 1);
        LOGGER.fine(sb.toString());
        this.closeConnection(openedConnection);
        return tags;
    }

    /**
     * Add a row for a new collection and return the new collection ID assigned to it.
     * @param collection the conditions object collection
     * @param description text description for the new collection ID record in the database
     * @return the collection's ID
     * @throws SQLException
     */
    public synchronized int getCollectionId(final ConditionsObjectCollection<?> collection, final String description)
            throws SQLException {

        final String caller = Thread.currentThread().getStackTrace()[2].getClassName();
        final String log = "created by " + System.getProperty("user.name") + " using "
                + caller.substring(caller.lastIndexOf('.') + 1);
        final boolean opened = this.openConnection();
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        int collectionId = -1;
        try {
            statement = this.connection.prepareStatement(
                    "INSERT INTO collections (table_name, log, description, created) VALUES (?, ?, ?, NOW())",
                    Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, collection.getTableMetaData().getTableName());
            statement.setString(2, log);
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
        collection.setCollectionId(collectionId);
        return collectionId;
    }

    /**
     * Get the list of conditions records for the run, filtered by the current set of active tags.
     *
     * @return the list of conditions records for the run
     */
    public ConditionsRecordCollection getConditionsRecords() {
        if (this.run == -1 || this.detectorName == null) {
            throw new IllegalStateException("Conditions system is not initialized.");
        }
        // If the collection is null then the new conditions records need to be retrieved from the database.
        if (this.conditionsRecordCollection == null) {

            // Get the collection of conditions that are applicable for the current run.
            this.conditionsRecordCollection = this.getCachedConditions(ConditionsRecordCollection.class, "conditions")
                    .getCachedData();

            // If there is one or more tags enabled then filter the collection by the tag names.
            if (this.conditionsTagCollection.size() > 0) {
                this.conditionsRecordCollection = this.conditionsTagCollection.filter(this.conditionsRecordCollection);
            }
        }
        return this.conditionsRecordCollection;
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
     * Get the JDBC connection.
     *
     * @return the JDBC connection
     */
    public Connection getConnection() {
        if (!this.isConnected()) {
            this.openConnection();
        }
        return this.connection;
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
     * True if there is a conditions record with the given name.
     *
     * @param name the conditions record name (usually will match to table name)
     * @return <code>true</code> if a conditions record exists with the given name
     */
    public boolean hasConditionsRecord(final String name) {
        return this.findConditionsRecords(name).size() != 0;
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

        LOGGER.config("initializing with detector " + detectorName + " and run " + runNumber);

        // Clear the conditions cache.
        // this.clearCache();

        // Set flag if run number is from Test Run 2012 data.
        if (isTestRun(runNumber)) {
            this.isTestRun = true;
        }

        // Is not configured yet?
        if (!this.isConfigured) {
            if (this.isTestRun()) {
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
        this.registerConverters();

        // Open the database connection.
        this.openConnection();

        // Reset the conditions records to trigger a re-caching.
        this.conditionsRecordCollection = null;

        // Call the super class's setDetector method to construct the detector object and activate conditions listeners.
        LOGGER.fine("activating default conditions manager");
        super.setDetector(detectorName, runNumber);

        // Should all conditions sets be cached?
        if (this.cacheAllConditions) {
            // Cache the conditions sets of all registered converters.
            LOGGER.fine("caching conditions sets");
            this.cacheConditionsSets();
        }

        if (this.closeConnectionAfterInitialize) {
            LOGGER.fine("closing connection after initialization");
            // Close the connection.
            this.closeConnection();
        }

        // Should the conditions system be frozen now?
        if (this.freezeAfterInitialize) {
            // Freeze the conditions system so subsequent updates will be ignored.
            this.freeze();
            LOGGER.config("system was frozen after initialization");
        }

        this.isInitialized = true;

        LOGGER.info("conditions system initialized successfully");
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
            LOGGER.config("setupSvtDetector = " + this.setupSvtDetector);
        }

        element = node.getChild("ecalName");
        if (element != null) {
            this.setEcalName(element.getText());
            LOGGER.config("ecalName = " + this.getEcalName());
        }

        element = node.getChild("svtName");
        if (element != null) {
            this.setSvtName(element.getText());
            LOGGER.config("svtName = " + this.svtName);
        }

        element = node.getChild("freezeAfterInitialize");
        if (element != null) {
            this.freezeAfterInitialize = Boolean.parseBoolean(element.getText());
            LOGGER.config("freezeAfterInitialize = " + this.freezeAfterInitialize);
        }

        element = node.getChild("cacheAllCondition");
        if (element != null) {
            this.cacheAllConditions = Boolean.parseBoolean(element.getText());
            LOGGER.config("cacheAllConditions = " + this.cacheAllConditions);
        }

        element = node.getChild("isTestRun");
        if (element != null) {
            this.isTestRun = Boolean.parseBoolean(element.getText());
            LOGGER.config("isTestRun = " + this.isTestRun);
        }

        element = node.getChild("closeConnectionAfterInitialize");
        if (element != null) {
            this.closeConnectionAfterInitialize = Boolean.parseBoolean(element.getText());
            LOGGER.config("closeConnectionAfterInitialize = " + this.closeConnectionAfterInitialize);
        }

        element = node.getChild("loginTimeout");
        if (element != null) {
            final Integer timeout = Integer.parseInt(element.getText());
            DriverManager.setLoginTimeout(timeout);
            LOGGER.config("loginTimeout = " + timeout);
        }
    }

    /**
     * Create a new collection with the given type.
     *
     * @param collectionType the collection type
     * @return the new collection
     */
    public <CollectionType extends ConditionsObjectCollection<?>> CollectionType newCollection(
            final Class<CollectionType> collectionType) {
        final List<TableMetaData> tableMetaDataList = TableRegistry.getTableRegistry().findByCollectionType(
                collectionType);
        if (tableMetaDataList.size() > 1) {
            throw new RuntimeException("More than one table meta data object returned for type: "
                    + collectionType.getName());
        }
        final TableMetaData tableMetaData = tableMetaDataList.get(0);
        CollectionType collection;
        try {
            collection = collectionType.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException("Error creating new collection.", e);
        }
        collection.setTableMetaData(tableMetaData);
        collection.setConnection(this.getConnection());
        return collection;
    }

    /**
     * Create a new collection with the given type and table name.
     *
     * @param collectionType the collection type
     * @param tableName the table name
     * @return the new collection
     */
    public <CollectionType extends ConditionsObjectCollection<?>> CollectionType newCollection(
            final Class<CollectionType> collectionType, final String tableName) {
        final TableMetaData tableMetaData = TableRegistry.getTableRegistry().findByTableName(tableName);
        CollectionType collection;
        try {
            collection = collectionType.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException("Error creating new collection.", e);
        }
        collection.setTableMetaData(tableMetaData);
        collection.setConnection(this.getConnection());
        return collection;
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
                LOGGER.info("opening connection ... " + '\n' + "connection: "
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
            LOGGER.config("registering Test Run conditions converters");
        } else {
            // Load the default converters.
            this.svtConverter = new SvtConditionsConverter();
            this.ecalConverter = new EcalConditionsConverter();
            LOGGER.config("registering default conditions converters");
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
    public ResultSet selectQuery(final String query) {
        LOGGER.fine("executing SQL select query ..." + '\n' + query);
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
        LOGGER.config("setting connection properties file " + file.getPath());
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
        LOGGER.config("setting connection resource " + resource);
        this.connectionParameters = ConnectionParameters.fromResource(resource);
    }

    /**
     * This method handles changes to the detector name and run number. It is called every time an LCSim event is
     * created, and so it has internal logic to figure out if the conditions system actually needs to be updated.
     */
    @Override
    public synchronized void setDetector(final String detectorName, final int runNumber)
            throws ConditionsNotFoundException {

        LOGGER.finest("setDetector " + detectorName + " with run number " + runNumber);

        if (detectorName == null) {
            throw new IllegalArgumentException("The detectorName argument is null.");
        }

        if (!this.isInitialized || !detectorName.equals(this.getDetector()) || runNumber != this.getRun()) {
            if (!this.isFrozen) {
                LOGGER.info("new detector " + detectorName + " and run #" + runNumber);
                this.initialize(detectorName, runNumber);
            } else {
                LOGGER.finest("Conditions changed but will be ignored because manager is frozen.");
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
        LOGGER.info("ECAL name set to " + ecalName);
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
        LOGGER.info("SVT name set to " + this.ecalName);
    }

    /**
     * Setup the database connection from a file specified by a Java system property setting. This could be overridden
     * by subsequent API calls to {@link #setConnectionProperties(File)} or {@link #setConnectionResource(String)}.
     */
    private void setupConnectionSystemPropertyFile() {
        final String systemPropertiesConnectionPath = (String) System.getProperties().get(CONNECTION_PROPERTY_FILE);
        if (systemPropertiesConnectionPath != null) {
            final File f = new File(systemPropertiesConnectionPath);
            if (!f.exists()) {
                throw new RuntimeException("Connection properties file from " + CONNECTION_PROPERTY_FILE
                        + " does not exist.");
            }
            this.setConnectionProperties(f);
            LOGGER.info("connection setup from system property " + CONNECTION_PROPERTY_FILE + " = "
                    + systemPropertiesConnectionPath);
        }
    }

    /**
     * Setup the database connection from a file specified by a Java system property setting. This could be overridden
     * by subsequent API calls to {@link #setConnectionProperties(File)} or {@link #setConnectionResource(String)}.
     */
    private void setupConnectionSystemPropertyResource() {
        final String systemPropertiesConnectionResource = (String) System.getProperties().get(
                CONNECTION_PROPERTY_RESOURCE);
        if (systemPropertiesConnectionResource != null) {
            this.setConnectionResource(systemPropertiesConnectionResource);
        }
    }

    /**
     * Configure some properties of this object from an XML file
     *
     * @param file the XML file
     */
    public void setXmlConfig(final File file) {
        LOGGER.config("setting XML config from file " + file.getPath());
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
        LOGGER.config("setting XML config from resource " + resource);
        final InputStream is = this.getClass().getResourceAsStream(resource);
        this.configure(is);
    }

    /**
     * Un-freeze the conditions system so that updates will be received again.
     */
    public synchronized void unfreeze() {
        this.isFrozen = false;
        LOGGER.info("conditions system unfrozen");
    }
}
