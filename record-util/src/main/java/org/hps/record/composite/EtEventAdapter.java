package org.hps.record.composite;

import java.io.IOException;

import org.freehep.record.loop.RecordEvent;
import org.freehep.record.source.NoSuchRecordException;
import org.hps.record.RecordProcessingException;
import org.hps.record.et.EtEventSource;
import org.jlab.coda.et.EtEvent;

/**
 * An adapter for directly using the CompositeLoop to supply and process EtEvents.
 */
public class EtEventAdapter extends RecordProcessorAdapter<EtEvent> {

    EtEventSource source;
    
    public EtEventAdapter(EtEventSource source) {
        this.source = source;
    }
              
    public void recordSupplied(RecordEvent record) {
        CompositeRecord compositeRecord = (CompositeRecord) record.getRecord();
        try {
            source.next();
            if (source.getCurrentRecord() != null) {
                compositeRecord.setEtEvent((EtEvent) source.getCurrentRecord());                
            } else {
                throw new NoSuchRecordException("No current ET record available from source.");
            }            
            process(compositeRecord.getEtEvent());
        } catch (IOException | NoSuchRecordException e) {
            throw new RecordProcessingException("Error processing ET record.", e);
        }
    }
}
