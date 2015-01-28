package org.hps.record.composite;

import org.hps.record.MaxRecordsException;
import org.hps.record.evio.EvioEventUtilities;

/**
 * A @{link CompositeProcessor} for throwing an error when the 
 * maximum number of records is reached or exceeded.
 */
public class MaxRecordsProcessor extends CompositeRecordProcessor {
    
    long maxRecords;
    long recordsReceived;
   
    /**
     * Constructor with the maximum number of records.
     * @param maxRecords The maximum number of records.
     */
    public MaxRecordsProcessor(long maxRecords) {
        this.maxRecords = maxRecords;
    }
    
    /**
     * Process a record and check if max number of records was reached.
     * Only records with certain types are considered in this total,
     * which are basically "physics" events when processing LCIO
     * or EVIO files.  For an ET system without any other record processing,
     * all events count towards the total. 
     */
    public void process(CompositeRecord record) {
        if (record.getLcioEvent() != null) {
            // All LCSim events count as records.
            ++recordsReceived;
        } else if (record.getEvioEvent() != null) {
            if (EvioEventUtilities.isPhysicsEvent(record.getEvioEvent())) {
                // Only EVIO physics events are counted.
                ++recordsReceived;
            }
        } else {
            // Otherwise (ET only?) count all records.
            ++recordsReceived;
        }        
        if (recordsReceived >= maxRecords) {
            // Throw exception if max records was reached or exceeded.
            throw new MaxRecordsException("Maximum number of records received.", maxRecords);
        }
    }
}
