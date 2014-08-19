package org.hps.monitoring.record.evio;

import org.hps.monitoring.record.AbstractRecordQueue;
import org.jlab.coda.jevio.EvioEvent;

/**
 * A dynamic queue providing <tt>EvioEvent</tt> objects to a loop.
 */
public final class EvioEventQueue extends AbstractRecordQueue<EvioEvent> {

    @Override
    public Class<EvioEvent> getRecordClass() {
        return EvioEvent.class;
    }
}
