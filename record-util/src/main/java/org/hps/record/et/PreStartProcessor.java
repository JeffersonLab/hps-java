package org.hps.record.et;

import java.io.IOException;

import org.hps.record.evio.EvioDetectorConditionsProcessor;
import org.jlab.coda.et.EtEvent;
import org.jlab.coda.jevio.EvioEvent;
import org.jlab.coda.jevio.EvioException;
import org.jlab.coda.jevio.EvioReader;

/**
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class PreStartProcessor extends EtEventProcessor {

    String detectorName;
    EvioDetectorConditionsProcessor conditionsProcessor;
    
    public PreStartProcessor(String detectorName) {
        this.detectorName = detectorName;
        this.conditionsProcessor = new EvioDetectorConditionsProcessor(detectorName);
    }
    
    public void process(EtEvent event) {
        EvioEvent evioEvent = null;
        try {
            evioEvent = new EvioReader(event.getDataBuffer()).parseNextEvent();
            conditionsProcessor.startRun(evioEvent);
        } catch (IOException | EvioException e) {
            throw new RuntimeException(e);
        }
    }
}
