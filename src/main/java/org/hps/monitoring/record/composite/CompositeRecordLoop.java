package org.hps.monitoring.record.composite;

import java.util.List;

import org.freehep.record.loop.DefaultRecordLoop;
import org.freehep.record.loop.RecordLoop;
import org.freehep.record.source.RecordSource;
import org.hps.monitoring.record.EventProcessingStep;

public class CompositeRecordLoop extends DefaultRecordLoop {

    CompositeRecordSource recordSource = new CompositeRecordSource();
    CompositeRecordLoopAdapter adapter = new CompositeRecordLoopAdapter();
    
    public CompositeRecordLoop() {
        setRecordSource(recordSource);
        addLoopListener(adapter);
        addRecordListener(adapter);
    }
    
    public void setRecordSource(RecordSource source) {
        if (!source.getRecordClass().isAssignableFrom(CompositeRecord.class)) {
            throw new IllegalArgumentException("The RecordSource has the wrong class.");
        }        
        super.setRecordSource(source);
    }
        
    public void addProcessingSteps(List<EventProcessingStep> processingSteps) {
        recordSource.addProcessingSteps(processingSteps);
    }
    
    public void registerRecordLoop(RecordLoop loop) {
        adapter.registerRecordLoop(loop);
    }
    
    public void loop(long n) {
        //super.loop();
        //execute(Command.GO, true);
        execute(Command.GO, true);
    }
}
