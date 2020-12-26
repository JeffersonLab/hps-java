package org.hps.record.composite;

import org.freehep.record.loop.RecordEvent;
import org.freehep.record.loop.RecordListener;
import org.hps.record.evio.EvioEventUtilities;
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
// TODO: Add event processing statistics
public class CompositeEventPrintLoopAdapter implements RecordListener {

    /**
     * Class logger.
     */
    //private static Logger LOGGER = Logger.getLogger(CompositeEventPrintLoopAdapter.class.getName());

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
                System.out.println("Event: " + compRec.getSequenceNumber());
                if (printEt && compRec.getEtEvent() != null) {
                    EtEvent etEvent = compRec.getEtEvent();
                    System.out.println("ET event: " + etEvent.getId()
                            + ", length: " + etEvent.getLength());
                }
                if (printEvio && compRec.getEvioEvent() != null) {
                    EvioEvent evioEvent = compRec.getEvioEvent();
                    int id = -1;
                    int data[] = EvioEventUtilities.getEventIdData(evioEvent);
                    if (data != null) {
                        id = data[0];
                    }
                    System.out.println("EVIO event: " + evioEvent.getEventNumber() + ", id: "
                            + id + ", bytes: " + evioEvent.getTotalBytes());
                }
                if (printLcio && compRec.getLcioEvent() != null) {
                    EventHeader lcioEvent = compRec.getLcioEvent();
                    System.out.println("LCIO event: " + lcioEvent.getEventNumber()
                            + ", time: " + lcioEvent.getTimeStamp());
                }
            }
        }
    }
}
