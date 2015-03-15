package org.hps.monitoring.application.util;

import java.io.IOException;

import org.hps.record.evio.EvioDetectorConditionsProcessor;
import org.hps.record.evio.EvioEventConstants;
import org.jlab.coda.et.EtEvent;
import org.jlab.coda.et.EtSystem;
import org.jlab.coda.jevio.EvioEvent;
import org.jlab.coda.jevio.EvioException;
import org.jlab.coda.jevio.EvioReader;

/**
 * This is an ET station that will initialize the conditions system
 * from EVIO PRESTART events.
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class PreStartEtStation extends RunnableEtStation {

    EvioDetectorConditionsProcessor processor;

    public PreStartEtStation(String detectorName, EtSystem system, String name, int order) {

        if (detectorName == null) {
            throw new IllegalArgumentException("detectorName is null");
        }

        config = new RunnableEtStationConfiguration();
        config.eventType = EvioEventConstants.GO_EVENT_TAG;
        config.order = order;
        config.name = name;
        config.prescale = 1;
        config.readEvents = 1;
        config.system = system;
        config.validate();

        processor = new EvioDetectorConditionsProcessor(detectorName);
    }

    public void processEvent(EtEvent event) {
        EvioEvent evioEvent = null;
        try {
            evioEvent = new EvioReader(event.getDataBuffer()).parseNextEvent();
            processor.startRun(evioEvent);
        } catch (IOException | EvioException e) {
            throw new RuntimeException(e);
        }
    }
}
