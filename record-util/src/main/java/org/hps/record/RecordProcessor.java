package org.hps.record;

/**
 * This is a generic interface for event processing which implements
 * hooks for starting the job, starting a new run, processing individual
 * records, ending a run and ending a job.  This interface should not
 * be implemented directly.  Instead the {@link AbstractRecordProcessor}
 * should be extended with a specific type declaration.
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
    
    /**
     * Action to be taken when recording processing is suspended.
     */
    void suspend();
}
