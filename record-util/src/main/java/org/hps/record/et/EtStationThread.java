package org.hps.record.et;

import java.io.IOException;

import org.jlab.coda.et.EtAttachment;
import org.jlab.coda.et.EtConstants;
import org.jlab.coda.et.EtEvent;
import org.jlab.coda.et.EtStation;
import org.jlab.coda.et.EtStationConfig;
import org.jlab.coda.et.EtSystem;
import org.jlab.coda.et.EtSystemOpenConfig;
import org.jlab.coda.et.enums.Mode;
import org.jlab.coda.et.enums.Modify;
import org.jlab.coda.et.exception.EtBusyException;
import org.jlab.coda.et.exception.EtClosedException;
import org.jlab.coda.et.exception.EtDeadException;
import org.jlab.coda.et.exception.EtEmptyException;
import org.jlab.coda.et.exception.EtException;
import org.jlab.coda.et.exception.EtTimeoutException;
import org.jlab.coda.et.exception.EtTooManyException;
import org.jlab.coda.et.exception.EtWakeUpException;

/**
 * <p>
 * This is a class which runs ET event processing on a separate thread using an ET station that is assigned to its own
 * unique <code>EtSystem</code>.
 * <p>
 * Specific processing of ET events is provided with an {@link EtEventProcessor}.
 *
 * @author <a href="mailto:jeremym@slac.stanford.edu">Jeremy McCormick</a>
 */
// FIXME: Currently this is unused in HPS Java.
public final class EtStationThread extends Thread {

    /**
     * The ET attachment.
     */
    private EtAttachment attachment;

    /**
     * The station name.
     */
    private final String name;

    /**
     * The ET event processor.
     */
    private final EtEventProcessor processor;

    /**
     * The station event selection array.
     */
    private int[] select;

    /**
     * The ET station.
     */
    private EtStation station;

    /**
     * The station position.
     */
    private final int stationPosition;

    /**
     * The <code>EtSystem</code> that represents the connection to the ET server.
     */
    private EtSystem system;

    /**
     * This creates an ET station that will run an ET processor on a separate thread.
     *
     * @param processor the ET processor
     * @param system the ET system
     * @param name the name of the station
     * @param stationPosition the station's position
     * @param select the station's select array (can be null)
     */
    public EtStationThread(final EtEventProcessor processor, final EtSystem system, final String name,
            final int stationPosition, final int[] select) {
        if (processor == null) {
            throw new IllegalArgumentException("processor is null");
        }
        if (system == null) {
            throw new IllegalArgumentException("system is null");
        }
        if (name == null) {
            throw new IllegalArgumentException("name is null");
        }
        if (stationPosition < 1) {
            throw new IllegalArgumentException("stationPosition must be > 0");
        }
        if (select != null) {
            if (select.length != EtConstants.stationSelectInts) {
                throw new IllegalArgumentException("control array must have length " + EtConstants.stationSelectInts);
            }
            this.select = select;
        }

        this.processor = processor;
        this.name = name;
        this.stationPosition = stationPosition;

        // Copy parameters from the provided EtSystem.
        try {
            this.system = new EtSystem(new EtSystemOpenConfig(system.getConfig()));
        } catch (final EtException e) {
            throw new RuntimeException("Error setting up station.", e);
        }
    }

    /**
     * Disconnect the station.
     * <p>
     * This happens automatically at the end of the {@link #run()} method.
     */
    synchronized final void disconnect() {
        if (this.system.alive()) {
            if (this.attachment.isUsable()) {
                try {
                    this.system.detach(this.attachment);
                } catch (IOException | EtException | EtClosedException | EtDeadException e) {
                    e.printStackTrace();
                }
            }
            this.system.close();
        }
    }

    /**
     * Run event processing on the station until woken up or interrupted.
     */
    @Override
    public final void run() {

        // Setup the ET system before processing events.
        // FIXME: Should be called outside this method?
        setup();

        // Process ET events.
        try {
            for (;;) {

                EtEvent[] events;

                try {
                    events = this.system.getEvents(this.attachment, Mode.SLEEP, Modify.NOTHING, 0, 1);
                    this.system.putEvents(this.attachment, events);
                } catch (final EtWakeUpException e) {
                    e.printStackTrace();
                    break;
                } catch (EtException | EtDeadException | EtClosedException | EtEmptyException | EtBusyException
                        | EtTimeoutException | IOException e) {
                    e.printStackTrace();
                    break;
                }

                try {
                    // Process the events.
                    for (final EtEvent event : events) {
                        this.processor.process(event);
                    }
                } catch (final Exception e) {
                    // If there is an event processing error then print stack trace and continue.
                    e.printStackTrace();
                    continue;
                }

                // Disconnect if interrupted.
                if (Thread.currentThread().isInterrupted()) {
                    break;
                }
            }
        } finally {
            // Disconnect the ET system.
            disconnect();
        }
    }

    /**
     * Setup this station for receiving events.
     */
    protected void setup() {

        if (!this.system.alive()) {
            try {
                this.system.open();
            } catch (IOException | EtException | EtTooManyException e) {
                throw new RuntimeException("Failed to open ET system.", e);
            }
        }

        try {
            // Create the basic station configuration.
            final EtStationConfig stationConfig = new EtStationConfig();
            stationConfig.setFlowMode(EtConstants.stationSerial);
            stationConfig.setBlockMode(EtConstants.stationNonBlocking);

            // Setup event selection.
            if (this.select != null) {
                stationConfig.setSelect(this.select);
                stationConfig.setSelectMode(EtConstants.stationSelectMatch);
            }

            // Create station and attach to the ET system.
            this.station = this.system.createStation(stationConfig, this.name, this.stationPosition);
            this.attachment = this.system.attach(this.station);

        } catch (final Exception e) {
            // Any errors during setup are re-thrown.
            throw new RuntimeException(e);
        }
    }

    /**
     * Wake up the station if it is blocked, which will cause it to disconnect.
     */
    public final synchronized void wakeUp() {
        if (this.system.alive()) {
            if (this.attachment.isUsable()) {
                try {
                    this.system.wakeUpAll(this.station);
                } catch (IOException | EtException | EtClosedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
