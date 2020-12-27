package org.hps.online.recon.eventbus;

import org.hps.record.evio.EvioEventUtilities;
import org.jlab.coda.et.EtEvent;
import org.jlab.coda.jevio.EvioEvent;
import org.jlab.coda.jevio.EvioReader;

import com.google.common.eventbus.Subscribe;

/**
 * Receive an ET event on the event bus, convert it to EVIO,
 * and post the new EVIO event.
 */
public class EtListener {

    private OnlineEventBus eventbus;

    EtListener(OnlineEventBus eventbus) {
        this.eventbus = eventbus;
    }

    @Subscribe
    public void receiveEtAndPostEvio(EtEvent etEvent) throws Exception {
        try {
            EvioEvent evioEvent = new EvioReader(etEvent.getDataBuffer()).parseNextEvent();
            eventbus.getLogger().info("Created EVIO event: " + EvioEventUtilities.getEventIdData(evioEvent)[0]);
            eventbus.post(evioEvent);
        } catch (Exception e) {
            eventbus.post(new EventProcessingError(e, false));
        }
    }
}
