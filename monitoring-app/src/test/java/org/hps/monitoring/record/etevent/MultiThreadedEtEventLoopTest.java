package org.hps.monitoring.record.etevent;

import org.freehep.record.loop.RecordLoop.Command;
import org.jlab.coda.et.EtEvent;
import org.jlab.coda.et.EtEventImpl;

/**
 * Test that the {@link EtEventLoop} works when the loop and source
 * are run on seperate threads.
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class MultiThreadedEtEventLoopTest {

    // Time in milliseconds to wait before queuing a new dummy event.
    static int EVENT_INTERVAL = 10;

    public void testThreadedQueue() {

        // Setup the loop.
        EtEventLoop loop = new EtEventLoop();
        loop.addEtEventProcessor(new DummyEtEventProcessor());

        // Create the event queue.
        EtEventQueue queue = new EtEventQueue();
        queue.setTimeOutMillis(10000);
        loop.setRecordSource(queue);
        
        // Create runnable objects.
        LoopRunnable loopRunnable = new LoopRunnable(loop);
        QueueRunnable queueRunnable = new QueueRunnable(queue, EVENT_INTERVAL);

        // Start loop thread.
        Thread loopThread = new Thread(loopRunnable);
        loopThread.start();

        // Start queue thread.
        Thread queueThread = new Thread(queueRunnable);
        queueThread.start();

        // Wait for queue thread to end.
        try {
            queueThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("loop got " + loop.getConsumed() + " records");

        System.out.println("disposing loop ...");
        loop.dispose();
        loop = null;
        System.gc();
    }

    static class LoopRunnable implements Runnable {

        EtEventLoop loop;

        LoopRunnable(EtEventLoop loop) {
            this.loop = loop;
        }

        public void run() {
            loop.execute(Command.GO, false);
        }
    }

    static class QueueRunnable implements Runnable {

        EtEventQueue queue = null;
        int waitTimeMillis = 0;

        QueueRunnable(EtEventQueue queue, int waitTimeMillis) {
            this.queue = queue;
            this.waitTimeMillis = waitTimeMillis;
        }

        public void run() {
            for (int i = 1; i <= 1000; i++) {
                byte[] data = new byte[256];
                EtEventImpl event = new EtEventImpl(256);
                event.setData(data);
                queue.addRecord(event);                
                delay();
            }
            System.out.println(this.getClass().getSimpleName() + " is done adding events.");
        }

        synchronized private void delay() {
            try {
                wait(waitTimeMillis);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    static class DummyEtEventProcessor extends EtEventProcessor {
        
        public void processEvent(EtEvent event) {
            System.out.println(this.getClass().getSimpleName() + " got EtEvent of length " + event.getLength());
        }
        
    }
}
