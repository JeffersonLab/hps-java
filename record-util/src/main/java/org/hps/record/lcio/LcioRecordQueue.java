package org.hps.record.lcio;

import org.hps.record.AbstractRecordQueue;
import org.lcsim.event.EventHeader;

/**
 * A dynamic queue providing <tt>EvioEvent</tt> objects to a loop.
 */
public final class LcioRecordQueue extends AbstractRecordQueue<EventHeader> {

    @Override
    public Class<EventHeader> getRecordClass() {
        return EventHeader.class;
    }
}