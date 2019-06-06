package org.hps.online.recon;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Logger;

import org.jlab.coda.et.enums.Mode;

public class Configuration {
    
    private static Logger LOGGER = Logger.getLogger(Configuration.class.getPackageName());
    
    private String detectorName = "HPS-EngRun2015-Nominal-v5-0";
    private String steering = "/org/hps/steering/recon/EngineeringRun2015FullRecon.lcsim";
    private Integer runNumber = 5772;
    private String outputName = "online_recon_events";
    private String outputDir = System.getProperty("user.dir");

    private Integer id = 1;
    private String bufferName = "ETBuffer";
    private String host = "localhost";
    private Integer port = 11111;
    private Boolean blocking = true;
    private Integer queueSize = 0;
    private Integer prescale = 0;
    private String station = "HPS_RECON";
    private Integer position = 1;
    private Mode waitMode = Mode.TIMED;
    private Integer waitTime = 5000000;
    private Integer chunkSize = 1;

    private Properties props = null;

    Configuration(File file) {
        if (file != null) {
            load(file);
        } else {
            throw new RuntimeException("The prop file points to null.");
        }
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

    private void setProperties() {
        if (props.containsKey("lcsim.detector")) {
            detectorName = props.getProperty("lcsim.detector");
        }
        if (props.contains("lcsim.steering")) {
            steering = props.getProperty("lcsim.steering");
        }
        if (props.contains("lcsim.run")) {
            runNumber = Integer.parseInt(props.getProperty("lcsim.run"));
        }
        if (props.contains("lcsim.outputName")) {
            outputName = props.getProperty("lcsim.outputName");
        }
        if (props.contains("lcsim.outputDir")) {
            outputDir = props.getProperty("lcsim.outputDir");
        }
        if (props.contains("et.id")) {
            id = Integer.valueOf(props.getProperty("et.id"));
        }
        if (props.contains("et.name")) {
            bufferName = props.getProperty("et.name");
        }
        if (props.contains("et.host")) {
            host = props.getProperty("et.host");
        }
        if (props.contains("et.port")) {
            port = Integer.valueOf(props.getProperty("et.port"));
        }
        if (props.contains("et.blocking")) {
            blocking = Boolean.valueOf(props.getProperty("et.blocking"));
        }
        if (props.contains("et.queueSize")) {
            queueSize = Integer.valueOf(props.getProperty("et.queueSize"));
        }
        if (props.contains("et.prescale")) {
            prescale = Integer.valueOf(props.getProperty("et.prescale"));
        }
        if (props.contains("et.station")) {
            station = props.getProperty("et.station");
        }
        if (props.contains("et.position")) {
            position = Integer.valueOf(props.getProperty("et.position"));
        }
        if (props.contains("et.waitMode")) {
            waitMode = Mode.valueOf(props.getProperty("et.waitMode"));
        }
        if (props.contains("et.waitTime")) {
            waitTime = Integer.valueOf(props.getProperty("et.waitMode"));
        }
        if (props.contains("et.chunkSze")) {
            chunkSize = Integer.valueOf(props.getProperty("et.chunkSize"));
        }
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

    Integer getID() {
        return id;
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

    Boolean getBlocking() {
        return blocking;
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

    Integer getPosition() {
        return position;
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
