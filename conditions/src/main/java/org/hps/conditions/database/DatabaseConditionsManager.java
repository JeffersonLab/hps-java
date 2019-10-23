package org.hps.conditions.database;

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
import java.util.Properties;
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
import org.hps.conditions.hodoscope.HodoscopeConditions;
import org.hps.conditions.hodoscope.HodoscopeConditionsConverter;
import org.hps.conditions.svt.SvtConditions;
import org.hps.conditions.svt.SvtConditionsConverter;
import org.hps.conditions.svt.TestRunSvtConditionsConverter;
import org.lcsim.conditions.ConditionsConverter;
import org.lcsim.conditions.ConditionsManager;
import org.lcsim.conditions.ConditionsManagerImplementation;
import org.lcsim.geometry.Detector;
import org.lcsim.util.loop.DetectorConditionsConverter;

/**
 * <p>
 * This class provides the top-level API for accessing database conditions, as
 * well as configuring the database connection, initializing all required
 * components, and loading required converters and table meta data. It is
 * registered as the global <code>ConditionsManager</code> in the constructor.
 * <p>
 * Differences between Test Run and Engineering Run configurations are handled
 * automatically.
 * <p>
 * The connection URL, username and password can be set via Java properties from
 * the command line, e.g. for the default connection:</br>
 * <code>java -Dorg.hps.conditions.url=jdbc:mysql://hpsdb.jlab.org:3306/hps_conditions
 *       -Dorg.hps.conditions.user=hpsuser -Dorg.hps.conditions.password=darkphoton [...]</code>
 * <p>
 * To use a local SQLite database, provide a URL that looks something like
 * this:</br>
 * <code>jdbc:sqlite:hps_conditions.db</code>
 * <p>
 * The last part should be the relative or absolute path to the SQLite db file.
 * <p>
 * When using a SQLite database, no username or password needs to be provided.
 * <p>
 * SQLite is not supported in write mode. The local database should be a clone
 * of a particular version of the master MySQL database at JLab.
 *
 * @see org.lcsim.conditions.ConditionsManager
 * @author Jeremy McCormick, SLAC
 */
@SuppressWarnings("rawtypes")
public final class DatabaseConditionsManager extends ConditionsManagerImplementation {

    /**
     * Initialize the logger.
     */
    private static Logger LOG = Logger.getLogger(DatabaseConditionsManager.class.getPackage().getName());

    private static DatabaseConditionsManager INSTANCE = null;

    /**
     * The max value for a run to be considered Test Run.
     */
    private static final int TEST_RUN_MAX_RUN = 1365;


    /**
     * The run marking the beginning of the 2019 Physics Run. 
     */
    private static final int PHYS_2019_MIN_RUN = 9000; 

    /**
     * Default database URL.
     */
    private static String DEFAULT_URL = "jdbc:mysql://hpsdb.jlab.org:3306/hps_conditions";

    /**
     * Default database user for read access.
     */
    private static String DEFAULT_USER = "hpsuser";

    /**
     * Password for default account 'hpsuser'.
     */
    private static String DEFAULT_PASSWORD = "darkphoton";

    /**
     * Number of connection retries allowed.
     */
    private static final int MAX_ATTEMPTS = 10;

    /**
     * Wait time (in millis) for the first retry. The nth retry waits for
     * n*RETRY_WAIT millis.
     */
    private static final int RETRY_WAIT = 5000;

