package org.hps.record.composite;

import java.util.ArrayList;
import java.util.List;

import org.freehep.record.loop.AbstractLoopListener;
import org.freehep.record.loop.LoopEvent;
import org.freehep.record.loop.RecordEvent;
import org.freehep.record.loop.RecordListener;
import org.hps.record.RecordProcessingException;

/**
 * Adapter for listening on the {@link CompositeLoop} for records and loop events.
 */
public final class CompositeLoopAdapter extends AbstractLoopListener implements RecordListener {

    List<CompositeProcessor> processors = new ArrayList<CompositeProcessor>();

    /**
     * Add a <tt>CompositeRecordProcessor</tt> that will listen to this loop.
     * @param processor The composite record processor to add.
     */
    void addProcessor(CompositeProcessor processor) {
        processors.add(processor);
    }
    
    /**
     * Callback for loop finish event.
     * @param loopEvent 
     */
    public void finish(LoopEvent loopEvent) {
        // Call end job hook on all processors.
        for (CompositeProcessor processor : processors) {
            processor.endJob();
        }
    }
        
    /**
     * Start event processing which will call {@link CompositeProcessor#startJob()}
     * on all the registered processors.
     * @param loopEvent
     */
    public void start(LoopEvent loopEvent) {
        for (CompositeProcessor processor : processors) {
            processor.startJob();
        }
    }
    
    /**
     * Suspend the loop.
     * @param loopEvent
     */
    public void suspend(LoopEvent loopEvent) { 
        if (loopEvent.getException() != null) {
            loopEvent.getException().printStackTrace();
        }
    }
        
    /**
     * Process one record.
     * @param record 
     */
    @Override
    public void recordSupplied(RecordEvent record) {
        for (CompositeProcessor processor : processors) {
            try {
                // Activate the processing step on the CompositeRecord.
                processor.process((CompositeRecord) record.getRecord());
            } catch (Exception e) {
                // Throw the processing error so the loop can perform proper handling of it.
                throw new RecordProcessingException("Exception occurred during record processing.", e);
            }
        }
    }      
}