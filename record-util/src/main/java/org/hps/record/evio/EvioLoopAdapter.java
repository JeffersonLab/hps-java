package org.hps.record.evio;

import org.hps.record.AbstractLoopAdapter;
import org.jlab.coda.jevio.EvioEvent;

/**
 * A loop adapter for the {@link EvioLoop} which manages and activates a list of {@link EvioEventProcessor} objects.
 *
 * @author Jeremy McCormick, SLAC
 */
public final class EvioLoopAdapter extends AbstractLoopAdapter<EvioEvent> {
}
