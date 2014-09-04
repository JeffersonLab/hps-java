package org.hps.record.composite;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.freehep.record.loop.LoopEvent;
import org.freehep.record.loop.RecordEvent;
import org.freehep.record.source.NoSuchRecordException;
import org.hps.record.RecordProcessingException;
import org.hps.record.et.EtProcessor;
import org.hps.record.et.EtSource;
import org.jlab.coda.et.EtEvent;

/**
 * An adapter for directly using the CompositeLoop to supply and process EtEvents.
 */
public class CompositeEtAdapter extends CompositeLoopAdapter {

    EtSource source;
    List<EtProcessor> processors = new ArrayList<EtProcessor>();
    
    public CompositeEtAdapter(EtSource source) {
        this.source = source;
    }
    
    public void addProcessor(EtProcessor processor) {
        processors.add(processor);
    }
    
    public void process(EtEvent event) {
        for (EtProcessor processor : processors) {
            processor.process(event);
        }
    }
    
    public void finish(LoopEvent event) {
        for (EtProcessor processor : processors) {
            processor.endJob();
        }
    }
    
    public void start(LoopEvent event) {
        for (EtProcessor processor : processors) {
            processor.startJob();
        }
    }
    
    public void recordSupplied(RecordEvent record) {
        //System.out.println("CompositeEtAdapter.recordSupplied");
        System.out.flush();
        CompositeRecord compositeRecord = (CompositeRecord) record.getRecord();
        try {
            source.next();
            if (source.getCurrentRecord() != null) {
                compositeRecord.setEtEvent((EtEvent) source.getCurrentRecord());                
            } else {
                throw new NoSuchRecordException("No ET current record available from EtSource.");
            }            
            process(compositeRecord.getEtEvent());
        } catch (IOException | NoSuchRecordException e) {
            throw new RecordProcessingException("Error getting next ET record.", e);
        }
    }
}
