package org.hps.record.epics;

import org.hps.record.evio.EvioEventConstants;
import org.hps.record.evio.EvioEventProcessor;
import org.jlab.coda.jevio.BaseStructure;
import org.jlab.coda.jevio.EvioEvent;

/**
 * This is an EVIO event processor that will read EPICS events (event tag 31) and turn them into {@link EpicsScalarData}
 * objects.
 *
 * @author <a href="mailto:jeremym@slac.stanford.edu">Jeremy McCormick</a>
 */
public final class EpicsEvioProcessor extends EvioEventProcessor {

    /**
     * The current EPICS data object.
     */
    private EpicsScalarData data;

    /**
     * Get the current {@link EpicsScalarData} object created from record processing.
     *
     * @return the {@link EpicsScalarData} object created from record processing
     */
    public EpicsScalarData getEpicsScalarData() {
        return this.data;
    }

    /**
     * Process EVIO data and create a {@link EpicsScalarData} if EPICS data bank exists in the event.
     *
     * @param evio the <code>EvioEvent</code> that possibly has EPICS data
     */
    @Override
    public void process(final EvioEvent evio) {

        if (evio.getHeader().getTag() != EvioEventConstants.EPICS_EVENT_TAG) {
            // Just silently skip these events because otherwise too many error messages might print.
            return;
        }

        // Find the bank with the EPICS information.
        BaseStructure epicsBank = null;
        final BaseStructure topBank = evio.getChildrenList().get(0);
        for (final BaseStructure childBank : topBank.getChildrenList()) {
            if (childBank.getHeader().getTag() == EvioEventConstants.EPICS_BANK_TAG) {
                epicsBank = childBank;
                break;
            }
        }

        if (epicsBank != null) {
            final String epicsData = epicsBank.getStringData()[0];
            this.data = new EpicsScalarData();
            this.data.fromString(epicsData);
        }
    }
}