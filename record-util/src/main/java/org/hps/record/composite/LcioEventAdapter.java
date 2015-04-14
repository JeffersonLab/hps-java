package org.hps.record.composite;

import java.io.IOException;

import org.freehep.record.loop.LoopEvent;
import org.freehep.record.loop.RecordEvent;
import org.freehep.record.source.AbstractRecordSource;
import org.freehep.record.source.NoSuchRecordException;
import org.hps.record.LCSimEventBuilder;
import org.hps.record.RecordProcessingException;
import org.hps.record.evio.EvioEventUtilities;
import org.jlab.coda.jevio.EvioEvent;
import org.lcsim.event.EventHeader;
import org.lcsim.util.Driver;
import org.lcsim.util.DriverAdapter;

/**
 * An adapter to supply and process LCSim EventHeader objects using an (optional) LCSimEventBuilder and the existing
 * DriverAdapter class.
 *
 * @author <a href="mailto:jeremym@slac.stanford.edu">Jeremy McCormick</a>
 */
public class LcioEventAdapter extends CompositeLoopAdapter {

    /**
     * The builder for creating LCIO from EVIO.
     */
    private LCSimEventBuilder builder;

    /**
     * A list of Drivers to execute on the LCIO events.
     */
    private final DriverAdapter drivers;

    /**
     * The record source.
     */
    private AbstractRecordSource source;

    /**
     * A top level Driver for executing a number of other Drivers as children.
     */
    private final Driver top = new Driver();

    /**
     * Class Constructor with no arguments.
     * <p>
     * This is used when the {@link CompositeRecord} should supply <code>EvioEvent</code> objects for the builder.
     */
    public LcioEventAdapter() {
        this.drivers = new DriverAdapter(this.top);
    }

    /**
     * Class constructor.
     * <p>
     * The provided record source should supply LCIO/LCSim events directly.
     *
     * @param source the record source of LCIO events
     */
    public LcioEventAdapter(final AbstractRecordSource source) {
        this.source = source;
        this.drivers = new DriverAdapter(this.top);
    }

    /**
     * Add an LCSim <code>Driver</code> to execute.
     *
     * @param driver the Driver to add
     */
    public void addDriver(final Driver driver) {
        this.top.add(driver);
    }

    /**
     * Activates the <code>endOfData</code> method on the registered <code>Driver</code> objects.
     */
    @Override
    public void finish(final LoopEvent loopEvent) {
        this.drivers.finish(loopEvent);
    }

    /**
     * Process a {@link CompositeRecord} which will create an LCSim event, add it to the composite record being
     * processed, and activate registered the <code>Driver</code> chain.
     */
    @Override
    public void recordSupplied(final RecordEvent record) {
        final CompositeRecord compositeRecord = (CompositeRecord) record.getRecord();
        EventHeader lcioEvent = null;
        try {
            // Is there an EVIO event to use for the conversion to LCIO?
            if (compositeRecord.getEvioEvent() != null) {
                // Create the EVIO event.
                final EvioEvent evioEvent = compositeRecord.getEvioEvent();

                // Pre-read the event in the builder to get non-physics event information.
                this.builder.readEvioEvent(evioEvent);

                // Is this a physics EvioEvent?
                if (EvioEventUtilities.isPhysicsEvent(evioEvent)) {
                    // Use the builder to create the LCIO event.
                    lcioEvent = this.builder.makeLCSimEvent(compositeRecord.getEvioEvent());
                } else {
                    // Non-physics events are ignored.
                    return;
                }
            } else {
                // Try to use the event source to get the next LCIO event.
                this.source.next();
                lcioEvent = (EventHeader) this.source.getCurrentRecord();
            }

            // Supply the EventHeader to the DriverAdapter.
            final RecordEvent recordEvent = new RecordEvent(null, lcioEvent);
            this.drivers.recordSupplied(recordEvent);

            // Set the reference to the LCIO event on the CompositeRecord.
            compositeRecord.setLcioEvent(lcioEvent);
        } catch (IOException | NoSuchRecordException e) {
            throw new RecordProcessingException("Error creating LCIO event.", e);
        }
    }

    /**
     * Set the <code>LCSimEventBuilder</code> that will convert from EVIO to LCIO events.
     *
     * @param builder the LCSim event builder for creating LCIO events from EVIO
     */
    public void setLCSimEventBuilder(final LCSimEventBuilder builder) {
        this.builder = builder;
    }

    /**
     * Activates the <code>startOfData</code> method on registered <code>Driver</code> objects.
     *
     * @param loopEvent the object with loop state information
     */
    @Override
    public void start(final LoopEvent loopEvent) {
        this.drivers.start(loopEvent);
    }

    /**
     * Activates the <code>suspend</code> method on registered <code>Driver</code> objects.
     *
     * @param loopEvent the object with loop state information
     */
    @Override
    public void suspend(final LoopEvent loopEvent) {
        this.drivers.suspend(loopEvent);
    }
}
