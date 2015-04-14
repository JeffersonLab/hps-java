package org.hps.record;

/**
 * An abstract implementation of {@link RecordProcessor} with "no operation" method implementations.
 * <p>
 * Concrete implementations of <code>RecordProcessor</code> should extend this class.
 *
 * @param <RecordType> the type of the record processed by this class
 * @author <a href="mailto:jeremym@slac.stanford.edu">Jeremy McCormick</a>
 */
public abstract class AbstractRecordProcessor<RecordType> implements RecordProcessor<RecordType> {

    /**
     * End of job action.
     */
    @Override
    public void endJob() {
    }

    /**
     * End of run action.
     *
     * @param record the current record being processed
     */
    @Override
    public void endRun(final RecordType record) {
    }

    /**
     * Process a record.
     *
     * @param record the record to process
     * @throws Exception never but sub-classes may throw
     */
    @Override
    public void process(final RecordType record) throws Exception {
    }

    /**
     * Start of job action.
     */
    @Override
    public void startJob() {
    }

    /**
     * Start of run action.
     *
     * @param record the current record being processed (usually some kind of start-of-run record such as an EVIO
     *            PRESTART record)
     */
    @Override
    public void startRun(final RecordType record) {
    }

    /**
     * Suspend event processing (usually this will not be implemented by a sub-class).
     */
    @Override
    public void suspend() {
    }
}
