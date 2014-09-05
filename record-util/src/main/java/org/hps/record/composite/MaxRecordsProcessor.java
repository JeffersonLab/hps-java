package org.hps.record.composite;

import org.hps.record.MaxRecordsException;
import org.hps.record.RecordProcessingException;

/**
 * A @{link CompositeProcessor} for throwing an error when the 
 * maximum number of records is reached or exceeded.
 */
// FIXME: This should be done different by using directly the loop and adapter.
public class MaxRecordsProcessor extends CompositeRecordProcessor {
    
    int maxRecords;
    int recordsReceived;
   
    /**
     * Constructor with the maximum number of records.
     * @param maxRecords The maximum number of records.
     */
    public MaxRecordsProcessor(int maxRecords) {
        this.maxRecords = maxRecords;
    }
    
    /**
     * Process a record and check if max number of records was reached.
     */
    public void process(CompositeRecord record) {
        if (recordsReceived >= maxRecords)
            throw new RecordProcessingException(
                    "Maximum number of records received.", 
                    new MaxRecordsException("Maximum number of records received.", maxRecords));
        ++recordsReceived;        
    }
}
