package org.hps.record.et;

import java.io.IOException;

import org.jlab.coda.et.EtAttachment;
import org.jlab.coda.et.EtConstants;
import org.jlab.coda.et.EtEvent;
import org.jlab.coda.et.EtStation;
import org.jlab.coda.et.EtStationConfig;
import org.jlab.coda.et.EtSystem;
import org.jlab.coda.et.EtSystemOpenConfig;
import org.jlab.coda.et.enums.Mode;
import org.jlab.coda.et.enums.Modify;
import org.jlab.coda.et.exception.EtBusyException;
import org.jlab.coda.et.exception.EtClosedException;
import org.jlab.coda.et.exception.EtDeadException;
import org.jlab.coda.et.exception.EtEmptyException;
import org.jlab.coda.et.exception.EtException;
import org.jlab.coda.et.exception.EtExistsException;
import org.jlab.coda.et.exception.EtTimeoutException;
import org.jlab.coda.et.exception.EtTooManyException;
import org.jlab.coda.et.exception.EtWakeUpException;

/**
 * A class for encapsulating the connection information for an ET client including the EtSystem and EtAttachment
 * objects.
 *
 * @author <a href="mailto:jeremym@slac.stanford.edu">Jeremy McCormick</a>
 */
public class EtConnection {
   
    protected EtConnection() {        
    }
    
    /**
     * Create an EtConnection with full list of configuration parameters.
     *
     * @param name the name of the ET system e.g. the buffer file on disk
     * @param host the name of the network host
     * @param port the port of the network host
     * @param blocking <code>true</code> for blocking behavior
     * @param queueSize the queue size
     * @param prescale the event prescale factor or 0 for none
     * @param stationName the name of the ET station
     * @param stationPosition he position of the ET station
     * @param waitMode the wait mode
     * @param waitTime the wait time if using timed wait mode
     * @param chunkSize the number of ET events to return at once
     * @return the <code>EtConnection</code> created from the parameters
     */
    public static EtConnection createConnection(final String name, final String host, final int port,
            final boolean blocking, final int queueSize, final int prescale, final String stationName,
            final int stationPosition, final Mode waitMode, final int waitTime, final int chunkSize) {
        try {

            // make a direct connection to ET system's tcp server
            final EtSystemOpenConfig etConfig = new EtSystemOpenConfig(name, host, port);

            // create ET system object with verbose debugging output
            final EtSystem sys = new EtSystem(etConfig, EtConstants.debugInfo);
            sys.open();

            // configuration of a new station
            final EtStationConfig stationConfig = new EtStationConfig();
            // statConfig.setFlowMode(cn.flowMode);
            // FIXME: Flow mode hard-coded.
            stationConfig.setFlowMode(EtConstants.stationSerial);
            if (!blocking) {
                stationConfig.setBlockMode(EtConstants.stationNonBlocking);
                if (queueSize > 0) {
                    stationConfig.setCue(queueSize);
                }
            }
            // Set prescale.
            if (prescale > 0) {
                // System.out.println("setting prescale to " + cn.prescale);
                stationConfig.setPrescale(prescale);
            }

            // Create the station.
            final EtStation stat = sys.createStation(stationConfig, stationName, stationPosition);

            // attach to new station
            final EtAttachment att = sys.attach(stat);

            // Return new connection.
            final EtConnection connection = new EtConnection(sys, att, stat, waitMode, waitTime, chunkSize);

            return connection;

        } catch (IOException | EtException | EtExistsException | EtClosedException | EtDeadException
                | EtTooManyException e) {
            throw new RuntimeException("Failed to create ET connection.", e);
        }
    }

    /**
     * Create an <code>EtConnection</code> with a set of default parameters.
     *
     * @return an <code>EtConnection</code> with default parameters
     */
    public static EtConnection createDefaultConnection() {
        return createConnection("ETBuffer", "localhost", 11111, false, 0, 0, "MY_STATION", 1, Mode.TIMED, 5000000, 1);
    }

    /**
     * The ET attachment.
     */
    protected EtAttachment att;

    /**
     * The chunk size.
     */
    protected int chunkSize;

    /**
     * The ET station.
     */
    protected EtStation stat;

    /**
     * The ET system object representing the connection to the server.
     */
    protected EtSystem sys;

    /**
     * The wait mode.
     */
    protected Mode waitMode;

    /**
     * The wait time.
     */
    protected int waitTime;

    /**
     * A class constructor for internal convenience.
     *
     * @param param The connection parameters.
     * @param sys The ET system.
     * @param att The ET attachment.
     * @param stat The ET station.
     */
    private EtConnection(final EtSystem sys, final EtAttachment att, final EtStation stat, final Mode waitMode,
            final int waitTime, final int chunkSize) {
        this.sys = sys;
        this.att = att;
        this.stat = stat;
        this.waitMode = waitMode;
        this.waitTime = waitTime;
        this.chunkSize = chunkSize;
    }

    /**
     * Cleanup the ET connection.
     */
    public void cleanup() {
        try {
            if (!this.sys.alive()) {
                throw new RuntimeException("EtSystem is not alive!");
            }
            this.sys.detach(this.att);
            this.sys.removeStation(this.stat);
            this.sys.close();
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Get the ET attachment.
     *
     * @return The ET attachment.
     */
    public EtAttachment getEtAttachment() {
        return this.att;
    }

    /**
     * Get the ET station.
     *
     * @return The ET station.
     */
    public EtStation getEtStation() {
        return this.stat;
    }

    /**
     * Get the ET system.
     *
     * @return The ET system.
     */
    public EtSystem getEtSystem() {
        return this.sys;
    }

    /**
     * Read an array of <code>EtEvent</code> objects from the ET server.
     * <p>
     * Method signature preserves all specific exception types in the throws clause so that the caller may easily
     * implement their own error and state handling depending on the kind of error that was thrown.
     *
     * @return The array of EtEvents.
     * @throws IOException if <code>getEvents</code> throws this exception type
     * @throws EtException if <code>getEvents</code> throws this exception type
     * @throws EtDeadException if <code>getEvents</code> throws this exception type
     * @throws EtEmptyException if <code>getEvents</code> throws this exception type
     * @throws EtBusyException if <code>getEvents</code> throws this exception type
     * @throws EtTimeoutException if <code>getEvents</code> throws this exception type
     * @throws EtWakeUpException if <code>getEvents</code> throws this exception type
     * @throws EtClosedException if <code>getEvents</code> throws this exception type
     */
    EtEvent[] readEtEvents() throws IOException, EtException, EtDeadException, EtEmptyException, EtBusyException,
            EtTimeoutException, EtWakeUpException, EtClosedException {
        return getEtSystem().getEvents(getEtAttachment(), this.waitMode, Modify.NOTHING, this.waitTime, this.chunkSize);
    }

}