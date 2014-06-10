package org.hps.monitoring.record;

/**
 * This is a very basic interface for event processing.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 *
 * @param <EventType> The concrete type of the event record.
 */
public interface EventProcessor<EventType> {
       
    /**
     * Start of job action.
     */
    void startJob();
    
    /**
     * Start run action.
     * @param event
     */
    void startRun(EventType event);
    
    /**
     * Process a single event.
     * @param event
     */
    void processEvent(EventType event);

    /**
     * End of run action.
     * @param event
     */
    void endRun(EventType event);
    
    /**
     * End of job action.
     */
    void endJob();
}
