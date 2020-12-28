package org.hps.online.recon.eventbus;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.lcsim.event.EventHeader;

import com.google.common.eventbus.Subscribe;

/**
 * Receive LCIO events and count them.
 */
// TODO: Optionally store a collection of all the event numbers processed
public class LcioListener {

    OnlineEventBus eventbus;
    int eventsReceived = 0;
    Set<Integer> eventNumbers = new HashSet<Integer>();
    boolean storeEventNumbers = false;

    LcioListener(OnlineEventBus eventbus, boolean storeEventNumbers) {
        this.eventbus = eventbus;
        this.storeEventNumbers = storeEventNumbers;
    }

    @Subscribe
    public void receiveLcio(EventHeader event) {
        // eventbus.getLogger().info("LcioListener - received event: " + event.getEventNumber());
        ++eventsReceived;
        if (storeEventNumbers) {
            eventNumbers.add(event.getEventNumber());
        }
    }

    @Subscribe
    public void receiveStop(Stop stop) {
        this.eventbus.getLogger().info("Total LCIO events created: " + eventsReceived);
        if (storeEventNumbers) {
            this.eventbus.getLogger().info("Events processed:" + '\n' + Arrays.toString(eventNumbers.toArray()));
        }
    }
}
