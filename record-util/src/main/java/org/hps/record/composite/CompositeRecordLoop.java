package org.hps.record.composite;

import org.freehep.record.loop.DefaultRecordLoop;
import org.freehep.record.source.RecordSource;
import org.hps.record.EndRunException;
import org.hps.record.EventProcessingException;

/**
 * Implementation of a composite record loop for processing
 * ET, EVIO and LCIO events using a single record source.
 */
public final class CompositeRecordLoop extends DefaultRecordLoop {

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
        System.out.println("CompositeRecordLoop.handleClientError");
        System.out.println("  error: " + x.getClass().getCanonicalName());
        System.out.println("  cause: " + x.getCause().getClass().getCanonicalName());
        System.out.println("  loop state: " + this.getState().toString());
        x.printStackTrace();
        if (isIgnorable(x)) {
            System.out.println("  error is ignored");
            return;
        }        
        adapter.finish(null);
        this.execute(Command.STOP);
        
        // Rethrow so the loop caller can catch and handle this error.
        throw new RuntimeException("Error from client during event processing.", x);
    }

    protected void handleSourceError(Throwable x) {     
        System.out.println("CompositeRecordLoop.handleSourceError");
        System.out.println("error: " + x.getClass().getCanonicalName());
        System.out.println("cause: " + x.getCause().getClass().getCanonicalName());
        x.printStackTrace();
        if (isIgnorable(x)) {
            System.out.println("error is ignored");
            return;
        }
        adapter.finish(null);
        this.execute(Command.STOP);
        
        // Rethrow so the loop caller can catch and handle this error.
        throw new RuntimeException("Error from record source during event processing.", x);
    }        
    
    private boolean isIgnorable(Throwable x) {
        if (!stopOnErrors) {
            if ((x instanceof EventProcessingException) 
                    && !(x.getCause() instanceof EndRunException)
                    && !(x.getCause() instanceof IllegalStateException))
                return true;
        } else {
            return false;
        }
        return false;
    }
}
 