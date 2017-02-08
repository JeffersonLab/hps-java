package org.hps.record.composite;

import org.jlab.coda.et.EtEvent;
import org.jlab.coda.jevio.EvioEvent;
import org.lcsim.event.EventHeader;

/**
 * This class is used to group together corresponding ET, EVIO and LCIO events for use by the {@link CompositeLoop}. The
 * loop's <code>RecordListener</code> objects may alter this record by setting references to event objects such as an
 * <code>EvioEvent</code>.
 */
public final class CompositeRecord {

    /**
     * The ET event.
     */
    private EtEvent etEvent;

    /**
     * The event number.
     */
    private int eventNumber = -1;

    /**
     * The EVIO event.
     */
    private EvioEvent evioEvent;

    /**
     * The LCIO/LCSim event.
     */
    private EventHeader lcioEvent;

    /**
     * The event's sequence number.
     */
    private int sequenceNumber = -1;

    /**
     * Get the <code>EtEvent</code>.
     *
     * @return the EtEvent
     */
    public EtEvent getEtEvent() {
        return this.etEvent;
    }

    /**
     * Get the event number.
     *
     * @return the event number
     */
    public int getEventNumber() {
        return this.eventNumber;
    }

    /**
     * Get the <code>EvioEvent</code>.
     *
     * @return the EvioEvent
     */
    public EvioEvent getEvioEvent() {
        return this.evioEvent;
    }

    /**
     * Get the org.lcsim event.
     *
     * @return the org.lcsim event
     */
    public EventHeader getLcioEvent() {
        return this.lcioEvent;
    }

    /**
     * Get the event sequence number.
     *
     * @return the event sequence number
     */
    public int getSequenceNumber() {
        return this.sequenceNumber;
    }

    /**
     * Set a reference to an <code>EtEvent</code>.
     *
     * @param etEvent the EtEvent
     */
    public void setEtEvent(final EtEvent etEvent) {
        this.etEvent = etEvent;
    }

    /**
     * Set the event number of this record e.g. from EVIO or LCIO.
     *
     * @param eventNumber the event number of this record
     */
    public void setEventNumber(final int eventNumber) {
        this.eventNumber = eventNumber;
    }

    /**
     * Set a reference to an <code>EvioEvent</code>.
     *
     * @param evioEvent the EvioEvent
     */
    public void setEvioEvent(final EvioEvent evioEvent) {
        this.evioEvent = evioEvent;
    }

    /**
     * Set a reference to an org.lcsim LCIO event (EventHeader).
     *
     * @param lcioEvent the LCIO EventHeader
     */
    public void setLcioEvent(final EventHeader lcioEvent) {
        this.lcioEvent = lcioEvent;
    }

    /**
     * Set the sequence number of this record.
     *
     * @param sequenceNumber the event sequence number
     */
    public void setSequenceNumber(final int sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }
}
