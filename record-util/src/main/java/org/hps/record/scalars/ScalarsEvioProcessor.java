package org.hps.record.scalars;

import org.hps.record.evio.EvioEventConstants;
import org.hps.record.evio.EvioEventProcessor;
import org.jlab.coda.jevio.BaseStructure;
import org.jlab.coda.jevio.EvioEvent;

/**
 * This is an EVIO event processor for creating a {@link ScalarData} object from scalar bank data.
 *
 * @author <a href="mailto:jeremym@slac.stanford.edu">Jeremy McCormick</a>
 */
public final class ScalarsEvioProcessor extends EvioEventProcessor {

    // Currently cached ScalarData object which was created by the process method.
    private ScalarData data;

    /**
     * Get the current scalar data or null if there was none in the last event processed.
     *
     * @return the current scalar data or <code>null</code> if none exists
     */
    public ScalarData getScalarData() {
        return this.data;
    }

    /**
     * This method will create a <code>ScalarData</code> object and cache it. The current object is first reset to
     * <code>null</code> every time this method is called.
     *
     * @param evio the EVIO event data
     */
    @Override
    public void process(final EvioEvent evio) {
        this.data = null;
        for (final BaseStructure bank : evio.getChildrenList()) {
            // Does the crate tag match?
            if (bank.getHeader().getTag() == EvioEventConstants.SCALARS_CRATE_TAG) {
                if (bank.getChildrenList() != null) {
                    for (final BaseStructure subBank : bank.getChildrenList()) {
                        // Does the bank tag match?
                        if (subBank.getHeader().getTag() == EvioEventConstants.SCALARS_BANK_TAG) {
                            // Scalar data exists in event so create object and stop processing.
                            this.data = new ScalarData(subBank.getIntData());
                            break;
                        }
                    }
                }
            }
        }
    }
}
