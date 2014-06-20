package org.hps.monitoring.record;

import java.io.File;
import java.io.IOException;

import org.hps.evio.LCSimTestRunEventBuilder;
import org.hps.monitoring.record.etevent.EtEventSource;
import org.hps.monitoring.record.evio.EvioEventProcessor;
import org.hps.monitoring.record.evio.EvioFileSource;
import org.jlab.coda.jevio.EvioEvent;
import org.lcsim.event.EventHeader;
import org.lcsim.event.EventHeader.LCMetaData;
import org.lcsim.util.Driver;
import org.lcsim.util.loop.LCIOEventSource;

public class EventProcessingChainTest {
    
    //static String evioFilePath = "/work/data/hps/hps_001351.evio.0";
    static String evioFilePath = "/nfs/slac/g/hps3/data/testrun/runs/evio/hps_001351.evio.0";
    //static String lcioFilePath = "/work/data/hps/hps_001351.evio.0_recon.slcio";
    static String lcioFilePath = "/nfs/slac/g/hps3/data/testrun/runs/recon_new/hps_001351.evio.0_recon.slcio";
    static String detectorName = "HPS-TestRun-v8-5";
    
    // ET ring with streaming EVIO file must be running for this to work.
    /*
    public void testEtSource() {
        EventProcessingChain processing = new EventProcessingChain();
        processing.setRecordSource(new EtEventSource());
        processing.setEventBuilder(new LCSimTestRunEventBuilder());
        processing.setDetectorName(detectorName);
        processing.add(new DummyEvioProcessor());
        processing.add(new DummyDriver());
        processing.setStopOnEndRun();
        processing.configure();
        processing.loop();
    }
    */
    
    public void testEvioFile() {
        EventProcessingChain processing = new EventProcessingChain();
        processing.setRecordSource(new EvioFileSource(new File(evioFilePath)));
        processing.setEventBuilder(new LCSimTestRunEventBuilder());
        processing.setDetectorName(detectorName);
        processing.add(new DummyEvioProcessor());
        processing.add(new DummyDriver());
        processing.setup();
        processing.loop();
    }
    
    /*
    public void testLcioFile() {
        EventProcessingChain processing = new EventProcessingChain();
        try {
            processing.setRecordSource(new LCIOEventSource(new File(lcioFilePath)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        processing.setEventBuilder(new LCSimTestRunEventBuilder());
        processing.setDetectorName(detectorName);
        processing.add(new DummyDriver());
        processing.configure();
        processing.loop();
    }
    */
    
    static class DummyDriver extends Driver {
        public void process(EventHeader event) {
            System.out.println(this.getClass().getSimpleName() + " got LCIO event #" + event.getEventNumber());
            for (LCMetaData metaData : event.getMetaData()) {
                String collectionName = metaData.getName();
                Class type = metaData.getType();
                System.out.println (collectionName + " " + event.get(type, collectionName).size());
            }
        }
    }
    
    static class DummyEvioProcessor extends EvioEventProcessor {
        public void processEvent(EvioEvent event) {
            System.out.println(this.getClass().getSimpleName() + " got EVIO event #" + event.getEventNumber());
        }
    }
}
