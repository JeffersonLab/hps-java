package org.hps.record.et;

import org.freehep.record.loop.DefaultRecordLoop;
import org.freehep.record.source.RecordSource;
import org.hps.record.ErrorState;
import org.hps.record.HasErrorState;
import org.jlab.coda.et.EtEvent;

/**
 * Record loop implementation for processing <tt>EtEvent</tt> objects.
 */
public final class EtLoop extends DefaultRecordLoop implements HasErrorState {

    EtAdapter adapter = new EtAdapter();
    ErrorState errorState = new ErrorState();
        
    public EtLoop() {
        addLoopListener(adapter);
        addRecordListener(adapter);
    }
 
    /**
     * Add an <code>EtEventProcessor</code> to the loop.
     * @param processor The <code>EtEventProcessor</code> to add.
     */
    public void addEtEventProcessor(EtProcessor processor) {
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
        getErrorState().setLastError((Exception) x);
        getErrorState().print();
    }

    protected void handleSourceError(Throwable x) {
        getErrorState().setLastError((Exception) x);
        getErrorState().print();
    }

    @Override
    public ErrorState getErrorState() {
        return errorState;
    }     
}
