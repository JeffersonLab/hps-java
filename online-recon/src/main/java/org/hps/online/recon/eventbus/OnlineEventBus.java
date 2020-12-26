package org.hps.online.recon.eventbus;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.hps.online.recon.Station;
import org.hps.record.et.EtConnection;
import org.jlab.coda.et.EtEvent;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

/**
 * An event bus to read ET events, convert to EVIO,
 * and generate LCIO recon events using an event
 * builder and job manager
 */
// TODO: add a test of ET -> EVIO -> LCIO
// TODO: needs error handling (could this class be a receiver and get a "stop processing" object on errors?)
// TODO: add max events setting and stop after it is reached
// TODO: use a logger instead of print outs
public class OnlineEventBus extends EventBus {

    EtConnection conn;
    Thread eventProc;
    Exception error = null;

    final Logger logger = Logger.getLogger(OnlineEventBus.class.getPackage().getName());

    public OnlineEventBus(Station station) {
        logger.config("Initializing event bus for stat: " + station.getStationName());
        this.conn = station.getEtConnection();
        register(new EtListener(this));
        register(new EvioListener(this, station));
        register(new LcioListener(this)); // dead event sink
        logger.config("Event bus initialized!");
    }

    /**
     * Continuously post ET events to drive the event bus
     * @throws Exception If there is an error reading ET events
     */
    public void loop() throws Exception {
        while (true) {
            // Any exceptions on ET reads will break the loop but that is okay.
            EtEvent[] events = conn.readEtEvents();
            logger.fine("Read ET events: " + events.length);
            for (EtEvent event : events) {
                // TODO: Should/could this make sure there aren't too many events on the bus already?
                System.out.println("Posting ET event: " + event.getId());
                this.post(event);
            }
        }
    }

    public void startProcessing() {
        logger.config("Starting event processing thread ...");
        eventProc = new EventProcessingThread();
        eventProc.start();
        logger.config("Event processing thread started!");
    }

    public void stopProcessing() {
        logger.info("Stopping event processing...");
        eventProc.interrupt();
        try {
            eventProc.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        logger.info("Event processing is stopped!");
    }

    void error(Exception e, boolean fatal) {
        post(new EventProcessingError(e, fatal));
    }

    @Subscribe
    public void handleError(EventProcessingError e) {
        if (e.fatal) {
            logger.log(Level.SEVERE, "Fatal error - event processing will stop", e);
            this.stopProcessing();
        } else {
            logger.log(Level.WARNING, "Error occurred - event processing will continue", e);
        }
    }

    public Thread getEventProcessingThread() {
        return this.eventProc;
    }

    public Logger getLogger() {
        return this.logger;
    }

    class EventProcessingThread extends Thread {

        final OnlineEventBus eventbus = OnlineEventBus.this;

        public void run() {
            try {
                eventbus.loop();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
