/**
 * 
 */
package org.hps.monitoring.application.util;

import java.io.IOException;
import java.util.Arrays;

import org.hps.monitoring.application.util.EventTagFilter.AcceptAllFilter;
import org.jlab.coda.et.EtAttachment;
import org.jlab.coda.et.EtConstants;
import org.jlab.coda.et.EtEvent;
import org.jlab.coda.et.EtStation;
import org.jlab.coda.et.EtStationConfig;
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
 * This is an implementation of Runnable that wraps an ET station.
 * It should be run on its own thread.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu
 */
public abstract class RunnableEtStation implements Runnable {
    
    // ET system objects.
    protected EtStation station;
    protected EtAttachment attachment;

    // Config for this object.
    protected RunnableEtStationConfiguration config;
    
    // An event filter which by default will accept all EvioEvents.
    protected EventTagFilter filter = new AcceptAllFilter();
    
    // Sub-classes should set this if the station needs to disconnect during the run method.
    protected volatile boolean disconnect = false;    
    
    /**
     * Configuration parameters for setting up an object of this class.
     */
    public static final class RunnableEtStationConfiguration {
        
        public EtSystem system;
        public String name;
        public int order = 1;
        public int prescale = 1;
        public int readEvents = 100;
        public Integer eventType;
        
        void validate() {
            if (system == null) {
                throw new IllegalArgumentException("system is null");
            }
            if (name == null) {
                throw new IllegalArgumentException("name is null");
            }
            if (order <= 0) {
                throw new IllegalArgumentException("order must be > 0");
            }
            if (prescale <= 0) {
                throw new IllegalArgumentException("prescale must be > 0");
            }
        }
    }

    protected RunnableEtStation() {
    }

    /**
     * Create the runnable ET station with the given config.
     * @param config The configuration parameters.
     */
    public RunnableEtStation(RunnableEtStationConfiguration config) {
        if (config == null) {
            throw new IllegalArgumentException("config is null");
        }
        config.validate();
        this.config = config;
    }
    
    /**
     * Process a single ET event.  
     * Sub-classes must implement this method.
     * @param event The EtEvent to process.
     */
    public abstract void processEvent(EtEvent event); 

    /**
     * Run this station by reading ET events and processing them.
     * Disconnection (detachment of the station) will occur automatically 
     * at the end of this method.
     */
    public void run() {
        
        setup();
        
        try {
            for (;;) {
                // Get the next array of events from the server.
                EtEvent[] events;
                
                try {
                    events = config.system.getEvents(attachment, Mode.SLEEP, Modify.NOTHING, 0, config.readEvents);
                } catch (EtWakeUpException e) {
                    System.out.println("ET station " + this.config.name + " was woken up and will disconnect");
                    break;
                } catch (EtException | EtDeadException | 
                        EtClosedException | EtEmptyException | 
                        EtBusyException | EtTimeoutException | 
                        IOException e) {
                    System.err.println("ET station " + this.config.name + " failed to read events ...");
                    e.printStackTrace();
                    break;
                }
                
                try {
                    // Process the events.
                    for (EtEvent event : events) {
                        // Process an event if it passes the filter (by default accepts all).
                        if (filter.accept(event.getControl()[0])) {
                            processEvent(event);
                        }
                    }
                } catch (Exception e) {
                    System.err.println("ET station " + this.config.name + " had event processing error ...");
                    e.printStackTrace();
                    continue;
                }
                
                // Disconnect if flag is set (by sub-class in its processEvent method).
                if (disconnect) {
                    break;
                }
                
                // Disconnect if interrupted.
                if (Thread.currentThread().isInterrupted()) {
                    break;
                }
            }             
        } finally {
            if (config.system.alive()) {               
                disconnect();
            } 
        }       
    }
       
    /**
     * Setup the ET station.
     */
    protected void setup() {
        try {
            // Create the basic station config.
            EtStationConfig stationConfig = new EtStationConfig();
            stationConfig.setFlowMode(EtConstants.stationSerial);
            stationConfig.setBlockMode(EtConstants.stationNonBlocking);
            stationConfig.setPrescale(config.prescale);

            // Setup event selection.
            if (config.eventType != null) {
                int[] select = new int[EtConstants.stationSelectInts];
                Arrays.fill(select, -1);
                select[0] = config.eventType;
                stationConfig.setSelect(select);
            }

            // Create station and attach to the ET system.
            station = config.system.createStation(stationConfig, config.name, config.order);
            attachment = config.system.attach(station);

        } catch (Exception e) {
            // Any errors during setup are re-thrown.
            throw new RuntimeException(e);
        }
    }

    /**
     * Detach the station if the ET system is still alive.
     */
    synchronized void disconnect() {
        if (config.system.alive()) {
            if (attachment.isUsable()) {
                try {
                    config.system.detach(attachment);
                } catch (IOException | EtException | EtClosedException | EtDeadException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    
    /**
     * Get the ET station.
     * @return The ET station.
     */
    public EtStation getEtStation() {
        return this.station;
    }
}
