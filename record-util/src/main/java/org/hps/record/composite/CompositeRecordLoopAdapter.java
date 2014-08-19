package org.hps.record.composite;

import java.util.ArrayList;
import java.util.List;

import org.freehep.record.loop.AbstractLoopListener;
import org.freehep.record.loop.LoopEvent;
import org.freehep.record.loop.RecordEvent;
import org.freehep.record.loop.RecordListener;

/**
 * Adapter for listening on the {@link CompositeRecordLoop} for records and loop events.
 */
public final class CompositeRecordLoopAdapter extends AbstractLoopListener implements RecordListener {

    List<CompositeRecordProcessor> processors = new ArrayList<CompositeRecordProcessor>();

    /**
     * Callback for loop finish event.
     * @param loopEvent 
     */
    public void finish(LoopEvent loopEvent) {
        for (CompositeRecordProcessor processor : processors) {
            // Call end job hook on all registered processors, which are 
            // responsible for sending the stop command to their loops.
            processor.endJob();
        }
    }

    /**
     * Add a <tt>CompositeRecordProcessor</tt> that will listen to this loop.
     * @param processor The composite record processor to add.
     */
    void addProcessor(CompositeRecordProcessor processor) {
        processors.add(processor);
    }
        
    /**
     * Start event processing which will call {@link CompositeRecordProcessor#startJob()}
     * on all the registered processors.
     * @param loopEvent
     */
    public void start(LoopEvent loopEvent) {
        for (CompositeRecordProcessor processor : processors) {
            processor.startJob();
        }
    }
    
    /**
     * Suspend the loop.
     * @param loopEvent
     */
    public void suspend(LoopEvent loopEvent) { 
        //if (loopEvent.getException() != null) {
        //    throw new RuntimeException("Error in event processing.", loopEvent.getException());
        //}
    }

    /**
     * Process one record.
     * @param record 
     */
    @Override
    public void recordSupplied(RecordEvent record) {
        for (CompositeRecordProcessor processor : processors) {
            try {
                processor.processEvent((CompositeRecord) record.getRecord());
            } catch (Exception e) {                
                throw new RuntimeException("Error processing CompositeRecord.", e);
            }
        }
    }    
}