package org.hps.record.composite;

import java.util.ArrayList;
import java.util.List;

import org.freehep.record.loop.LoopEvent;
import org.hps.record.RecordProcessingException;
import org.hps.record.RecordProcessor;

/**
 * An extension of {@link CompositeLoopAdapter} which has a list of {@link org.hps.record.RecordProcessor} objects that
 * are activated in the appropriate hook methods for every event.
 *
 * @param <RecordType> The concrete type of the record being processed.
 */
public abstract class RecordProcessorAdapter<RecordType> extends CompositeLoopAdapter {

    /**
     * The list of composite record processors.
     */
    private final List<RecordProcessor<RecordType>> processors = new ArrayList<RecordProcessor<RecordType>>();

    /**
     * Add a <code>RecordProcessor</code>.
     *
     * @param processor the <code>RecordProcessor</code> to add
     */
    public void addProcessor(final RecordProcessor<RecordType> processor) {
        this.processors.add(processor);
    }

    /**
     * Activate the <code>endRun</code> methods of the registered processors.
     *
     * @param record the current record.
     */
    public void endRun(final RecordType record) {
        for (final RecordProcessor<RecordType> processor : this.processors) {
            processor.endRun(record);
        }
    }

    /**
     * Activate the <code>endJob</code> methods of the registered processors.
     *
     * @param loopEvent the <code>LoopEvent</code> which activated <code>finish</code>
     */
    @Override
    public void finish(final LoopEvent loopEvent) {
        for (final RecordProcessor<RecordType> processor : this.processors) {
            processor.endJob();
        }
    }

    /**
     * Activate the <code>process</code> methods of the registered processors.
     *
     * @param record the current record
     */
    public void process(final RecordType record) {
        for (final RecordProcessor<RecordType> processor : this.processors) {
            try {
                processor.process(record);
            } catch (final Exception e) {
                throw new RecordProcessingException("Error processing record.", e);
            }
        }
    }

    /**
     * Remove a <code>RecordProcessor</code> from the adapter.
     *
     * @param processor the <code>RecordProcessor</code> to remove
     */
    public void removeProcessor(final RecordProcessor<RecordType> processor) {
        this.processors.remove(processor);
    }

    /**
     * Activate the <code>startJob</code> methods of the registered processors.
     *
     * @param loopEvent the <code>LoopEvent</code> which activated the start
     */
    @Override
    public void start(final LoopEvent loopEvent) {
        for (final RecordProcessor<RecordType> processor : this.processors) {
            processor.startJob();
        }
    }

    /**
     * Activate the <code>startRun</code> methods of the registered processors.
     *
     * @param record the current record
     */
    public void startRun(final RecordType record) {
        for (final RecordProcessor<RecordType> processor : this.processors) {
            processor.startRun(record);
        }
    }

    /**
     * Activate the <code>suspend</code> methods of the registered processors.
     *
     * @param loopEvent the <code>LoopEvent</code> which activated <code>suspend</code>.
     */
    @Override
    public void suspend(final LoopEvent loopEvent) {
        for (final RecordProcessor<RecordType> processor : this.processors) {
            processor.suspend();
        }
    }
}
