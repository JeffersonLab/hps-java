package org.hps.record.composite;

import java.util.logging.Logger;

import org.freehep.record.loop.RecordEvent;
import org.freehep.record.loop.RecordListener;
import org.jlab.coda.et.EtEvent;
import org.jlab.coda.jevio.EvioEvent;
import org.lcsim.event.EventHeader;

/**
 * Record listener for printing event numbers from composite records.
 * 
 * Class can be configured to print information from composite record,
 * LCIO, EVIO and ET.
 * 
 * @author jeremym
 */
public class CompositeEventPrintLoopAdapter implements RecordListener {

    /**
     * Class logger.
     */
    private static Logger LOGGER = Logger.getLogger(CompositeEventPrintLoopAdapter.class.getName());

    /**
     * Sequence number of events processed.
     * 
     * This is set event-by-event from the value of the sequence number in the composite loop.
     */
    private long eventSequence = 0;
    
    /**
     * Event print interval which means every Nth event will be printed.
     * 
     * By default, every event will be printed.
     */
    private long printInterval = 1;

    /**
     * Whether to print LCIO events.
     */
    private boolean printLcio = true;
    
    /**
     * Whether to print EVIO events.
     */
    private boolean printEvio = false;
    
    /**
     * Whether to print ET events.
     */
    private boolean printEt = false;
    
    /**
     * Class constructor.
     */
    public CompositeEventPrintLoopAdapter() {        
    }
    
    /**
     * Class constructor.
     * @param printInterval the event print interval
     */
    public CompositeEventPrintLoopAdapter(long printInterval) {
        this.printInterval = printInterval;
    }

    /**
     * Set the event printing interval.
     * @param printInterval The event printing interval
     */
    public void setPrintInterval(long printInterval) {
        this.printInterval = printInterval;
    }
    
    /**
     * Set whether to print LCIO event information
     * @param printLcio True to print LCIO event information
     */
    public void setPrintLcio(boolean printLcio) {
        this.printLcio = printLcio;
    }
    
    /**
     * Set whether to print EVIO event information
     * @param printLcio True to print EVIO event information
     */
    public void setPrintEvio(boolean printEvio) {
        this.printEvio = printEvio;
    }
    
    /**
     * Set whether to print ET event information
     * @param printLcio True to print ET event information
     */
    public void setPrintEt(boolean printEt) {
        this.printEt = printEt;
    }

    /**
     * Process an event and print the event information.
     */
    @Override
    public void recordSupplied(RecordEvent recordEvent) {
        Object record = recordEvent.getRecord();
        if (record instanceof CompositeRecord) {
            CompositeRecord compRec = (CompositeRecord) record;
            eventSequence = compRec.getSequenceNumber();
            if (eventSequence % printInterval == 0) {
                LOGGER.info("Event: " + compRec.getSequenceNumber());
                if (printLcio && compRec.getLcioEvent() != null) {
                    EventHeader lcioEvent = compRec.getLcioEvent();
                    LOGGER.info("LCIO event: " + lcioEvent.getEventNumber()
                            + "; time: " + lcioEvent.getTimeStamp());
                }
                if (printEvio && compRec.getEvioEvent() != null) {
                    EvioEvent evioEvent = compRec.getEvioEvent();
                    LOGGER.info("EVIO event: " + evioEvent.getEventNumber());
                    // TODO: Add information from ID bank
                } 
                if (printEt && compRec.getEtEvent() != null) {
                    EtEvent etEvent = compRec.getEtEvent();
                    LOGGER.info("ET event: " + etEvent.getId() 
                            + "; length: " + etEvent.getLength());
                }
            }
        }        
    }
}
