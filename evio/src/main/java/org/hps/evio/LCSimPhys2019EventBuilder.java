package org.hps.evio; 


/**
 * This is the {@link org.hps.record.LCSimEventBuilder} implementation for the
 * 2019 Physics Run for converting EVIO to LCIO events. 
 * <p>
 * This implementation makes uses a new SVT EVIO reader that was updated to 
 * parse RSSI frames. 
 * <p>
 *
 * @author Omar Moreno, SLAC National Accelerator Laboratory
 */
public class LCSimPhys2019EventBuilder extends LCSimEngRunEventBuilder { 

    /** Constructor */
    public LCSimPhys2019EventBuilder() {
        super(); 
        svtReader = new Phys2019SvtEvioReader(); 
        
        svtEventFlagger = null;  
    }
}
