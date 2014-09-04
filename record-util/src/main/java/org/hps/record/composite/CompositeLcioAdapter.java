package org.hps.record.composite;

import java.io.IOException;

import org.freehep.record.loop.LoopEvent;
import org.freehep.record.loop.RecordEvent;
import org.freehep.record.source.AbstractRecordSource;
import org.freehep.record.source.NoSuchRecordException;
import org.hps.evio.EventConstants;
import org.hps.evio.LCSimEventBuilder;
import org.hps.record.RecordProcessingException;
import org.jlab.coda.jevio.EvioEvent;
import org.lcsim.event.EventHeader;
import org.lcsim.util.Driver;
import org.lcsim.util.DriverAdapter;

/**
 * An adapter to supply and process LCSim EventHeader objects using 
 * an (optional) LCSimEventBuilder and the existing DriverAdapter class.
 */
public class CompositeLcioAdapter extends CompositeLoopAdapter {

    DriverAdapter drivers;
    Driver top = new Driver();
    LCSimEventBuilder builder;
    AbstractRecordSource source;

    public CompositeLcioAdapter(AbstractRecordSource source) {
        this.source = source;
        drivers = new DriverAdapter(top);
    }

    public CompositeLcioAdapter() {
        drivers = new DriverAdapter(top);
    }

    public void addDriver(Driver driver) {
        top.add(driver);
    }

    public void setLCSimEventBuilder(LCSimEventBuilder builder) {
        this.builder = builder;
    }

    public void recordSupplied(RecordEvent record) {
        //System.out.println("CompositeLcioAdapter.recordSupplied");
        System.out.flush();
        CompositeRecord compositeRecord = (CompositeRecord) record.getRecord();
        EventHeader lcioEvent = null;
        try {
            // Is there an EVIO event?
            if (compositeRecord.getEvioEvent() != null) {
                // Create the EVIO event.
                EvioEvent evioEvent = compositeRecord.getEvioEvent();
                // Is this a physics EvioEvent?
                if (EventConstants.isPhysicsEvent(evioEvent)) {
                    // Use the builder to create the LCIO event.
                    lcioEvent = builder.makeLCSimEvent(compositeRecord.getEvioEvent());
                } else {
                    // Non-physics events are ignored.
                    return;                    
                }
            } else {
                // Try to use an event source to get the next LCIO event.
                source.next();
                lcioEvent = (EventHeader) source.getCurrentRecord();
            }
            
            // Supply the EventHeader to the DriverAdapter.
            RecordEvent recordEvent = new RecordEvent(null, lcioEvent);
            drivers.recordSupplied(recordEvent);
            
            compositeRecord.setLcioEvent(lcioEvent);
        } catch (IOException | NoSuchRecordException e) {
            throw new RecordProcessingException("Error creating LCIO event.", e);
        }
    }

    public void finish(LoopEvent loopEvent) {
        drivers.finish(loopEvent);
    }

    public void start(LoopEvent loopEvent) {
        drivers.start(loopEvent);
    }
}
