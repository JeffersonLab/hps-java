package org.hps.online.recon.eventbus;

import org.hps.online.recon.properties.Property;
import org.lcsim.event.EventHeader;

import com.google.common.eventbus.Subscribe;

/**
 * Receive LCIO events and count them.
 */
public class LcioListener {

    private OnlineEventBus eventbus;
    private int eventsReceived = 0;
    private Integer printInterval = 1;

    LcioListener(OnlineEventBus eventbus) {
        this.eventbus = eventbus;
        Property<Integer> printInterval = eventbus.getStation().getProperties().get("station.printInterval");
        if (printInterval.valid()) {
            this.printInterval = printInterval.value();
            eventbus.getLogger().config("Set print interval: " + this.printInterval);
        }
    }

    @Subscribe
    public void receiveLcio(EventHeader event) {
        if (eventsReceived % printInterval == 0) {
            eventbus.getLogger().info("Received LCIO event: " + event.getEventNumber());
            eventbus.getLogger().info("Total received: " + eventsReceived);
        }
        ++eventsReceived;
    }

    @Subscribe
    public void receiveStop(Stop stop) {
        this.eventbus.getLogger().info("Total LCIO events: " + eventsReceived);
    }

    @Subscribe
    public void receiveStart(Start start) {
        if (this.eventsReceived > 0) {
            this.eventsReceived = 0;
        }
    }
}
