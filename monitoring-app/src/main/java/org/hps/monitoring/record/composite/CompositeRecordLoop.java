package org.hps.monitoring.record.composite;

import java.io.IOException;

import org.freehep.record.loop.DefaultRecordLoop;
import org.freehep.record.source.RecordSource;

/**
 * Implementation of a composite record loop for processing
 * ET, EVIO and LCIO events using a single record source.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class CompositeRecordLoop extends DefaultRecordLoop {

    CompositeRecordSource recordSource = new CompositeRecordSource();
    CompositeRecordLoopAdapter adapter = new CompositeRecordLoopAdapter();
    
    public CompositeRecordLoop() {
        setRecordSource(recordSource);
        addLoopListener(adapter);
        addRecordListener(adapter);
    }
    
    /**
     * Set the <code>RecordSource</code> which provides <code>CompositeRecord</code> objects.
     */
    public void setRecordSource(RecordSource source) {
        if (!source.getRecordClass().isAssignableFrom(CompositeRecord.class)) {
            throw new IllegalArgumentException("The RecordSource has the wrong class.");
        }        
        super.setRecordSource(source);
    }
    
    /**
     * Add a <code>CompositeRecordProcessor</code> which will receive <code>CompositeRecord</code>
     * objects.
     * @param processor The <code>CompositeRecordProcessor</code> to add.
     */
    public void addProcessor(CompositeRecordProcessor processor) {
        adapter.addProcessor(processor);
    }
    
    protected void handleClientError(Throwable x) {
        //System.out.println("CompositeRecordLoop.handleClientError - handling error " + x.getClass().getCanonicalName());
        //x.printStackTrace();
        throw new RuntimeException("Error during event processing.", x);
    }

    protected void handleSourceError(Throwable x) {
        //System.out.println("CompositeRecordLoop.handleSourceError - handling error " + x.getClass().getCanonicalName());        
        //x.printStackTrace();
        throw new RuntimeException("Error during event processing.", x);
    }        
}
