package org.hps.record.evio;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.freehep.record.loop.AbstractLoopListener;
import org.freehep.record.loop.LoopEvent;
import org.freehep.record.loop.LoopListener;
import org.freehep.record.loop.RecordEvent;
import org.freehep.record.loop.RecordListener;
import org.jlab.coda.jevio.EvioEvent;

/**
 * A loop adapter for the {@link EvioLoop} which manages and activates a list of {@link EvioEventProcessor} objects.
 *
 * @author Jeremy McCormick, SLAC
 */
public final class EvioLoopAdapter extends AbstractLoopListener implements RecordListener, LoopListener {

    /**
     * Initialize the logger.
     */
    private static final Logger LOGGER = Logger.getLogger(EvioLoopAdapter.class.getPackage().getName());

    /**
     * List of event processors to activate.
     */
    private final List<EvioEventProcessor> processors = new ArrayList<EvioEventProcessor>();

    /**
     * Create a new loop adapter.
     */
    EvioLoopAdapter() {
    }

    /**
     * Add an EVIO processor to the adapter.
     *
     * @param processor the EVIO processor to add to the adapter
     */
    void addEvioEventProcessor(final EvioEventProcessor processor) {
        LOGGER.info("adding " + processor.getClass().getName() + " to EVIO processors");
        this.processors.add(processor);
    }

    /**
     * Implementation of the finish hook which activates the {@link EvioEventProcessor#endJob()} method of all
     * registered processors.
     */
    @Override
    protected void finish(final LoopEvent event) {
        LOGGER.info("finish");
        for (final EvioEventProcessor processor : processors) {
            processor.endJob();
        }
    }

    /**
     * Primary event processing method that activates the {@link EvioEventProcessor#process(EvioEvent)} method of all
     * registered processors.
     *
     * @param recordEvent the record event to process which should have an EVIO event
     * @throws IllegalArgumentException if the record is the wrong type
     */
    @Override
    public void recordSupplied(final RecordEvent recordEvent) {
        final Object record = recordEvent.getRecord();
        if (record instanceof EvioEvent) {
            final EvioEvent evioEvent = EvioEvent.class.cast(record);
            for (final EvioEventProcessor processor : processors) {
                try {
                    processor.process(evioEvent);
                } catch (final Exception e) {
                    throw new RuntimeException(e);
                }
            }
        } else {
            throw new IllegalArgumentException("The supplied record has the wrong type: " + record.getClass());
        }
    }

    /**
     * Implementation of the start hook which activates the {@link EvioEventProcessor#startJob()} method of all
     * registered processors.
     */
    @Override
    protected void start(final LoopEvent event) {
        LOGGER.info("start");
        for (final EvioEventProcessor processor : processors) {
            processor.startJob();
        }
    }
}
