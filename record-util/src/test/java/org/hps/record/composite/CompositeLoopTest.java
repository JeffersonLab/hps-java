package org.hps.record.composite;

import junit.framework.TestCase;

import org.hps.evio.EventConstants;
import org.hps.evio.LCSimEventBuilder;
import org.hps.evio.LCSimTestRunEventBuilder;
import org.hps.record.enums.DataSourceType;
import org.hps.record.et.EtConnection;
import org.hps.record.et.EtEventProcessor;
import org.hps.record.evio.EvioEventProcessor;
import org.jlab.coda.et.EtEvent;
import org.jlab.coda.jevio.EvioEvent;
import org.lcsim.event.EventHeader;
import org.lcsim.geometry.Detector;
import org.lcsim.job.EventMarkerDriver;
import org.lcsim.util.Driver;

public class CompositeLoopTest extends TestCase {
    
    public void testProcessingChain() {
        
        CompositeLoopConfiguration config = new CompositeLoopConfiguration();
                
        LCSimEventBuilder builder = new LCSimTestRunEventBuilder();
        builder.setDebug(false);
        
        DummyEtProcessor etProcessor = new DummyEtProcessor();
        DummyEvioProcessor evioProcessor = new DummyEvioProcessor();
        DummyDriver driver = new DummyDriver();
        DummyCompositeProcessor compositeProcessor = new DummyCompositeProcessor();
        
        config.setDataSourceType(DataSourceType.ET_SERVER)
            .setDetectorName("HPS-TestRun-v8-5")
            .setEtConnection(EtConnection.createDefaultConnection())
            .setLCSimEventBuilder(builder)
            .setStopOnEndRun(true)
            .setStopOnErrors(true)
            .add(etProcessor)
            .add(evioProcessor)
            .add(new EventMarkerDriver())
            .add(driver)
            .add(compositeProcessor);
        
        CompositeLoop loop = new CompositeLoop(config);
        loop.loop(-1);
        
        //
        // Test assertions...
        //        
        TestCase.assertTrue(etProcessor.startJobCalled);
        TestCase.assertTrue(etProcessor.processCalled);
        TestCase.assertTrue(etProcessor.endJobCalled);        
        
        TestCase.assertTrue(evioProcessor.startJobCalled);
        TestCase.assertTrue(evioProcessor.processCalled);
        TestCase.assertTrue(evioProcessor.endJobCalled);
        
        TestCase.assertTrue(driver.startOfDataCalled);
        TestCase.assertTrue(driver.detectorChangedCalled);
        TestCase.assertTrue(driver.processCalled);
        TestCase.assertTrue(driver.endOfDataCalled);    
        
        TestCase.assertTrue(compositeProcessor.startJobCalled);
        TestCase.assertTrue(compositeProcessor.processCalled);
        TestCase.assertTrue(compositeProcessor.endJobCalled);                              
    }    

    static class DummyCompositeProcessor extends CompositeRecordProcessor {
        
        boolean startJobCalled = false;
        boolean endJobCalled = false;
        boolean processCalled = false;
        
        public void process(CompositeRecord record) {
            System.out.println("DummyCompositeProcessor.process");
            System.out.flush();
            TestCase.assertNotNull(record);
            TestCase.assertNotNull(record.getEtEvent());
            TestCase.assertNotNull(record.getEvioEvent());            
            if (EventConstants.isPhysicsEvent(record.getEvioEvent()))
                TestCase.assertNotNull(record.getLcioEvent());
            processCalled = true;
        }
        
        public void startJob() {
            System.out.println("DummyCompositeProcessor.startJob");
            System.out.flush();
            startJobCalled = true;
        }
        
        public void endJob() {
            System.out.println("DummyCompositeProcessor.endJob");
            System.out.flush();
            endJobCalled = true;
        }              
    }
        
    static class DummyDriver extends Driver {
        
        boolean endOfDataCalled = false;
        boolean startOfDataCalled = false;
        boolean processCalled = false;
        boolean detectorChangedCalled = false;
        
        public void process(EventHeader event) {
            System.out.println("DummyDriver.process");
            System.out.flush();
            TestCase.assertNotNull(event);
            processCalled = true;
        }
        
        public void endOfData() {
            System.out.println("DummyDriver.endOfData");
            System.out.flush();
            endOfDataCalled = true;
        }
        
        public void startOfData() {
            System.out.println("DummyDriver.startOfData");
            System.out.flush();
            startOfDataCalled = true;
        }
        
        public void detectorChanged(Detector detector) {
            System.out.println("DummyDriver.detectorChanged");
            System.out.flush();
            detectorChangedCalled = true;
        }
    }    
    
    static class DummyEtProcessor extends EtEventProcessor {
        
        boolean startJobCalled = false;
        boolean endJobCalled = false;
        boolean processCalled = false;
        
        public void process(EtEvent event) {
            System.out.println("DummyEtProcessor.process");
            System.out.flush();
            TestCase.assertNotNull(event);
            processCalled = true;
        }
        
        public void startJob() {
            System.out.println("DummyEtProcessor.startJob");
            System.out.flush();
            startJobCalled = true;
        }
        
        public void endJob() {
            System.out.println("DummyEtProcessor.endJob");
            System.out.flush();
            endJobCalled = true;
        }        
    }
    
    static class DummyEvioProcessor extends EvioEventProcessor {
        
        boolean startJobCalled = false;
        boolean endJobCalled = false;
        boolean processCalled = false;
        
        public void process(EvioEvent event) {
            System.out.println("DummyEvioProcessor.process");
            System.out.flush();
            TestCase.assertNotNull(event);
            processCalled = true;
        }
        
        public void startJob() {
            System.out.println("DummyEvioProcessor.startJob");
            System.out.flush();
            startJobCalled = true;
        }
        
        public void endJob() {
            System.out.println("DummyEvioProcessor.endJob");
            System.out.flush();
            endJobCalled = true;
        }              
    }
}
