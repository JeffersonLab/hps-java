package org.hps.monitoring.record;

import junit.framework.TestCase;

import org.freehep.record.loop.RecordLoop.Command;
import org.hps.evio.LCSimTestRunEventBuilder;
import org.hps.monitoring.enums.DataSourceType;
import org.hps.monitoring.record.evio.EvioEventProcessor;
import org.lcsim.event.EventHeader;
import org.lcsim.util.Driver;


public class EventProcessingErrorTest extends TestCase {
       
    static String evioFilePath = "/nfs/slac/g/hps3/data/testcase/hps_000975.evio.0";
    
    public void testEventProcessingError() {
                
        EventProcessingConfiguration config = new EventProcessingConfiguration();
        config.setFilePath(evioFilePath);
        config.setDataSourceType(DataSourceType.EVIO_FILE);
        config.setLCSimEventBuild(new LCSimTestRunEventBuilder());
        config.setDetectorName("HPS-TestRun-v8-5");
        config.add(new DummyErrorDriver());
        config.add(new DummyEvioDriver());
        
        EventProcessingChain processing = new EventProcessingChainOverride(config);
        processing.loop();
    }
    
    public void testEventProcessingNoError() {
        
        EventProcessingConfiguration config = new EventProcessingConfiguration();
        config.setFilePath(evioFilePath);
        config.setDataSourceType(DataSourceType.EVIO_FILE);
        config.setLCSimEventBuild(new LCSimTestRunEventBuilder());
        config.setDetectorName("HPS-TestRun-v8-5");
        config.add(new DummyDriver());
        config.add(new DummyEvioDriver());
        
        EventProcessingChain processing = new EventProcessingChainOverride(config);
        processing.loop();
    }

    static class DummyDriver extends Driver {
        public void process(EventHeader event) {
            System.out.println("DummyDriver.process");
        }
        
        public void endOfData() {
            System.out.println("DummyErrorDriver.endOfData");
        }
    }
    
    static class DummyErrorDriver extends Driver {
        
        public void process(EventHeader event) {
            throw new RuntimeException("Dummy error.");
        }
        
        public void endOfData() {
            System.out.println("DummyErrorDriver.endOfData");
        }
    }
    
    static class DummyEvioDriver extends EvioEventProcessor {
        public void endJob() {
            System.out.println("DummyEvioDriver.endJob");
        }        
    }
    
    static class EventProcessingChainOverride extends EventProcessingChain {

        EventProcessingChainOverride(EventProcessingConfiguration config) {
            super(config);
        }
        
        /**
         * Loop over events until processing ends for some reason.
         */
        public void loop() {
            while (!done) {
                if (!paused) {
                    try {
                        compositeLoop.execute(Command.GO, true);
                    } catch (Exception exception) {
                        System.out.println("error occurred - " + exception.getMessage());
                        setLastError(exception);
                    } 
                    if (lastError != null) {                        
                        if (!done) {
                            System.out.println(this.getClass().getSimpleName() + " - executing STOP ...");
                            System.out.println("CompositeRecordLoop.getState - " + compositeLoop.getState().toString());
                            compositeLoop.execute(Command.STOP);
                            System.out.println(this.getClass().getSimpleName() + " - done executing STOP");
                            done = true;
                        }
                    } 
                }
            }
        }
        
        
    }
}
