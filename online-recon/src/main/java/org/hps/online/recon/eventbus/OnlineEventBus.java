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
 * builder and job manager
 */
public class OnlineEventBus extends EventBus {

    private EtConnection conn;
    private Thread eventProc;
    private Station station;

    final Logger logger = Logger.getLogger(OnlineEventBus.class.getPackage().getName());

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
                this.error(e, true);
            }
        }
    }

    public void startProcessing() {
        logger.config("Starting event processing thread ...");
        eventProc = new EventProcessingThread();
        eventProc.start();
        logger.config("Event processing thread started!");
    }

    @Subscribe
    public void handleStopProcessing(StopProcessing stop) {
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
            post(new StopProcessing("Fatal error"));
        } else {
            logger.log(Level.WARNING, "Error occurred - event processing will continue", e);
        }
    }

    @Subscribe
    public void checkEndRun(EvioEvent evioEvent) {
        if (EvioEventUtilities.isEndEvent(evioEvent)) {
            post(new EndRun(ConditionsManager.defaultInstance().getRun()));
        }
    }

    @Subscribe
    public void handleEndRun(EndRun end) {
        logger.info("Run ended: " + end.getRun());
        post(new StopProcessing("Run ended"));
    }

    @Subscribe
    public void stopProcessing(StopProcessing stop) {
        getStation().getJobManager().getDriverAdapter().finish(null);
    }

    public Thread getEventProcessingThread() {
        return this.eventProc;
    }

    public Logger getLogger() {
        return this.logger;
    }

    public Station getStation() {
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
