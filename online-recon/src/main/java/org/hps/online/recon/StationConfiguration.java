package org.hps.online.recon;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Logger;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.jlab.coda.et.enums.Mode;
import org.json.JSONObject;

/**
 * Configuration parameters for an online reconstruction ET station.
 * 
 * @author jeremym
 */
final class StationConfiguration {
    
    private static final String CHUNK_SIZE_PROPERTY = "et.chunkSize";

    private static final String WAIT_TIME_PROPERTY = "et.waitTime";

    private static final String WAIT_MODE_PROPERTY = "et.waitMode";

    private static final String STATION_PROPERTY = "et.station";

    private static final String PRESCALE_PROPERTY = "et.prescale";

    private static final String QUEUE_SIZE_PROPERTY = "et.queueSize";

    private static final String PORT_PROPERTY = "et.port";

    private static final String HOST_PROPERTY = "et.host";

    private static final String ET_NAME_PROPERTY = "et.name";

    private static final String EVENT_PRINT_INTERVAL_PROPERTY = "lcsim.eventPrintInterval";

    private static final String EVENT_SAVE_INTERVAL_PROPERTY = "lcsim.eventSaveInterval";

    private static final String RUN_PROPERTY = "lcsim.run";

    private static final String STEERING_PROPERTY = "lcsim.steering";

    private static final String DETECTOR_PROPERTY = "lcsim.detector";

    private static Logger LOGGER = Logger.getLogger(StationConfiguration.class.getPackageName());

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
     * It cannot be set by property file.
     */
    private String outputName;
    
    /**
     * Directory for writing output files such as AIDA plots.
     * The default is the process' working directory.
     * It cannot be set by property file.
     */
    private String outputDir = System.getProperty("user.dir");

    /**
     * Event saving interval for intermediate outputs such as AIDA plots.
     * Default is saving every 1000 processed events.
     */
    private Integer eventSaveInterval = 1000;
    
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
     * This cannot be set via the command line.
     */
    private Mode waitMode = Mode.SLEEP; // sleep = 0; timed = 1; async = 2
    
    /**
     * The wait time when timed mode is selected.
     * This cannot be set via the command line.
     */
    private Integer waitTime = 999999999;
    
    /**
     * The chunk size when getting ET events.
     * This cannot be set via the command line.
     */
    private Integer chunkSize = 1;
    
    /**
     * The number of events to queue at once.
     * This cannot be set via the command line.
     */
    private Integer queueSize = 0;
    
    /**
     * The event prescale parameter.
     * This cannot be set via the command line.
     */
    private Integer prescale = 0;
    
    /**
     * The backing properties for the station configuration.
     */
    private Properties props = new Properties();;
    
    /**
     * The valid command line options for reading in configuration from the command line.
     */
    private static Options OPTIONS = new Options();
        
    /**
     * Set the command line options.
     */
    static {
        OPTIONS.addOption(new Option("d", "detector", true, "detector name"));
        OPTIONS.addOption(new Option("s", "steering", true, "steering resource"));
        OPTIONS.addOption(new Option("r", "run", true, "run number"));
        OPTIONS.addOption(new Option("n", "station", true, "station name"));
        OPTIONS.addOption(new Option("o", "output", true, "output file name"));
        OPTIONS.addOption(new Option("l", "dir", true, "output directory"));
        OPTIONS.addOption(new Option("P", "print", true, "event print interval"));
        OPTIONS.addOption(new Option("e", "event", true, "event save interval (AIDA)"));
        OPTIONS.addOption(new Option("h", "host", true, "ET host name"));
        OPTIONS.addOption(new Option("p", "port", true, "ET port name"));
        OPTIONS.addOption(new Option("help", false, "help"));
    }
    
    StationConfiguration(File file) {
        if (file != null) {
            load(file);
        } else {
            throw new RuntimeException("The prop file points to null.");
        }
    }
    
    StationConfiguration() {        
    }
        
