package org.hps.monitoring.record.etevent;

import java.io.IOException;

import org.jlab.coda.et.EtAttachment;
import org.jlab.coda.et.EtConstants;
import org.jlab.coda.et.EtEvent;
import org.jlab.coda.et.EtStation;
import org.jlab.coda.et.EtStationConfig;
import org.jlab.coda.et.EtSystem;
import org.jlab.coda.et.EtSystemOpenConfig;
import org.jlab.coda.et.enums.Modify;
import org.jlab.coda.et.exception.EtBusyException;
import org.jlab.coda.et.exception.EtClosedException;
import org.jlab.coda.et.exception.EtDeadException;
import org.jlab.coda.et.exception.EtEmptyException;
import org.jlab.coda.et.exception.EtException;
import org.jlab.coda.et.exception.EtTimeoutException;
import org.jlab.coda.et.exception.EtWakeUpException;

/**
 * Create an EtSystem and EtAttachment from ConnectionParameters.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class EtConnection {

    EtConnectionParameters param;
    EtSystem sys;
    EtAttachment att;
    EtStation stat;

    /**
     * Class constructor.
     * @param param The connection parameters.
     * @param sys The ET system.
     * @param att The ET attachment.
     * @param stat The ET station.
     */
    private EtConnection(EtConnectionParameters param, EtSystem sys, EtAttachment att, EtStation stat) {
        this.param = param;
        this.sys = sys;
        this.att = att;
        this.stat = stat;
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
     * Get the connection parameters.
     * @return The connection parameters.
     */
    public EtConnectionParameters getConnectionParameters() {
        return param;
    }

    /**
     * Cleanup the ET connection.
     */
    public void cleanup() {
        boolean debug = false;
        try {
            if (debug)
                System.out.println("ET cleanup - sys.detach ...");
            sys.detach(att);
            if (debug)
                System.out.println("ET cleanup - sys.removeStation ...");
            sys.removeStation(stat);
            if (debug)
                System.out.println("ET cleanup - sys.close ...");
            sys.close();
            if (debug)
                System.out.println("ET cleanup - successful");
        }
        catch (Exception e) {
            e.printStackTrace();
        }		
    }

    /**
     * Create an ET connection from connection parameters.
     * @param cn The connection parameters.
     * @return The ET connection.
     */
    public static EtConnection createEtConnection(EtConnectionParameters cn) {
        try {

            // make a direct connection to ET system's tcp server
            EtSystemOpenConfig config = new EtSystemOpenConfig(cn.bufferName, cn.host, cn.port);

            // create ET system object with verbose debugging output
            EtSystem sys = new EtSystem(config, EtConstants.debugInfo);
            sys.open();

            // configuration of a new station
            EtStationConfig statConfig = new EtStationConfig();
            statConfig.setFlowMode(cn.flowMode);
            if (!cn.blocking) {
                statConfig.setBlockMode(EtConstants.stationNonBlocking);
                if (cn.qSize > 0) {
                    statConfig.setCue(cn.qSize);
                }
            }
            // Set prescale.
            if (cn.prescale > 0) {
                System.out.println("setting prescale to " + cn.prescale);
                statConfig.setPrescale(cn.prescale);
            }

            // Create the station.
            EtStation stat = sys.createStation(statConfig, cn.statName, cn.position, cn.pposition);

            // attach to new station
            EtAttachment att = sys.attach(stat);

            // Return new connection.
            return new EtConnection(cn, sys, att, stat);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }       
    
    /**
     * Read EtEvent objects from the ET ring.  
     * Preserve all specific Exception types in throws clause so caller
     * can implement their own specific error and state handling.
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
    public EtEvent[] readEtEvents() 
            throws IOException, EtException, EtDeadException, 
            EtEmptyException, EtBusyException, EtTimeoutException, 
            EtWakeUpException, EtClosedException {
        return getEtSystem().getEvents(
            getEtAttachment(),
            getConnectionParameters().getWaitMode(), 
            Modify.NOTHING,
            getConnectionParameters().getWaitTime(), 
            getConnectionParameters().getChunkSize());
    }
}