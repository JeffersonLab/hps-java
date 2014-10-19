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
 * Classes that should be part of an "event processing chain" implemented by the 
 * {@link CompositeLoop} should extend this API in order to receive {@link CompositeRecord}
 * objects that can be modified.
 */
public class CompositeLoopAdapter extends AbstractLoopListener implements RecordListener {

    List<CompositeRecordProcessor> processors = new ArrayList<CompositeRecordProcessor>();

    /**
     * Add a <tt>CompositeRecordProcessor</tt> that will listen to this loop.
     * @param processor The composite record processor to add.
     */
    public void addProcessor(CompositeRecordProcessor processor) {
        processors.add(processor);
    }
    
    /**
     * Callback for loop finish event.
     * @param loopEvent 
     */
    public void finish(LoopEvent loopEvent) {
        System.out.println(this.getClass().getCanonicalName() + ".finish");
        // Call end job hook on all processors.
        for (CompositeRecordProcessor processor : processors) {
            processor.endJob();
        }
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
     * Process one record.
     * @param record 
     */
    @Override
    public void recordSupplied(RecordEvent record) {
        for (CompositeRecordProcessor processor : processors) {
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