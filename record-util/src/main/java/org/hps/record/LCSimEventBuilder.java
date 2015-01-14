package org.hps.record;

import org.jlab.coda.jevio.EvioEvent;
import org.lcsim.conditions.ConditionsListener;
import org.lcsim.event.EventHeader;

/**
 * This is an interface that should be implemented by classes that 
 * build LCSim events from EVIO raw data.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public interface LCSimEventBuilder extends ConditionsListener {

    /**
     * Read information from an EVIO control event such as go or pre start
     * to set the event builder's state. This does not actually build an 
     * LCSim event.
     * @param evioEvent The input EvioEvent.
     */
    void readEvioEvent(EvioEvent evioEvent);

    /**
     * Build the LCSim event from EVIO data.
     * @param evioEvent The input EvioEvent.
     * @return The LCSim event.
     */
    EventHeader makeLCSimEvent(EvioEvent evioEvent);
}
