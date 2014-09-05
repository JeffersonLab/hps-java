package org.hps.record.et;

import java.io.IOException;
import java.util.Arrays;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import org.freehep.record.source.AbstractRecordSource;
import org.freehep.record.source.NoSuchRecordException;
import org.jlab.coda.et.EtEvent;

/**
 * Implement a loop record source supplying <tt>EtEvent</tt> objects 
 * from an ET server connection.
 */
public final class EtEventSource extends AbstractRecordSource {
    
    EtConnection connection;
    EtEvent currentRecord;
    Queue<EtEvent> eventQueue = new LinkedBlockingQueue<EtEvent>();
        
    /**
     * Constructor that requires the connection parameters.
     * @param connection The EtConnection that should have a valid set of ET 
     *                   connection parameters.
     */
    public EtEventSource(EtConnection connection) {
        this.connection = connection;
    }
          
    /**
     * Get the current record.
     * @return The current record.
     */
    @Override
    public Object getCurrentRecord() throws IOException {
        return currentRecord;
    }
    
    /**
     * True because this source supports the <code>next</code> method.
     * @return True because this source supports next.
     */
    @Override
    public boolean supportsNext() {
        return true;
    }
  
    /**
     * True if the current record is non-null.
     * @return True if current record is non-null.
     */
    @Override
    public boolean hasCurrent() {
        return currentRecord != null;
    }
    
    /**
     * Load the next <code>EtEvent</code> which will either read
     * a cached record from the queue or fetch more records from
     * the ET server if the queue is empty.
     * @throws NoSuchRecordException if the queue is empty and getting
     * more records from the ET server fails.
     */
    @Override
    public void next() throws IOException, NoSuchRecordException {
        
        // Fill the queue if there are no events cached.
        if (eventQueue.size() == 0) {
            readEtEvents();
        }
        
        // Poll the queue.
        currentRecord = eventQueue.poll();
          
        if (currentRecord == null) {
            throw new NoSuchRecordException("ET record queue is empty.");
        }
    }
    
    /**
     * Get the number of records which is the size of the current queue.
     * @return The size of the queue.
     */
    @Override
    public long size() {
        return this.eventQueue.size();
    }
    
    /**
     * Read the next <code>EtEvent</code> array from the ET server.
     * @throws IOException if reading events fails.
     */
    private void readEtEvents() throws IOException {
        try {
            EtEvent[] mevs = connection.readEtEvents();
            eventQueue.addAll(Arrays.asList(mevs));        
        } catch (Exception e) {
            throw new EtSourceException("Error while reading ET events.", e);
        }
    }
    
    /**
     * An error that is used to indicate an error in the ET system
     * for the error handling of the loop.
     */
    public static class EtSourceException extends IOException {
        public EtSourceException(String message, Exception cause) {
            super(message, cause);
        }
    }
}