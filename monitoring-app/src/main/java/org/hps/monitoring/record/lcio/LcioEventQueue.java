package org.hps.monitoring.record.lcio;

import org.hps.monitoring.record.AbstractRecordQueue;
import org.lcsim.event.EventHeader;

/**
 * A dynamic queue providing <tt>EvioEvent</tt> objects to a loop.
 */
public final class LcioEventQueue extends AbstractRecordQueue<EventHeader> {

    @Override
    public Class<EventHeader> getRecordClass() {
        return EventHeader.class;
    }
}