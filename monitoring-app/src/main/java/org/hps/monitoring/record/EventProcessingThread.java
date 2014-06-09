package org.hps.monitoring.record;

/**
 * Thread for handling the event processing chain.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class EventProcessingThread extends Thread {
    
    EventProcessingChain processing;
    
    EventProcessingThread(EventProcessingChain processing) {
        super("EventProcessingThread");
        this.processing = processing;
    }
    
    @Override
    public void run() {
        processing.run();
    }
    
    public void pause() {
        processing.pause();
    }
    
    public void stopProcessing() {
        processing.stop();
    }
    
    public void resumeProcessing() {
        processing.resume();
    }
}
