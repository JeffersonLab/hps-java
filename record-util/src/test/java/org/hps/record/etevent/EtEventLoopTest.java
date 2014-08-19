package org.hps.monitoring.record.etevent;

import java.io.IOException;

import org.freehep.record.loop.RecordLoop.Command;
import org.jlab.coda.et.EtEvent;
import org.jlab.coda.et.EtEventImpl;

/**
 * Test that the {@link EtEventLoop} works.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class EtEventLoopTest {
    
    public void testEtEventLoop() {
        
        EtEventLoop loop = new EtEventLoop();
        loop.addEtEventProcessor(new DummyEtEventProcessor());
        EtEventQueue queue = new EtEventQueue();
        queue.setTimeOutMillis(10000);
        loop.setRecordSource(queue);
               
        for (int i=0; i<100000; i++) {
            EtEvent event = new EtEventImpl(1000);
            queue.addRecord(event);
        }
        
        loop.execute(Command.GO, true);
        
        System.out.println("loop processed " + loop.getTotalSupplied() + " records");
    }

    static class DummyEtEventProcessor extends EtEventProcessor {
        
        public void processEvent(EtEvent event) {
            System.out.println(this.getClass().getSimpleName() + " got EtEvent of length " + event.getLength());
        }
        
    }
    
}
