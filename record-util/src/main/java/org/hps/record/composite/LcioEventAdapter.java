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
 * An adapter to supply and process LCSim EventHeader objects using 
 * an (optional) LCSimEventBuilder and the existing DriverAdapter class.
 */
public class LcioEventAdapter extends CompositeLoopAdapter {

    DriverAdapter drivers;
    Driver top = new Driver();
    LCSimEventBuilder builder;
    AbstractRecordSource source;

    /**
     * Constructor taking a record source which should supply
     * LCSim LCIO events.
     * @param source
     */
    public LcioEventAdapter(AbstractRecordSource source) {
        this.source = source;
        drivers = new DriverAdapter(top);
    }

    /**
     * No argument constructor in which case the {@link CompositeRecord}
     * should supply <code>EvioEvent</code> objects for the builder.
     */
    public LcioEventAdapter() {
        drivers = new DriverAdapter(top);
    }

    /**
     * Add an LCSim <code>Driver</code> 
     * @param driver The Driver to add.
     */
    public void addDriver(Driver driver) {
        top.add(driver);
    }

    /**
     * Set the <code>LCSimEventBuilder</code> that will convert
     * from EVIO to LCIO events.
     * @param builder
     */
    public void setLCSimEventBuilder(LCSimEventBuilder builder) {
        this.builder = builder;
    }

    /**
     * Process a {@link CompositeRecord} which will add an LCSim event
     * and activate registered <code>Driver</code> objects.
     */
    public void recordSupplied(RecordEvent record) {
        CompositeRecord compositeRecord = (CompositeRecord) record.getRecord();
        EventHeader lcioEvent = null;
        try {
            // Is there an EVIO event to use for the conversion to LCIO?
            if (compositeRecord.getEvioEvent() != null) {
                // Create the EVIO event.
                EvioEvent evioEvent = compositeRecord.getEvioEvent();
                
                // Pre-read the event in the builder to get non-physics event information.
                builder.readEvioEvent(evioEvent);
                
                // Is this a physics EvioEvent?
                if (EvioEventUtilities.isPhysicsEvent(evioEvent)) {
                    // Use the builder to create the LCIO event.
                    lcioEvent = builder.makeLCSimEvent(compositeRecord.getEvioEvent());
                } else {
                    // Non-physics events are ignored.
                    return;                    
                }
            } else {
                // Try to use the event source to get the next LCIO event.
                source.next();
                lcioEvent = (EventHeader) source.getCurrentRecord();
            }
            
            // Supply the EventHeader to the DriverAdapter.
            RecordEvent recordEvent = new RecordEvent(null, lcioEvent);
            drivers.recordSupplied(recordEvent);
            
            // Set the reference to the LCIO event on the CompositeRecord.
            compositeRecord.setLcioEvent(lcioEvent);
        } catch (IOException | NoSuchRecordException e) {
            throw new RecordProcessingException("Error creating LCIO event.", e);
        }
    }

    /**
     * Activates the <code>endOfData</code> method on the registered
     * <code>Driver</code> objects.
     */
    public void finish(LoopEvent loopEvent) {
        drivers.finish(loopEvent);
    }

    /**
     * Activates the <code>startOfData</code> method on registered
     * <code>Driver</code> objects.
     */
    public void start(LoopEvent loopEvent) {
        drivers.start(loopEvent);
    }
    
    /**
     * Activates the <code>suspend</code> method on registered
     * <code>Driver</code> objects.
     */
    public void suspend(LoopEvent loopEvent) {
        drivers.suspend(loopEvent);
    }
}
