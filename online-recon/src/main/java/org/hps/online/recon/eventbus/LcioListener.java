package org.hps.online.recon.eventbus;

import org.lcsim.event.EventHeader;

import com.google.common.eventbus.Subscribe;

/**
 * Receive LCIO events and count them.
 */
public class LcioListener {

    OnlineEventBus eventbus;
    int eventsReceived = 0;
    //Set<Integer> eventNumbers = new HashSet<Integer>();
    //boolean storeEventNumbers = false;

    LcioListener(OnlineEventBus eventbus/*, boolean storeEventNumbers*/) {
        this.eventbus = eventbus;
        //this.storeEventNumbers = storeEventNumbers;
    }

    @Subscribe
    public void receiveLcio(EventHeader event) {
        eventbus.getLogger().info("LcioListener - received event: " + event.getEventNumber());
        ++eventsReceived;
        /*if (storeEventNumbers) {
            eventNumbers.add(event.getEventNumber());
        }*/
    }

    @Subscribe
    public void receiveStop(Stop stop) {
        this.eventbus.getLogger().info("Total LCIO events received: " + eventsReceived);
        /*
        if (storeEventNumbers) {
            this.eventbus.getLogger().info("Events processed:" + '\n' + Arrays.toString(eventNumbers.toArray()));
        }
        */
    }
}
