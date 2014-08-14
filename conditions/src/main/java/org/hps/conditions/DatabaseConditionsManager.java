package org.hps.conditions;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.hps.conditions.ConditionsRecord.ConditionsRecordCollection;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.lcsim.conditions.ConditionsConverter;
import org.lcsim.conditions.ConditionsManager;
import org.lcsim.conditions.ConditionsManagerImplementation;
import org.lcsim.conditions.ConditionsReader;
import org.lcsim.conditions.readers.BaseClasspathConditionsReader;
import org.lcsim.geometry.Detector;
import org.lcsim.util.loop.DetectorConditionsConverter;

/**
 * <p>
 * This class should be used as the top-level ConditionsManager for database access to
 * conditions data.
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
public final class DatabaseConditionsManager extends ConditionsManagerImplementation {

    static String connectionProperty = "org.hps.conditions.connection.file";
    protected int runNumber = -1;
    protected String detectorName;
    protected List<TableMetaData> tableMetaData;
    protected List<ConditionsConverter> converters;
    protected File connectionPropertiesFile;
    protected ConditionsReader baseReader;
    protected static Logger logger = null;
    protected ConnectionParameters connectionParameters;
    protected Connection connection;
    protected boolean wasConfigured = false;
    protected boolean isConnected = false;
    protected ConditionsSeriesConverter conditionsSeriesConverter = new ConditionsSeriesConverter(this);

    /**
     * Class constructor.
     */
    public DatabaseConditionsManager() {
        registerConditionsConverter(new DetectorConditionsConverter());
        baseReader = new BaseClasspathConditionsReader();
        setupConnectionFromSystemProperty();
    }

    /**
     * Simple log formatter for this class.
     */
    private static final class LogFormatter extends Formatter {

        public String format(LogRecord record) {
            StringBuilder sb = new StringBuilder();
            sb.append(record.getLoggerName() + " [ " + record.getLevel() + " ] " + record.getMessage() + '\n');
            return sb.toString();
        }
    }

    /**
     * Setup the logger for this class, with initial level of ALL.
     */
    static {
        logger = Logger.getLogger(DatabaseConditionsManager.class.getSimpleName());
        logger.setUseParentHandlers(false);
        logger.setLevel(Level.ALL);
        ConsoleHandler handler = new ConsoleHandler();
        handler.setLevel(Level.ALL);
        handler.setFormatter(new LogFormatter());
        logger.addHandler(handler);
        logger.config("logger initialized with level " + handler.getLevel());
    }

    /**
     * Register this conditions manager as the global default.
     */
    public void register() {
        ConditionsManager.setDefaultConditionsManager(this);
    }

    /**
     * Get the static instance of this class, which must have been registered first from a
     * call to {@link #register()}.
     * @return The static instance of the manager.
     */
    public static DatabaseConditionsManager getInstance() {

        // Perform default setup if necessary.
        if (!ConditionsManager.isSetup()) {
            DatabaseConditionsManager manager = new DatabaseConditionsManager();
            manager.register();
        }

        // Get the instance of the manager from the conditions system and
        // that the type is valid.
        ConditionsManager manager = ConditionsManager.defaultInstance();
        if (!(manager instanceof DatabaseConditionsManager)) {
            throw new RuntimeException("The default ConditionsManager has the wrong type.");
        }

        return (DatabaseConditionsManager) manager;
    }
    
    public Connection getConnection() {
        return this.connection;
    }

    /**
     * This method catches changes to the detector name and run number. Somewhat
     * unintuitively, this method is actually called every time an lcsim event is created,
     * so it has internal logic to figure out if the conditions system actually needs to
     * be updated.
     */
    @Override
    public void setDetector(String detectorName, int runNumber) throws ConditionsNotFoundException {
        // Detector update.
        if (getDetector() == null || !getDetector().equals(detectorName)) {
            logger.config("setting new detector " + detectorName);
            setup(detectorName);
        }

        // Run number update.
        if (this.runNumber != runNumber) {
            logger.config("setting new run number " + runNumber);
            this.runNumber = runNumber;
        }

        // Let the super class do whatever it think it needs to do.
        super.setDetector(detectorName, runNumber);
    }

    /**
     * Perform setup for a new detector description.
     * @param detectorName the name of the detector
     */
    void setup(String detectorName) {
        if (baseReader instanceof BaseClasspathConditionsReader) {
            ((BaseClasspathConditionsReader) baseReader).setResourcePath(detectorName);
            logger.config("set resource path " + detectorName + " on conditions reader");
        }
        if (!isConnected())
            openConnection();
        else
            logger.config("using existing connection " + connectionParameters.getConnectionString());
    }

    /**
     * Get the lcsim compact <code>Detector</code> object from the conditions system.
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
    public void configure(File file) {
        logger.config("setting configuration from file " + file.getPath());
        if (!file.exists()) {
            throw new IllegalArgumentException("Config file does not exist.");
        }
        try {
            configure(new FileInputStream(file));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Configure this object from an embedded XML resource.
     * @param resource The embedded XML resource.
     */
    public void configure(String resource) {
        logger.config("setting configuration from resource " + resource);
        InputStream in = getClass().getResourceAsStream(resource);
        if (in == null)
            throw new IllegalArgumentException("The resource does not exist.");
        configure(in);
    }

    /**
     * Set the path to a properties file containing connection settings.
     * @param file The properties file
     */
    public void setConnectionProperties(File file) {
        logger.config("setting connection prop file " + file.getPath());
        if (!file.exists())
            throw new IllegalArgumentException("The connection properties file does not exist: " + connectionPropertiesFile.getPath());
        connectionParameters = ConnectionParameters.fromProperties(file);
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
     * Set externally the base ConditionsReader that will be used to find non-database
     * conditions such as the compact.xml file for the detector.
     * @param reader The base ConditionsReader.
     */
    public void setBaseConditionsReader(ConditionsReader baseReader) {
        logger.config("setting conditions reader to " + baseReader.getClass().getCanonicalName());
        this.baseReader = baseReader;
    }

    /**
     * Get the next collection ID for a database conditions table.
     * @param tableName The name of the table.
     * @return The next collection ID.
     */
    public int getNextCollectionID(String tableName) {
        TableMetaData tableData = findTableMetaData(tableName);
        if (tableData == null)
            throw new IllegalArgumentException("There is no meta data for table " + tableName);
        ResultSet resultSet = selectQuery("SELECT MAX(collection_id)+1 FROM " + tableName);
        int collectionId = -1;
        try {
            resultSet.next();
            collectionId = resultSet.getInt(1);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        logger.fine("new collection ID " + collectionId + " created for table " + tableName);
        return collectionId;
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
    public TableMetaData findTableMetaData(Class type) {
        for (TableMetaData meta : tableMetaData) {
            if (meta.getCollectionClass().equals(type)) {
                return meta;
            }
        }
        return null;
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
            close(statement);
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
    }

    /**
     * Find a collection of conditions validity records by key name. The key name is
     * distinct from the table name, but they are usually set to the same value in the XML
     * configuration.
     * @param name The conditions key name.
     * @return The set of matching conditions records.
     */
    public ConditionsRecordCollection findConditionsRecords(String name) {
        ConditionsRecordCollection runConditionsRecords = getConditionsData(ConditionsRecordCollection.class, TableConstants.CONDITIONS_RECORD);
        logger.fine("searching for condition " + name + " in " + runConditionsRecords.getObjects().size() + " records ...");
        ConditionsRecordCollection foundConditionsRecords = new ConditionsRecordCollection();
        for (ConditionsRecord record : runConditionsRecords.getObjects()) {
            if (record.getName().equals(name)) {
                foundConditionsRecords.add(record);
            }
        }
        if (foundConditionsRecords.getObjects().size() > 0) {
            for (ConditionsRecord record : foundConditionsRecords.getObjects()) {
                logger.fine("found ConditionsRecord with key " + name + " ..." + '\n' + record.toString());
            }
        }
        return foundConditionsRecords;
    }

    /**
     * Return true if the connection parameters are valid, e.g. non-null.
     * @return true if connection parameters are non-null
     */
    public boolean hasConnectionParameters() {
        return connectionParameters != null;
    }

    /**
     * Return if the manager was configured e.g. from an XML configuration file.
     * @return true if manager was configured
     */
    public boolean wasConfigured() {
        return wasConfigured;
    }

    /**
     * Get the current run number.
     * @return the current run number
     */
    public int getRunNumber() {
        return runNumber;
    }
                
    /**
     * Close a JDBC <code>Statement</code>.
     * @param statement the Statement to close
     */
    static void close(Statement statement) {
        if (statement != null) {
            try {
                if (!statement.isClosed())
                    statement.close();
                else
                    logger.log(Level.WARNING, "Statement is already closed!");
            } catch (SQLException x) {
                throw new RuntimeException("Failed to close statement.", x);
            }
        }
    }

    /**
     * Close a JDBC <code>ResultSet</code>, or rather the Statement connected to it.
     * @param resultSet the ResultSet to close
     */
    static void close(ResultSet resultSet) {
        if (resultSet != null) {
            try {
                Statement statement = resultSet.getStatement();
                if (!statement.isClosed())
                    statement.close();
                else
                    logger.log(Level.WARNING, "Statement is already closed!");
            } catch (SQLException x) {
                throw new RuntimeException("Failed to close statement.", x);
            }
        }
    }

    /**
     * Check if connected to the database.
     * @return true if connected
     */
    private boolean isConnected() {
        return isConnected;
    }

    /**
     * Configure this class from an <code>InputStream</code> which should point to an XML
     * document.
     * @param in the InputStream which should be in XML format
     */
    private void configure(InputStream in) {

        // Create XML document from stream.
        Document config = createDocument(in);

        // Load the table meta data.
        loadTableMetaData(config);

        // Load the converter classes.
        loadConverters(config);

        // Set configured state to true.
        wasConfigured = true;
    }

    /**
     * Create an XML document from an <code>InputStream</code>.
     * @param in The InputStream.
     * @return The XML document.
     */
    private Document createDocument(InputStream in) {
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

        // Load the list of converters from the "converters" section of the config
        // document.
        (this.new ConditionsConverterLoader()).load(config.getRootElement().getChild("converters"));

        // Register the list of converters with this manager.
        // FIXME: Should this happen here or when setup is called on the manager?
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
        // Load table meta data from the "tables" section of the config document.
        (this.new TableMetaDataLoader()).load(config.getRootElement().getChild("tables"));
    }
    
    /**
     * Setup the database connection from a file specified by Java system property setting.
     * This is possible overridden by subsequent API calls to {@link #setConnectionProperties(File)} or
     * {@link #setConnectionResource(String)}, as it is setup in this class's constructor.
     */
    private void setupConnectionFromSystemProperty() {
        String systemPropertiesConnectionPath = (String)System.getProperties().get(connectionProperty);
        if (systemPropertiesConnectionPath != null) {
            File f = new File(systemPropertiesConnectionPath);
            if (!f.exists())
                throw new RuntimeException("Connection properties specified from system property does not exist!");
            this.setConnectionProperties(f);
        }
    }

    /**
     * Open the database connection.
     */
    public void openConnection() {
        if (connectionParameters == null)
            throw new RuntimeException("The connection parameters were not configured.");
        connection = connectionParameters.createConnection();
        logger.config("created connection " + connectionParameters.getConnectionString());
        isConnected = true;
    }

    /**
     * Close the database connection.
     */
    public void closeConnection() {
        logger.config("closing connection");
        if (connection != null) {
            try {
                if (!connection.isClosed()) {
                    connection.close();
                    logger.config("connection closed");
                } else {
                    logger.config("connection already closed");
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
        connection = null;
        connectionParameters = null;
    }

    @Override
    public void finalize() {
        if (isConnected())
            closeConnection();
    }
    
    /**
     * Get multiple <code>ConditionsObjectCollection</code> objects that may have overlapping time validity information.
     * @param conditionsKey The conditions key.
     * @return The <code>ConditionsSeries</code> containing the matching <code>ConditionsObjectCollection</code>. 
     */
    public <CollectionType extends ConditionsObjectCollection> ConditionsSeries<CollectionType> getConditionsSeries(String conditionsKey) {
        return conditionsSeriesConverter.createSeries(conditionsKey);
    }
       
    /**
     * Simple utility method to cast the generic <code>ConditionsManager</code> to this class.
     * @param conditionsManager The <code>ConditionsManager</code>.
     * @return The <code>DatabaseConditionsManager</code> object.
     */
    public static DatabaseConditionsManager castFrom(ConditionsManager conditionsManager) {
        if (conditionsManager instanceof DatabaseConditionsManager) {
            return (DatabaseConditionsManager) conditionsManager;
        } else {
            throw new RuntimeException("The conditionsManager points to an object of the wrong type: " + conditionsManager.getClass().getCanonicalName());
        }
    }    
        
    /**
     * This class loads an XML configuration of conditions table meta data.
     * 
     * @author Jeremy McCormick <jeremym@slac.stanford.edu>
     */
    class TableMetaDataLoader {

        /**
         * This method expects an XML element containing child "table" elements.
         * @param element
         */
        @SuppressWarnings("unchecked")
        void load(Element element) {

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

                Class<? extends ConditionsObjectCollection<?>> collectionClass;
                Class<?> rawCollectionClass;
                try {
                    rawCollectionClass = Class.forName(collectionName);
                    if (!ConditionsObjectCollection.class.isAssignableFrom(rawCollectionClass))
                        throw new RuntimeException("The class " + rawCollectionClass.getSimpleName() + " does not extend ConditionsObjectCollection.");
                    collectionClass = (Class<? extends ConditionsObjectCollection<?>>) rawCollectionClass;
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
    }

    /**
     * This class reads in an XML configuration specifying a list of converter classes,
     * e.g. from the config file for the {@link DatabaseConditionsManager}.
     * 
     * @author Jeremy McCormick <jeremym@slac.stanford.edu>
     */
    class ConditionsConverterLoader {

        void load(Element element) {
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

}
