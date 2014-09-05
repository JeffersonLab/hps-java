package org.hps.record.composite;

/**
 * Class for running the {@link CompositeLoop} on a separate thread.
 */
public final class EventProcessingThread extends Thread {
    
    CompositeLoop loop;
           
    /**
     * Constructor requiring the loop object.
     * @param loop The loop object.
     */
    public EventProcessingThread(CompositeLoop loop) {
        super("EventProcessingThread");
        this.loop = loop;
    }
        
    /**
     * Run this thread, which will process records until the loop is done.
     */
    @Override
    public void run() {                
                
        // Keep looping until the event processing is flagged as done.
        while (true) {
            // Is the processing unpaused?            
            if (!loop.isPaused()) {
                
                // Loop until done, error occurs, or pause is requested.
                // FIXME: The maximum number of records should be used here.
                loop.loop(-1);
                
                // Is loop done?
                if (loop.isDone()) {
                    // Stop record processing.
                    break;
                }
            }
            
            // Sleep for a little while between loop iterations (e.g. while paused).
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        
    }
}