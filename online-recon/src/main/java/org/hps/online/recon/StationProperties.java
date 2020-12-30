package org.hps.online.recon;

import org.hps.evio.LCSimEngRunEventBuilder;
//import org.hps.online.recon.properties.BooleanProperty;
import org.hps.online.recon.properties.IntegerProperty;
import org.hps.online.recon.properties.Property;
import org.hps.online.recon.properties.PropertyStore;
import org.hps.online.recon.properties.StringProperty;
import org.jlab.coda.et.EtConstants;
import org.jlab.coda.et.enums.Mode;

/**
 * Configuration properties for an online reconstruction
 * {@link org.hps.online.recon.Station}
 */
public class StationProperties extends PropertyStore {

    private final static String DIR = System.getProperty("user.dir");

    /*
    From EtConstants.java
    debugNone           = 0;
    debugSevere         = 1;
    debugError          = 2;
    debugWarn           = 3;
    debugInfo           = 4;
    */
    private final static int LEVEL = EtConstants.debugInfo;

    /*
    The wait mode for the station.
    sleep = 0
    timed = 1
    async = 2
    */
    private final static Mode MODE = Mode.SLEEP;

    private final static String BUFFER = "/tmp/ETBuffer";

    private final static String BUILDER = LCSimEngRunEventBuilder.class.getCanonicalName();

    public StationProperties() {
        this.add(new Property<?>[] {
                new StringProperty ( "lcsim.detector",        "Name of detector",               null,           true),
                new StringProperty ( "lcsim.steering",        "Steering resource or file",      null,           true),
                new IntegerProperty( "lcsim.run",             "Run number for conditions",      null,           false),
                new StringProperty(  "lcsim.remoteTreeBind",  "Remote AIDA RMI binding",        null,           true),
                new StringProperty ( "lcsim.conditions",      "Conditions URL",                 null,           false),
                new StringProperty(  "lcsim.tag",             "Conditions tag",                 null,           false),
                new StringProperty(  "lcsim.builder",         "LCIO event builder",             BUILDER,        true),
                new StringProperty ( "station.outputName",    "Base name for output files",     "output",       true),
                new StringProperty ( "station.outputDir",     "Directory for output files",     DIR,            true),
                new StringProperty(  "station.loggingConfig", "Logging config file",            null,           false),
                new StringProperty ( "et.buffer",             "Name of ET buffer file",         BUFFER,         true),
                new StringProperty ( "et.host",               "Host for ET connection",         "localhost",    true),
                new IntegerProperty( "et.port",               "Port for ET connection",         11111,          true),
                new StringProperty ( "et.stationName",        "Name of ET station",             null,           true),
                new IntegerProperty( "et.logLevel",           "ET log level",                   LEVEL,          false),
                new IntegerProperty( "et.waitTime",           "Wait time when getting events",  0,              false),
                new IntegerProperty( "et.mode",               "Mode when getting events",       MODE.ordinal(), false),
                new IntegerProperty( "et.chunk",              "Chunk size when getting events", 1,              false),
                new IntegerProperty( "et.prescale",           "Event prescale factor",          1,              false),
                new IntegerProperty( "et.connectionAttempts", "Max ET connection attempts",     10,             false)
        });
    }

    /**
     * Copy constructor
     * @param sp The source properties to copy into the new object
     */
    public StationProperties(StationProperties sp) {
        for (Property<?> p : sp.props.values()) {
            add((Property<?>) p.clone());
        }
    }
}
