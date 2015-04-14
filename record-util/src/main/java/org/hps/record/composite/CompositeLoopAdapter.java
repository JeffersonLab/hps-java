package org.hps.record.composite;

import java.util.ArrayList;
import java.util.List;

import org.freehep.record.loop.AbstractLoopListener;
import org.freehep.record.loop.LoopEvent;
import org.freehep.record.loop.RecordEvent;
import org.freehep.record.loop.RecordListener;
import org.hps.record.RecordProcessingException;

/**
 * Adapter for listening on the {@link CompositeLoop} for records and loop events.
 * <p>
 * Classes that should be part of an event processing chain implemented by the {@link CompositeLoop} should extend this
 * API in order to receive {@link CompositeRecord} objects that can be read or modified.
 *
 * @author <a href="mailto:jeremym@slac.stanford.edu">Jeremy McCormick</a>
 */
public class CompositeLoopAdapter extends AbstractLoopListener implements RecordListener {

    /**
     * The list of processors that will be activated for each record this adapter receives.
     */
    private final List<CompositeRecordProcessor> processors = new ArrayList<CompositeRecordProcessor>();

    /**
     * Add a <code>CompositeRecordProcessor</code> that will process records from the loop as they are received by the
     * adapter.
     *
     * @param processor the record processor to add
     */
    public void addProcessor(final CompositeRecordProcessor processor) {
        this.processors.add(processor);
    }

    /**
     * Action for loop finish event.
     *
     * @param loopEvent the <code>LoopEvent</code> with information about record processing session
     */
    @Override
    public void finish(final LoopEvent loopEvent) {
        // System.out.println(this.getClass().getCanonicalName() + ".finish");
        // Call end job hook on all processors.
        for (final CompositeRecordProcessor processor : this.processors) {
            processor.endJob();
        }
    }

    /**
     * Process one record with this adapter.
     *
     * @param record the record to process
     */
    @Override
    public void recordSupplied(final RecordEvent record) {
        for (final CompositeRecordProcessor processor : this.processors) {
            try {
                // Activate the processing step on the CompositeRecord.
                processor.process((CompositeRecord) record.getRecord());
            } catch (final Exception e) {
                // Throw the processing error so the loop can perform proper handling of it.
                throw new RecordProcessingException("Exception occurred during record processing.", e);
            }
        }
    }

    /**
     * Start event processing which will call {@link CompositeRecordProcessor#startJob()} on all the registered
     * processors.
     *
     * @param loopEvent the <code>LoopEvent</code> which has record processing information about the session
     */
    @Override
    public void start(final LoopEvent loopEvent) {
        for (final CompositeRecordProcessor processor : this.processors) {
            processor.startJob();
        }
    }
}