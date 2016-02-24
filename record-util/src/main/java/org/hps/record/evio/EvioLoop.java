package org.hps.record.evio;

import org.hps.record.AbstractRecordLoop;
import org.jlab.coda.jevio.EvioEvent;

/**
 * Implementation of a Freehep <code>RecordLoop</code> for EVIO data.
 *
 * @author Jeremy McCormick, SLAC
 */
public class EvioLoop extends AbstractRecordLoop<EvioEvent> {

    /**
     * Create a new record loop.
     */
    public EvioLoop() {
        this.adapter = new EvioLoopAdapter();
        this.addLoopListener(adapter);
        this.addRecordListener(adapter);
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
