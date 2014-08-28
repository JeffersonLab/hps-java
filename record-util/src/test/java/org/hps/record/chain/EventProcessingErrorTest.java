package org.hps.record.chain;

import junit.framework.TestCase;

import org.hps.evio.LCSimTestRunEventBuilder;
import org.hps.record.DataSourceType;
import org.hps.record.evio.EvioEventProcessor;
import org.lcsim.event.EventHeader;
import org.lcsim.util.Driver;


public class EventProcessingErrorTest extends TestCase {
       
    static String evioFilePath = "/nfs/slac/g/hps3/data/testcase/hps_000975.evio.0";
    
    public void testError() {                
        EventProcessingConfiguration config = new EventProcessingConfiguration();
        config.setFilePath(evioFilePath);
        config.setDataSourceType(DataSourceType.EVIO_FILE);
        LCSimTestRunEventBuilder builder = new LCSimTestRunEventBuilder();
        builder.setDebug(false);
        config.setLCSimEventBuild(builder);
        config.setDetectorName("HPS-TestRun-v8-5");
        config.add(new DummyErrorDriver());
        config.add(new DummyEvioProcessor());       
        EventProcessingChain processing = new EventProcessingChain(config);
        processing.run();
    }
    
    public void testNoError() {        
        EventProcessingConfiguration config = new EventProcessingConfiguration();
        config.setFilePath(evioFilePath);
        config.setDataSourceType(DataSourceType.EVIO_FILE);
        LCSimTestRunEventBuilder builder = new LCSimTestRunEventBuilder();
        builder.setDebug(false);
        config.setLCSimEventBuild(builder);
        config.setDetectorName("HPS-TestRun-v8-5");
        config.add(new DummyDriver());
        config.add(new DummyEvioProcessor());        
        EventProcessingChain processing = new EventProcessingChain(config);
        processing.run();
    }
    
    public void testContinueOnError() {
        EventProcessingConfiguration config = new EventProcessingConfiguration();
        config.setFilePath(evioFilePath);
        config.setDataSourceType(DataSourceType.EVIO_FILE);
        LCSimTestRunEventBuilder builder = new LCSimTestRunEventBuilder();
        builder.setDebug(false);
        config.setLCSimEventBuild(builder);
        config.setDetectorName("HPS-TestRun-v8-5");
        config.add(new DummyErrorDriver());
        config.add(new DummyEvioProcessor());
        config.setStopOnErrors(false);        
        EventProcessingChain processing = new EventProcessingChain(config);       
        processing.run();
    }
    
    public void testIgnoreEndRun() {
        EventProcessingConfiguration config = new EventProcessingConfiguration();
        config.setFilePath(evioFilePath);
        config.setDataSourceType(DataSourceType.EVIO_FILE);
        LCSimTestRunEventBuilder builder = new LCSimTestRunEventBuilder();
        builder.setDebug(false);
        config.setLCSimEventBuild(builder);
        config.setDetectorName("HPS-TestRun-v8-5");
        config.add(new DummyDriver());
        config.add(new DummyEvioProcessor());
        config.setStopOnEndRun(false);        
        EventProcessingChain processing = new EventProcessingChain(config);       
        processing.run();
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
