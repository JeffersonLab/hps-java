package org.hps.monitoring.record.etevent;

import java.io.IOException;

import org.hps.monitoring.gui.model.ConfigurationModel;
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
import org.jlab.coda.et.exception.EtTimeoutException;
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
    private EtConnection(EtSystem sys, EtAttachment att, EtStation stat, 
            Mode waitMode, int waitTime, int chunkSize) {
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
        boolean debug = false;
        try {
            if (!sys.alive()) {
                throw new RuntimeException("EtSystem is not alive!");
            }               
            if (debug) {
                System.out.println("EtConnection cleanup ...");
                System.out.println("sys.detach ...");
            }
            //if (!att.isUsable()) {
            //    throw new RuntimeException("EtAttachment is not usable!");
            //}
            // FIXME: This can hang forever when in getEvents() call!!!
            sys.detach(att);
            if (debug) {
                System.out.println("sys.detach okay");
                System.out.println("sys.removeStation ...");
            }
            sys.removeStation(stat);
            if (debug) {
                System.out.println("sys.removeStation okay");
                System.out.println("sys.close ...");
            }
            sys.close();
            if (debug) {
                System.out.println("sys.close okay");
                System.out.println("EtConnection cleanup successful!");
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }		
    }
    
    public static EtConnection fromConfigurationModel(ConfigurationModel configurationModel) {
        try {
            
            // make a direct connection to ET system's tcp server            
            EtSystemOpenConfig etConfig = new EtSystemOpenConfig(
                    configurationModel.getEtName(), 
                    configurationModel.getHost(), 
                    configurationModel.getPort());

            // create ET system object with verbose debugging output
            EtSystem sys = new EtSystem(etConfig, EtConstants.debugInfo);
            sys.open();

            // configuration of a new station
            EtStationConfig statConfig = new EtStationConfig();
            //statConfig.setFlowMode(cn.flowMode);
            // FIXME: Flow mode hard-coded.
            statConfig.setFlowMode(EtConstants.stationSerial);
            boolean blocking = configurationModel.getBlocking();
            if (!blocking) {
                statConfig.setBlockMode(EtConstants.stationNonBlocking);
                int qSize = configurationModel.getQueueSize();
                if (qSize > 0) {
                    statConfig.setCue(qSize);
                }
            }
            // Set prescale.
            int prescale = configurationModel.getPrescale();
            if (prescale > 0) {
                //System.out.println("setting prescale to " + cn.prescale);
                statConfig.setPrescale(prescale);
            }

            // Create the station.
            //System.out.println("position="+config.getInteger("position"));
            EtStation stat = sys.createStation(
                    statConfig, 
                    configurationModel.getStationName(),
                    configurationModel.getStationPosition());

            // attach to new station
            EtAttachment att = sys.attach(stat);

            // Return new connection.
            EtConnection connection = new EtConnection(
                    sys, 
                    att, 
                    stat,
                    configurationModel.getWaitMode(),
                    configurationModel.getWaitTime(),
                    configurationModel.getChunkSize()
                    );
            
            return connection;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
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
}