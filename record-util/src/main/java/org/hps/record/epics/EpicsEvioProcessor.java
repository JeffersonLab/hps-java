package org.hps.record.epics;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.hps.record.evio.EventTagConstant;
import org.hps.record.evio.EvioBankTag;
import org.hps.record.evio.EvioEventProcessor;
import org.jlab.coda.jevio.BaseStructure;
import org.jlab.coda.jevio.EvioEvent;
import org.lcsim.util.log.DefaultLogFormatter;
import org.lcsim.util.log.LogUtil;

/**
 * This is an EVIO event processor that will read EPICS events (event tag 31) and turn them into {@link EpicsData}
 * objects.
 *
 * @author <a href="mailto:jeremym@slac.stanford.edu">Jeremy McCormick</a>
 */
public class EpicsEvioProcessor extends EvioEventProcessor {

    /**
     * Setup class logger.
     */
    private static final Logger LOGGER = LogUtil
            .create(EpicsEvioProcessor.class, new DefaultLogFormatter(), Level.INFO);

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
     * @param evio the <code>EvioEvent</code> that possibly has EPICS data
     */
    @Override
    public void process(final EvioEvent evio) {

        // Is this an EPICS event?
        if (EventTagConstant.EPICS.isEventTag(evio)) {

            LOGGER.info("processing EPICS event " + evio.getEventNumber());

            // Find the bank with the EPICS data string.
            final BaseStructure epicsBank = EvioBankTag.EPICS_STRING.findBank(evio);

            // Was EPICS data found in the event?
            if (epicsBank != null) {

                // Create EpicsData object from bank's string data.
                this.data = new EpicsData(epicsBank.getStringData()[0]);

                // Find the header information in the event.
                final BaseStructure headerBank = EvioBankTag.EPICS_HEADER.findBank(evio);

                if (headerBank != null) {
                    // Set the header object.
                    this.data.setEpicsHeader(EpicsHeader.fromEvio(headerBank.getIntData()));
                } else {
                    LOGGER.warning("No EPICS header bank found in event.");
                }

            } else {
                // This is an error because the string data bank should always be present in EPICS events.
                final RuntimeException x = new RuntimeException("No EPICS data bank found in EPICS event.");
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