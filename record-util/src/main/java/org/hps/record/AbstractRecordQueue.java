package org.hps.record;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.freehep.record.source.AbstractRecordSource;
import org.freehep.record.source.NoSuchRecordException;

/**
 * Implementation of <tt>AbstractRecordSource</tt> using a dynamic queue that 
 * can receive events "on the fly" e.g. from an ET ring.  Polling is used in the
 * {@link #next()} method to get the next record, which might not be immediately
 * available.
 */
// TODO: Add max elements argument to limit pile up of unconsumed events.
public abstract class AbstractRecordQueue<RecordType> extends AbstractRecordSource {

    // The queue, which is a linked list with blocking behavior. 
    BlockingQueue<RecordType> records;
    
    // The current LCIO events.
    RecordType currentRecord;
    
    // The amount of time to wait for an LCIO event from the queue before dying.
    long timeOutMillis = -1;
    
    /**
     * Constructor that takes the timeout time in seconds.
     * @param timeoutSeconds the timeout time in seconds
     */
    public AbstractRecordQueue(long timeoutMillis, int maxSize) {
        this.timeOutMillis = timeoutMillis;
        records = new LinkedBlockingQueue<RecordType>(maxSize);
    }
    
    public AbstractRecordQueue() {
        // Unlimited queue size.
        records = new LinkedBlockingQueue<RecordType>();
    }
    
    /**
     * Add a record to the queue.
     * If the queue is full, then drain it first.
     * @param event the LCIO event to add
     */
    public void addRecord(RecordType record) {
        if (records.remainingCapacity() > 0)
            records.add(record);
        // TODO: Maybe automatically drain the queue here if at capacity???
    }
  
    @Override
    public Object getCurrentRecord() throws IOException {
        return currentRecord;
    }
    
    @Override
    public boolean supportsCurrent() {
        return true;
    }

    @Override
    public boolean supportsNext() {
        return true;
    }

    @Override
    public boolean hasCurrent() {
        return currentRecord != null;
    }

    @Override
    public boolean hasNext() {
        return records.size() != 0;
    }
    
    @Override
    public void next() throws IOException, NoSuchRecordException {
        try {
            if (timeOutMillis > 0L)
                // Poll the queue for the next record or until timeout is exceeded.
                currentRecord = records.poll(timeOutMillis, TimeUnit.MILLISECONDS);
            else
                // Poll without an explicit wait time which will immediately return
                // null if queue is empty.
                currentRecord = records.poll();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (currentRecord == null) {
            throw new NoSuchRecordException("No records in queue.");
        }
    }
   
    @Override
    public long size() {
        return records.size();
    }
}