package org.hps.record.et;

import org.freehep.record.loop.RecordLoop.Command;
import org.hps.record.et.EtLoop;
import org.hps.record.et.EtProcessor;
import org.hps.record.et.EtRecordQueue;
import org.jlab.coda.et.EtEvent;
import org.jlab.coda.et.EtEventImpl;

/**
 * Test that the {@link EtLoop} works.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class EtLoopTest {
    
    public void testEtEventLoop() {
        
        EtLoop loop = new EtLoop();
        loop.addEtEventProcessor(new DummyEtEventProcessor());
        EtRecordQueue queue = new EtRecordQueue();
        queue.setTimeOutMillis(10000);
        loop.setRecordSource(queue);
               
        for (int i=0; i<100000; i++) {
            EtEvent event = new EtEventImpl(1000);
            queue.addRecord(event);
        }
        
        loop.execute(Command.GO, true);
        
        System.out.println("loop processed " + loop.getTotalSupplied() + " records");
    }

    static class DummyEtEventProcessor extends EtProcessor {
        
        public void process(EtEvent event) {
            System.out.println(this.getClass().getSimpleName() + " got EtEvent of length " + event.getLength());
        }
        
    }
    
}
