package org.hps.record.composite;

import org.jlab.coda.et.EtEvent;
import org.jlab.coda.jevio.EvioEvent;
import org.lcsim.event.EventHeader;

/**
 * This class is used to group together corresponding ET, EVIO and LCIO events
 * for use by the {@link CompositeLoop}.  The loop's <code>RecordListener</code>
 * objects may alter this record by setting references to event objects 
 * such as an <code>EvioEvent</code>.
 */
public final class CompositeRecord {
    
    EtEvent etEvent;
    EvioEvent evioEvent;
    EventHeader lcioEvent;
    
    int sequenceNumber = -1;
    int eventNumber = -1;
    
    /**
     * Set the sequence number of this record.
     * @param sequenceNumber The sequence number.
     */
    public void setSequenceNumber(int sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }
    
    /**
     * Set the event number of this record e.g. from EVIO or LCIO.
     * @param eventNumber The event number of this recrod.
     */
    public void setEventNumber(int eventNumber) {
        this.eventNumber = eventNumber;
    }
    
    /**
     * Set a reference to an <code>EtEvent</code>.
     * @param etEvent The EtEvent.
     */
    public void setEtEvent(EtEvent etEvent) {
        this.etEvent = etEvent;
    }
    
    /**
     * Set a reference to an <code>EvioEvent</code>.
     * @param evioEvent The EvioEvent.
     */
    public void setEvioEvent(EvioEvent evioEvent) {
        this.evioEvent = evioEvent;        
    }
    
    /**
     * Set a reference to an org.lcsim LCIO event (EventHeader).
     * @param lcioEvent The LCIO EventHeader.
     */
    public void setLcioEvent(EventHeader lcioEvent) {
        this.lcioEvent = lcioEvent;
    }
    
    /**
     * Get the <code>EtEvent</code>.
     * @return The EtEvent.
     */
    public EtEvent getEtEvent() {
        return etEvent;
    }
    
    /**
     * Get the <code>EvioEvent</code>.
     * @return The EvioEvent.
     */
    public EvioEvent getEvioEvent() {
        return evioEvent;
    }
    
    /**
     * Get the org.lcsim event.
     * @return The org.lcsim event.
     */
    public EventHeader getLcioEvent() {
        return lcioEvent;
    }
    
    /**
     * Get the event sequence number.
     * @return The event sequence number.
     */
    public int getSequenceNumber() {
        return sequenceNumber;
    }
    
    /**
     * Get the event number.
     * @return The event number.
     */
    public int getEventNumber() {
        return eventNumber;
    }
}
