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
 * from an ET ring server connection.
 */
public final class EtSource extends AbstractRecordSource {
    
    EtConnection connection;
    EtEvent currentRecord;
    Queue<EtEvent> eventQueue = new LinkedBlockingQueue<EtEvent>();
        
    public EtSource(EtConnection connection) {
        this.connection = connection;
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
        return true;
    }
    
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
    
    @Override
    public long size() {
        return this.eventQueue.size();
    }
    
    void readEtEvents() throws IOException {
        try {
            EtEvent[] mevs = connection.readEtEvents();
            eventQueue.addAll(Arrays.asList(mevs));        
        } catch (Exception e) {
            throw new IOException("Error while reading ET events.", e);
        }
    }
}