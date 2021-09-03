package org.hps.online.recon;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hps.online.recon.properties.Property;
import org.hps.record.et.EtConnection;
import org.jlab.coda.et.EtConstants;
import org.jlab.coda.et.EtStationConfig;
import org.jlab.coda.et.EtSystem;
import org.jlab.coda.et.EtSystemOpenConfig;
import org.jlab.coda.et.enums.Mode;
import org.jlab.coda.et.exception.EtClosedException;
import org.jlab.coda.et.exception.EtDeadException;
import org.jlab.coda.et.exception.EtException;
import org.jlab.coda.et.exception.EtExistsException;
import org.jlab.coda.et.exception.EtTooManyException;

/**
 * Create an ET connection for a pool of stations to process
 * reconstruction jobs in parallel.
 */
class EtParallelStation extends EtConnection {

    private static Logger LOGGER = Logger.getLogger(EtParallelStation.class.getPackage().getName());

    private static final int STATION_POSITION = 1;

    /**
     * Create an ET connection from station properties
     *
     * If there is an existing station with the same name, it will be woken
     * up and removed before a new one is created.
     *
     * @param props The station properties
     * @throws EtException
     * @throws EtTooManyException
     * @throws EtExistsException
     * @throws EtClosedException
     * @throws EtDeadException
     * @throws IOException
     * @throws Exception If there is an error opening the ET system
     */
    EtParallelStation(StationProperties props)
            throws EtException, IOException, EtDeadException,
            EtClosedException, EtExistsException, EtTooManyException {

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

        // Connection attempt loop
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

            try {
                Thread.sleep(1000L);
            } catch (InterruptedException e) {
                LOGGER.info("Connection attempt was interrupted!");
                break;
            }
        }

        // It failed :(
        if (!sys.alive()) {
            RuntimeException rte = new RuntimeException(
                    "Failed to connect to ET system after "
                    + connAttempts.value() + " attempts");
            LOGGER.log(Level.SEVERE, rte.getMessage(), rte);
            throw rte;
        }

        // See if there is an existing ET station
        try {
            stat = sys.stationNameToObject(stationName.value());
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error while attempting to find station", e);
        }

        // Raise an error if the station exists already.
        if (stat != null) {
            throw new RuntimeException("ET station already exists: " + stationName.value());
        }

        // Create the new ET station
        final EtStationConfig stationConfig = new EtStationConfig();
        stationConfig.setFlowMode(EtConstants.stationParallel);
        stationConfig.setBlockMode(EtConstants.stationBlocking);
        stationConfig.setSelectMode(EtConstants.stationSelectRRobin);
        //stationConfig.setRestoreMode(EtConstants.stationRestoreGC);
        stat = sys.createStation(stationConfig, stationName.value(), STATION_POSITION, EtConstants.end);
        if (prescale.value() > 0) {
            stationConfig.setPrescale(prescale.value());
        }

        att = sys.attach(stat);

        LOGGER.info("Created ET station: name=" + stat.getName() + "; pos=" + sys.getStationPosition(stat)
                + "; ppos: " + sys.getStationParallelPosition(stat));

        this.waitMode = Mode.getMode(waitMode.value());
        this.waitTime = waitTime.value();
        this.chunkSize = chunk.value();
    }

    /**
     * Cleanup the connection by detaching it.
     */
    public void cleanup() {
        try {
            if (sys!= null && sys.alive()) {
                sys.wakeUpAttachment(att);
                sys.detach(att);
                sys.removeStation(stat);
                sys.close();
            }
        } catch (final Exception e) {
            e.printStackTrace();
        }
        // Can't use a logger when this is called in the shutdown hook
        System.out.println("Cleaned up ET station");
    }
}
