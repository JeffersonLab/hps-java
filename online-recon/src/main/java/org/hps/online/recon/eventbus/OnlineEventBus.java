package org.hps.online.recon.eventbus;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.hps.online.recon.Station;
import org.hps.record.et.EtConnection;
import org.jlab.coda.et.EtEvent;
import org.jlab.coda.et.exception.EtWakeUpException;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

/**
 * An event bus to read ET events, convert to EVIO, and generate reconstructed
 * LCIO events using an event builder and job manager.
 *
 * Special objects are used for error handling, signaling end of run, and stopping
 * the ET event loop.
 */
public class OnlineEventBus extends EventBus {

    private EtConnection conn;
    private Station station;

    private boolean halt;

    private final Logger logger = Logger.getLogger(OnlineEventBus.class.getPackage().getName());

    public OnlineEventBus(Station station) {
        logger.config("Initializing online event bus for station: " + station.getStationName());
        this.station = station;
        this.conn = this.station.getEtConnection();
        register(this);
        register(new ConditionsListener(this));
        register(new EtListener(this));
        register(new EvioListener(this));
        register(new LcioListener(this));
    }

    /**
     * Continuously post ET events to drive the event bus
     * @throws Exception If there is an error reading ET events
     */
    public void loop() {
        halt = false;
        post(new Start());
        logger.info("Event loop starting");
        while (true) {
            EtEvent[] events = null;
            try {
                logger.finest("Reading ET events");
                events = conn.readEtEvents();
                for (EtEvent event : events) {
                    logger.fine("Read ET event: " + event.getId());
                    this.post(event);
                }
                conn.getEtSystem().dumpEvents(conn.getEtAttachment(), events);
            } catch (EtWakeUpException e) {
                post(new Stop("ET wake up received"));
            } catch (Exception e) {
                // Errors when reading ET data are considered fatal
                post(new EventProcessingError(e, true));
            }
            if (halt) {
                break;
            }
        }
        logger.info("Event loop exiting");
    }

    @Subscribe
    public void receiveStop(Stop stop) {
        try {
            logger.info("Stopping event processing: " + stop.getReason());
            this.halt = true;
        } catch (Exception e) {
            logger.log(Level.WARNING, "Problem receiving stop command", e);
        }
    }

    @Subscribe
    public void receiveError(EventProcessingError e) {
        try {
            if (e.fatal()) {
                logger.log(Level.SEVERE, "Fatal error occurred -- event processing will stop", e.getException());
                post(new Stop("Fatal error"));
            } else {
                logger.log(Level.WARNING, "A non-fatal error occurred -- processing will continue", e.getException());
            }
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Problem processing error", ex);
        }
    }


    @Subscribe
    public void receiveStart(Start start) {
        try {
            logger.info("Received start: " + start.getDate().toString());

            logger.info("Activating start on drivers");
            this.station.getJobManager().getDriverAdapter().start(null);

        } catch (Exception e) {
            logger.log(Level.WARNING, "Problem receiving the Start event", e);
        }
    }

    @Subscribe
    public void receiveEndRun(EndRun end) {
        try {
            logger.info("Run " + end.getRun() + " ended at: " + end.getDate());
            post(new Stop("Run ended"));
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error receiving end run", e);
        }
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
}
