package org.hps.record.composite;

import java.util.ArrayList;
import java.util.List;

import org.freehep.record.loop.DefaultRecordLoop;
import org.freehep.record.loop.RecordEvent;
import org.freehep.record.loop.RecordListener;
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
            
    boolean stopOnErrors = true;
    boolean done = false;
    
    List<CompositeLoopAdapter> adapters = new ArrayList<CompositeLoopAdapter>();
            
    public CompositeLoop() {
        setRecordSource(recordSource);
    }
    
    public void setStopOnErrors(boolean stopOnErrors) {
        this.stopOnErrors = stopOnErrors;
    }
    
    public void addAdapter(CompositeLoopAdapter adapter) {
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
     * Handle errors in the client such as adapters.
     * If the loop is setup to try and continue on errors, 
     * only non-fatal record processing exceptions are ignored.
     */
    protected void handleClientError(Throwable x) {      
        
        x.printStackTrace();
        
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

        x.printStackTrace();

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
        
            // When this occurs one of the loops is probably messed up, 
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