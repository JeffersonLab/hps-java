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
    
    private Logger LOGGER = Logger.getLogger(EtParallelStation.class.getPackage().getName());
    
    /**
     * Class constructor.
     * @param name The file buffer name
     * @param host The ET system hostname
     * @param port The ET system port
     * @param queueSize The queue size when reading events
     * @param prescale The prescale factor
     * @param stationName The name of the new station
     * @param waitMode The wait mode (see ET documentation)
     * @param waitTime The wait time if using timed mode
     * @param chunkSize The chunk size when reading events
     * @param logLevel The ET system log level
     * @throws Exception If there is an error initializing and connecting to the ET system
     */
    EtParallelStation(
            final String name, 
            final String host,
            final int port, 
            final int queueSize, 
            final int prescale, 
            final String stationName, 
            final Mode waitMode, 
            final int waitTime, 
            final int chunkSize,
            final int logLevel) throws Exception {

        // make a direct connection to ET system's tcp server
        final EtSystemOpenConfig etConfig = new EtSystemOpenConfig(name, host, port);
        
        // create ET system object with verbose debugging output
        sys = new EtSystem(etConfig, logLevel);
        sys.open();
                        
        // configuration of a new station
        final EtStationConfig stationConfig = new EtStationConfig();
        stationConfig.setFlowMode(EtConstants.stationParallel);
        stationConfig.setBlockMode(EtConstants.stationBlocking);
        stationConfig.setSelectMode(EtConstants.stationSelectRRobin);
        
        // Set prescale.
        if (prescale > 0) {
            stationConfig.setPrescale(prescale);
        }

        // Position relative to grand central.
        int position = 1;
        
        // Parallel position of station which is always after the last one.
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
    
    /**
     * Cleanup the connection by detaching the station and removing it from the ET system.
     */
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
