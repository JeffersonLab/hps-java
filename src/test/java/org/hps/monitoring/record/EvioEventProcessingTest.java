package org.hps.monitoring.record;

import java.io.File;

import org.hps.evio.LCSimTestRunEventBuilder;
import org.hps.monitoring.record.evio.EvioEventProcessor;
import org.hps.monitoring.record.evio.EvioFileSource;
import org.jlab.coda.jevio.EvioEvent;
import org.lcsim.event.EventHeader;
import org.lcsim.event.EventHeader.LCMetaData;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;


public class EvioEventProcessingTest {
    
    static String evioFilePath = "/nfs/slac/g/hps3/data/testrun/runs/evio/hps_001351.evio.0";   
    static String detectorName = "HPS-TestRun-v8-5";
         
    public void testEvioFile() {
        EventProcessingChain processing = new EventProcessingChain();
        processing.setRecordSource(new EvioFileSource(new File(evioFilePath)));
        processing.setEventBuilder(new LCSimTestRunEventBuilder());
        processing.setDetectorName(detectorName);
        processing.add(new DummyEvioProcessor());
        processing.add(new DummyDriver());
        processing.setStopOnEndRun();
        processing.configure();
        processing.loop();
        processing.finish();
    }
        
    static class DummyDriver extends Driver {
        
        public void detectorChanged(Detector detector) {
            System.out.println(this.getClass().getSimpleName() + ".detectorChanged - " + detector.getDetectorName());
        }
        
        public void startOfData() {
            System.out.println(this.getClass().getSimpleName() + ".startOfData");
        }
        
        public void process(EventHeader event) {
            System.out.println(this.getClass().getSimpleName() + " got LCIO event #" + event.getEventNumber());
            for (LCMetaData metaData : event.getMetaData()) {
                String collectionName = metaData.getName();
                Class type = metaData.getType();
                System.out.println (collectionName + " " + event.get(type, collectionName).size());
            }
        }
        
        public void endOfData() {
            System.out.println(this.getClass().getSimpleName() + ".endOfData");
        }
    }
    
    static class DummyEvioProcessor extends EvioEventProcessor {
        
        public void startRun(EvioEvent event) {
            System.out.println(this.getClass().getSimpleName() + ".startRun");
        }
        
        public void endRun(EvioEvent event) {
            System.out.println(this.getClass().getSimpleName() + ".endRun");
        }
        
        public void startJob() {
            System.out.println(this.getClass().getSimpleName() + ".startJob");
        }
        
        public void endJob() {
            System.out.println(this.getClass().getSimpleName() + ".endJob");
        }
        
        public void processEvent(EvioEvent event) {
            System.out.println(this.getClass().getSimpleName() + " got EVIO event #" + event.getEventNumber());
        }
    }
}
