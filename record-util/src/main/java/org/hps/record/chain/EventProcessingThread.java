package org.hps.record.chain;

/**
 * Thread for running the event processing chain.
 */
public final class EventProcessingThread extends Thread {
    
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
            processing.run();
        } catch (Exception e) {
            throw new RuntimeException("Error in event processing.", e);
        } 
    }
}