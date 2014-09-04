package org.hps.record.composite;

import org.hps.record.MaxRecordsException;
import org.hps.record.RecordProcessingException;

/**
 * A @{link CompositeProcessor} for throwing an error when the 
 * maximum number of records is reached or exceeded.
 */
public class MaxRecordsProcessor extends CompositeRecordProcessor {
    
    int maxRecords;
    int recordsReceived;
    
    public MaxRecordsProcessor(int maxRecords) {
        this.maxRecords = maxRecords;
    }
    
    public void process(CompositeRecord record) {
        if (recordsReceived >= maxRecords)
            throw new RecordProcessingException(
                    "Maximum number of records received.", 
                    new MaxRecordsException("Maximum number of records received.", maxRecords));
        ++recordsReceived;        
    }    
}
