package org.hps.record.epics;

import java.io.IOException;

import org.hps.record.et.EtEventProcessor;
import org.jlab.coda.et.EtEvent;
import org.jlab.coda.jevio.EvioEvent;
import org.jlab.coda.jevio.EvioException;
import org.jlab.coda.jevio.EvioReader;

/**
 * An ET event processor that builds EPICS events.
 *
 */
// FIXME: Class is currently unused.
public class EpicsEtProcessor extends EtEventProcessor {

    /**
     * The processor for creating the EPICS data class from EVIO data.
     */
    private final EpicsEvioProcessor evioProcessor = new EpicsEvioProcessor();

    /**
     * Process an <code>EtEvent</code> and create an EPICS data object from it.
     *
     * @param event the <code>EtEvent</code> to process
     */
    @Override
    public void process(final EtEvent event) {
        EvioEvent evio;
        try {
            evio = new EvioReader(event.getDataBuffer()).parseNextEvent();
        } catch (IOException | EvioException e) {
            throw new RuntimeException(e);
        }
        this.evioProcessor.process(evio);
        if (this.evioProcessor.getEpicsData() != null) {
            System.out.println("EpicsEtProcessor created EpicsData ...");
            System.out.println(this.evioProcessor.getEpicsData());
        }
    }
}
