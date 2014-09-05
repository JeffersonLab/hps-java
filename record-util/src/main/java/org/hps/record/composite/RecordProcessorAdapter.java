package org.hps.record.composite;

import java.util.ArrayList;
import java.util.List;

import org.freehep.record.loop.LoopEvent;
import org.hps.record.RecordProcessingException;
import org.hps.record.RecordProcessor;

/**
 * An extension of {@link CompositeLoopAdapter} that has a list of {@link org.hps.record.RecordProcessor}
 * objects that are activated in the appropriate hook methods.
 * @param <RecordType> The concrete type of the record being processed.
 */
public abstract class RecordProcessorAdapter<RecordType> extends CompositeLoopAdapter {

    List<RecordProcessor<RecordType>> processors = new ArrayList<RecordProcessor<RecordType>>();
    
    /**
     * Add a <code>RecordProcessor</code>.
     * @param processor The RecordProcessor to add.
     */
    public void addProcessor(RecordProcessor<RecordType> processor) {
        processors.add(processor);
    }
    
    /**
     * Remove a <code>RecordProcessor</code>.
     * @param processor The RecordProcessor to remove.
     */
    public void removeProcessor(RecordProcessor<RecordType> processor) {
        processors.remove(processor);
    }
    
    /**
     * Activate the <code>startRun</code> methods of the 
     * registered processors.
     * @param record The current record.
     */
    public void startRun(RecordType record) {
        for (RecordProcessor<RecordType> processor : processors) {
            processor.startRun(record);
        }
    }
    
    /**
     * Activate the <code>endRun</code> methods of the 
     * registered processors.
     * @param record The current record.
     */
    public void endRun(RecordType record) {
        for (RecordProcessor<RecordType> processor : processors) {
            processor.endRun(record);
        }
    }
    
    /**
     * Activate the <code>process</code> methods of the 
     * registered processors.
     * @param record The current record.
     */
    public void process(RecordType record) {
        for (RecordProcessor<RecordType> processor : processors) {
            try {
                processor.process(record);
            } catch (Exception e) {
                throw new RecordProcessingException("Error processing record.", e);
            }
        }
    }
    
    /**
     * Activate the <code>endJob</code> methods of the
     * registered processors.
     * @param The LoopEvent which activated finish.
     */
    public void finish(LoopEvent loopEvent) {
        for (RecordProcessor<RecordType> processor : processors) {
            processor.endJob();
        }
    }

    /**
     * Activate the <code>startJob</code> methods of the
     * registered processors.
     * @param The LoopEvent which activated the start.
     */
    public void start(LoopEvent loopEvent) {
        for (RecordProcessor<RecordType> processor : processors) {
            processor.startJob();
        }
    }
    
    /**
     * Activate the <code>suspend</code> methods of the 
     * registered processors.
     * @param The LoopEvent which activated the suspend.
     */
    public void suspend(LoopEvent loopEvent) {
        for (RecordProcessor<RecordType> processor : processors) {
            processor.suspend();
        }
    }           
}
