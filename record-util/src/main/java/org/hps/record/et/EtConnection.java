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
 * Create an EtSystem and EtAttachment from ConnectionParameters.
 */
public final class EtConnection {

    EtSystem sys;
    EtAttachment att;
    EtStation stat;
    
    Mode waitMode;
    int waitTime;
    int chunkSize;

    /**
     * Class constructor.
     * @param param The connection parameters.
     * @param sys The ET system.
     * @param att The ET attachment.
     * @param stat The ET station.
     */
    public EtConnection(
            EtSystem sys, 
            EtAttachment att, 
            EtStation stat, 
            Mode waitMode, 
            int waitTime, 
            int chunkSize) {
        this.sys = sys;
        this.att = att;
        this.stat = stat;
        this.waitMode = waitMode;
        this.waitTime = waitTime;
        this.chunkSize = chunkSize;
    }
  
    /**
     * Get the ET system.
     * @return The ET system.
     */
    public EtSystem getEtSystem() {
        return sys;
    }

    /**
     * Get the ET attachment.
     * @return The ET attachment.
     */
    public EtAttachment getEtAttachment() {
        return att;
    }

    /**
     * Get the ET station. 
     * @return The ET station.
     */
    public EtStation getEtStation() {              
        return stat;        
    }
    
    /**
     * Cleanup the ET connection.
     */
    public void cleanup() {
        try {
            if (!sys.alive()) {
                throw new RuntimeException("EtSystem is not alive!");
            }               
            sys.detach(att);
            sys.removeStation(stat);
            sys.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }		
    }
    
    /**
     * Read EtEvent objects from the ET ring.  
     * Preserve all specific Exception types in the throws clause so caller
     * may implement their own error and state handling.
     * @return
     * @throws IOException
     * @throws EtException
     * @throws EtDeadException
     * @throws EtEmptyException
     * @throws EtBusyException
     * @throws EtTimeoutException
     * @throws EtWakeUpException
     * @throws EtClosedException
     */
    EtEvent[] readEtEvents() 
            throws IOException, EtException, EtDeadException, 
            EtEmptyException, EtBusyException, EtTimeoutException, 
            EtWakeUpException, EtClosedException {
        return getEtSystem().getEvents(
            getEtAttachment(),
            waitMode,
            Modify.NOTHING,
            waitTime,
            chunkSize);        
    }     
    
    /**
     * Create an EtConnection with full list of configuration parameters.
     * @param name The name of the ET system e.g. the buffer file on disk.
     * @param host The name of the network host.
     * @param port The port of the network host.
     * @param blocking True for blocking behavior.
     * @param queueSize The queue size.
     * @param prescale The event prescale or 0 for none.
     * @param stationName The name of the ET station.
     * @param stationPosition The position of the ET station.
     * @param waitMode The wait mode.
     * @param waitTime The wait time if using timed wait.
     * @param chunkSize The number of ET events to return at once.
     * @return The EtConnection created from the parameters.
     */
    public static EtConnection createConnection(
            String name,
            String host,
            int port,
            boolean blocking,
            int queueSize,
            int prescale,
            String stationName,
            int stationPosition,
            Mode waitMode,
            int waitTime,
            int chunkSize) {
        try {
            
            // make a direct connection to ET system's tcp server            
            EtSystemOpenConfig etConfig = new EtSystemOpenConfig(
                    name, 
                    host, 
                    port);

            // create ET system object with verbose debugging output
            EtSystem sys = new EtSystem(etConfig, EtConstants.debugInfo);
            sys.open();

            // configuration of a new station
            EtStationConfig stationConfig = new EtStationConfig();
            //statConfig.setFlowMode(cn.flowMode);
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
                //System.out.println("setting prescale to " + cn.prescale);
                stationConfig.setPrescale(prescale);
            }

            // Create the station.
            EtStation stat = sys.createStation(
                    stationConfig, 
                    stationName,
                    stationPosition);

            // attach to new station
            EtAttachment att = sys.attach(stat);

            // Return new connection.
            EtConnection connection = new EtConnection(
                    sys, 
                    att, 
                    stat,
                    waitMode,
                    waitTime,
                    chunkSize
                    );
            
            return connection;

        } catch (IOException | 
                EtException | 
                EtExistsException | 
                EtClosedException | 
                EtDeadException | 
                EtTooManyException e) {
            throw new RuntimeException("Failed to create ET connection.", e);
        }
    }
    
    /**
     * Create an EtConnection with a set of default parameters.
     * @return An EtConnection with default parameters.
     */
    public static EtConnection createDefaultConnection() {
        return createConnection(
                "ETBuffer",
                "localhost",
                11111,
                false,
                0,
                0,
                "MY_STATION",
                1,
                Mode.TIMED,
                5000000,
                1);                
    }
    
}