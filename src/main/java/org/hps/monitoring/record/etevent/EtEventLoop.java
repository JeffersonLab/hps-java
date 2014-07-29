package org.hps.monitoring.record.etevent;

import org.freehep.record.loop.DefaultRecordLoop;
import org.freehep.record.source.RecordSource;
import org.jlab.coda.et.EtEvent;

/**
 * Record loop implementation for processing <tt>EtEvent</tt> objects.
 */
public class EtEventLoop extends DefaultRecordLoop {

    EtEventAdapter adapter = new EtEventAdapter();
        
    public EtEventLoop() {
        addLoopListener(adapter);
        addRecordListener(adapter);
    }
 
    /**
     * Add an <code>EtEventProcessor</code> to the loop.
     * @param processor The <code>EtEventProcessor</code> to add.
     */
    public void addEtEventProcessor(EtEventProcessor processor) {
        adapter.addEtEventProcessor(processor);
    }
    
    /**
     * Set the <code>RecordSource</code> for the loop.
     * @param source The <code>RecordSource</code> for the loop.
     */
    public void setRecordSource(RecordSource source) {
        if (!source.getRecordClass().isAssignableFrom(EtEvent.class)) {
            throw new IllegalArgumentException("The RecordSource has the wrong class.");
        }        
        super.setRecordSource(source);
    }
    
    protected void handleClientError(Throwable x) {
        if (x != null) {
            throw new RuntimeException(x);
        }
    }

    protected void handleSourceError(Throwable x) {
        if (x != null) {
            throw new RuntimeException(x);
        }
    }     
}
