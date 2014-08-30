package org.hps.record.processing;

/**
 * Thread for running the event processing chain.
 */
public final class ProcessingThread extends Thread {
    
    ProcessingChain processing;
           
    public ProcessingThread(ProcessingChain processing) {
        super("EventProcessingThread");
        this.processing = processing;
    }
    
    public ProcessingChain getEventProcessingChain() {
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