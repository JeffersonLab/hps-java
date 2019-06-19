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
     * This is a required parameter with no default.
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
    private Properties props = null;
    
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
        this.props = new Properties();
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
        
        if (cl.hasOption("d")) {
            this.detectorName = cl.getOptionValue("d");
        }
        
        if (cl.hasOption("s")) {
            this.steering = cl.getOptionValue("s");
        }
        
        if (cl.hasOption("r")) {
            this.runNumber = Integer.parseInt(cl.getOptionValue("r"));
        }
        
        if (cl.hasOption("n")) {
            this.stationName = cl.getOptionValue("n");
        }
        
        if (cl.hasOption("o")) {
            this.outputName = cl.getOptionValue("o");
        }
            
        if (cl.hasOption("l")) {
            this.outputDir = cl.getOptionValue("l");
        }
        
        if (cl.hasOption("P")) {
            this.eventPrintInterval = Integer.parseInt(cl.getOptionValue("P"));
        }
        
        if (cl.hasOption("e")) {
            this.eventSaveInterval = Integer.parseInt(cl.getOptionValue("e"));
        }               
        
        if (cl.hasOption("p")) {
            this.port = Integer.parseInt(cl.getOptionValue("p"));
        }
        
        if (cl.hasOption("h")) {
            this.host = cl.getOptionValue("h");
        }
    }

    private void setProperties() {
        if (props.containsKey("lcsim.detector")) {
            detectorName = props.getProperty("lcsim.detector");
        }
        if (props.containsKey("lcsim.steering")) {
            steering = props.getProperty("lcsim.steering");
        }
        if (props.containsKey("lcsim.run")) {
            runNumber = Integer.parseInt(props.getProperty("lcsim.run"));
        }
        if (props.containsKey("lcsim.eventSaveInterval")) {
            eventSaveInterval = Integer.parseInt(props.getProperty("lcsim.eventSaveInterval"));
        }
        if (props.containsKey("lcsim.eventPrintInterval")) {
            eventPrintInterval = Integer.parseInt(props.getProperty("lcsim.eventPrintInterval"));
        }
        if (props.containsKey("et.name")) {
            bufferName = props.getProperty("et.name");
        }
        if (props.containsKey("et.host")) {
            host = props.getProperty("et.host");
        }
        if (props.containsKey("et.port")) {
            port = Integer.valueOf(props.getProperty("et.port"));
        }
        if (props.containsKey("et.queueSize")) {
            queueSize = Integer.valueOf(props.getProperty("et.queueSize"));
        }
        if (props.containsKey("et.prescale")) {
            prescale = Integer.valueOf(props.getProperty("et.prescale"));
        }
        if (props.containsKey("et.station")) {
            stationName = props.getProperty("et.station");
        }
        if (props.containsKey("et.waitMode")) {
            waitMode = Mode.getMode(Integer.valueOf(props.getProperty("et.waitMode")));
        }
        if (props.containsKey("et.waitTime")) {
            waitTime = Integer.valueOf(props.getProperty("et.waitMode"));
        }
        if (props.containsKey("et.chunkSize")) {
            chunkSize = Integer.valueOf(props.getProperty("et.chunkSize"));
        }
    }
    
    /**
     * Load parameter settings from JSON data.
     * @param jo
     */
    void fromJSON(JSONObject jo) {
        if (jo.has("lcsim.detector")) {
            detectorName = jo.getString("lcsim.detector");
        }
        if (jo.has("lcsim.steering")) {
            steering = jo.getString("lcsim.steering");
        }
        if (jo.has("lcsim.run")) {
            runNumber = jo.getInt("lcsim.run");
        }
        if (jo.has("lcsim.eventSaveInterval")) {
            eventSaveInterval = jo.getInt("lcsim.eventSaveInterval");
        }
        if (jo.has("lcsim.eventPrintInterval")) {
            eventPrintInterval = jo.getInt("lcsim.eventPrintInterval");
        }
        if (jo.has("et.name")) {
            bufferName = jo.getString("et.name");
        }
        if (jo.has("et.host")) {
            host = jo.getString("et.host");
        }
        if (jo.has("et.port")) {
            port = jo.getInt("et.port");
        }
        if (jo.has("et.queueSize")) {
            queueSize = jo.getInt("et.queueSize");
        }
        if (jo.has("et.prescale")) {
            prescale = jo.getInt("et.prescale");
        }
        if (jo.has("et.station")) {
            stationName = jo.getString("et.station");
        }
        if (jo.has("et.waitMode")) {
            waitMode = Mode.getMode(jo.getInt("et.waitMode"));
        }
        if (jo.has("et.waitTime")) {
            waitTime = jo.getInt("et.waitMode");
        }
        if (jo.has("et.chunkSize")) {
            chunkSize = jo.getInt("et.chunkSize");
        }
    }
    
    JSONObject toJSON() {
        JSONObject jo = new JSONObject();
        jo.put("lcsim.detector", detectorName);
        jo.put("lcsim.steering", steering);
        jo.put("lcsim.run", runNumber);
        jo.put("lcsim.eventSaveInterval", eventSaveInterval);
        jo.put("lcsim.eventPrintInterval", eventPrintInterval);
        jo.put("et.name", bufferName);
        jo.put("et.host", host);
        jo.put("et.port", port);
        jo.put("et.station", stationName);
        jo.put("et.waitMode", waitMode);
        jo.put("et.waitTime", waitTime);
        jo.put("et.chunkSize", chunkSize);
        jo.put("et.queueSize", queueSize); 
        jo.put("et.prescale", prescale);
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
