package org.hps.monitoring.record.evio;

import java.io.IOException;

import org.freehep.record.loop.DefaultRecordLoop;
import org.freehep.record.source.RecordSource;
import org.jlab.coda.jevio.EvioEvent;

/**
 * Implementation of record loop for processing <tt>EvioEvent</tt> objects.
 */
public final class EvioEventLoop extends DefaultRecordLoop {

    EvioAdapter adapter = new EvioAdapter();
        
    public EvioEventLoop() {
        addLoopListener(adapter);
        addRecordListener(adapter);
    }
    
    public void addEvioEventProcessor(EvioEventProcessor processor) {
        adapter.addEvioEventProcessor(processor);
    }
    
    @Override
    public void setRecordSource(RecordSource source) {
        if (!source.getRecordClass().isAssignableFrom(EvioEvent.class)) {
            System.err.println("The class " + source.getRecordClass().getCanonicalName() + " is invalid.");
            throw new IllegalArgumentException("The record class is invalid.");
        }        
        super.setRecordSource(source);
    }
    
    public long loop(long number) throws IOException {
        if (number < 0L) {
            execute(Command.GO, true);
        } else {
            execute(Command.GO_N, number, true);
            execute(Command.STOP);
        }
        Throwable t = getProgress().getException();
        if (t != null && t instanceof IOException)
            throw (IOException) t;
        return getSupplied();
    }
    
    protected void handleClientError(Throwable x) {
        if (x != null) {
            x.printStackTrace();
            throw new RuntimeException(x);
        }
    }

    protected void handleSourceError(Throwable x) {
        if (x != null) {
            x.printStackTrace();
            throw new RuntimeException(x);
        }
    }
}
