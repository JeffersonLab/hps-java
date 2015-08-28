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
 * @author Jeremy McCormick, SLAC
 */
public class ScalersEvioProcessor extends EvioEventProcessor {

    private static final Logger LOGGER = LogUtil.create(ScalersEvioProcessor.class, new DefaultLogFormatter(),
            Level.ALL);

    /**
     * Currently cached ScalerData object which was created by the process method.
     */
    private ScalerData currentScalerData;

    /**
     * Set to <code>true</code> if cached data object should be reset between every event even if scaler data is not
     * present.
     */
    private boolean resetEveryEvent = true;

    /**
     * The complete set of scaler data found in the job.
     */
    private Set<ScalerData> scalerDataSet = new LinkedHashSet<ScalerData>();

    /**
     * Get the current cached scaler data object.
     *
     * @return the current scaler data object
     */
    public ScalerData getCurrentScalerData() {
        return this.currentScalerData;
    }

    /**
     * Get the list of scaler data found in the job.
     *
     * @return the current scaler data or <code>null</code> if none exists
     */
    public List<ScalerData> getScalerData() {
        return new ArrayList<ScalerData>(this.scalerDataSet);
    }

    /**
     * Get scaler data from the EVIO event.
     *
     * @param evioEvent the EVIO event
     * @return the scaler data or <code>null</code> if none exists
     */
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

                            // Get event ID.
                            final int eventId = EvioEventUtilities.getEventIdData(evioEvent)[0];

                            // Get event's timestamp.
                            final int timestamp = EvioEventUtilities.getHeadBankData(evioEvent)[3];

                            // Create scaler data.
                            scalerData = new ScalerData(subBank.getIntData(), eventId, timestamp);

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

    /**
     * Set to <code>true</code> to reset scaler data object between every EVIO event.
     *
     * @param resetEveryEvent <code>true</code> to reset scaler data between every EVIO event
     */
    public void setResetEveryEvent(final boolean resetEveryEvent) {
        this.resetEveryEvent = resetEveryEvent;
    }

    /**
     * Start of job hook which resets collecton of cached scaler data.
     */
    @Override
    public void startJob() {
        this.scalerDataSet = new LinkedHashSet<ScalerData>();
    }
}
