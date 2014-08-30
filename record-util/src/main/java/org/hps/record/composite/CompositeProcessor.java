package org.hps.record.composite;

import org.hps.record.ErrorState;
import org.hps.record.HasErrorState;
import org.hps.record.RecordProcessor;

/**
 * An <code>EventProcessor</code> implementation for processing <code>CompositeRecord</code>
 * records.
 */
public abstract class CompositeProcessor implements RecordProcessor<CompositeRecord>, HasErrorState {

    ErrorState errorState = new ErrorState();
    
    public ErrorState getErrorState() {
        return errorState;
    }
    
    @Override
    public void startJob() {
    }

    @Override
    public void startRun(CompositeRecord event) {
    }

    @Override
    public void process(CompositeRecord event) throws Exception {
    }

    @Override
    public void endRun(CompositeRecord event) {
    }

    @Override
    public void endJob() {
    } 
}