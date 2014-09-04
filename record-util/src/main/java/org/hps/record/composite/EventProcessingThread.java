package org.hps.record.composite;

import org.freehep.record.loop.RecordLoop.Command;

/**
 * Thread for running the event processing chain.
 */
public final class EventProcessingThread extends Thread {
    
    CompositeLoop loop;
           
    public EventProcessingThread(CompositeLoop loop) {
        super("EventProcessingThread");
        this.loop = loop;
    }
        
    @Override
    public void run() {                
                
        // Keep looping until the event processing is flagged as done.
        while (true) {
            // Is the processing unpaused?            
            if (!loop.isPaused()) {
                
                // Loop until done, error occurs, or pause is requested.                
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