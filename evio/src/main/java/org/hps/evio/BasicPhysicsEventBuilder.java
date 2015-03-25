package org.hps.monitoring.application.util;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.hps.evio.LCSimEngRunEventBuilder;
import org.hps.record.evio.EvioEventUtilities;
import org.jlab.coda.jevio.EvioEvent;
import org.lcsim.event.EventHeader;


/**
 * Build LCSim events from EVIO data.
 * <p>
 * This class only builds physics events and essentially 
 * ignores EPICS scalar data, scalar banks, and control 
 * events.
 *
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class BasicPhysicsEventBuilder extends LCSimEngRunEventBuilder {
    
    public BasicPhysicsEventBuilder() {
        super();
    }
           
    @Override
    public EventHeader makeLCSimEvent(EvioEvent evioEvent) {
        
        if (!EvioEventUtilities.isPhysicsEvent(evioEvent)) {
            throw new RuntimeException("Not a physics event: event tag " + evioEvent.getHeader().getTag());
        }

        // Create a new LCSimEvent.
        EventHeader lcsimEvent = getEventData(evioEvent);
             
        // Make RawCalorimeterHit collection, combining top and bottom section
        // of ECal into one list.
        try {
            ecalReader.makeHits(evioEvent, lcsimEvent);
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Error making ECal hits", e);
        }
        
        // TODO: Add SVT reader here.

        return lcsimEvent;
    }           
}
