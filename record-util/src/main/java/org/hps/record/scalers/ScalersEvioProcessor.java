package org.hps.record.scalers;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hps.record.evio.EvioEventConstants;
import org.hps.record.evio.EvioEventProcessor;
import org.hps.record.evio.EvioEventUtilities;
import org.jlab.coda.jevio.BaseStructure;
import org.jlab.coda.jevio.EvioEvent;
import org.lcsim.util.log.DefaultLogFormatter;
import org.lcsim.util.log.LogUtil;

/**
 * This is an EVIO event processor for creating a {@link ScalerData} object from scaler bank data.
 *
 * @author <a href="mailto:jeremym@slac.stanford.edu">Jeremy McCormick</a>
 */
public class ScalersEvioProcessor extends EvioEventProcessor {

    private static final Logger LOGGER = LogUtil.create(ScalersEvioProcessor.class, new DefaultLogFormatter(),
            Level.ALL);

    /**
     * Currently cached ScalerData object which was created by the process method.
     */
    private ScalerData currentScalerData;
    private boolean resetEveryEvent = true;

    private Set<ScalerData> scalerDataSet = new LinkedHashSet<ScalerData>();

    public ScalerData getCurrentScalerData() {
        return this.currentScalerData;
    }

    /**
     * Get the current scaler data or null if there was none in the last event processed.
     *
     * @return the current scaler data or <code>null</code> if none exists
     */
    public List<ScalerData> getScalerData() {
        return new ArrayList<ScalerData>(this.scalerDataSet);
    }

    private ScalerData getScalerData(final EvioEvent evioEvent) {
        ScalerData scalerData = null;
        // Proceed if sync bit checking is not enabled or sync bit is on.
        outerBankLoop: for (final BaseStructure bank : evioEvent.getChildrenList()) {
            // Does the crate tag match?
            if (bank.getHeader().getTag() == EvioEventConstants.SCALERS_CRATE_TAG) {
                if (bank.getChildrenList() != null) {
                    for (final BaseStructure subBank : bank.getChildrenList()) {
                        // Does the bank tag match?
                        if (subBank.getHeader().getTag() == EvioEventConstants.SCALERS_BANK_TAG) {

                            LOGGER.fine("found scaler data in bank " + subBank.getHeader().getTag()
                                    + " and EVIO event " + evioEvent.getEventNumber());

                            // Scaler data exists in event so create object and stop processing.
                            scalerData = new ScalerData(subBank.getIntData(),
                                    EvioEventUtilities.getEventIdData(evioEvent)[0]);

                            break outerBankLoop;
                        }
                    }
                }
            }
        }
        return scalerData;
    }

    /**
     * This method will create a <code>ScalerData</code> object and cache it. The current object is first reset to
     * <code>null</code> every time this method is called.
     *
     * @param evioEvent the EVIO event data
     */
    @Override
    public void process(final EvioEvent evioEvent) {
        if (resetEveryEvent) {
            // Reset the cached data object.
            this.currentScalerData = null;
        }

        final ScalerData scalerData = this.getScalerData(evioEvent);
        if (scalerData != null) {
            this.currentScalerData = scalerData;
            this.scalerDataSet.add(this.currentScalerData);
        }
    }

    public void setResetEveryEvent(final boolean resetEveryEvent) {
        this.resetEveryEvent = resetEveryEvent;
    }

    @Override
    public void startJob() {
        this.scalerDataSet = new LinkedHashSet<ScalerData>();
    }
}
