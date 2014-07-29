package org.hps.monitoring.record;

/**
 * Thread for running the event processing chain.
 */
public class EventProcessingThread extends Thread {
    
    EventProcessingChain processing;
           
    public EventProcessingThread(EventProcessingChain processing) {
        super("EventProcessingThread");
        this.processing = processing;
    }
    
    public EventProcessingChain getEventProcessingChain() {
        return processing;
    }
    
    @Override
    public void run() {
        try {
            processing.loop();
        } catch (Exception e) {
            throw new RuntimeException("Error in event processing.", e);
        }
    }
}