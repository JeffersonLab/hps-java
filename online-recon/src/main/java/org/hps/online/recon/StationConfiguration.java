package org.hps.online.recon;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Logger;

import org.jlab.coda.et.EtConstants;
import org.jlab.coda.et.enums.Mode;
import org.json.JSONObject;

/**
 * Configuration parameters for an online reconstruction ET station.
 * 
 * These are key-value pairs that can be loaded from a properties file
 * or a JSON object.
 * 
 * @author jeremym
 */
final class StationConfiguration {

    /*
     * Property name definitions.    
     */    
    static final String RUN_PROPERTY = "lcsim.run";

    static final String STEERING_PROPERTY = "lcsim.steering";

    static final String DETECTOR_PROPERTY = "lcsim.detector";
    
    static final String STATION_PROPERTY = "station.name";

    static final String EVENT_PRINT_INTERVAL_PROPERTY = "station.eventPrintInterval";

    static final String PLOT_SAVE_INTERVAL_PROPERTY = "station.plotSaveInterval";

    static final String EVENT_STATISTICS_INTERVAL_PROPERTY = "station.eventStatisticsInterval";

    static final String PLOT_RESET_PROPERTY = "station.resetPlots";

    static final String OUTPUT_DIR_PROPERTY = "station.outputDir";
    
    static final String OUTPUT_NAME_PROPERTY = "station.outputName";
    
    static final String PRINT_LCIO_PROPERTY = "station.printLcio";
    
    static final String PRINT_EVIO_PROPERTY = "station.printEvio";
    
    static final String PRINT_ET_PROPERTY = "station.printEt";
    
    static final String CONNECTION_ATTEMPTS_PROPERTY = "station.connectionAttempts";
        
    static final String CHUNK_SIZE_PROPERTY = "et.chunkSize";

    static final String WAIT_TIME_PROPERTY = "et.waitTime";

    static final String WAIT_MODE_PROPERTY = "et.waitMode";

    static final String PRESCALE_PROPERTY = "et.prescale";

    static final String QUEUE_SIZE_PROPERTY = "et.queueSize";

    static final String PORT_PROPERTY = "et.port";

    static final String HOST_PROPERTY = "et.host";

    static final String ET_NAME_PROPERTY = "et.name";
    
    static final String ET_LOG_LEVEL_PROPERTY = "et.logLevel";
                
    /**
     * Package logger.
     */
    private static Logger LOGGER = Logger.getLogger(StationConfiguration.class.getPackage().getName());

    /** 
     * Name of detector for conditions system.  
     * This is a required parameter with no default.
     */
    private String detectorName;
    
    /**
     * Path of steering resource in the runnable jar.
     * This is a required parameter with no default. 
     */
    private String steering;
    
    /**
     * Run number for conditions system.
     * If left null then run number will be read from EVIO files.
     */
    private Integer runNumber;
    
    /**
     * Name for output files such as LCIO events.
     * This is a required parameter with no default.
     */
    private String outputName;
    
    /**
     * Directory for writing output files such as AIDA plots.
     * The default is the process' working directory.
     */
    private String outputDir = System.getProperty("user.dir");

    /**
     * Event interval for periodically saving plots.
     * Default is saving every 1000 processed events.
     */
    private Integer plotSaveInterval = 1000;
    
    /**
     * Event printing interval.
     * Default is printing a message for every event.
     */
    private Integer eventPrintInterval = 1;
   
    /**
     * The name of the ET buffer file with default.
     */
    private String bufferName = "ETBuffer";
    
    /**
     * The name of the ET system host with default.
     */
    private String host = "localhost";
    
    /**
     * The ET system port number with default.
     */
    private Integer port = 11111;

    /**
     * The complete name of the ET station which should be unique for the system.
     * This is a required argument with no default.
     */
    private String stationName;
        
    /**
     * The wait mode which is for the station to sleep forever until events arrive.
     */
    private Mode waitMode = Mode.SLEEP; // sleep = 0; timed = 1; async = 2
    
    /**
     * The wait time when timed mode is selected.
     */
    private Integer waitTime = 999999999;
    
    /**
     * The chunk size when getting ET events.
     */
    private Integer chunkSize = 1;
    
    /**
     * The number of events to queue at once.
     */
    private Integer queueSize = 0;
    
    /**
     * The event prescale parameter.
     */
    private Integer prescale = 0;

    /**
     * The backing properties for the station configuration.
     */
    private Properties props = new Properties();

    /**
     * Log level of the ET system.
     */
    private int etLogLevel = EtConstants.debugInfo;
    // From EtConstants.java
    /*
    public static final int    debugNone           = 0;
    public static final int    debugSevere         = 1;
    public static final int    debugError          = 2;
    public static final int    debugWarn           = 3;
    public static final int    debugInfo           = 4;
    */
        
