package org.hps.record.composite;

import org.jlab.coda.et.EtEvent;
import org.jlab.coda.jevio.EvioEvent;
import org.lcsim.event.EventHeader;

/**
 * This class is used to group together corresponding ET, EVIO and LCIO events
 * for use by the {@link CompositeLoop}.
 */
public final class CompositeRecord {
    
    EtEvent etEvent;
    EvioEvent evioEvent;
    EventHeader lcioEvent;
    
    int sequenceNumber = -1;
    int eventNumber = -1;
    
    public void setSequenceNumber(int sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }
    
    public void setEventNumber(int eventNumber) {
        this.eventNumber = eventNumber;
    }
    
    public void setEtEvent(EtEvent etEvent) {
        this.etEvent = etEvent;
    }
    
    public void setEvioEvent(EvioEvent evioEvent) {
        this.evioEvent = evioEvent;        
    }
    
    public void setLcioEvent(EventHeader lcioEvent) {
        this.lcioEvent = lcioEvent;
    }
    
    public EtEvent getEtEvent() {
        return etEvent;
    }
    
    public EvioEvent getEvioEvent() {
        return evioEvent;
    }
    
    public EventHeader getLcioEvent() {
        return lcioEvent;
    }
    
    public int getSequenceNumber() {
        return sequenceNumber;
    }
    
    public int getEventNumber() {
        return eventNumber;
    }
}
