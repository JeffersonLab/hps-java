package org.hps.monitoring.record;

/**
 * This is a very basic interface for event processing.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 *
 * @param <EventType> The concrete type of the event record.
 */
public interface EventProcessor<EventType> {          
    void processEvent(EventType event);    
}
