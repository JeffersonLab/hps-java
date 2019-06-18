package org.hps.online.recon;

import java.util.logging.Logger;

import org.hps.record.et.EtConnection;
import org.jlab.coda.et.EtConstants;
import org.jlab.coda.et.EtStationConfig;
import org.jlab.coda.et.EtSystem;
import org.jlab.coda.et.EtSystemOpenConfig;
import org.jlab.coda.et.enums.Mode;

/**
 * Create a parallel ET connection appropriate for a pool of stations to 
 * process reconstruction jobs.
 * 
 * Sub-class {@link org.hps.record.et.EtConnection} so we don't accidentally 
 * screw up the monitoring application. 
 */
class EtParallelStation extends EtConnection {
    
    private Logger LOGGER = Logger.getLogger(EtParallelStation.class.getPackageName());
    
    EtParallelStation(
            final String name, 
            final String host,
            final int port, 
            final int queueSize, 
            final int prescale, 
            final String stationName, 
            final Mode waitMode, 
            final int waitTime, 
            final int chunkSize) throws Exception {

        // make a direct connection to ET system's tcp server
        final EtSystemOpenConfig etConfig = new EtSystemOpenConfig(name, host, port);
        
        // create ET system object with verbose debugging output
        sys = new EtSystem(etConfig, EtConstants.debugInfo);
        sys.open();
        
        //LOGGER.config("ET system has " + sys.getNumStations() + " stations.");
                
        // configuration of a new station
        final EtStationConfig stationConfig = new EtStationConfig();
        stationConfig.setFlowMode(EtConstants.stationParallel);
        stationConfig.setBlockMode(EtConstants.stationBlocking);
        stationConfig.setSelectMode(EtConstants.stationSelectRRobin);

        //stationConfig.compatibleParallelConfigs(group, config)

        // Set prescale.
        if (prescale > 0) {
            stationConfig.setPrescale(prescale);
        }

        int position = 1;
        int pposition = EtConstants.end;

        // Create the station.
        stat = sys.createStation(stationConfig, stationName, position, pposition);

        // attach to new station
        att = sys.attach(stat);
        
        // These are used when getting events later.
        this.waitMode = waitMode;
        this.waitTime = waitTime;
        this.chunkSize = chunkSize;
    }
    
    public void cleanup() {
        try {
            if (!this.sys.alive()) {
                return;
            }
            this.sys.detach(this.att);
            this.sys.removeStation(this.stat);
            this.sys.close();
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }
}
