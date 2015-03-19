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
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class EtStationThread extends Thread {
    
    EtSystem system;
    EtEventProcessor processor;
    int stationPosition;
    String name;
    
    EtStation station;
    EtAttachment attachment;
    
    public EtStationThread(EtEventProcessor processor, EtSystem system, String name, int stationPosition) {
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
        this.processor = processor;
        try {
            this.system = new EtSystem(new EtSystemOpenConfig(system.getConfig()));
        } catch (EtException e) {
            throw new RuntimeException("Error setting up station.", e);
        }
        this.stationPosition = stationPosition;
        this.name = name;
    }
    
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

            // Create station and attach to the ET system.
            station = system.createStation(stationConfig, name, stationPosition);
            attachment = system.attach(station);

        } catch (Exception e) {
            // Any errors during setup are re-thrown.
            throw new RuntimeException(e);
        }
    }
                
    public void run() {
        
        // FIXME: Should be called independently?
        setup();
        
        try {
            for (;;) {                
                
                EtEvent[] events;
                
                try {
                    events = system.getEvents(attachment, Mode.SLEEP, Modify.NOTHING, 0, 1 /* read 1 event */);
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
    
    synchronized void disconnect() {
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
}
