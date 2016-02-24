package org.hps.record;

import java.util.Collection;

import org.freehep.record.loop.DefaultRecordLoop;

public abstract class AbstractRecordLoop<RecordType> extends DefaultRecordLoop {
    
    protected AbstractLoopAdapter<RecordType> adapter;
    
    public void addProcessors(Collection<AbstractRecordProcessor<RecordType>> processors) {
        for (AbstractRecordProcessor<RecordType> processor : processors) {
            adapter.addProcessor(processor);
        }
    }
    
    public void addProcessor(AbstractRecordProcessor<RecordType> processor) {
        adapter.addProcessor(processor);
    }
    
    /**
     * Loop over events from the source.
     *
     * @param number the number of events to process or -1L for all events from the source
     * @return the number of records that were processed
     */
    public long loop(final long number) {
        if (number < 0L) {
            this.execute(Command.GO, true);
        } else {
            this.execute(Command.GO_N, number, true);
            this.execute(Command.STOP);
        }
        return this.getSupplied();
    }
}
