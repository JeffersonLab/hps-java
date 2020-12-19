package org.hps.online.recon;

import java.util.logging.Logger;

import org.hps.record.et.EtConnection;
import org.jlab.coda.et.EtConstants;
import org.jlab.coda.et.EtStationConfig;
import org.jlab.coda.et.EtSystem;
import org.jlab.coda.et.EtSystemOpenConfig;

/**
 * Create an ET connection for a pool of stations to process
 * reconstruction jobs in parallel.
 */
class EtParallelStation extends EtConnection {

    private Logger LOGGER = Logger.getLogger(EtParallelStation.class.getPackage().getName());

    EtParallelStation(StationConfiguration config) throws Exception {

        LOGGER.config("Opening ET system: " + config.getHost() + config.getBufferName() + ":" + config.getPort());
        EtSystemOpenConfig etConfig =
                new EtSystemOpenConfig(config.getBufferName(), config.getHost(), config.getPort());

        for (int i = 1; i <= config.getConnectionAttempts(); i++) {
            LOGGER.config("Attempting ET connection: " + i);
            try {
                sys = new EtSystem(etConfig, config.getEtLogLevel());
                sys.open();

            } catch (Exception e) {
                e.printStackTrace();
            }
            if (sys.alive()) {
                LOGGER.config("Connection successful!");
                break;
            }

            Thread.sleep(1000L);
        }
        if (!sys.alive()) {
            LOGGER.severe("Failed to connect to ET system after "
                    + config.getConnectionAttempts() + " attempts!");
            throw new RuntimeException("Failed to connect to ET system!");
        }

        final EtStationConfig stationConfig = new EtStationConfig();
        stationConfig.setFlowMode(EtConstants.stationParallel);
        stationConfig.setBlockMode(EtConstants.stationBlocking);
        stationConfig.setSelectMode(EtConstants.stationSelectRRobin);

        if (config.getPrescale() > 0) {
            stationConfig.setPrescale(config.getPrescale());
        }

        LOGGER.config("ET station config: " + stationConfig.toString());

        // Position relative to grand central
        int position = 1;

        // Parallel position of station which is always after the last one
        int pposition = EtConstants.end;

        LOGGER.config("Creating station with position, pposition: " + position + ", " + pposition);

        stat = sys.createStation(stationConfig, config.getStation(), position, pposition);
        att = sys.attach(stat);

        this.waitMode = config.getWaitMode();
        this.waitTime = config.getWaitTime();
        this.chunkSize = config.getChunkSize();

        LOGGER.config("Station waitMode, waitTime, chunkSize: " + this.waitMode + ", " + this.waitTime +
                ", " + this.chunkSize);
    }

    /**
     * Cleanup the connection by detaching the station and removing it from the ET system.
     */
    synchronized public void cleanup() {
        LOGGER.fine("Cleaning up ET connection");
        try {
            LOGGER.fine("Checking if sys is alive");
            if (!this.sys.alive()) {
                LOGGER.fine("sys is not alive!");
                return;
            }
            LOGGER.fine("Detaching from sys");
            this.sys.detach(this.att);
            LOGGER.fine("Removing station");
            this.sys.removeStation(this.stat);
            LOGGER.fine("Closing station");
            this.sys.close();
            LOGGER.fine("Closed station");
        } catch (final Exception e) {
            LOGGER.warning("Error during cleanup");
            e.printStackTrace();
        }
        LOGGER.fine("Done with ET cleanup!");
    }
}
