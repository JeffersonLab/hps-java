package org.hps.record.et;

import java.io.IOException;

import org.jlab.coda.et.EtAttachment;
import org.jlab.coda.et.EtEvent;
import org.jlab.coda.et.EtStation;
import org.jlab.coda.et.EtSystem;
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
    public EtConnection(EtSystem sys, EtAttachment att, EtStation stat, 
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
}