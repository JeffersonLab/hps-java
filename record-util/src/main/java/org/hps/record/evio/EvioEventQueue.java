package org.hps.record.evio;

import org.hps.record.AbstractRecordQueue;
import org.jlab.coda.jevio.EvioEvent;

/**
 * A dynamic queue providing <tt>EvioEvent</tt> objects to a loop.
 */
public final class EvioEventQueue extends AbstractRecordQueue<EvioEvent> {

    public EvioEventQueue(long timeoutMillis, int maxSize) {
        super(timeoutMillis, maxSize);
    }
    
	/**
	 * Get the class of the supplied records.
	 * @return The class of the supplied records.
	 */
    @Override
    public Class<EvioEvent> getRecordClass() {
        return EvioEvent.class;
    }
}
