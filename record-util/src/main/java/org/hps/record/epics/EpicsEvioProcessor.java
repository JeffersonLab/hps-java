package org.hps.record.epics;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.hps.record.evio.EventTagConstant;
import org.hps.record.evio.EvioBankTag;
import org.hps.record.evio.EvioEventProcessor;
import org.jlab.coda.jevio.BaseStructure;
import org.jlab.coda.jevio.EvioEvent;

/**
 * This is an EVIO event processor that will read EPICS events (event tag 31) and turn them into {@link EpicsData}
 * objects.
 */
public class EpicsEvioProcessor extends EvioEventProcessor {

    /**
     * Setup class logger.
     */
    private static final Logger LOGGER = Logger.getLogger(EpicsEvioProcessor.class.getPackage().getName());

    /**
     * The current EPICS data object.
     */
    private EpicsData data;

    /**
     * Get the current {@link EpicsData} object created from record processing.
     *
     * @return the {@link EpicsData} object created from record processing
     */
    public EpicsData getEpicsData() {
        return this.data;
    }

    /**
     * Process EVIO data and create a {@link EpicsData} if EPICS data bank exists in the event.
     *
     * @param evioEvent the <code>EvioEvent</code> that possibly has EPICS data
     */
    @Override
    public void process(final EvioEvent evioEvent) {

        // Is this an EPICS event?
        if (EventTagConstant.EPICS.matches(evioEvent)) {

            LOGGER.fine("processing EPICS event " + evioEvent.getEventNumber());

            // Find the bank with the EPICS data string.
            final BaseStructure epicsBank = EvioBankTag.EPICS_STRING.findBank(evioEvent);

            // Was EPICS data found in the event?
            if (epicsBank != null) {

                // Create EpicsData object from bank's string data.
                this.data = new EpicsData(epicsBank.getStringData()[0]);

                // Find the header information in the event.
                final BaseStructure headerBank = EvioBankTag.EPICS_HEADER.findBank(evioEvent);

                if (headerBank != null) {
                    // Set the header object.
                    this.data.setEpicsHeader(EpicsHeader.fromEvio(headerBank.getIntData()));
                } else {
                    LOGGER.warning("No EPICS header bank found in event.");
                }

            } else {
                // This is an error because the string data bank should always be present in EPICS events.
                final RuntimeException x = new RuntimeException("No data bank found in EPICS event.");
                LOGGER.log(Level.SEVERE, x.getMessage(), x);
                throw x;
            }
        }
    }

    /**
     * Reset the current <code>EpicsData</code> object to <code>null</code>.
     */
    public void reset() {
        this.data = null;
    }
}