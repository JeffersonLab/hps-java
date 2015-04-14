package org.hps.record.evio;

import org.hps.record.AbstractRecordQueue;
import org.jlab.coda.jevio.EvioEvent;

/**
 * A dynamic queue providing <tt>EvioEvent</tt> objects to a loop.
 *
 * @author <a href="mailto:jeremym@slac.stanford.edu">Jeremy McCormick</a>
 */
public final class EvioEventQueue extends AbstractRecordQueue<EvioEvent> {

    /**
     * Class constructor.
     *
     * @param timeoutMillis the timeout for accessing records from the queue
     * @param maxSize the maximum queue size
     */
    public EvioEventQueue(final long timeoutMillis, final int maxSize) {
        super(timeoutMillis, maxSize);
    }

    /**
     * Get the class of the supplied records.
     *
     * @return the class of the supplied records
     */
    @Override
    public Class<EvioEvent> getRecordClass() {
        return EvioEvent.class;
    }
}
