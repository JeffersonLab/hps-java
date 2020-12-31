package org.hps.online.recon.eventbus;

import java.util.Date;
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

    // Store a collection of all event numbers processed for debugging
    //private final boolean STORE_EVENT_NUMBERS = false;

    public OnlineEventBus(Station station) {
        logger.config("Initializing online event bus for station: " + station.getStationName());
        this.station = station;
        this.conn = this.station.getEtConnection();
        register(this);
        register(new EtListener(this));
        register(new EvioListener(this));
        register(new LcioListener(this/*, STORE_EVENT_NUMBERS*/));
        logger.config("Online event bus initialized");
    }

    /**
     * Continuously post ET events to drive the event bus
     * @throws Exception If there is an error reading ET events
     */
    void loop() throws Exception {
        while (true) {
            EtEvent[] events = null;
            try {
                logger.fine("Reading ET events");
                events = conn.readEtEvents();
                //logger.fine("Read ET events: " + events.length);
                for (EtEvent event : events) {
                    logger.fine("Read ET event: " + event.getId());
                    this.post(event);
                }
                conn.getEtSystem().dumpEvents(conn.getEtAttachment(), events);
            } catch (Exception e) {
                // ET system errors are always considered fatal.
                post(new EventProcessingError(e, true));
            }
            if (Thread.interrupted()) {
                // Thread was externally interrupted and processing should stop.
                post(new Stop("Interrupted"));
            }
        }
    }

    public void startProcessing() {
        logger.config("Starting event processing thread");
        post(new Start(new Date()));
        eventProc = new EventProcessingThread();
        eventProc.start();
        logger.config("Event processing thread started");
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
        this.station.getJobManager().getDriverAdapter().finish(null);
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
    public void receiveStart(Start start) {
        //station.getJobManager().getDriverAdapter().start(null);
    }

    @Subscribe
    public void postEndRun(EvioEvent evioEvent) {
        if (EvioEventUtilities.isEndEvent(evioEvent)) {
            post(new EndRun(ConditionsManager.defaultInstance().getRun(), new Date()));
        }
    }

    @Subscribe
    public void receiveEndRun(EndRun end) {
        logger.info("Run " + end.getRun() + " ended at: " + end.getDate());
        post(new Stop("Run ended"));
    }

    /**
     * The user can stop event processing by accessing and then interrupting this thread.
     * @return The event processing thread
     */
    public Thread getEventProcessingThread() {
        return this.eventProc;
    }

    /**
     * Get the logger for the event bus
     * @return The logger for the event bus
     */
    Logger getLogger() {
        return this.logger;
    }

    /**
     * Get the {@link org.hps.online.recon.Station} associated with this event bus
     * @return The {@link org.hps.online.recon.Station} associated with this event bus
     */
    Station getStation() {
        return this.station;
    }

    /**
     * A thread for running the event bus loop
     *
     * This shouldn't be used directly. Instead call the {@link OnlineEventBus#startProcessing()}
     * method.
     */
    class EventProcessingThread extends Thread {

        final OnlineEventBus eventbus = OnlineEventBus.this;

        public void run() {
            try {
                getLogger().info("event bus loop starting");
                eventbus.loop();
                getLogger().info("event bus loop stopped");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
