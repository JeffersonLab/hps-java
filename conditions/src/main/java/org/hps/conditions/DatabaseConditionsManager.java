package org.hps.conditions;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

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
// TODO: Move query methods from ConnectionManager to this API so that ConnectionManager need not 
// be itself statically accessible.  Add access to ConnectionManager object to this class.
public class DatabaseConditionsManager extends LCSimConditionsManagerImplementation {

    static DatabaseConditionsManager _instance;
    int _runNumber = -1;
    String _detectorName;
    List<TableMetaData> _tableData;
    List<ConditionsConverter> _converters;
    ConditionsReader _baseReader;    
    static Logger _logger = null;
    ConnectionManager _connectionManager;

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
     * Setup the logger.
     */
    static {
        _logger = Logger.getLogger(DatabaseConditionsManager.class.getSimpleName());
        _logger.setUseParentHandlers(false);
        _logger.setLevel(Level.ALL);
        ConsoleHandler handler = new ConsoleHandler();
        handler.setFormatter(new LogFormatter());
        _logger.addHandler(handler);
        _logger.info("setup logger");
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

            _logger.config("setting up with detector: " + _detectorName);
            _logger.config("setting up with run number: " + _runNumber);
            // Setup the manager with the detector and run number.
            setDetector(_detectorName, _runNumber);
        } catch (ConditionsNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Set the run number. This will not trigger conditions change until {@link #setup()} is
     * called.
     * @param runNumber The new run number.
     */
    public void setRunNumber(int runNumber) {
        _runNumber = runNumber;
    }

    /**
     * Set the detector name. This will not trigger conditions change until {@link #setup()} is
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
     * Get the lcsim compact Detector object from the conditions system.
     * @return The detector object.
     */
    public Detector getDetectorObject() {
        return getCachedConditions(Detector.class, "compact.xml").getCachedData();
    }

    /**
     * Get conditions data by class and name.
     * @param type The class of the conditions.
     * @param name The name of the conditions set.
     * @return The conditions or null (???) if does not exist.
     */
    public <T> T getConditionsData(Class<T> type, String name) {
        _logger.info("getting conditions " + name + " of type " + type.getSimpleName());
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
        _logger.config("configuring from resource: " + resource);
        InputStream in = getClass().getResourceAsStream(resource);
        if (in == null)
            throw new IllegalArgumentException("The resource does not exist.");
        configure(in);
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
     * Get the next collection ID for a database table.
     * @param tableName The name of the table.
     * @return The next collection ID.
     */
    public int getNextCollectionId(String tableName) {
        TableMetaData tableData = findTableMetaData(tableName);
        if (tableData == null)
            throw new IllegalArgumentException("There is no meta data for table " + tableName);
        ResultSet resultSet = ConnectionManager.getConnectionManager().query("SELECT MAX(collection_id)+1 FROM " + tableName);
        int collectionId = -1;
        try {
            resultSet.next();
            collectionId = resultSet.getInt(1);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return collectionId;
    }

    /**
     * Get the list of table meta data.
     * @return The list of table meta data.
     */
    public List<TableMetaData> getTableMetaDataList() {
        return _tableData;
    }

    /**
     * Find a table's meta data.
     * @param name The name of the table.
     * @return The table's meta data or null if does not exist.
     */
    public TableMetaData findTableMetaData(String name) {
        for (TableMetaData meta : _tableData) {
            if (meta.getTableName().equals(name))
                return meta;
        }
        return null;
    }

    private void configure(InputStream in) {

        // Create XML document.
        Document config = createDocument(in);

        // Load the connection parameters from XML.
        loadConnectionParameters(config);

        // Load the table meta data from XML.
        loadTableMetaData(config);

        // Load the converter classes from XML.
        loadConverters(config);
    }

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

    private void loadConverters(Document config) {
        // Load the list of converters from the "converters" section of the config document.
        (this.new ConditionsConverterLoader()).load(config.getRootElement().getChild("converters"));

        // Register the list of converters with this manager.
        // FIXME: Should this happen here or when setup is called?
        for (ConditionsConverter converter : _converters) {
            registerConditionsConverter(converter);
            _logger.config("registered converter " + converter.getClass().getSimpleName() + " which handles type " + converter.getType().getSimpleName());
        }
    }

    private void loadTableMetaData(Document config) {
        // Load table meta data from the "tables" section of the config document.
        (this.new TableMetaDataLoader()).load(config.getRootElement().getChild("tables"));
    }

    private void loadConnectionParameters(Document config) {
        // Setup the connection parameters from the "connection" section of the config document.
        _connectionManager = ConnectionManager.getConnectionManager();
        _connectionManager.setConnectionParameters(
                ConnectionParameters.fromXML(config.getRootElement().getChild("connection")));
        ConnectionParameters p = _connectionManager.getConnectionParameters();
        
        _logger.config("set connection parameters ...");
        _logger.config("database: " + p.getDatabase());
        _logger.config("user: " + p.getUser());
        _logger.config("password: " + p.getPassword());
        _logger.config("hostname: " + p.getHostname());
        _logger.config("port: " + p.getPort());
        _logger.config("connection: " + p.getConnectionString());
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

            _tableData = new ArrayList<TableMetaData>();

            for (Iterator<?> iterator = element.getChildren("table").iterator(); iterator.hasNext();) {
                Element tableElement = (Element) iterator.next();
                String tableName = tableElement.getAttributeValue("name");

                // System.out.println("tableName: " + tableName);

                Element classesElement = tableElement.getChild("classes");
                Element classElement = classesElement.getChild("object");
                Element collectionElement = classesElement.getChild("collection");

                String className = classElement.getAttributeValue("class");
                String collectionName = collectionElement.getAttributeValue("class");

                // System.out.println("className: " + className);
                // System.out.println("collectionName: " + collectionName);

                Class<? extends ConditionsObject> objectClass;
                Class<?> rawObjectClass;
                try {
                    rawObjectClass = Class.forName(className);
                    // System.out.println("created raw object class: " +
                    // rawObjectClass.getSimpleName());
                    if (!ConditionsObject.class.isAssignableFrom(rawObjectClass)) {
                        throw new RuntimeException("The class " + rawObjectClass.getSimpleName() + " does not extend ConditionsObject.");
                    }
                    objectClass = (Class<? extends ConditionsObject>) rawObjectClass;
                    // System.out.println("created ConditionsObject class: " +
                    // objectClass.getSimpleName());
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }

                Class<? extends ConditionsObjectCollection<?>> collectionClass;
                Class<?> rawCollectionClass;
                try {
                    rawCollectionClass = Class.forName(collectionName);
                    // System.out.println("created raw collection class: " +
                    // rawCollectionClass.getSimpleName());
                    if (!ConditionsObjectCollection.class.isAssignableFrom(rawCollectionClass))
                        throw new RuntimeException("The class " + rawCollectionClass.getSimpleName() + " does not extend ConditionsObjectCollection.");
                    collectionClass = (Class<? extends ConditionsObjectCollection<?>>) rawCollectionClass;
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }

                TableMetaData tableData = new TableMetaData(tableName, objectClass, collectionClass);

                Element fieldsElement = tableElement.getChild("fields");

                for (Iterator<?> fieldsIterator = fieldsElement.getChildren("field").iterator(); fieldsIterator.hasNext();) {
                    Element fieldElement = (Element) fieldsIterator.next();

                    String fieldName = fieldElement.getAttributeValue("name");
                    // System.out.println("field: " + fieldName);

                    tableData.addField(fieldName);
                }

                _tableData.add(tableData);

                // System.out.println();
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
                            // System.out.println("adding converter: " +
                            // converterClass.getSimpleName());
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
