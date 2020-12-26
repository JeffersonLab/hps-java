package org.hps.online.recon.eventbus;

import org.hps.record.evio.EvioEventUtilities;
import org.jlab.coda.et.EtEvent;
import org.jlab.coda.jevio.EvioEvent;
import org.jlab.coda.jevio.EvioReader;

import com.google.common.eventbus.Subscribe;

/**
 * Receive an ET event, convert to EVIO, and post the
 * new EVIO event
 */
public class EtListener {

    private OnlineEventBus eventbus;

    EtListener(OnlineEventBus eventbus) {
        this.eventbus = eventbus;
    }

    // Could this receive an array of ET events?
    @Subscribe
    public void receiveEtAndPostEvio(EtEvent etEvent) throws Exception {
        try {
            eventbus.getLogger().info("EtListener - got etEvent: " + etEvent.getId());
            EvioEvent evioEvent = new EvioReader(etEvent.getDataBuffer()).parseNextEvent();
            eventbus.getLogger().info("EtListener - posting evioEvent: " + EvioEventUtilities.getEventIdData(evioEvent)[0]);
            eventbus.post(evioEvent);
        } catch (Exception e) {
            eventbus.error(e, false);
        }

        // TODO: Post an EVIO "conditions event" here if event has a new run number
    }
}
