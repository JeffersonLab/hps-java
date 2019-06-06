package org.hps.online.recon;

import org.hps.record.et.EtConnection;
import org.jlab.coda.et.EtConstants;
import org.jlab.coda.et.EtStationConfig;
import org.jlab.coda.et.EtSystem;
import org.jlab.coda.et.EtSystemOpenConfig;
import org.jlab.coda.et.enums.Mode;

// Sub-class EtConnection so we don't accidentally screw up the monitoring application.  --JM
public class EtParallelStation extends EtConnection {

    //static Logger LOGGER = Logger.getLogger(EtParallelStation.class.getPackageName());
    
    public EtParallelStation(
            final String name, 
            final String host,
            final int port, 
            final boolean blocking,
            final int queueSize, 
            final int prescale, 
            final String stationName, 
            final int stationPosition,
            final Mode waitMode, 
            final int waitTime, 
            final int chunkSize) throws Exception {

        // make a direct connection to ET system's tcp server
        final EtSystemOpenConfig etConfig = new EtSystemOpenConfig(name, host, port);

        // create ET system object with verbose debugging output
        sys = new EtSystem(etConfig, EtConstants.debugInfo);
        sys.open();

        // configuration of a new station
        final EtStationConfig stationConfig = new EtStationConfig();
        stationConfig.setFlowMode(EtConstants.stationParallel);
        if (!blocking) {
            stationConfig.setBlockMode(EtConstants.stationNonBlocking);
            if (queueSize > 0) {
                // Is this only applicable for non-blocking???
                stationConfig.setCue(queueSize);
            }
        } else {
            stationConfig.setBlockMode(EtConstants.stationBlocking);
            // Need to set queue size???
        }
        // Set prescale.
        if (prescale > 0) {
            // System.out.println("setting prescale to " + cn.prescale);
            stationConfig.setPrescale(prescale);
        }

        // Create the station.
        //LOGGER.config("Creating ET station: " + stationName);
        stat = sys.createStation(stationConfig, stationName, stationPosition);
        
        // attach to new station
        att = sys.attach(stat);

        this.waitMode = waitMode;
        this.waitTime = waitTime;
        this.chunkSize = chunkSize;
    }
}
