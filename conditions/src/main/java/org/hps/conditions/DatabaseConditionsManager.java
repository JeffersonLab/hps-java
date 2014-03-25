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
import org.lcsim.conditions.ConditionsReader;
import org.lcsim.conditions.readers.BaseClasspathConditionsReader;
import org.lcsim.geometry.Detector;
import org.lcsim.util.loop.LCSimConditionsManagerImplementation;

/**
 * <p>
 * This class should be used as the top-level ConditionsManager for database access to conditions
 * data.
 * </p>
 * <p>
 * In general, this will be overriding the <code>LCSimConditionsManagerImplementation</code> which
 * is setup from within <code>LCSimLoop</code>.
 * </p>
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */ 
public class DatabaseConditionsManager extends LCSimConditionsManagerImplementation {

    static DatabaseConditionsManager _instance;
    int _runNumber = -1;
    String _detectorName;
    List<TableMetaData> _tableMetaData;
    List<ConditionsConverter> _converters;
    File _connectionPropertiesFile;
    ConditionsReader _baseReader;    
    static Logger _logger = null;
    ConnectionParameters _connectionParameters;
    Connection _connection;
    String _conditionsTableName;

    /**
     * Constructor is set to private as this class should not be instantiated directly. Use
     * {@link #createInstance()} instead.
     */
    private DatabaseConditionsManager() {
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
     * Setup the logger for this class.
     */
    static {
        _logger = Logger.getLogger(DatabaseConditionsManager.class.getSimpleName());
        _logger.setUseParentHandlers(false);
        _logger.setLevel(Level.ALL);
        ConsoleHandler handler = new ConsoleHandler();
        handler.setLevel(Level.ALL);
        handler.setFormatter(new LogFormatter());
        _logger.addHandler(handler);
        _logger.config("logging initialized with level " + handler.getLevel());
    }

    /**
     * Create a static instance of this class and register it as the default conditions manager.
     * 
     * @return The new conditions manager.
     */
    public static DatabaseConditionsManager createInstance() {
        _instance = new DatabaseConditionsManager();
        ConditionsManager.setDefaultConditionsManager(_instance);
        // setupLogger();
        _logger.config("created and registered " + DatabaseConditionsManager.class.getSimpleName());
        return _instance;
    }

    /**
     * Get the static instance of this class.
     * @return The static instance of the manager.
     */
    public static DatabaseConditionsManager getInstance() {
        return _instance;
    }

    /**
     * Perform setup for the current detector and run number.
     */
    public void setup() {
        try {
            try {
                // Create a new, default base ConditionsReader if one was not externally set.
                if (_baseReader == null)
                    // Setup the default base reader to handle classpath resources.
                    _baseReader = new BaseClasspathConditionsReader(_detectorName);
                _logger.config("using base conditions reader: " + _baseReader.getClass().getSimpleName());
                
                // Set the ConditionsReader on the manager.
                setConditionsReader(new DatabaseConditionsReader(_baseReader), _detectorName);
                                
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            _logger.config("setting detector: " + _detectorName);
            _logger.config("setting run number: " + _runNumber);
            
            // Setup the manager with the detector and run number.
            setDetector(_detectorName, _runNumber);
        } catch (ConditionsNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Set the run number. This will not trigger a conditions change until {@link #setup()} is
     * called.
     * @param runNumber The new run number.
     */
    public void setRunNumber(int runNumber) {
        _runNumber = runNumber;
    }

    /**
     * Set the detector name. This will not trigger a conditions change until {@link #setup()} is
     * called.
     * @param detectorName The name of the new detector.
     */
    public void setDetectorName(String detectorName) {
        _detectorName = detectorName;
    }

    /**
     * Get the current run number.
     * @return The current run number.
     */
    public int getRunNumber() {
        return _runNumber;
    }

    /**
     * Get the current detector name.
     * @return The name of the current detector.
     */
    public String getDetectorName() {
        return this.getDetector();
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
    // TODO: Need to check if this returns null or will throw an exception if does not exist.
    public <T> T getConditionsData(Class<T> type, String name) {
        _logger.fine("getting conditions " + name + " of type " + type.getSimpleName());
        return getCachedConditions(type, name).getCachedData();
    }

    /**
     * Configure this object from an XML file.
     * @param file The XML file.
     */
    public void configure(File file) {
        try {
            _logger.info("configuring from file: " + file.getCanonicalPath());
        } catch (IOException e) {
        }
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
        _logger.config("configuring manager from resource: " + resource);
        InputStream in = getClass().getResourceAsStream(resource);
        if (in == null)
            throw new IllegalArgumentException("The resource does not exist.");
        configure(in);
    }
    
    /**
     * Set the path to a properties file containing connection settings.
     * @param path
     */
    public void setConnectionProperties(String path) {
        _connectionPropertiesFile = new File(path);
        if (!_connectionPropertiesFile.exists())
            throw new IllegalArgumentException("The connection properties file does not exist: " + _connectionPropertiesFile.getPath());
    }

    /**
     * Set externally the base ConditionsReader that will be used to find non-database conditions
     * such as the compact.xml file for the detector.
     * @param reader The base ConditionsReader.
     */
    public void setBaseConditionsReader(ConditionsReader reader) {
        _baseReader = reader;
    }

    /**
     * Get the next collection ID for a database conditions table.
     * @param tableName The name of the table.
     * @return The next collection ID.
     */
    // TODO: If there are no collections that exist, this method should simply return the value '1'
    // or it could throw an exception.
    public int getNextCollectionId(String tableName) {
        TableMetaData tableData = findTableMetaData(tableName);
        if (tableData == null)
            throw new IllegalArgumentException("There is no meta data for table " + tableName);
        ResultSet resultSet = query("SELECT MAX(collection_id)+1 FROM " + tableName);
        int collectionId = -1;
        try {
            resultSet.next();
            collectionId = resultSet.getInt(1);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        _logger.fine("new collection ID " + collectionId + " created for table " + tableName);
        return collectionId;
    }

    /**
     * Get the list of table meta data.
     * @return The list of table meta data.
     */
    public List<TableMetaData> getTableMetaDataList() {
        return _tableMetaData;
    }

    /**
     * Find a table's meta data.
     * @param name The name of the table.
     * @return The table's meta data or null if does not exist.
     */
    public TableMetaData findTableMetaData(String name) {
        for (TableMetaData meta : _tableMetaData) {
            if (meta.getTableName().equals(name))
                return meta;
        }
        return null;
    }
    
    /**
     * Find meta data by collection class type.
     * @param type The collection class.
     * @return The table meta data.
     */
    public TableMetaData findTableMetaData(Class type) {
        for (TableMetaData meta : _tableMetaData) {
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
    public ResultSet query(String query) {
        _logger.fine(query);
        ResultSet result = null;
        Statement statement = null;
        try {
            statement = _connection.createStatement();
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
    public List<Integer> update(String query) {        
        _logger.fine(query);
        List<Integer> keys = new ArrayList<Integer>();
        Statement statement = null;
        try {
            statement = _connection.createStatement();
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
        _logger.setLevel(level);
        _logger.getHandlers()[0].setLevel(level);
        _logger.config("set log level to " + level);
    }
    
    /**
     * Get the name of the conditions table containing validity data.
     * @return The name of the conditions table with validity data.
     */
    public String getConditionsTableName() {
        return _conditionsTableName;
    }

    /**
     * Find a collection of conditions validity records by key name.
     * The key name is distinct from the table name, but they are usually
     * set to the same value in the XML configuration.    
     * @param name The conditions key name.
     * @return The set of matching conditions records.
     */
    public ConditionsRecordCollection findConditionsRecords(String name) {
        ConditionsRecordCollection runConditionsRecords = getConditionsData(ConditionsRecordCollection.class, getConditionsTableName());
        _logger.fine("searching for condition " + name + " in " + runConditionsRecords.getObjects().size() + " records ...");
        ConditionsRecordCollection foundConditionsRecords = new ConditionsRecordCollection();
        for (ConditionsRecord record : runConditionsRecords.getObjects()) {
            if (record.getName().equals(name)) {
                foundConditionsRecords.add(record);
            }
        }
        if (foundConditionsRecords.getObjects().size() > 0) {
            for (ConditionsRecord record : foundConditionsRecords.getObjects()) {
                _logger.fine("found ConditionsRecord with key " + name + " ..." 
                        + '\n' + foundConditionsRecords.get(0).toString());
            }
            _logger.fine("");
        }
        return foundConditionsRecords;
    }    
        
    /**
     * Close a JDBC <code>Statement</code>.
     * @param statement The Statement to close.
     */
    static void close(Statement statement) {
        if (statement != null) {
            try {
                if (!statement.isClosed())
                    statement.close();
                else
                    _logger.log(Level.WARNING, "Statement is already closed!");
            } catch (SQLException x) {
                throw new RuntimeException("Failed to close statement.", x);
            }
        }
    }
    
    /**
     * Close a JDBC <code>ResultSet</code>, or rather the Statement connected to it.
     * @param resultSet The ResultSet to close.
     */
    static void close(ResultSet resultSet) {        
        if (resultSet != null) {
            try {
                Statement statement = resultSet.getStatement();
                if (!statement.isClosed())
                    statement.close();
                else
                    _logger.log(Level.WARNING, "Statement is already closed!");
            } catch (SQLException x) {
                throw new RuntimeException("Failed to close statement.", x);
            }
        }
    }

    /**
     * Configure this class from an <code>InputStream</code> which should point 
     * to an XML document.
     * @param in The InputStream.
     */
    private void configure(InputStream in) {

        // Create XML document.
        Document config = createDocument(in);

        // Load the table meta data from XML.
        loadTableMetaData(config);

        // Load the converter classes from XML.
        loadConverters(config);        
        
        // Open a connection to the database.
        openConnection();
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
        
        // Load the list of converters from the "converters" section of the config document.
        (this.new ConditionsConverterLoader()).load(config.getRootElement().getChild("converters"));

        // Register the list of converters with this manager.
        // FIXME: Should this happen here or when setup is called on the manager?
        for (ConditionsConverter converter : _converters) {
            registerConditionsConverter(converter);
            _logger.config("registered converter " + converter.getClass().getSimpleName());
        }
        
        // Find the mandatory converter for ConditionsRecord class which must be present in the configuration.
        TableMetaData conditionsTableMetaData = findTableMetaData(ConditionsRecordCollection.class);
        if (conditionsTableMetaData == null) {
            throw new RuntimeException("No conditions converter found for ConditionsRecord type in the supplied configuration.");            
        }
        _conditionsTableName = conditionsTableMetaData.getTableName();
        _logger.config("conditions validity table set to " + _conditionsTableName);
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
     * Open the database connection.
     */
    private void openConnection() {
        if (_connectionPropertiesFile == null)
            throw new RuntimeException("Connection properties were not set.");
        _connectionParameters = ConnectionParameters.fromProperties(_connectionPropertiesFile); 
        _connection = _connectionParameters.createConnection();
        _logger.config("created connection: " + _connectionParameters.getConnectionString());
    }
    
    /**
     * Close the database connection.
     */
    private void closeConnection() {
        if (_connection != null) {
            try {
                if (!_connection.isClosed()) {
                    _connection.close();
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
        _connection = null;
        _connectionParameters = null;
    }
       
    /**
     * This class loads an XML configuration of conditions table meta data.
     * 
     * @author Jeremy McCormick <jeremym@slac.stanford.edu>
     */
    class TableMetaDataLoader {

        @SuppressWarnings("unchecked")
        /**
         * This method expects an XML element containing child "table" elements.
         * @param element
         */
        void load(Element element) {

            _tableMetaData = new ArrayList<TableMetaData>();

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

                _tableMetaData.add(tableData);
            }
        }      
    }

    /**
     * This class reads in an XML configuration specifying a list of converter classes, e.g. from
     * the config file for the {@link DatabaseConditionsManager}.
     * 
     * @author Jeremy McCormick <jeremym@slac.stanford.edu>
     */
    class ConditionsConverterLoader {

        void load(Element element) {
            _converters = new ArrayList<ConditionsConverter>();
            for (Iterator iterator = element.getChildren("converter").iterator(); iterator.hasNext();) {
                Element converterElement = (Element) iterator.next();
                try {
                    Class converterClass = Class.forName(converterElement.getAttributeValue("class"));
                    if (ConditionsConverter.class.isAssignableFrom(converterClass)) {
                        try {
                            _converters.add((ConditionsConverter) converterClass.newInstance());
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
