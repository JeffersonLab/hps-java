package org.hps.record.composite;

import junit.framework.TestCase;

import org.hps.evio.LCSimTestRunEventBuilder;
import org.hps.record.enums.DataSourceType;
import org.hps.record.evio.EvioEventProcessor;
import org.lcsim.event.EventHeader;
import org.lcsim.util.Driver;

/**
 * Test the handling of various types of errors than can occurr when using the CompositeLoop.
 */
public class CompositeLoopErrorTest extends TestCase {
       
    static String evioFilePath = "/nfs/slac/g/hps3/data/testcase/hps_000975.evio.0";   
    
    public void testError() {
        LCSimTestRunEventBuilder builder = new LCSimTestRunEventBuilder();
        builder.setDebug(false);        
        CompositeLoopConfiguration config = new CompositeLoopConfiguration()
            .setFilePath(evioFilePath)
            .setDataSourceType(DataSourceType.EVIO_FILE)
            .setLCSimEventBuilder(builder)
            .setDetectorName("HPS-TestRun-v8-5")
            .add(new DummyErrorDriver())
            .add(new DummyEvioProcessor());               
        CompositeLoop loop = new CompositeLoop(config);
        loop.loop(-1);
    }
    
    public void testStopOnEndRun() {
        LCSimTestRunEventBuilder builder = new LCSimTestRunEventBuilder();
        builder.setDebug(false);        
        CompositeLoopConfiguration config = new CompositeLoopConfiguration()
            .setFilePath(evioFilePath)
            .setDataSourceType(DataSourceType.EVIO_FILE)        
            .setLCSimEventBuilder(builder)
            .setDetectorName("HPS-TestRun-v8-5")
            .add(new DummyDriver())
            .add(new DummyEvioProcessor());        
        CompositeLoop loop = new CompositeLoop(config);
        loop.loop(-1);
    }
    
    public void testContinueOnError() {        
        LCSimTestRunEventBuilder builder = new LCSimTestRunEventBuilder();
        builder.setDebug(false);                
        CompositeLoopConfiguration config = new CompositeLoopConfiguration()
            .setFilePath(evioFilePath)
            .setDataSourceType(DataSourceType.EVIO_FILE)
            .setLCSimEventBuilder(builder)
            .setDetectorName("HPS-TestRun-v8-5")
            .add(new DummyErrorDriver())
            .add(new DummyEvioProcessor())
            .setStopOnErrors(false);        
        CompositeLoop loop = new CompositeLoop(config);
        loop.loop(-1);
    }
    
    public void testIgnoreEndRun() {
        LCSimTestRunEventBuilder builder = new LCSimTestRunEventBuilder();
        builder.setDebug(false);
        
        CompositeLoopConfiguration config = new CompositeLoopConfiguration()
            .setFilePath(evioFilePath)
            .setDataSourceType(DataSourceType.EVIO_FILE)
            .setLCSimEventBuilder(builder)
            .setDetectorName("HPS-TestRun-v8-5")
            .add(new DummyDriver())
            .add(new DummyEvioProcessor())
            .setStopOnEndRun(false);     
        CompositeLoop loop = new CompositeLoop(config);       
        loop.loop(-1);
    }
    
    public void testMaxRecords() {
        LCSimTestRunEventBuilder builder = new LCSimTestRunEventBuilder();
        builder.setDebug(false);        
        CompositeLoopConfiguration config = new CompositeLoopConfiguration()
            .setFilePath(evioFilePath)
            .setDataSourceType(DataSourceType.EVIO_FILE)        
            .setLCSimEventBuilder(builder)
            .setDetectorName("HPS-TestRun-v8-5")
            .add(new DummyDriver())
            .add(new DummyEvioProcessor())
            .setMaxRecords(2);
        CompositeLoop loop = new CompositeLoop(config);
        loop.loop(-1);
    }    

    static class DummyDriver extends Driver {
        public void process(EventHeader event) {
        }
        
        public void endOfData() {
            System.out.println("DummyErrorDriver.endOfData");
        }
    }
    
    static class DummyErrorDriver extends Driver {
        
        public void process(EventHeader event) {
            throw new RuntimeException("Dummy processing error.");
        }
        
        public void endOfData() {
            System.out.println("DummyErrorDriver.endOfData");
        }
    }
    
    static class DummyEvioProcessor extends EvioEventProcessor {
        public void endJob() {
            System.out.println("DummyEvioDriver.endJob");
        }        
    }      
}
