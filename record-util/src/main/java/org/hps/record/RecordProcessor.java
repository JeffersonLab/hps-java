package org.hps.record;

/**
 * This is a generic interface for event processing.
 *
 * @param <RecordType> The concrete type of the event record.
 */
public interface RecordProcessor<RecordType> {
       
    /**
     * Start of job action.
     */
    void startJob();
    
    /**
     * Start run action.
     * @param record
     */
    void startRun(RecordType record);
    
    /**
     * Process a single event.
     * @param record
     */
    void process(RecordType record) throws Exception;

    /**
     * End of run action.
     * @param record
     */
    void endRun(RecordType record);
    
    /**
     * End of job action.
     */
    void endJob();
}
