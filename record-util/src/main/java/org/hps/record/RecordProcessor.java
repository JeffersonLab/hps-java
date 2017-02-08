package org.hps.record;

/**
 * This is a generic interface for event processing which implements hooks for starting the job, starting a new run,
 * processing individual records, ending a run and ending a job. This interface should not be implemented directly.
 * Instead the {@link AbstractRecordProcessor} should be extended with a specific type declaration.
 *
 * @param <RecordType> the concrete type of the event record
 */
public interface RecordProcessor<RecordType> {

    /**
     * End of job action.
     */
    void endJob();

    /**
     * End of run action.
     *
     * @param record the record to process
     */
    void endRun(RecordType record);

    /**
     * Process a single event.
     *
     * @param record the record to process
     */
    void process(RecordType record) throws Exception;

    /**
     * Start of job action.
     */
    void startJob();

    /**
     * Start run action.
     *
     * @param record the record to process
     */
    void startRun(RecordType record);

    /**
     * Suspend processing action.
     */
    void suspend();
    
    /**
     * Return <code>true</code> if processor is active.
     * 
     * @return <code>true</code> if processor is active
     */
    boolean isActive();
}
