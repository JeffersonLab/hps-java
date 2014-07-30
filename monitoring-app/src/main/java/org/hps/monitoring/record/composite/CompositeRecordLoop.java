package org.hps.monitoring.record.composite;

import org.freehep.record.loop.DefaultRecordLoop;
import org.freehep.record.source.NoSuchRecordException;
import org.freehep.record.source.RecordSource;

/**
 * Implementation of a composite record loop for processing
 * ET, EVIO and LCIO events using a single record source.
 */
public class CompositeRecordLoop extends DefaultRecordLoop {

    CompositeRecordSource recordSource = new CompositeRecordSource();
    CompositeRecordLoopAdapter adapter = new CompositeRecordLoopAdapter();
    boolean stopOnErrors = true;
    
    public CompositeRecordLoop() {
        setRecordSource(recordSource);
        addLoopListener(adapter);
        addRecordListener(adapter);
    }
    
    public void setStopOnErrors(boolean stopOnErrors) {
        this.stopOnErrors = stopOnErrors;
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
        if (stopOnErrors || x instanceof NoSuchRecordException) {
            throw new RuntimeException("Error during event processing.", x);
        } else {
            x.printStackTrace();
        }        
    }

    protected void handleSourceError(Throwable x) {
        throw new RuntimeException("Error during event processing.", x);
    }        
}
