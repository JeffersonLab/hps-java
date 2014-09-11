package org.hps.record.lcio;

import org.hps.record.AbstractRecordQueue;
import org.lcsim.event.EventHeader;

public class LcioEventQueue extends AbstractRecordQueue<EventHeader> {    
    
    public LcioEventQueue(long timeoutMillis, int maxQueueSize) {
        super(timeoutMillis, maxQueueSize);
    }
    
    public Class<?> getRecordClass() {
        return EventHeader.class;
    }
    
    public boolean supportsCurrent() {
        return true;
    }

    public boolean supportsNext() {
        return true;
    }
    
    public boolean supportsPrevious() {
        return false;
    }
    
    public boolean supportsIndex() {
        return false;
    }
    
    public boolean supportsShift() {
        return false;
    }
    
    public boolean supportsRewind() {
        return false;
    }
    
    public boolean hasCurrent() {
        return this.size() != 0L;
    }

    public boolean hasNext() {
        return true;
    }      
}
