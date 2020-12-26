package org.hps.online.recon.eventbus;

import org.lcsim.event.EventHeader;

import com.google.common.eventbus.Subscribe;

/**
 * Make sure LCIO events have an event sink
 */
public class LcioListener {

    OnlineEventBus eventbus;

    LcioListener(OnlineEventBus eventbus) {
        this.eventbus = eventbus;
    }

    @Subscribe
    public void receiveLcio(EventHeader event) {
        eventbus.getLogger().info("LcioListener - received event: " + event.getEventNumber());
    }
}
