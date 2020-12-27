package org.hps.online.recon.eventbus;

import org.lcsim.event.EventHeader;

import com.google.common.eventbus.Subscribe;

/**
 * Receive LCIO events and count them.
 */
public class LcioListener {

    OnlineEventBus eventbus;
    int eventsReceived = 0;

    LcioListener(OnlineEventBus eventbus) {
        this.eventbus = eventbus;
    }

    @Subscribe
    public void receiveLcio(EventHeader event) {
        // eventbus.getLogger().info("LcioListener - received event: " + event.getEventNumber());
        ++eventsReceived;
    }

    @Subscribe
    public void receiveStop(Stop stop) {
        this.eventbus.getLogger().info("Total LCIO events created: " + eventsReceived);
    }
}
