package org.hps.record.composite;

import org.hps.record.EventProcessor;

/**
 * An <code>EventProcessor</code> implementation for processing <code>CompositeRecord</code>
 * records.
 */
public abstract class CompositeRecordProcessor implements EventProcessor<CompositeRecord> {

    @Override
    public void startJob() {
    }

    @Override
    public void startRun(CompositeRecord event) {
    }

    @Override
    public void processEvent(CompositeRecord event) throws Exception {
    }

    @Override
    public void endRun(CompositeRecord event) {
    }

    @Override
    public void endJob() {
    } 
}