package org.hps.monitoring.record.lcio;

import org.hps.monitoring.record.AbstractRecordQueue;
import org.lcsim.event.EventHeader;

/**
 * A dynamic queue providing <tt>EvioEvent</tt> objects to a loop.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class LcioEventQueue extends AbstractRecordQueue<EventHeader> {

    @Override
    public Class<EventHeader> getRecordClass() {
        return EventHeader.class;
    }
}