    void load(File file) {
        LOGGER.config("Loading properties from file: " + file.getPath());
        try {
            props.load(new FileInputStream(file));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        setProperties();
        LOGGER.config("Loaded properties: " + this.props.toString());
    }
   
    void parse(String args[]) throws ParseException {
            
        CommandLineParser parser = new DefaultParser();
        CommandLine cl = parser.parse(OPTIONS, args);
    
        // If help option is present then program will print usage and exit!
        if (cl.hasOption("help")) {
            HelpFormatter help = new HelpFormatter();
            help.printHelp("[class]", "configure online recon command line options", OPTIONS, "");
            System.exit(0);
        }
        
        if (cl.hasOption("o")) {
            this.outputName = cl.getOptionValue("o");
        }
            
        if (cl.hasOption("l")) {
            this.outputDir = cl.getOptionValue("l");
        }
       
        if (cl.hasOption("d")) {
            props.setProperty(DETECTOR_PROPERTY, cl.getOptionValue("d"));
        }
        
        if (cl.hasOption("s")) {
            props.setProperty(STEERING_PROPERTY, cl.getOptionValue("s"));
        }
        
        if (cl.hasOption("r")) {
            props.setProperty(RUN_PROPERTY, cl.getOptionValue("r"));
        }
        
        if (cl.hasOption("n")) {
            props.setProperty(STATION_PROPERTY, cl.getOptionValue("n"));
        }
                
        if (cl.hasOption("P")) {
            props.setProperty(EVENT_PRINT_INTERVAL_PROPERTY, cl.getOptionValue("P"));
        }
        
        if (cl.hasOption("e")) {
            props.setProperty(EVENT_SAVE_INTERVAL_PROPERTY, cl.getOptionValue("e"));
        }               
        
        if (cl.hasOption("p")) {
            props.setProperty(PORT_PROPERTY, cl.getOptionValue("p"));
        }
        
        if (cl.hasOption("h")) {
            props.setProperty(HOST_PROPERTY, cl.getOptionValue("h"));
        }
        
        setProperties();
    }

    /**
     * Set typed variables from property keys and values.
     * 
     * This is called after properties are loaded from command line,
     * properties file, or JSON data.
     */
    private void setProperties() {
        if (props.containsKey(DETECTOR_PROPERTY)) {
            detectorName = props.getProperty(DETECTOR_PROPERTY);
        }
        if (props.containsKey(STEERING_PROPERTY)) {
            steering = props.getProperty(STEERING_PROPERTY);
        }
        if (props.containsKey(RUN_PROPERTY)) {
            runNumber = Integer.parseInt(props.getProperty(RUN_PROPERTY));
        }
        if (props.containsKey(EVENT_SAVE_INTERVAL_PROPERTY)) {
            eventSaveInterval = Integer.parseInt(props.getProperty(EVENT_SAVE_INTERVAL_PROPERTY));
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
    }
    
    /**
     * Load parameter settings from JSON data.
     * @param jo
     */
    void fromJSON(JSONObject jo) {
        for (String key : jo.keySet()) {
            this.props.setProperty(key, jo.get(key).toString());
        }
        this.setProperties();
    }

    /**
     * Convert properties to JSON.
     * @return
     */
    JSONObject toJSON() {
        JSONObject jo = new JSONObject();
        for (Object ko : this.props.keySet()) {
            jo.put((String) ko, this.props.get(ko));
        }
        return jo;
    }
    
    boolean isValid() {
        if (this.detectorName == null) {
            LOGGER.severe("Detector name was not set.");
            return false;
        }
        if (this.runNumber != null) {
            if (this.runNumber < 0) {
                LOGGER.severe("Bad run number: " + this.runNumber);
                return false;
            }
        }
        if (this.steering == null) {
            LOGGER.severe("Steering resource was not set.");
            return false;
        }
        if (this.outputName == null) {
            LOGGER.severe("Output file name was not set.");
            return false;
        }
        if (this.stationName == null) {
            LOGGER.severe("Station name was not set.");
            return false;
        }
        return true;
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
    
    Integer getEventSaveInterval() {
        return this.eventSaveInterval;
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
}
