package org.hps.monitoring.record.composite;

import org.hps.monitoring.record.EventProcessor;

/**
 * An <code>EventProcessor</code> implementation for processing <code>CompositeRecord</code>
 * records.
 */
public class CompositeRecordProcessor implements EventProcessor<CompositeRecord> {

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