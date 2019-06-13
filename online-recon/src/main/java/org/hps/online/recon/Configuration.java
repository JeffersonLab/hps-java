package org.hps.online.recon;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.jlab.coda.et.enums.Mode;

public class Configuration {
    
    private static Logger LOGGER = Logger.getLogger(Configuration.class.getPackageName());

    // No defaults provided for these.
    private String detectorName;
    private String steering;
    private Integer runNumber;
    
    // Must be provided as command line argument.
    private String outputName;
    
    private String outputDir = System.getProperty("user.dir");

    private String bufferName = "ETBuffer";
    private String host = "localhost";
    private Integer port = 11111;
    private Integer queueSize = 0;
    private Integer prescale = 0;
    private String station = "HPS_RECON";
    private Mode waitMode = Mode.SLEEP; // sleep = 0; timed = 1; async = 2
    private Integer waitTime = 999999999;    
    private Integer chunkSize = 1;

    private Integer eventSaveInterval = 1000;
    private Integer eventPrintInterval = 1;
    
    private Properties props = null;
    
    private static Options OPTIONS = new Options();
    static {
        OPTIONS.addOption(new Option("s", "station", true, "station name"));
        OPTIONS.addOption(new Option("o", "output", true, "output file name"));
        OPTIONS.addOption(new Option("d", "dir", true, "output directory"));
    }
    private CommandLineParser parser = new DefaultParser();

    Configuration(File file) {
        if (file != null) {
            load(file);
        } else {
            throw new RuntimeException("The prop file points to null.");
        }
    }
    
    Configuration() {
    }

    void load(File file) {
        LOGGER.config("Loading properties from file <" + file.getPath() + ">");
        this.props = new Properties();
        try {
            props.load(new FileInputStream(file));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        setProperties();
        LOGGER.config("Loaded properties <" + this.props.toString() + ">");
    }
    
    void parse(String args[]) throws ParseException {
        CommandLine cl = parser.parse(OPTIONS, args);
        
        // Load prop file first.
        List<String> argList = cl.getArgList();
        if (argList.size() > 1) {
            throw new RuntimeException("Too many extra arguments in " + argList.toString());
        }
        File propFile = new File(argList.get(0));
        if (!propFile.exists()) {
            throw new RuntimeException("Prop file <" + propFile.getPath() + "> does not exist.");
        }        
        load(propFile);
        
        // Process command line arguments which can override prop file settings.
        if (cl.hasOption("s")) {
            this.station = cl.getOptionValue("s");
        }
        if (cl.hasOption("d")) {
            this.outputDir = cl.getOptionValue("d");
        }
        if (cl.hasOption("o")) {
            this.outputName = cl.getOptionValue("o");
        } else {
            throw new RuntimeException("Missing required -o argument with output file name.");
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
            station = props.getProperty("et.station");
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
    
    boolean isValid() {
        if (this.detectorName == null) {
            LOGGER.severe("Config is missing detector name.");
            return false;
        }
        if (this.runNumber == null) {
            LOGGER.severe("Config is missing run number.");
            return false;
        }
        if (this.steering == null) {
            LOGGER.severe("Config is missing steering resource");
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
        return station;
    }

    Mode getMode() {
        return waitMode;
    }

    Integer getWaitTime() {
        return waitTime;
    }

    Integer getChunkSize() {
        return chunkSize;
    }
}
