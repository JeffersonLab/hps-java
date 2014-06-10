package org.hps.monitoring.record;

/**
 * Thread for handling the event processing chain.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
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
        processing.loop();
        /*
        while (!processing.isDone()) {
            try {
                sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        */
    }
}
