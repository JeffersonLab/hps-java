package org.hps.evio;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.hps.record.evio.EvioEventUtilities;
import org.jlab.coda.jevio.EvioEvent;
import org.lcsim.event.EventHeader;

/**
 * This is the {@link org.hps.record.LCSimEventBuilder} implementation for the
 * 2019 Physics Run for converting EVIO to LCIO events. 
 * <p>
 * This implementation makes uses a new SVT EVIO reader that was updated to 
 * parse RSSI frames. 
 * <p>
 *
 * @author Omar Moreno,    SLAC National Accelerator Laboratory
 * @author Maurik Holtrop, University of New Hampshire.  
 */
public class LCSimPhys2019EventBuilder extends LCSimEngRunEventBuilder { 

    /**
     * Setup logger.
     */
    private static final Logger LOGGER = Logger.getLogger(LCSimPhys2019EventBuilder.class.getPackage().getName());
    
    protected TSEvioReader tsReader = null;
    
    /** Constructor */
    public LCSimPhys2019EventBuilder() {
        super(); 
        svtReader = new Phys2019SvtEvioReader(); 
        vtpReader = new VTPEvioReader();
        tsReader = new TSEvioReader();

        // in 2019 the RF signal changed crates relative to previous runs:
        ecalReader.setRfBankTag(0x27);

        svtEventFlagger = null;  
    }
    
    
    /**
     * Make an lcsim event from EVIO data.
     *
     * @param evioEvent the input EVIO event
     */
    @Override
    public EventHeader makeLCSimEvent(final EvioEvent evioEvent) {

        LOGGER.finest("creating LCSim event from EVIO event " + evioEvent.getEventNumber());

        if (!EvioEventUtilities.isPhysicsEvent(evioEvent)) {
            throw new RuntimeException("Not a physics event: event tag " + evioEvent.getHeader().getTag());
        }

        // Create a new LCSimEvent.
        final EventHeader lcsimEvent = super.makeLCSimEvent(evioEvent);
        LOGGER.finest("created new LCSim event " + lcsimEvent.getEventNumber());

        // Make TS collection
        // into one list.
        try {
            tsReader.makeHits(evioEvent, lcsimEvent);
        } catch (final Exception e) {
            LOGGER.log(Level.SEVERE, "Error reading TS bank", e);
        }

        return lcsimEvent;
    }
    
}
