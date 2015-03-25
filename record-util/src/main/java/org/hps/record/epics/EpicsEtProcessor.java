package org.hps.record.epics;

import java.io.IOException;

import org.hps.record.et.EtEventProcessor;
import org.jlab.coda.et.EtEvent;
import org.jlab.coda.jevio.EvioEvent;
import org.jlab.coda.jevio.EvioException;
import org.jlab.coda.jevio.EvioReader;

/**
 * An ET event processor that builds EPICS events.
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class EpicsEtProcessor extends EtEventProcessor {
    
    EpicsEvioProcessor evioProcessor = new EpicsEvioProcessor();
    
    public void process(EtEvent event) {
        EvioEvent evio;
        try {
            evio = new EvioReader(event.getDataBuffer()).parseNextEvent();
        } catch (IOException | EvioException e) {
            throw new RuntimeException(e);
        }        
        evioProcessor.process(evio);
        if (evioProcessor.getEpicsScalarData() != null) {
            System.out.println("EpicsEtProcessor created EpicsScalarData ...");
            System.out.println(evioProcessor.getEpicsScalarData());
        }
    }
}                   
 