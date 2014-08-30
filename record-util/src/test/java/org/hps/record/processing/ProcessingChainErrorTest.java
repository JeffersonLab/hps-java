package org.hps.record.processing;

import junit.framework.TestCase;

import org.hps.evio.LCSimTestRunEventBuilder;
import org.hps.record.evio.EvioProcessor;
import org.hps.record.processing.DataSourceType;
import org.hps.record.processing.ProcessingChain;
import org.hps.record.processing.ProcessingConfiguration;
import org.lcsim.event.EventHeader;
import org.lcsim.util.Driver;

/**
 * Test the handling of various types of errors than can occur in the processing chain.
 */
public class ProcessingChainErrorTest extends TestCase {
       
    static String evioFilePath = "/nfs/slac/g/hps3/data/testcase/hps_000975.evio.0";
    
    public void testError() {                
        ProcessingConfiguration config = new ProcessingConfiguration();
        config.setFilePath(evioFilePath);
        config.setDataSourceType(DataSourceType.EVIO_FILE);
        LCSimTestRunEventBuilder builder = new LCSimTestRunEventBuilder();
        builder.setDebug(false);
        config.setLCSimEventBuild(builder);
        config.setDetectorName("HPS-TestRun-v8-5");
        config.add(new DummyErrorDriver());
        config.add(new DummyEvioProcessor());       
        ProcessingChain processing = new ProcessingChain(config);
        processing.run();
    }
    
    public void testStopOnEndRun() {        
        ProcessingConfiguration config = new ProcessingConfiguration();
        config.setFilePath(evioFilePath);
        config.setDataSourceType(DataSourceType.EVIO_FILE);
        LCSimTestRunEventBuilder builder = new LCSimTestRunEventBuilder();
        builder.setDebug(false);
        config.setLCSimEventBuild(builder);
        config.setDetectorName("HPS-TestRun-v8-5");
        config.add(new DummyDriver());
        config.add(new DummyEvioProcessor());        
        ProcessingChain processing = new ProcessingChain(config);
        processing.run();
    }
    
    public void testContinueOnError() {
        ProcessingConfiguration config = new ProcessingConfiguration();
        config.setFilePath(evioFilePath);
        config.setDataSourceType(DataSourceType.EVIO_FILE);
        LCSimTestRunEventBuilder builder = new LCSimTestRunEventBuilder();
        builder.setDebug(false);
        config.setLCSimEventBuild(builder);
        config.setDetectorName("HPS-TestRun-v8-5");
        config.add(new DummyErrorDriver());
        config.add(new DummyEvioProcessor());
        config.setStopOnErrors(false);        
        ProcessingChain processing = new ProcessingChain(config);       
        processing.run();
    }
    
    public void testIgnoreEndRun() {
        ProcessingConfiguration config = new ProcessingConfiguration();
        config.setFilePath(evioFilePath);
        config.setDataSourceType(DataSourceType.EVIO_FILE);
        LCSimTestRunEventBuilder builder = new LCSimTestRunEventBuilder();
        builder.setDebug(false);
        config.setLCSimEventBuild(builder);
        config.setDetectorName("HPS-TestRun-v8-5");
        config.add(new DummyDriver());
        config.add(new DummyEvioProcessor());
        config.setStopOnEndRun(false);        
        ProcessingChain processing = new ProcessingChain(config);       
        processing.run();
    }
    
    public void testMaxRecords() {
        ProcessingConfiguration config = new ProcessingConfiguration();
        config.setFilePath(evioFilePath);
        config.setDataSourceType(DataSourceType.EVIO_FILE);
        LCSimTestRunEventBuilder builder = new LCSimTestRunEventBuilder();
        builder.setDebug(false);
        config.setLCSimEventBuild(builder);
        config.setDetectorName("HPS-TestRun-v8-5");
        config.add(new DummyDriver());
        config.add(new DummyEvioProcessor());
        config.setMaxRecords(2);
        ProcessingChain processing = new ProcessingChain(config);
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
    
    static class DummyEvioProcessor extends EvioProcessor {
        public void endJob() {
            System.out.println("DummyEvioDriver.endJob");
        }        
    }      
}
