package org.hps.record.composite;

import org.freehep.record.loop.DefaultRecordLoop;
import org.freehep.record.source.NoSuchRecordException;
import org.freehep.record.source.RecordSource;
import org.hps.record.EndRunException;
import org.hps.record.MaxRecordsException;
import org.jlab.coda.et.exception.EtBusyException;
import org.jlab.coda.et.exception.EtClosedException;
import org.jlab.coda.et.exception.EtDeadException;
import org.jlab.coda.et.exception.EtEmptyException;
import org.jlab.coda.et.exception.EtException;
import org.jlab.coda.et.exception.EtExistsException;
import org.jlab.coda.et.exception.EtReadException;
import org.jlab.coda.et.exception.EtTimeoutException;
import org.jlab.coda.et.exception.EtTooManyException;
import org.jlab.coda.et.exception.EtWakeUpException;
import org.jlab.coda.et.exception.EtWriteException;

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
        // Print full error traceback.
        x.printStackTrace();
                
        // Is the error ignorable?
        if (isIgnorable(x)) {
            // We can return.
            return;
        }
        
        // Stop the event processing.
        this.execute(Command.STOP);
        done = true;
    }

    protected void handleSourceError(Throwable x) {
        
        // Print full error traceback.
        x.printStackTrace();
        
        // Is the error ignorable?
        if (isIgnorable(x)) {
            return;
        }
        
        // Stop the event processing.
        this.execute(Command.STOP);
        done = true;
    }        
    
    private boolean isIgnorable(Throwable x) {
        
        // EndRunExceptions are never ignored.
        if (x.getCause() instanceof EndRunException)
            return false;
        
        // MaxRecordsExceptions are never ignored.
        if (x.getCause() instanceof MaxRecordsException)
            return false;
        
        // ET system errors are always considered fatal.
        if (isEtError((Exception) x.getCause()))
            return false;
        
        // Should the loop try to recover from the error?
        if (!stopOnErrors) {
            // Is the cause of the error ignorable?
            if (!(x.getCause() instanceof IllegalStateException) 
                    && !(x.getCause() instanceof NoSuchRecordException))
                // Ignore the error.
                return true;
        } 
        
        // Error is not ignorable. 
        return false;
    }
    
    /**
     * True if the Exception is from the ET system.
     * @param e The Exception that was thrown.
     * @return True if the Exception is from the ET system.
     */
    private boolean isEtError(Exception e) {       
        // Get the actual cause e.g. from 
        // RecordProcessingException -> IOException -> EtException
        // which originates from EtRecordSource.
        Throwable t = e.getCause().getCause(); 
        if ((t instanceof EtBusyException) ||
                (t instanceof EtClosedException) ||
                (t instanceof EtDeadException) ||
                (t instanceof EtEmptyException) ||
                (t instanceof EtException) ||
                (t instanceof EtExistsException) ||
                (t instanceof EtReadException) ||
                (t instanceof EtTimeoutException) ||
                (t instanceof EtTooManyException) ||
                (t instanceof EtWakeUpException) ||
                (t instanceof EtWriteException)) {
            return true;
        } else {
            return false;
        }     
    }
    
    public boolean isDone() {
        return done;
    }
}
 