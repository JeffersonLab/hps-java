package org.hps.record.composite;

import org.freehep.record.loop.RecordLoop;

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
                
        // Flag that is turned on when looping starts.
        boolean started = false;
        
        // Keep looping until the event processing is done.
        while (true) {                        
            
            // If the loop was started and now is in the IDLE state, it means
            // that STOP was executed, so break from the processing while loop.
            if (started && loop.getState().equals(RecordLoop.State.IDLE)) {                
                // Stop record processing.
                break;
            }
                        
            // Is the processing unpaused?
            if (!loop.isPaused()) {                                                
                
                // Set a flag to indicate that looping has started.
                started = true;
                
                // Loop until done, error occurs, or pause is requested.
                // FIXME: The maximum number of records should be used here.
                loop.loop(-1);
            }            
        }
    }
}