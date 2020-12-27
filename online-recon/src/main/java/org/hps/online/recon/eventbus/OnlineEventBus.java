package org.hps.online.recon.eventbus;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.hps.online.recon.Station;
import org.hps.record.et.EtConnection;
import org.hps.record.evio.EvioEventUtilities;
import org.jlab.coda.et.EtEvent;
import org.jlab.coda.jevio.EvioEvent;
import org.lcsim.conditions.ConditionsManager;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

/**
 * An event bus to read ET events, convert to EVIO,
 * and generate LCIO recon events using an event
 * builder and job manager.
 *
 * Special objects are used for error handling,
 * signaling end of run, and stopping the ET event loop.
 *
 * Event processing is run on a separate thread so that
 * it can be stopped easily.
 */
public class OnlineEventBus extends EventBus {

    private EtConnection conn;
    private Thread eventProc;
    private Station station;

    private final Logger logger = Logger.getLogger(OnlineEventBus.class.getPackage().getName());

    public OnlineEventBus(Station station) {
        logger.config("Initializing event bus for station: " + station.getStationName());
        this.station = station;
        this.conn = this.station.getEtConnection();
        register(this);
        register(new EtListener(this));
        register(new ConditionsListener(this));
        register(new EvioListener(this));
        register(new LcioListener(this)); // dead event sink
        logger.config("Event bus initialized!");
    }

    /**
     * Continuously post ET events to drive the event bus
     * @throws Exception If there is an error reading ET events
     */
    public void loop() throws Exception {
        while (true) {
            EtEvent[] events = null;
            try {
                events = conn.readEtEvents();
                //logger.fine("Read ET events: " + events.length);
                for (EtEvent event : events) {
                    logger.fine("Read ET event: " + event.getId());
                    this.post(event);
                }
                conn.getEtSystem().dumpEvents(conn.getEtAttachment(), events);
            } catch (Exception e) {
                // ET system errors are always fatal.
                post(new EventProcessingError(e, true));
            }
            if (Thread.interrupted()) {
                // Thread was externally interrupted and processing should stop.
                post(new Stop("Interrupted"));
            }
        }
    }

    @Subscribe
    public void receiveStart(Start start) {
        getStation().getJobManager().getDriverAdapter().start(null);
    }

    public void startProcessing() {
        logger.config("Starting event processing thread ...");
        post(new Start());
        eventProc = new EventProcessingThread();
        eventProc.start();
        logger.config("Event processing thread started!");
    }

    @Subscribe
    public void receiveStop(Stop stop) {
        logger.info("Stopping event processing: " + stop.getReason());
        if (eventProc.isAlive()) {
            eventProc.interrupt();
            try {
                eventProc.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        getStation().getJobManager().getDriverAdapter().finish(null);
        logger.info("Event processing is stopped!");
    }

    @Subscribe
    public void receiveError(EventProcessingError e) {
        if (e.fatal()) {
            logger.log(Level.SEVERE, "Fatal error - event processing will stop", e.getException());
            e.getException().printStackTrace();
            post(new Stop("Fatal error"));
        } else {
            logger.log(Level.WARNING, "Error occurred - event processing will continue", e.getException());
            e.getException().printStackTrace();
        }
    }

    @Subscribe
    public void postEndRun(EvioEvent evioEvent) {
        if (EvioEventUtilities.isEndEvent(evioEvent)) {
            post(new EndRun(ConditionsManager.defaultInstance().getRun()));
        }
    }

    @Subscribe
    public void receiveEndRun(EndRun end) {
        logger.info("Run ended: " + end.getRun());
        post(new Stop("Run ended"));
    }

    /**
     * The user can stop event processing by interrupting this thread.
     * @return The event processing thread
     */
    public Thread getEventProcessingThread() {
        return this.eventProc;
    }

    Logger getLogger() {
        return this.logger;
    }

    Station getStation() {
        return this.station;
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
