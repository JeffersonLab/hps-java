package org.hps.monitoring.record.composite;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.freehep.record.source.AbstractRecordSource;
import org.freehep.record.source.NoSuchRecordException;
import org.hps.monitoring.record.EventProcessingStep;


public class CompositeRecordSource extends AbstractRecordSource {

    CompositeRecord currentRecord;
    List<EventProcessingStep> processingSteps = new ArrayList<EventProcessingStep>();
            
    public void next() throws IOException, NoSuchRecordException {
        currentRecord = new CompositeRecord();
        
        // Execute sub-processing that will alter the CompositeRecord.
        // FIXME: Should this happen here???
        for (EventProcessingStep step : this.processingSteps) {
            try {
                step.execute();
            } catch (Exception e) {
                System.out.println("Exception " + e.getClass().getCanonicalName() + " caught from " + step.getClass().getCanonicalName() + ".");                
                System.out.println(e.getMessage());
                currentRecord = null;
                throw e;
            }
        }
    }
        
    void addProcessingSteps(List<EventProcessingStep> processingSteps) {
        this.processingSteps = processingSteps;
    }    
    
    @Override
    public Object getCurrentRecord() throws IOException {
        return currentRecord;
    }
    
    @Override
    public boolean supportsCurrent() {
        return true;
    }

    @Override
    public boolean supportsNext() {
        return true;
    }
  
    @Override
    public boolean supportsPrevious() {
        return false;
    }
  
    @Override
    public boolean supportsIndex() {
        return false;
    }
  
    @Override 
    public boolean supportsShift() {
        return false;
    }
  
    @Override
    public boolean supportsRewind() {
        return false;
    }

    @Override
    public boolean hasCurrent() {
        return currentRecord != null;
    }

    @Override
    public boolean hasNext() {
        return true;
    }
}
