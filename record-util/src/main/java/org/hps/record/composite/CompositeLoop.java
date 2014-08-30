package org.hps.record.composite;

import org.freehep.record.loop.DefaultRecordLoop;
import org.freehep.record.source.NoSuchRecordException;
import org.freehep.record.source.RecordSource;
import org.hps.record.EndRunException;
import org.hps.record.MaxRecordsException;
import org.hps.record.et.EtSource.EtSourceException;

/**
 * Implementation of a composite record loop for processing
 * ET, EVIO and LCIO events using a single record source.
 */
public final class CompositeLoop extends DefaultRecordLoop {

    CompositeSource recordSource = new CompositeSource();
    CompositeLoopAdapter adapter = new CompositeLoopAdapter();
    
    boolean stopOnErrors = true;
    boolean done = false;
    
    public CompositeLoop() {
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
    public void addProcessor(CompositeProcessor processor) {
        adapter.addProcessor(processor);
    }
            
    /**
     * Handle errors in the client such as adapters.
     * If the loop is setup to try and continue on errors, 
     * only non-fatal record processing exceptions are ignored.
     */
    protected void handleClientError(Throwable x) {      
                
        // Is the error ignorable?
        if (isIgnorable(x)) {
            // Ignore the error!
            return;
        }
        
        // Set the exception on the super class.
        this._exception = x;
        
        // Stop the event processing.
        this.execute(Command.STOP);
        done = true;
    }

    protected void handleSourceError(Throwable x) {
                
        // Is the error ignorable?
        if (isIgnorable(x)) {
            // Ignore the error!
            return;
        }
        
        // Set the exception on the super class.
        this._exception = x;
        
        // Stop the event processing.
        this.execute(Command.STOP);
        done = true;
    }        
    
    private boolean isIgnorable(Throwable x) {
        
        // Should the loop try to recover from the error if possible?
        if (!stopOnErrors) {
        
            // EndRunExceptions are never ignored.
            if (x.getCause() instanceof EndRunException)
                return false;
        
            // MaxRecordsExceptions are never ignored.
            if (x.getCause() instanceof MaxRecordsException)
                return false;
        
            // ET system errors are always considered fatal.
            if (x.getCause() instanceof EtSourceException)
                return false;
        
            // The NoSuchRecordException indicates a RecordSource 
            // was exhausted so processing needs to end.
            if (x.getCause() instanceof NoSuchRecordException)
                return false;
        
            // When this occurs on of the loops is probably messed up, 
            // so it is not considered recoverable.
            if (x.getCause() instanceof IllegalStateException) 
                return false;
        
            // Ignore the error.
            return true;
            
        } else {        
            // Error is not ignored. 
            return false;
        }
    }
        
    public boolean isDone() {
        return done;
    }
    
    public Throwable getLastError() {
        return _exception;
    }
}
 