package org.hps.monitoring.record.composite;

import org.jlab.coda.et.EtEvent;
import org.jlab.coda.jevio.EvioEvent;
import org.lcsim.event.EventHeader;

/**
 * This class is used to group together corresponding ET, EVIO and LCIO events
 * for use by the {@link CompositeRecordLoop}.
 */
public class CompositeRecord {
    
    EtEvent etEvent;
    EvioEvent evioEvent;
    EventHeader lcioEvent;
    
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
}
