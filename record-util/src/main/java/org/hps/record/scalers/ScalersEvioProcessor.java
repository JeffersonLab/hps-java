package org.hps.record.scalers;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.hps.record.evio.EvioEventConstants;
import org.hps.record.evio.EvioEventProcessor;
import org.jlab.coda.jevio.BaseStructure;
import org.jlab.coda.jevio.EvioEvent;
import org.lcsim.util.log.DefaultLogFormatter;
import org.lcsim.util.log.LogUtil;

/**
 * This is an EVIO event processor for creating a {@link ScalerData} object from scaler bank data.
 *
 * @author <a href="mailto:jeremym@slac.stanford.edu">Jeremy McCormick</a>
 */
public final class ScalersEvioProcessor extends EvioEventProcessor {

    private static final Logger LOGGER = LogUtil.create(ScalersEvioProcessor.class, new DefaultLogFormatter(), Level.INFO);

    /**
     * Currently cached ScalerData object which was created by the process method.
     */
    private ScalerData data;

    boolean resetEveryEvent = true;

    /**
     * Get the current scaler data or null if there was none in the last event processed.
     *
     * @return the current scaler data or <code>null</code> if none exists
     */
    public ScalerData getScalerData() {
        return this.data;
    }

    /**
     * This method will create a <code>ScalerData</code> object and cache it. The current object is first reset to <code>null</code> every time this
     * method is called.
     *
     * @param evio the EVIO event data
     */
    @Override
    public void process(final EvioEvent evio) {
        if (resetEveryEvent) {
            this.data = null;
        }
        for (final BaseStructure bank : evio.getChildrenList()) {
            // Does the crate tag match?
            if (bank.getHeader().getTag() == EvioEventConstants.SCALERS_CRATE_TAG) {
                if (bank.getChildrenList() != null) {
                    for (final BaseStructure subBank : bank.getChildrenList()) {
                        // Does the bank tag match?
                        if (subBank.getHeader().getTag() == EvioEventConstants.SCALERS_BANK_TAG) {

                            LOGGER.fine("found scaler data in bank " + subBank.getHeader().getTag() + " and EVIO event " + evio.getEventNumber());

                            // Scaler data exists in event so create object and stop processing.
                            this.data = new ScalerData(subBank.getIntData());
                            break;
                        }
                    }
                }
            }
        }
    }

    public void setResetEveryEvent(final boolean resetEveryEvent) {
        this.resetEveryEvent = resetEveryEvent;
    }
}
