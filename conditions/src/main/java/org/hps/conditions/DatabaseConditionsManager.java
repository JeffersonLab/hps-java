package org.hps.conditions;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.lcsim.conditions.ConditionsConverter;
import org.lcsim.conditions.ConditionsManager;
import org.lcsim.conditions.readers.BaseClasspathConditionsReader;
import org.lcsim.geometry.Detector;
import org.lcsim.util.loop.LCSimConditionsManagerImplementation;

/**
 * This class should be used as the top-level ConditionsManager for HPS when legacy access to text
 * conditions is not needed.
 */
// TODO: Add a method to set the "base" reader for resource or file-based conditions, instead
// of hard-coding to BaseClasspathConditionsReader.
public class DatabaseConditionsManager extends LCSimConditionsManagerImplementation {

    static DatabaseConditionsManager _instance;
    int _runNumber = -1;
    String _detectorName;    
    ConnectionManager _connectionManager;
    ConditionsTableMetaDataXMLLoader _loader;
    ConverterXMLLoader _converters;

    public DatabaseConditionsManager() {
    }
    
    /**
     * Create a static instance of this class and register it as the default conditions manager.
     */
    public static DatabaseConditionsManager createInstance() {
        _instance = new DatabaseConditionsManager();
        ConditionsManager.setDefaultConditionsManager(_instance); // FIXME: This probably should not be called here.
        return _instance;
    }

    /**
     * Perform setup for the current detector and run number.
     */
    public void setup() {
        try {
            // Setup a reader to handle both jar based detector resources (given first priority) 
            // and database conditions via the DatabaseConditionsReader.
            try {
                // FIXME: Make the reader passed to the DataConditionsReader configurable via public method or XML arg.
                setConditionsReader(new DatabaseConditionsReader(
                        new BaseClasspathConditionsReader( _detectorName)), 
                        _detectorName);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            
            // Setup the manager with the detector and run number.
            setDetector(_detectorName, _runNumber);
        } catch (ConditionsNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
    
    public void setRunNumber(int runNumber) {
        _runNumber = runNumber;
    }

    public void setDetectorName(String detectorName) {
        _detectorName = detectorName;
    }
    
    public int getRunNumber() {
        return _runNumber;
    }

    public String getDetectorName() {
        return this.getDetector();
    }
    
    public Detector getDetectorObject() {
        return getCachedConditions(Detector.class, "compact.xml").getCachedData();
    }
    
    public <T> T getConditionsData(Class<T> klass, String name) {
        return getCachedConditions(klass, name).getCachedData();
    }
    
    /**
     * Configure this object from an XML file.
     * @param file The XML file.
     */
    public void configure(File file) {                        
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
        configure(getClass().getResourceAsStream(resource)); 
    }
    
    List<ConditionsTableMetaData> getTableMetaDataList() {
        return _loader.getTableMetaDataList();
    }
    
    ConditionsTableMetaData findTableMetaData(String name) {
        return _loader.findTableMetaData(name);
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
        _converters = new ConverterXMLLoader();
        _converters.load(config.getRootElement().getChild("converters"));
        
        // Register the converters with this manager.
        for (ConditionsConverter converter : _converters.getConverterList()) {
            registerConditionsConverter(converter);
        }
    }

    private void loadTableMetaData(Document config) {
        // Load table meta data from the "tables" section of the config document.
        _loader = new ConditionsTableMetaDataXMLLoader();
        _loader.load(config.getRootElement().getChild("tables"));
    }

    private void loadConnectionParameters(Document config) {
        // Setup the connection parameters from the "connection" section of the config document.
        ConnectionManager.getConnectionManager().setConnectionParameters(
                ConnectionParameters.fromXML(config.getRootElement().getChild("connection")));
    }
}
