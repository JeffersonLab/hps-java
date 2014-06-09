package org.hps.monitoring;

import java.util.logging.Level;

import org.hps.monitoring.record.etevent.EtEventListener;
import org.jlab.coda.et.EtEvent;
import org.jlab.coda.et.exception.EtTimeoutException;

/**
 * This is an interface for processing EtEvent objects and receiving callbacks
 * via an {@link EtEventListener}.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 * @version $Id: EtEventProcessor.java,v 1.4 2013/11/05 17:15:04 jeremy Exp $
 */
interface OldEtEventProcessor
{
    /** 
     * Process a single EtEvent.
     * @param event The ET event.
     * @throws EventProcessingException if there was an error processing the event.
     * @throws MaxEventsException if the maximum number of events was reached or exceeded.
     */
    void processEtEvent(EtEvent event) throws EventProcessingException, MaxEventsException;
    
    /**
     * Process the next array of EtEvents.  This method may block for a long time or forever.
     * The default implementation calls processEtEvent() on each event, but this is not required.
     * @throws EtTimeoutException if the connection times out.
     * @throws MaxEventsException if the maximum number of events was reached or exceeded.
     * @throws EventProcessingException if there was an error processing events.
     * @throws Exception if some other exception occurs.
     */
    void processEtEvents() throws EtTimeoutException, MaxEventsException, EventProcessingException, Exception;
    
    /** 
     * Process all incoming EtEvents until ET system goes down or stop is requested.
     */
    void process();
    
    /**
     * Get the current status as a {@link ConnectionStatus} code.
     */
    int getStatus();
    
    /** 
     * Get the total number of events processing thusfar, including those with errors.
     * @return The number of events processed.
     */
    int getNumberOfEventsProcessed();
    
    /**
     * Get the maximum number of event to process before automatically disconnecting.
     * @return The maximum number of events to process.
     */
    int getMaxEvents();
    
    /**
     * Reset the number of events processed to zero.
     */
    void resetNumberOfEventsProcessed();
    
    /**
     * Add a listener that will receive notifications during event processing.
     * @param callme The ET event listener.
     */
    void addListener(EtEventListener callme);
    
    /**
     * Request that the processor stop processing events.
     */
    void stop();
    
    /**
     * Set the maximum number of events to process before disconnecting.
     * @param maxEvents The maximum number of events to process.
     */
    void setMaxEvents(int maxEvents);
    
    /**
     * Turn pause mode on or off.
     * @param p The pause mode setting; true for on; false for off.
     */
    void pauseMode(boolean p);
    
    /**
     * If using pause mode, this will get the next set of events and then pause again.
     */
    void nextEvents();
    
    /**
     * Set the log level of this object.
     * @param level The log level.
     */
    void setLogLevel(Level level);
    
    /**
     * Check if the processor is done.
     * @return True if processer is done processing events; false if not.
     */
    boolean done();
    
    /**
     * Check if the processor is in the blocked state, e.g. if it is waiting 
     * for events from the ET ring.
     * @return True if blocked; false if not.
     */
    boolean blocked();
           
    /** 
     * Exception that is thrown when an error occurs during event processing.
     */
    static class EventProcessingException extends Exception {

        /**
         * Class constructor.
         * @param e Another Exception object.
         */
        EventProcessingException(Exception e) {
            super(e);
        }
        
        /**
         * Class constructor.
         * @param m The error message.
         * @param e Another Exception object.
         */
        EventProcessingException(String m, Exception e) {
            super(m, e);
        }
        
        /**
         * Class constructor.
         * @param m The error message.
         */
        EventProcessingException(String m) {
        	super(m);
        }
    }
    
    /**
     * Exception that is throw when the {@link OldEtEventProcessor#getMaxEvents()} is exceeded.
     */
    static final class MaxEventsException extends Exception {
        
        /**
         * Class constructor
         */
        MaxEventsException() {
            super("Maximum number of events was reached.");
        }
    }
}