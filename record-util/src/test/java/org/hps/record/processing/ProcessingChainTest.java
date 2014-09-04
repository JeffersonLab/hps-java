package org.hps.record.processing;

import junit.framework.TestCase;

import org.freehep.record.loop.RecordListener;
import org.freehep.record.loop.RecordLoop.Command;
import org.hps.evio.EventConstants;
import org.hps.evio.LCSimEventBuilder;
import org.hps.evio.LCSimTestRunEventBuilder;
import org.hps.record.composite.CompositeEtAdapter;
import org.hps.record.composite.CompositeEvioAdapter;
import org.hps.record.composite.CompositeLcioAdapter;
import org.hps.record.composite.CompositeLoop;
import org.hps.record.composite.CompositeProcessor;
import org.hps.record.composite.CompositeRecord;
import org.hps.record.et.EtConnection;
import org.hps.record.et.EtProcessor;
import org.hps.record.et.EtSource;
import org.hps.record.evio.EvioProcessor;
import org.jlab.coda.et.EtEvent;
import org.jlab.coda.jevio.EvioEvent;
import org.lcsim.event.EventHeader;
import org.lcsim.geometry.Detector;
import org.lcsim.job.EventMarkerDriver;
import org.lcsim.util.Driver;

public class ProcessingChainTest extends TestCase {
    
    public void testProcessingChain() {
        
        ProcessingConfiguration configuration = new ProcessingConfiguration();
        configuration.setDataSourceType(DataSourceType.ET_SERVER);
        configuration.setDetectorName("HPS-TestRun-v8-5");
        configuration.setEtConnection(EtConnection.createDefaultConnection());
        LCSimEventBuilder builder = new LCSimTestRunEventBuilder();
        builder.setDebug(false);
        configuration.setLCSimEventBuild(builder);
        configuration.setStopOnEndRun(true);
        configuration.setStopOnErrors(true);                
        
        DummyEtProcessor etProcessor = new DummyEtProcessor();
        DummyEvioProcessor evioProcessor = new DummyEvioProcessor();
        DummyDriver driver = new DummyDriver();
        DummyCompositeProcessor compositeProcessor = new DummyCompositeProcessor();
        
        configuration.add(etProcessor);
        configuration.add(evioProcessor);
        configuration.add(new EventMarkerDriver());
        configuration.add(driver);
        configuration.add(compositeProcessor);
        
        ProcessingChain processing = new ProcessingChain(configuration);
        for (RecordListener listener : processing.getLoop().getRecordListeners()) {
            System.out.println("listener: " + listener.getClass().getCanonicalName());
        }
        processing.run();
        
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

    public void testCompositeLoop() {
        
        // Setup the primary loop.
        CompositeLoop loop = new CompositeLoop();
        
        // Setup ET system.
        EtConnection connection = EtConnection.createDefaultConnection();
        CompositeEtAdapter etAdapter = new CompositeEtAdapter(new EtSource(connection));
        DummyEtProcessor etProcessor = new DummyEtProcessor();
        etAdapter.addProcessor(etProcessor);
        loop.addAdapter(etAdapter);
        
        // Setup EVIO adapter.
        CompositeEvioAdapter evioAdapter = new CompositeEvioAdapter();
        DummyEvioProcessor evioProcessor = new DummyEvioProcessor();
        evioAdapter.addProcessor(evioProcessor);
        loop.addAdapter(evioAdapter);
        
        // Setup LCIO adapter and LCSim.
        LCSimTestRunEventBuilder builder = new LCSimTestRunEventBuilder();
        builder.setDebug(false);
        builder.setDetectorName("HPS-TestRun-v8-5");        
        CompositeLcioAdapter lcioAdapter = new CompositeLcioAdapter();
        DummyDriver driver = new DummyDriver();
        lcioAdapter.addDriver(driver);
        lcioAdapter.setLCSimEventBuilder(builder);
        loop.addAdapter(lcioAdapter);
        
        // Loop over records.
        loop.execute(Command.GO, true);
        loop.dispose();
        
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
    }
    
    static class DummyCompositeProcessor extends CompositeProcessor {
        
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
    
    static class DummyEtProcessor extends EtProcessor {
        
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
    
    static class DummyEvioProcessor extends EvioProcessor {
        
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
