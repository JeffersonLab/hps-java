package org.hps.monitoring.record;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.freehep.record.source.AbstractRecordSource;
import org.freehep.record.source.NoSuchRecordException;

/**
 * Implementation of <tt>AbstractRecordSource</tt> using a dynamic queue that 
 * can receive events "on the fly" e.g. from an ET ring.
 */
public abstract class AbstractRecordQueue<RecordType> extends AbstractRecordSource {

    // The queue, which is a linked list with blocking behavior. 
    BlockingQueue<RecordType> records = new LinkedBlockingQueue<RecordType>();
    
    // The current LCIO events.
    RecordType currentRecord;
    
    // The amount of time to wait for an LCIO event from the queue before dying.
    long timeOutMillis = 1000;
    
    /**
     * Constructor that takes the timeout time in seconds.
     * @param timeoutSeconds the timeout time in seconds
     */
    public AbstractRecordQueue(long timeoutMillis) {
        this.timeOutMillis = timeoutMillis;
    }
    
    public AbstractRecordQueue() {
    }
    
    /**
     * Set the time wait time before the poll call times out.
     * @param timeoutMillis
     */
    public void setTimeOutMillis(long timeoutMillis) {
        this.timeOutMillis = timeoutMillis;
    }
    
    /**
     * Add a record to the queue.
     * @param event the LCIO event to add
     */
    public void addRecord(RecordType record) {
        records.add(record);
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
    public boolean supportsPrevious() {
        return false;
    }
  
    @Override
    public boolean supportsIndex() {
        return false;
    }
  
    @Override 
    public boolean supportsShift() {
        return false;
    }
  
    @Override
    public boolean supportsRewind() {
        return false;
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
    
    public long size() {
        return records.size();
    }
}