    /**
     * True to reset plots after they are saved.
     * This should be enabled if pre-existing target plot file
     * is being included in the hadd command.
     */
    private boolean resetPlots = false;
    
    /**
     * True to print out job statistics with event interval.
     * A value below 0 means no statistics will be enabled.
     */
    private int eventStatisticsInterval = -1;
    
    /**
     * Whether to print LCIO event information.
     */
    boolean printLcio = false;
    
    /**
     * Whether to print EVIO event information.
     */
    boolean printEvio = false;
    
    /**
     * Whether to print ET event information.
     */
    boolean printEt = false;
    
    /**
     * Number of times to try and connect to ET system before failing.
     */
    Integer connectionAttempts = 1;
    
    /**
     * Create station configuration from a properties file.
     * @param file The properties file
     */
    StationConfiguration(File file) {
        if (file != null) {
            load(file);
        } else {
            throw new RuntimeException("The prop file points to null.");
        }
    }
    
    /**
     * Copy constructor.
     * @param config Existing configuration object
     */
    StationConfiguration(StationConfiguration config) {        
        this.props = (Properties) config.props.clone();
        update();
    }
    
    /**
     * No argument constructor.
     */
    StationConfiguration() {
    }
        
    /**
     * Get the backing properties.
     * @return The backing properties
     */
    Properties getProperties() {
        return this.props;
    }
    
    /**
     * Set a property value.
     * @param name The name of the property
     * @param value The value of the property
     */
    void setProperty(String name, String value) {
        this.props.setProperty(name, value);
    }
    
    /**
     * Write properties to a file.
     * @param file The output file
     * @param comment The comment (can be null)
     * @throws FileNotFoundException If file is not found
     * @throws IOException If there is an IO error
     */
    void write(File file, String comment) throws FileNotFoundException, IOException {
        this.props.store(new FileOutputStream(file), comment);
    }
            
