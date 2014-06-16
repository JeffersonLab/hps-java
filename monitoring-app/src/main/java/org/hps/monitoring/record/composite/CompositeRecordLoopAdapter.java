package org.hps.monitoring.record.composite;

import java.util.ArrayList;
import java.util.List;

import org.freehep.record.loop.AbstractLoopListener;
import org.freehep.record.loop.LoopEvent;
import org.freehep.record.loop.RecordEvent;
import org.freehep.record.loop.RecordListener;
import org.freehep.record.loop.RecordLoop;
import org.freehep.record.loop.RecordLoop.Command;

public class CompositeRecordLoopAdapter extends AbstractLoopListener implements RecordListener {

    List<RecordLoop> registeredLoops = new ArrayList<RecordLoop>();
    
    public void finish(LoopEvent loopEvent) {
        System.out.println("CompositeRecordLoopAdapter.finish");
        for (RecordLoop loop : registeredLoops) {
            loop.execute(Command.STOP);
        }
        if (loopEvent.getException() != null)
            loopEvent.getException().printStackTrace();
    }
        
    void registerRecordLoop(RecordLoop loop) {
        registeredLoops.add(loop);
    }
    
    /*
    public void suspend(LoopEvent loopEvent) {
        for (RecordLoop loop : registeredLoops) {
            loop.execute(Command.PAUSE);
        }
    }
    */
    
    // NOTE: IOExceptions from loop processing show up here!!!
    public void suspend(LoopEvent loopEvent) {        
        System.out.println("CompositeRecordLoopAdapter.suspend");
        if (loopEvent.getException() != null)
            loopEvent.getException().printStackTrace();
    }

    @Override
    public void recordSupplied(RecordEvent record) {
    }    
}
