package org.hps.record.evio;

import org.hps.record.AbstractRecordQueue;
import org.jlab.coda.jevio.EvioEvent;

/**
 * A dynamic queue providing <tt>EvioEvent</tt> objects to a loop.
 */
public final class EvioRecordQueue extends AbstractRecordQueue<EvioEvent> {

    @Override
    public Class<EvioEvent> getRecordClass() {
        return EvioEvent.class;
    }
}
