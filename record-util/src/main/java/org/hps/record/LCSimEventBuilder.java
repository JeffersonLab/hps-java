package org.hps.record;

import org.jlab.coda.jevio.EvioEvent;
import org.lcsim.conditions.ConditionsListener;
import org.lcsim.event.EventHeader;

/**
 * This is an interface that should be implemented by classes which build LCSim events from EVIO raw data.
 *
 * @author <a href="mailto:jeremym@slac.stanford.edu">Jeremy McCormick</a>
 */
public interface LCSimEventBuilder extends ConditionsListener {

    /**
     * Build the LCSim event from an EVIO event.
     *
     * @param evioEvent the input <code>EvioEvent</code>
     * @return the LCSim event
     */
    EventHeader makeLCSimEvent(EvioEvent evioEvent);

    /**
     * Read information from an EVIO control event such as a PRESTART event to set the event builder's state.
     * <p>
     * This does not actually build an LCSim event.
     *
     * @param evioEvent the input <code>EvioEvent</code>
     */
    void readEvioEvent(EvioEvent evioEvent);
}
