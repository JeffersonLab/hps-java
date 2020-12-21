package org.hps.online.recon;

import org.hps.online.recon.properties.IntegerProperty;
import org.hps.online.recon.properties.Property;
import org.hps.online.recon.properties.PropertyStore;
import org.hps.online.recon.properties.StringProperty;
import org.jlab.coda.et.EtConstants;
import org.jlab.coda.et.enums.Mode;

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

    private final static String BUFF = "/tmp/ETBuffer";

    public StationProperties() {
        this.add(new Property<?>[] {
            new StringProperty ( "lcsim.detector",        "Name of detector",               null,           true),
            new StringProperty ( "lcsim.steering",        "Steering resource or file",      null,           true),
            new IntegerProperty( "lcsim.run",             "Run number for conditions",      null,           false),
            new IntegerProperty( "lcsim.printInterval",   "Event print interval",           1,              false),
            new IntegerProperty( "lcsim.remoteAidaPort",  "Port for remote AIDA",           2001,           true),
            new StringProperty ( "lcsim.conditions",      "Conditions URL",                 null,           false),
            new StringProperty ( "station.outputName",    "Base name for output files",     "output",       true),
            new StringProperty ( "station.outputDir",     "Directory for output files",     DIR,            true),
            new IntegerProperty( "station.queueSize",     "Size of event queue",            1,              false),
            new StringProperty ( "et.buffer",             "Name of ET buffer file",         BUFF,           true),
            new StringProperty ( "et.host",               "Host for ET connection",         "localhost",    true),
            new IntegerProperty( "et.port",               "Port for ET connection",         11111,          true),
            new StringProperty ( "et.stationName",        "Name of ET station",             null,           true),
            new IntegerProperty( "et.logLevel",           "ET log level",                   LEVEL,          false),
            new IntegerProperty( "et.waitTime",           "Wait time when getting events",  0,              false),
            new IntegerProperty( "et.mode",               "Mode when getting events",       MODE.ordinal(), false),
            new IntegerProperty( "et.chunk",              "Chunk size when getting events", 1,              false),
            new IntegerProperty( "et.prescale",           "Event prescale factor",          1,              false),
            new IntegerProperty( "et.connectionAttempts", "ET connection attempts",         10,             false)
        });
    }
}
