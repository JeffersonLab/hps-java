package org.hps.record.scalars;

import org.hps.record.evio.EvioEventProcessor;
import org.jlab.coda.jevio.BaseStructure;
import org.jlab.coda.jevio.EvioEvent;

/**
 * This is an EVIO event processor for creating a {@link ScalarData} object from scalar bank data.
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class ScalarsEvioProcessor extends EvioEventProcessor {

    // Tag of the crate bank, which is a child of the top bank.
    static final int SCALARS_CRATE_TAG = 39;

    // Tag of the scalars integer bank, which is a child of the crate bank.
    static final int SCALARS_BANK_TAG = 57621;

    // Currently cached ScalarData object which was created by the process method.
    ScalarData data;

    /**
     * This method will create a <code>ScalarData</code> object and cache it.
     * The current object is first reset to null every time this is called.
     * 
     * @param evio The EVIO event data.
     */
    public void process(EvioEvent evio) {
        data = null;
        for (BaseStructure bank : evio.getChildrenList()) {
            // Does the crate tag match?
            if (bank.getHeader().getTag() == SCALARS_CRATE_TAG) {
                if (bank.getChildrenList() != null) {
                    for (BaseStructure subBank : bank.getChildrenList()) {
                        // Does the bank tag match?
                        if (subBank.getHeader().getTag() == SCALARS_BANK_TAG) {
                            // Scalar data exists in event so create object and stop processing.
                            data = new ScalarData(subBank.getIntData());
                            break;
                        }
                    }
                }
            }
        }
    }

    /**
     * Get the current scalar data or null if there was none in the last event processed.
     * @return The current scalar data or null if none exists.
     */
    public ScalarData getScalarData() {
        return data;
    }
}
