package org.hps.record.composite;

import java.io.IOException;
import java.nio.BufferUnderflowException;

import org.freehep.record.loop.RecordEvent;
import org.freehep.record.source.AbstractRecordSource;
import org.freehep.record.source.NoSuchRecordException;
import org.hps.record.EndRunException;
import org.hps.record.RecordProcessingException;
import org.hps.record.evio.EvioEventConstants;
import org.hps.record.evio.EvioEventUtilities;
import org.jlab.coda.et.EtEvent;
import org.jlab.coda.jevio.BaseStructure;
import org.jlab.coda.jevio.EvioEvent;
import org.jlab.coda.jevio.EvioException;
import org.jlab.coda.jevio.EvioReader;

/**
 * An adapter for directly using the CompositeLoop to supply and process EvioEvents.
 */
public class EvioEventAdapter extends RecordProcessorAdapter<EvioEvent> {

    /**
     * The record source.
     */
    private AbstractRecordSource source;

    /**
     * Flag to stop processing when an END event is received.
     */
    private boolean stopOnEndRun = true;

    /**
     * No argument constructor, for when ET events will be converted to EVIO.
     */
    public EvioEventAdapter() {
    }

    /**
     * Constructor that takes a record source.
     *
     * @param source the record source
     */
    public EvioEventAdapter(final AbstractRecordSource source) {
        this.source = source;
    }

    /**
     * Create an <code>EvioEvent</code> from an <code>EtEvent</code> byte buffer.
     *
     * @param etEvent the input <code>EtEvent</code>
     * @return the <code>EvioEvent</code> created from the <code>EtEvent</code>
     * @throws IOException if there is an IO problem from EVIO
     * @throws EvioException if there is any EVIO related error when creating the event
     * @throws BufferUnderflowException if there isn't enough data in the byte buffer
     */
    private EvioEvent createEvioEvent(final EtEvent etEvent) throws IOException, EvioException,
            BufferUnderflowException {
        return new EvioReader(etEvent.getDataBuffer()).parseNextEvent();
    }

    /**
     * Process one record which will create an <code>EvioEvent</code> or get it from the source and set a reference to
     * it on the {@link CompositeRecord}.
     */
    @Override
    public void recordSupplied(final RecordEvent record) {
        final CompositeRecord compositeRecord = (CompositeRecord) record.getRecord();
        try {
            EvioEvent evioEvent;
            // Using ET system?
            if (compositeRecord.getEtEvent() != null) {
                try {
                    // Create EVIO from ET byte buffer.
                    evioEvent = createEvioEvent(compositeRecord.getEtEvent());
                } catch (BufferUnderflowException | EvioException e) {
                    // There was a problem creating EVIO from ET.
                    throw new RecordProcessingException("Failed to create EvioEvent from EtEvent.", e);
                }
            } else {
                // Load the next record from the EVIO record source.
                if (this.source.hasNext()) {
                    this.source.next();
                    evioEvent = (EvioEvent) this.source.getCurrentRecord();
                } else {
                    throw new NoSuchRecordException("EVIO event source has no more records.");
                }
            }
            // Failed to create an EvioEvent?
            if (evioEvent == null) {
                // Throw an error because EvioEvent was not created.
                throw new NoSuchRecordException("Failed to get next EVIO record.");
            }

            // Set event number on the EvioEvent.
            setEventNumber(evioEvent);

            // Is pre start event?
            if (EvioEventUtilities.isPreStartEvent(evioEvent)) {
                // Activate start of run hook on processors.
                startRun(evioEvent);
                // Is end run event?
            } else if (EvioEventUtilities.isEndEvent(evioEvent)) {
                // Activate end of run hook on processors.
                endRun(evioEvent);

                // Stop on end run enabled?
                if (this.stopOnEndRun) {
                    // Throw exception to stop processing from end run.
                    throw new EndRunException("EVIO end event received.", evioEvent.getIntData()[1]);
                }
                // Is physics event?
            } else if (EvioEventUtilities.isPhysicsEvent(evioEvent)) {
                // Process a single physics EvioEvent.
                process(evioEvent);
            }

            // Set EvioEvent on CompositeRecord.
            compositeRecord.setEvioEvent(evioEvent);
        } catch (IOException | NoSuchRecordException e) {
            throw new RecordProcessingException("No next EVIO record available from source.", e);
        }
    }

    /**
     * Set the EVIO event number manually from the event ID bank.
     *
     * @param evioEvent the <code>EvioEvent</code> on which to set the event number
     */
    private void setEventNumber(final EvioEvent evioEvent) {
        int eventNumber = -1;
        if (evioEvent.getChildrenList() != null) {
            for (final BaseStructure bank : evioEvent.getChildrenList()) {
                if (bank.getHeader().getTag() == EvioEventConstants.EVENTID_BANK_TAG) {
                    eventNumber = bank.getIntData()[0];
                    break;
                }
            }
        }
        if (eventNumber != -1) {
            evioEvent.setEventNumber(eventNumber);
        }
    }

    /**
     * Returns <code>true</code> if processing should be stopped when end of run occurs, e.g. from an EVIO END record
     * being received.
     *
     * @param stopOnEndRun <code>true</code> to stop after EVIO END records are received
     */
    public final void setStopOnEndRun(final boolean stopOnEndRun) {
        this.stopOnEndRun = stopOnEndRun;
    }
}
