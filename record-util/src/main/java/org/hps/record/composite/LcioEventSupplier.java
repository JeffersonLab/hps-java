package org.hps.record.composite;

import org.freehep.application.Application;
import org.freehep.application.studio.Studio;
import org.freehep.record.loop.RecordEvent;
import org.hps.record.lcio.LcioEventQueue;

/**
 * This is an adapter that can supply LCIO <code>EventHeader</code> objects to JAS3 via a registered
 * <code>DataSource</code> in order to activate Wired, the LCSim Event Browser, etc.
 */
public class LcioEventSupplier extends CompositeLoopAdapter {

    /**
     * The LCIO event queue which supplies records.
     */
    private final LcioEventQueue events;

    /**
     * Class constructor.
     *
     * @param timeoutMillis the queue timeout in milliseconds
     * @param maxSize the maximum size of the record queue
     */
    LcioEventSupplier(final long timeoutMillis, final int maxSize) {
        this.events = new LcioEventQueue(timeoutMillis, maxSize);
        this.events.setName(LcioEventSupplier.class.getName());
        final Studio studio = (Studio) Application.getApplication();
        if (studio != null) {
            studio.getLookup().add(this.events);
        }
    }

    /**
     * Process composite records by adding their LCIO event references to the queue in order to supply them to JAS3.
     *
     * @param record the composite record
     */
    @Override
    public void recordSupplied(final RecordEvent record) {
        final CompositeRecord compositeRecord = (CompositeRecord) record.getRecord();
        if (compositeRecord.getLcioEvent() != null) {
            System.out.println("LcioEventSupplier - adding event #" + compositeRecord.getLcioEvent().getEventNumber()
                    + " to queue");
            this.events.addRecord(compositeRecord.getLcioEvent());
        }
    }
}
