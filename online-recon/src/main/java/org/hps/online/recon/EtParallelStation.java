package org.hps.online.recon;

import java.util.logging.Logger;

import org.hps.online.recon.properties.Property;
import org.hps.record.et.EtConnection;
import org.jlab.coda.et.EtConstants;
import org.jlab.coda.et.EtStationConfig;
import org.jlab.coda.et.EtSystem;
import org.jlab.coda.et.EtSystemOpenConfig;
import org.jlab.coda.et.enums.Mode;

/**
 * Create an ET connection for a pool of stations to process
 * reconstruction jobs in parallel.
 */
class EtParallelStation extends EtConnection {

    private Logger LOGGER = Logger.getLogger(EtParallelStation.class.getPackage().getName());

    private static final int STATION_POSITION = 1;

    /**
     * Create an ET connection from station properties
     * @param props The station properties
     * @throws Exception If there is an error opening the ET system
     */
    EtParallelStation(StationProperties props) throws Exception {

        Property<String> host = props.get("et.host");
        Property<String> buffer = props.get("et.buffer");
        Property<Integer> port = props.get("et.port");
        Property<Integer> connAttempts = props.get("et.connectionAttempts");
        Property<Integer> logLevel = props.get("et.logLevel");
        Property<Integer> prescale = props.get("et.prescale");
        Property<String> stationName = props.get("et.stationName");
        Property<Integer> waitMode = props.get("et.mode");
        Property<Integer> waitTime = props.get("et.waitTime");
        Property<Integer> chunk = props.get("et.chunk");

        LOGGER.config("Opening ET system: " + host.value() + ":" + port.value() + buffer.value());
        EtSystemOpenConfig etConfig =
                new EtSystemOpenConfig(buffer.value(), host.value(), port.value());

        for (int i = 1; i <= connAttempts.value(); i++) {
            LOGGER.config("Attempting ET connection: " + i);
            try {
                sys = new EtSystem(etConfig, logLevel.value());
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
                    + connAttempts.value() + " attempts!");
            throw new RuntimeException("Failed to connect to ET system!");
        }

        final EtStationConfig stationConfig = new EtStationConfig();
        stationConfig.setFlowMode(EtConstants.stationParallel);
        stationConfig.setBlockMode(EtConstants.stationBlocking);
        stationConfig.setSelectMode(EtConstants.stationSelectRRobin);

        if (prescale.value() > 0) {
            stationConfig.setPrescale(prescale.value());
        }

        stat = sys.createStation(stationConfig, stationName.value(), STATION_POSITION, EtConstants.end);
        att = sys.attach(stat);

        LOGGER.info("Initialized station: " + stat.getName());
        LOGGER.info("Station pos: " + sys.getStationPosition(stat));
        LOGGER.info("Station parallel pos: " + sys.getStationParallelPosition(stat));
        LOGGER.info("Num stations: " + sys.getNumStations());

        this.waitMode = Mode.getMode(waitMode.value());
        this.waitTime = waitTime.value();
        this.chunkSize = chunk.value();

        LOGGER.config("Station waitMode, waitTime, chunkSize: " + this.waitMode + ", " + this.waitTime +
                ", " + this.chunkSize);
    }

    /**
     * Cleanup the connection by detaching the station and removing it from the ET system.
     */
    synchronized public void cleanup() {
        LOGGER.info("Cleaning up ET connection");
        try {
            LOGGER.fine("Checking if sys is alive");
            if (!this.sys.alive()) {
                LOGGER.fine("sys is not alive!");
                return;
            }
            LOGGER.fine("Waking up attachment");
            try {
                this.sys.wakeUpAttachment(att);
            } catch (Exception e) {
                e.printStackTrace();
            }
            LOGGER.fine("Detaching from sys");
            this.sys.detach(this.att);
            LOGGER.fine("Removing station");
            this.sys.removeStation(this.stat);
            LOGGER.fine("Closing station");
            this.sys.close();
        } catch (final Exception e) {
            LOGGER.warning("Error during cleanup");
            e.printStackTrace();
        }
        LOGGER.info("Done cleaning up ET connection!");
    }
}
