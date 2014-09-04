package org.hps.record.composite;

import java.util.ArrayList;
import java.util.List;

import org.freehep.record.loop.LoopEvent;
import org.hps.record.RecordProcessingException;
import org.hps.record.RecordProcessor;

public abstract class RecordProcessorAdapter<RecordType> extends CompositeLoopAdapter {

    List<RecordProcessor<RecordType>> processors = new ArrayList<RecordProcessor<RecordType>>();
    
    public void addProcessor(RecordProcessor<RecordType> processor) {
        processors.add(processor);
    }
    
    public void removeProcessor(RecordProcessor<RecordType> processor) {
        processors.remove(processor);
    }
    
    public void startRun(RecordType record) {
        for (RecordProcessor<RecordType> processor : processors) {
            processor.startRun(record);
        }
    }
    
    public void endRun(RecordType record) {
        for (RecordProcessor<RecordType> processor : processors) {
            processor.endRun(record);
        }
    }
    
    public void process(RecordType record) {
        for (RecordProcessor<RecordType> processor : processors) {
            try {
                processor.process(record);
            } catch (Exception e) {
                throw new RecordProcessingException("Error processing record.", e);
            }
        }
    }
    
    public void finish(LoopEvent loopEvent) {
        for (RecordProcessor<RecordType> processor : processors) {
            processor.endJob();
        }
    }

    public void start(LoopEvent loopEvent) {
        for (RecordProcessor<RecordType> processor : processors) {
            processor.startJob();
        }
    }
    
    public void suspend(LoopEvent loopEvent) {
        for (RecordProcessor<RecordType> processor : processors) {
            processor.suspend();
        }
    }           
}
