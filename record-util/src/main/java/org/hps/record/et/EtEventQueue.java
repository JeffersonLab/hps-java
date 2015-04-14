package org.hps.record.et;

import org.hps.record.AbstractRecordQueue;
import org.jlab.coda.et.EtEvent;

/**
 * A dynamic queue for supplying <tt>EtEvent</tt> objects to a loop.
 * <p>
 * This should be run on a separate thread than the loop to avoid undesired blocking behavior.
 *
 * @author <a href="mailto:jeremym@slac.stanford.edu">Jeremy McCormick</a>
 */
// FIXME: This class is unused within HPS Java.
public final class EtEventQueue extends AbstractRecordQueue<EtEvent> {

    /**
     * Get the class of the record that is supplied.
     *
     * @return the class of the supplied records
     */
    @Override
    public Class<EtEvent> getRecordClass() {
        return EtEvent.class;
    }
}