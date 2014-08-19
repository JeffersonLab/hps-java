package org.hps.monitoring.subsys.et;

import junit.framework.TestCase;

/**
 * Test that the {@link EtSystemMonitor} works.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class EtSystemMonitorTest extends TestCase {

    /*
    public void testEtEventMonitoring() {
        
        EtEventLoop loop = new EtEventLoop();
        EtSystemMonitor monitor = new EtSystemPrinter();
        monitor.getSystemInfo().getStatus().addListener(new DummyListener());
        loop.addEtEventProcessor(monitor);
        EtEventQueue queue = new EtEventQueue();
        queue.setTimeOutMillis(1000);
        loop.setRecordSource(queue);
               
        for (int i=0; i<100000; i++) {                  
            byte[] data = new byte[256];
            EtEventImpl event = new EtEventImpl(256);
            event.setData(data);            
            queue.addRecord(event);
        }
        
        try {
            loop.loop(-1);
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        System.out.println("loop supplied " + loop.getTotalSupplied() + " records");
        System.out.println("loop consumed " + loop.getCountableConsumed() + " records");
    }    
    
    static class DummyListener implements SystemStatusListener {

        public void statusChanged(SystemStatus status) {
            System.out.println(this.getClass().getSimpleName() + " saw status changed to " + status.getStatusCode().toString() + " at " + status.getLastChangedMillis() + " millis");
        }        
    }
    
    static class EtSystemPrinter extends EtSystemMonitor{

        int eventsProcessed = 0;
        
        public void start() {
            super.start();
            info.getStatistics().printSession(System.out);
        }
        
        public void processEvent(EtEvent event) {
            super.processEvent(event);
            ++eventsProcessed;
            if (eventsProcessed % 1000 == 0)
                info.getStatistics().printTick(System.out);
        }    
        
        public void stop() {
            super.stop();
            info.getStatistics().printSession(System.out);
        }
    }
    */
}
