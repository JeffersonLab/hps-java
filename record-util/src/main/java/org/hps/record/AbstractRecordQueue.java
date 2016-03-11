package org.hps.record;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.freehep.record.source.AbstractRecordSource;
import org.freehep.record.source.NoSuchRecordException;

/**
 * Implementation of <tt>AbstractRecordSource</tt> using a dynamic queue that can receive events "on the fly", e.g. from
 * an ET ring.
 * <p>
 * Polling is used in the {@link #next()} method to get the next record, which might not be immediately available.
 *
 * @author <a href="mailto:jeremym@slac.stanford.edu">Jeremy McCormick</a>
 */
public abstract class AbstractRecordQueue<RecordType> extends AbstractRecordSource {

    /**
     * The current record.
     */
    private RecordType currentRecord;

    /**
     * The record queue which is a linked list with blocking behavior.
     */
    private final BlockingQueue<RecordType> records;

    /**
     * The amount of time to wait for a record to be available in the queue. The default value of -1 will essentially
     * use a zero wait time.
     */
    long timeOutMillis = -1;

    /**
     * Class constructor.
     */
    public AbstractRecordQueue() {
        // Use an unlimited queue size by default.
        this.records = new LinkedBlockingQueue<RecordType>();
    }

    /**
     * Class constructor with the timeout in seconds.
     *
     * @param timeOutMillis the timeout in seconds
     * @param maxSize the maximum size of the queue
     */
    public AbstractRecordQueue(final long timeOutMillis, final int maxSize) {
        this.timeOutMillis = timeOutMillis;
        this.records = new LinkedBlockingQueue<RecordType>(maxSize);
    }

    /**
     * Add a record to the queue if there is space.
     *
     * @param record the LCIO event to add
     */
    // FIXME: Should drain queue if over capacity.
    public void addRecord(final RecordType record) {
        if (this.records.remainingCapacity() > 0) {
            this.records.add(record);
        }
    }

    /**
     * Get the current record.
     *
     * @return the current record
     * @throws IOException never
     */
    @Override
    public Object getCurrentRecord() throws IOException {
        return this.currentRecord;
    }

    /**
     * Return <code>true</code> if there is a current record.
     *
     * @return <code>true</code> if there is a current record
     */
    @Override
    public boolean hasCurrent() {
        return this.currentRecord != null;
    }

    /**
     * Return <code>true</code> if there is a next record (e.g. queue is not empty).
     *
     * @return <code>true</code> if there is a next record
     */
    @Override
    public boolean hasNext() {
        return this.records.size() != 0;
    }

    /**
     * Load the next record.
     *
     * @throws IOException never
     * @throws NoSuchRecordException if there are no records available from the queue
     */
    @Override
    public synchronized void next() throws IOException, NoSuchRecordException {
        try {
            if (this.timeOutMillis > 0L) {
                // Poll the queue for the next record until timeout is exceeded.
                this.currentRecord = this.records.poll(this.timeOutMillis, TimeUnit.MILLISECONDS);
            } else {
                // Poll without an explicit wait time which will immediately return
                // null if the queue is empty.
                this.currentRecord = this.records.poll();
            }
        } catch (final InterruptedException e) {
            e.printStackTrace();
        }
        if (this.currentRecord == null) {
            throw new NoSuchRecordException("No records in queue.");
        }
    }

    /**
     * Get the number of records in the queue.
     *
     * @return the number of records in the queue
     */
    @Override
    public long size() {
        return this.records.size();
    }

    /**
     * Returns <code>true</code> to indicate current record capability is supported.
     *
     * @return <code>true</code> because current record capability is supported
     */
    @Override
    public boolean supportsCurrent() {
        return true;
    }

    /**
     * Returns <code>true</code> to indicate next record capability is supported.
     *
     * @return <code>true</code> because next record capability is supported
     */
    @Override
    public boolean supportsNext() {
        return true;
    }
}