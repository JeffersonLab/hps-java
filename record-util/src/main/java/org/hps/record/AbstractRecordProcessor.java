package org.hps.record;


/**
 * An abstract implementation of {@link RecordProcessor} with "no op" method implementions.
 * Concrete implementations of <code>RecordProcessor</code> should extend this class.
 *
 * @param <RecordType> The type of the record processed by this class.
 */
public abstract class AbstractRecordProcessor<RecordType> implements RecordProcessor<RecordType> {

    @Override
    public void startJob() {        
    }

    @Override
    public void startRun(RecordType record) {        
    }

    @Override
    public void process(RecordType record) throws Exception {        
    }

    @Override
    public void endRun(RecordType record) {        
    }

    @Override
    public void endJob() {        
    }

    @Override
    public void suspend() {        
    }
}