    /**
     * Get a connection to the conditions database, possibly using properties
     * from the command line for the database URL, username, and password
     * (otherwise the defaults are used to provide a read only connection).
     *
     * @return A connection to the conditions database
     */
    private Connection createConnection() {

        Connection connection = null;

        String url = System.getProperty("org.hps.conditions.url");
        if (url == null) {
            url = DEFAULT_URL;
        }
        String user = System.getProperty("org.hps.conditions.user");
        if (user == null) {
            user = DEFAULT_USER;
        }
        String password = System.getProperty("org.hps.conditions.password");
        if (password == null) {
            password = DEFAULT_PASSWORD;
        }

        Properties props = new Properties();
        props.setProperty("user", user);
        props.setProperty("password", password);

        LOG.info("Opening conditions db connection ..." + '\n' + "url: " + url + '\n' + "user: " + user);
        //+ '\n' + "password: " + password + '\n');

        if (url.contains("mysql://")) {
            // Remote connection using MySQL with connection retry
            int attempt = 0;
            while (true) {
                attempt++;
                try {
                    connection = DriverManager.getConnection(url, props);
                } catch (final SQLException x) {
                    // FIXME: The message of the exception should be checked here, as there could be other problems
                    //        other than max connections exceeded, such as the password or URL being wrong, in which case
                    //        the method should just fail immediately.
                    Logger.getLogger(this.getClass().getPackage().getName())
                            .warning("Failed to connect to database " + url + " - " + x.getMessage());
                    if (attempt >= MAX_ATTEMPTS) {
                        throw new RuntimeException(
                                "Failed to connect to database after " + attempt + " attempts: " + url, x);
                    }
                    try {
                        Thread.sleep(attempt * RETRY_WAIT);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(ConnectionParameters.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    continue;
                }
                if (attempt > 1) {
                    Logger.getLogger(this.getClass().getPackage().getName())
                            .warning("Connected to database " + url + " - after " + attempt + " attempts");
                }
                break;
            }
        } else {
            // Local connection using SQLite
            try {
                Class.forName("org.sqlite.JDBC");
                connection = DriverManager.getConnection(url, props);
            } catch (final SQLException x) {
                throw new RuntimeException("Error connecting to local SQLite database", x);
            } catch (final ClassNotFoundException x) {
                throw new RuntimeException("SQLite db driver not found", x);
            }
        }
        return connection;
    }

    static {
        DriverManager.setLoginTimeout(30);
    }

    /**
     * Get the static instance of this class.
     *
     * @return the static instance of the manager
     */
    public static DatabaseConditionsManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new DatabaseConditionsManager();
        }
        return INSTANCE;
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
     * Utility method to determine if a run number is from the 2019 Physics Run.
     *
     * @param runNumber The run number.
     * @return <code>true</code> if the run number is from the 2019 Physics Run.
     */
    public static boolean isPhys2019Run(final int runNumber) { 
        return runNumber > PHYS_2019_MIN_RUN;  
    }

    /**
     * The current set of conditions records for the run.
     */
    private ConditionsRecordCollection conditionsRecordCollection = null;

    /**
     * The currently active conditions tag; an empty collection means that no
     * tag is active.
     */
    private final ConditionsTagCollection conditionsTagCollection = new ConditionsTagCollection();

    /**
     * The current database connection.
     */
    private Connection connection = null;

    /**
     * Create the global registry of conditions object converters.
     */
    private final ConverterRegistry converters = ConverterRegistry.create();

    /**
     * The converter for creating the combined ECAL conditions object.
     */
    private ConditionsConverter ecalConverter;

    /**
     * True if the conditions system has been frozen and will ignore updates
     * after it is initialized.
     */
    private boolean isFrozen = false;

    /**
     * True if the manager has been initialized, e.g. the
     * {@link #setDetector(String, int)} method was called.
     */
    private boolean isInitialized = false;

    /**
     * True if current run number is from Test Run.
     */
    private boolean isTestRun = false;

    /**
     * The converter for creating the combined SVT conditions object.
     */
    private ConditionsConverter svtConverter;

    /**
     * Create the global registry of table meta data.
     */
    private final TableRegistry tableRegistry = TableRegistry.getTableRegistry();

    /**
     * The currently applied conditions tags.
     */
    private final Set<String> tags = new HashSet<String>();

    private ConditionsConverter hodoscopeConverter;

    /**
     * Class constructor. Calling this will automatically register this manager
     * as the global default.
     */
    private DatabaseConditionsManager() {

        // Register detector conditions converter.
        this.registerConditionsConverter(new DetectorConditionsConverter());

        // Set run to invalid number.
        this.setRun(Integer.MIN_VALUE);

        // Register conditions converters.
        for (final AbstractConditionsObjectConverter converter : this.converters.values()) {
            this.registerConditionsConverter(converter);
        }

        // Set default global conditions manager.
        ConditionsManager.setDefaultConditionsManager(this);
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
            LOG.info("adding tag " + tag);
            final ConditionsTagCollection findConditionsTag = this
                    .getCachedConditions(ConditionsTagCollection.class, tag).getCachedData();
            if (findConditionsTag.size() == 0) {
                throw new IllegalArgumentException("The tag " + tag + " does not exist in the database.");
            }
            LOG.info("adding conditions tag " + tag + " with " + conditionsTagCollection.size() + " records");
            this.conditionsTagCollection.addAll(findConditionsTag);
            this.tags.add(tag);
        } else {
            LOG.warning("tag " + tag + " is already added");
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
    /*
     * private void cacheConditionsSets() { for (final TableMetaData meta :
     * this.tableRegistry.values()) { try { LOGGER.fine("caching conditions " +
     * meta.getKey() + " with type " +
     * meta.getCollectionClass().getCanonicalName());
     * this.getCachedConditions(meta.getCollectionClass(), meta.getKey()); } catch
     * (final Exception e) { LOGGER.warning("could not cache conditions " +
     * meta.getKey()); } } }
     */
    /**
     * Clear the tags used to filter the
     * {@link org.hps.conditions.api.ConditionsRecord}s.
     */
    public void clearTags() {
        this.tags.clear();
        this.conditionsTagCollection.clear();
    }

    /**
     * Close the database connection.
     */
    public synchronized void closeConnection() {
        if (this.connection != null) {
            try {
                if (!this.connection.isClosed()) {
                    LOG.info("Closing database connection.");
                    this.connection.close();
                } else {
                    LOG.warning("Database connection was already closed!");
                }
            } catch (final SQLException e) {
                e.printStackTrace();
            }
        }
        this.connection = null;
    }

    /**
     * This method will return <code>true</code> if the given collection ID
     * already exists in the table.
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
     * Find a collection of conditions validity records by key name. The key
     * name is distinct from the table name, but they are usually set to the
     * same value.
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
     * This method can be called to "freeze" the conditions system so that any
     * subsequent updates to run number or detector name will be ignored.
     */
    public synchronized void freeze() {
        if (this.getDetector() != null && this.getRun() != -1) {
            this.isFrozen = true;
            LOG.config("Conditions system is now frozen and will not accept updates to detector or run.");
        } else {
            LOG.warning("Conditions system cannot be frozen because it is not initialized yet!");
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
            LOG.log(Level.WARNING, "Error closing ResultSet.", e);
        }
        final StringBuffer sb = new StringBuffer();
        sb.append("found unique conditions tags: ");
        for (final String tag : tags) {
            sb.append(tag + " ");
        }
        sb.setLength(sb.length() - 1);
        LOG.fine(sb.toString());
        return tags;
    }

    /**
     * Add a row for a new collection and return the new collection ID assigned
     * to it.
     *
     * @param collection the conditions object collection
     * @param description text description for the new collection ID record in
     * the database
     * @return the collection's ID
     * @throws SQLException
     */
    public synchronized int getCollectionId(final ConditionsObjectCollection<?> collection, final String description)
            throws SQLException {

        final String caller = Thread.currentThread().getStackTrace()[2].getClassName();
        final String log = "created by " + System.getProperty("user.name") + " using "
                + caller.substring(caller.lastIndexOf('.') + 1);
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
        }
        collection.setCollectionId(collectionId);
        return collectionId;
    }

    /**
     * Get the list of conditions records for the run, filtered by the current
     * set of active tags.
     *
     * @return the list of conditions records for the run
     */
    public ConditionsRecordCollection getConditionsRecords() {
        if (this.run == -1 || this.detectorName == null) {
            throw new IllegalStateException("Conditions system is not initialized.");
        }
        // If the collection is null then the new conditions records need to be
        // retrieved from the database.
        if (this.conditionsRecordCollection == null) {

            // Get the collection of conditions that are applicable for the current run.
            this.conditionsRecordCollection = this.getCachedConditions(ConditionsRecordCollection.class, "conditions")
                    .getCachedData();

            // If there is one or more tags enabled then filter the collection by the tag
            // names.
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
        try {
            if (this.connection == null || this.connection.isClosed()) {
                this.openConnection();
            }
        } catch (final SQLException x) {
            throw new RuntimeException(x);
        }
        return this.connection;
    }

    /**
     * Get the current LCSim compact <code>Detector</code> object with the
     * geometry and detector model.
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
     * Get the combined Hodo conditions for this run.
     *
     * @return the combined Hodo conditions
     */
    public HodoscopeConditions getHodoConditions() {
        return this.getCachedConditions(HodoscopeConditions.class, "hodo_conditions").getCachedData();
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
     * @return <code>true</code> if a conditions record exists with the given
     * name
     */
    public boolean hasConditionsRecord(final String name) {
        return this.findConditionsRecords(name).size() != 0;
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
     * Reset the static instance..
     */
    public static void reset() {
        INSTANCE = new DatabaseConditionsManager();
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
     * Create a new collection with the given type.
     *
     * @param collectionType the collection type
     * @return the new collection
     */
    public <CollectionType extends ConditionsObjectCollection<?>> CollectionType newCollection(
            final Class<CollectionType> collectionType) {
        final List<TableMetaData> tableMetaDataList = TableRegistry.getTableRegistry()
                .findByCollectionType(collectionType);
        if (tableMetaDataList.size() > 1) {
            throw new RuntimeException(
                    "More than one table meta data object returned for type: " + collectionType.getName());
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
     * @return <code>true</code> if a connection was opened; <code>false</code>
     * if using an existing connection.
     */
    public synchronized void openConnection() {
        try {
            if (this.connection == null || this.connection.isClosed()) {
                this.connection = createConnection();
            }
        } catch (SQLException x) {
            throw new RuntimeException("Error opening connection", x);
        }
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
            this.removeConditionsConverter(this.ecalConverter);
        }

        if (this.hodoscopeConverter != null) {
            this.removeConditionsConverter(this.hodoscopeConverter);
        }

        // Is configured for TestRun?
        if (this.isTestRun()) {
            // Load Test Run specific converters.
            this.svtConverter = new TestRunSvtConditionsConverter();
            this.ecalConverter = new TestRunEcalConditionsConverter();
        } else {
            // Load the default converters.
            this.svtConverter = new SvtConditionsConverter();
            this.ecalConverter = new EcalConditionsConverter();
            this.hodoscopeConverter = new HodoscopeConditionsConverter();
        }
        this.registerConditionsConverter(this.svtConverter);
        this.registerConditionsConverter(this.ecalConverter);
        if (this.hodoscopeConverter != null) {
            this.registerConditionsConverter(this.hodoscopeConverter);
        }
    }

    /**
     * This method can be used to perform a database SELECT query.
     *
     * @param query the SQL query string
     * @return the <code>ResultSet</code> from the query
     * @throws RuntimeException if there is a query error
     */
    public ResultSet selectQuery(final String query) {
        ResultSet result = null;
        Statement statement = null;
        try {
            statement = getConnection().createStatement();
            result = statement.executeQuery(query);
        } catch (final SQLException x) {
            throw new RuntimeException("Error in query: " + query, x);
        }
        return result;
    }

    /**
     * This method handles changes to the detector name and run number. It is
     * called every time an LCSim event is created, and so it has internal logic
     * to figure out if the conditions system actually needs to be updated.
     */
    @Override
    public synchronized void setDetector(final String detectorName, final int runNumber)
            throws ConditionsNotFoundException {

        if (!this.isInitialized || !detectorName.equals(this.getDetector()) || runNumber != this.getRun()) {

            if (!this.isFrozen) {

                LOG.config("Initializing conditions system with detector '" + detectorName + "' and run " + runNumber);

                // Set flag if run number is from Test Run 2012 data.
                if (isTestRun(runNumber)) {
                    this.isTestRun = true;
                }

                // Register the converters for this initialization.
                this.registerConverters();

                // Open the database connection.
                this.openConnection();

                // Reset the conditions records.
                this.conditionsRecordCollection = null;

                // Call the super class's setDetector method to construct the detector object
                // and activate conditions listeners.
                super.setDetector(detectorName, runNumber);

                // Close the connection.
                this.closeConnection();

                this.isInitialized = true;
            }
        }
    }

    /**
     * Un-freeze the conditions system so that updates will be received again.
     */
    public synchronized void unfreeze() {
        this.isFrozen = false;
        LOG.info("Conditions system was unfrozen and will now accept updates.");
    }
}
