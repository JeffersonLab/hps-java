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
import org.jlab.coda.et.exception.EtTimeoutException;
import org.jlab.coda.et.exception.EtTooManyException;
import org.jlab.coda.et.exception.EtWakeUpException;

/**
 * <p>
 * This is a class which runs ET event processing on a separate thread
 * using an ET station that is assigned to its own unique <code>EtSystem</code>.
 * <p>
 * Specific processing of ET events is provided with an {@link EtEventProcessor}.
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public final class EtStationThread extends Thread {
        
    EtEventProcessor processor;
    int stationPosition;
    String name;    
    
    EtSystem system;
    EtStation station;
    EtAttachment attachment;
    
    int[] select;
    
    /**
     * This creates an ET station that will run an ET processor on a separate thread.
     * @param processor The ET processor.
     * @param system The ET system.
     * @param name The name of the station.
     * @param stationPosition The station's position.
     * @param select The station's select array (can be null).
     */
    public EtStationThread(EtEventProcessor processor, EtSystem system, String name, int stationPosition, int[] select) {
        if (processor == null) {
            throw new IllegalArgumentException("processor is null");
        }
        if (system == null) {
            throw new IllegalArgumentException("system is null");           
        }
        if (name == null) {
            throw new IllegalArgumentException("name is null");
        }
        if (stationPosition < 1) {
            throw new IllegalArgumentException("stationPosition must be > 0");
        }
        if (select.length != EtConstants.stationSelectInts) {
            throw new IllegalArgumentException("control array must have length " + EtConstants.stationSelectInts);
        }
        
        this.processor = processor;
        this.name = name;
        this.stationPosition = stationPosition;                
        this.select = select;
        
        // Copy parameters from the provided EtSystem.
        try {
            this.system = new EtSystem(new EtSystemOpenConfig(system.getConfig()));
        } catch (EtException e) {
            throw new RuntimeException("Error setting up station.", e);
        }
    }
    
    /**
     * Setup this station for receiving events.
     */
    protected void setup() {
        
        if (!system.alive()) {
            try {
                system.open();
            } catch (IOException | EtException | EtTooManyException e) {
                throw new RuntimeException("Failed to open ET system.", e);
            }
        }
        
        try {
            // Create the basic station configuration.
            EtStationConfig stationConfig = new EtStationConfig();
            stationConfig.setFlowMode(EtConstants.stationSerial);
            stationConfig.setBlockMode(EtConstants.stationNonBlocking);
            
            // Setup event selection.
            if (select != null) {
                stationConfig.setSelect(select);
                stationConfig.setSelectMode(EtConstants.stationSelectMatch); // ?????
            }
            
            // Create station and attach to the ET system.
            station = system.createStation(stationConfig, name, stationPosition);
            attachment = system.attach(station);

        } catch (Exception e) {
            // Any errors during setup are re-thrown.
            throw new RuntimeException(e);
        }
    }
              
    /**
     * Run event processing on the station until woken up or interrupted.
     */
    public final void run() {
        
        setup();
        
        try {
            for (;;) {                
                
                EtEvent[] events;
                
                try {
                    events = system.getEvents(attachment, Mode.SLEEP, Modify.NOTHING, 0, 1 /* hard-coded to read 1 event for now */);
                    system.putEvents(attachment, events);
                } catch (EtWakeUpException e) {
                    e.printStackTrace();
                    break;
                } catch (EtException | EtDeadException | 
                        EtClosedException | EtEmptyException | 
                        EtBusyException | EtTimeoutException | 
                        IOException e) {
                    e.printStackTrace();
                    break;
                }
                
                try {
                    // Process the events.
                    for (EtEvent event : events) {
                        processor.process(event);
                    }
                } catch (Exception e) {
                    // If there is an event processing error then print stack trace and continue.
                    e.printStackTrace();
                    continue;
                }
                                
                // Disconnect if interrupted.
                if (Thread.currentThread().isInterrupted()) {
                    break;
                }
            }             
        } finally {
            disconnect();
        }        
    }
    
    /**
     * Disconnect the station.
     * This will happen automatically at the end of the {@link #run()} method.
     */
    synchronized final void disconnect() {
        if (system.alive()) {
            if (attachment.isUsable()) {
                try {
                    system.detach(attachment);
                } catch (IOException | EtException | EtClosedException | EtDeadException e) {
                    e.printStackTrace();
                }
            }
            system.close();
        }
    }
    
    /**
     * Wake up the station if it is blocked which will cause it to disconnect.
     */
    public final synchronized void wakeUp() {
        if (system.alive()) {
            if (attachment.isUsable()) {
                try {
                    system.wakeUpAll(station);
                } catch (IOException | EtException | EtClosedException e) {
                    e.printStackTrace();
                } 
            }
        }
    }
}
