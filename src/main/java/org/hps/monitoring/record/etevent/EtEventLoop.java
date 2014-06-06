package org.hps.monitoring.record.etevent;

import java.io.IOException;

import org.freehep.record.loop.DefaultRecordLoop;
import org.freehep.record.source.RecordSource;
import org.jlab.coda.et.EtEvent;

/**
 * Record loop implementation for processing <tt>EtEvent</tt> objects.
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class EtEventLoop extends DefaultRecordLoop {

    EtEventAdapter adapter = new EtEventAdapter();
        
    public EtEventLoop() {
        addLoopListener(adapter);
        addRecordListener(adapter);
    }
    
    public void addEtEventProcessor(EtEventProcessor processor) {
        adapter.addEtEventProcessor(processor);
    }
    
    public void setRecordSource(RecordSource source) {
        if (!source.getRecordClass().isAssignableFrom(EtEvent.class)) {
            throw new IllegalArgumentException("The RecordSource has the wrong class.");
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
}
