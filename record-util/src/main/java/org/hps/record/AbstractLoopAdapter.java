package org.hps.record;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.freehep.record.loop.AbstractLoopListener;
import org.freehep.record.loop.LoopEvent;
import org.freehep.record.loop.LoopListener;
import org.freehep.record.loop.RecordEvent;
import org.freehep.record.loop.RecordListener;

public abstract class AbstractLoopAdapter<RecordType> extends AbstractLoopListener implements RecordListener, LoopListener {

    private Logger LOGGER = Logger.getLogger(AbstractLoopAdapter.class.getPackage().getName());
    
    private List<AbstractRecordProcessor<RecordType>> processors = new ArrayList<AbstractRecordProcessor<RecordType>>();
           
    @Override
    public void recordSupplied(RecordEvent recordEvent) {
        //LOGGER.info("recordSupplied " + recordEvent.toString());
        final RecordType record = (RecordType) recordEvent.getRecord();
        for (final AbstractRecordProcessor<RecordType> processor : processors) {
            try {
                if (processor.isActive()) {
                    //LOGGER.info("activating processor " + processor.getClass().getName());
                    processor.process(record);
                }
            } catch (final Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
    
    @Override
    protected void finish(final LoopEvent event) {
        for (final AbstractRecordProcessor<RecordType> processor : processors) {
            processor.endJob();
        }
    }
    
    @Override
    protected void start(final LoopEvent event) {
        for (final AbstractRecordProcessor<RecordType> processor : processors) {
            processor.startJob();
        }
    }
    
    void addProcessor(AbstractRecordProcessor<RecordType> processor) {
        this.processors.add(processor);
    }    
}