    /**
     * Load configuration from a properties file.
     * @param file The properties filec
     */
    void load(File file) {
        LOGGER.config("Loading properties from file: " + file.getPath());
        try {
            props.load(new FileInputStream(file));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        update();
        LOGGER.config("Loaded properties: " + this.props.toString());
    }

    /**
     * Set typed variables from property keys and values.
     * 
     * This is called after properties are loaded from properties file or JSON data.
     * 
     * It should be called manually after any property values are added or changed.
     */
    void update() {
        if (props.containsKey(DETECTOR_PROPERTY)) {
            detectorName = props.getProperty(DETECTOR_PROPERTY);
        }
        if (props.containsKey(STEERING_PROPERTY)) {
            steering = props.getProperty(STEERING_PROPERTY);
        }
        if (props.containsKey(RUN_PROPERTY)) {
            runNumber = Integer.parseInt(props.getProperty(RUN_PROPERTY));
        }
        if (props.containsKey(PLOT_SAVE_INTERVAL_PROPERTY)) {
            plotSaveInterval = Integer.parseInt(props.getProperty(PLOT_SAVE_INTERVAL_PROPERTY));
        }
        if (props.containsKey(EVENT_PRINT_INTERVAL_PROPERTY)) {
            eventPrintInterval = Integer.parseInt(props.getProperty(EVENT_PRINT_INTERVAL_PROPERTY));
        }
        if (props.containsKey(ET_NAME_PROPERTY)) {
            bufferName = props.getProperty(ET_NAME_PROPERTY);
        }
        if (props.containsKey(HOST_PROPERTY)) {
            host = props.getProperty(HOST_PROPERTY);
        }
        if (props.containsKey(PORT_PROPERTY)) {
            port = Integer.valueOf(props.getProperty(PORT_PROPERTY));
        }
        if (props.containsKey(QUEUE_SIZE_PROPERTY)) {
            queueSize = Integer.valueOf(props.getProperty(QUEUE_SIZE_PROPERTY));
        }
        if (props.containsKey(PRESCALE_PROPERTY)) {
            prescale = Integer.valueOf(props.getProperty(PRESCALE_PROPERTY));
        }
        if (props.containsKey(STATION_PROPERTY)) {
            stationName = props.getProperty(STATION_PROPERTY);
        }
        if (props.containsKey(WAIT_MODE_PROPERTY)) {
            waitMode = Mode.getMode(Integer.valueOf(props.getProperty(WAIT_MODE_PROPERTY)));
        }
        if (props.containsKey(WAIT_TIME_PROPERTY)) {
            waitTime = Integer.valueOf(props.getProperty(WAIT_MODE_PROPERTY));
        }
        if (props.containsKey(CHUNK_SIZE_PROPERTY)) {
            chunkSize = Integer.valueOf(props.getProperty(CHUNK_SIZE_PROPERTY));
        }
        if (props.containsKey(OUTPUT_DIR_PROPERTY)) {
            outputDir = props.getProperty(OUTPUT_DIR_PROPERTY);
        }
        if (props.containsKey(OUTPUT_NAME_PROPERTY)) {
            outputName = props.getProperty(OUTPUT_NAME_PROPERTY);
        }
        if (props.containsKey(STATION_PROPERTY)) {
            stationName = props.getProperty(STATION_PROPERTY);
        }
        if (props.containsKey(ET_LOG_LEVEL_PROPERTY)) {
            etLogLevel = Integer.parseInt(props.getProperty(ET_LOG_LEVEL_PROPERTY));
        }
        if (props.containsKey(EVENT_STATISTICS_INTERVAL_PROPERTY)) {
            eventStatisticsInterval = Integer.parseInt(props.getProperty(EVENT_STATISTICS_INTERVAL_PROPERTY));
        }        
        if (props.containsKey(PLOT_RESET_PROPERTY)) {
            resetPlots = Boolean.parseBoolean(PLOT_RESET_PROPERTY);
        }
        if (props.containsKey(PRINT_LCIO_PROPERTY)) {
            printLcio = Boolean.parseBoolean(props.getProperty(PRINT_LCIO_PROPERTY));
        }
        if (props.containsKey(PRINT_EVIO_PROPERTY)) {
            printEvio = Boolean.parseBoolean(props.getProperty(PRINT_EVIO_PROPERTY));
        }
        if (props.containsKey(PRINT_ET_PROPERTY)) {
            printEt = Boolean.parseBoolean(props.getProperty(PRINT_ET_PROPERTY));
        }
        if (props.containsKey(CONNECTION_ATTEMPTS_PROPERTY)) {
            connectionAttempts = Integer.parseInt(props.getProperty(CONNECTION_ATTEMPTS_PROPERTY));
        }
    }
    
    /**
     * Load parameter settings from JSON data.
     * @param jo The JSON object
     */
    void fromJSON(JSONObject jo) {
        for (String key : jo.keySet()) {
            this.props.setProperty(key, jo.get(key).toString());
        }
        update();
    }

    /**
     * Convert properties to JSON.
     * @return The converted JSON object
     */
    JSONObject toJSON() {
        JSONObject jo = new JSONObject();
        for (Object ko : this.props.keySet()) {
            jo.put((String) ko, this.props.get(ko));
        }
        return jo;
    }
    
    /**
     * Check if required parameters are set and look valid.
     * @return True if configuration looks valid
     */
    boolean isValid() {
        boolean okay = true;
        if (this.detectorName == null) {
            LOGGER.severe("Detector name was not set.");
            okay = false;
        }
        if (this.runNumber != null) {
            if (this.runNumber < 0) {
                LOGGER.severe("Bad run number: " + this.runNumber);
                okay = false;
            }
        }
        if (this.steering == null) {
            LOGGER.severe("Steering resource was not set.");
            okay = false;
        }
        if (this.outputName == null) {
            LOGGER.severe("Output file name was not set.");
            okay = false;
        }
        if (this.stationName == null) {
            LOGGER.severe("Station name was not set.");
            okay = false;
        }
        if (this.etLogLevel < EtConstants.debugNone || this.etLogLevel > EtConstants.debugInfo) {
            LOGGER.severe("ET log level is not valid: " + this.etLogLevel);
            okay = false;
        }
        if (this.connectionAttempts < 0) {
            LOGGER.severe("Connection attempts is not valid (must be > 0): " + this.connectionAttempts);
            okay = false;
        }
        return okay;
    }

    String getDetectorName() {
        return detectorName;
    }

    String getSteeringResource() {
        return steering;
    }

    Integer getRunNumber() {
        return runNumber;
    }

    String getOutputName() {
        return outputName;
    }

    String getOutputDir() {
        return outputDir;
    }
    
    Integer getPlotSaveInterval() {
        return this.plotSaveInterval;
    }
    
    Integer getEventPrintInterval() {
        return this.eventPrintInterval;
    }
    
    String getBufferName() {
        return bufferName;
    }

    String getHost() {
        return host;
    }

    Integer getPort() {
        return port;
    }

    Integer getQueueSize() {
        return queueSize;
    }

    Integer getPrescale() {
        return prescale;
    }

    String getStation() {
        return stationName;
    }

    Mode getWaitMode() {
        return waitMode;
    }

    Integer getWaitTime() {
        return waitTime;
    }

    Integer getChunkSize() {
        return chunkSize;
    }
    
    int getEtLogLevel() {
        return etLogLevel;
    }
    
    int getEventStatisticsInterval() {
        return eventStatisticsInterval;
    }
    
    Boolean getResetPlots() {
        return resetPlots;
    }
    
    Boolean getPrintLcio() {
        return this.printLcio;
    }
    
    Boolean getPrintEvio() {
        return this.printEvio;
    }
    
    Boolean getPrintEt() {
        return this.printEt;
    }
    
    Integer getConnectionAttempts() {
        return this.connectionAttempts;
    }
}
