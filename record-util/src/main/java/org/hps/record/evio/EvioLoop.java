package org.hps.record.evio;

import org.freehep.record.loop.DefaultRecordLoop;

/**
 * Implementation of a Freehep <code>RecordLoop</code> for EVIO data.
 *
 * @author Jeremy McCormick, SLAC
 */
public class EvioLoop extends DefaultRecordLoop {

    /**
     * The record adapter.
     */
    private final EvioLoopAdapter adapter = new EvioLoopAdapter();

    /**
     * Create a new record loop.
     */
    public EvioLoop() {
        this.addLoopListener(adapter);
        this.addRecordListener(adapter);
    }

    /**
     * Add an EVIO event processor to the adapter which will be activated for every EVIO event that is processed.
     *
     * @param evioEventProcessor the EVIO processor to add
     */
    public void addEvioEventProcessor(final EvioEventProcessor evioEventProcessor) {
        adapter.addEvioEventProcessor(evioEventProcessor);
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

    /**
     * Set the EVIO data source.
     *
     * @param evioFileSource the EVIO data source
     */
    public void setEvioFileSource(final EvioFileSource evioFileSource) {
        this.setRecordSource(evioFileSource);
    }
}
