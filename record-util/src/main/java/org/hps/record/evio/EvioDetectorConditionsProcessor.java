package org.hps.record.evio;

import org.jlab.coda.jevio.BaseStructure;
import org.jlab.coda.jevio.EvioEvent;
import org.lcsim.conditions.ConditionsManager;
import org.lcsim.conditions.ConditionsManager.ConditionsNotFoundException;

/**
 * <p>
 * This is an {@link EvioEventProcessor} for initializing the conditions system from EVIO events. The
 * {@link #startRun(EvioEvent)} method will setup conditions from PRESTART events. The {@link #process(EvioEvent)}
 * method will setup conditions from a head bank, if it is present in the event.
 */
public class EvioDetectorConditionsProcessor extends EvioEventProcessor {

    /**
     * The name of the detector model.
     */
    private final String detectorName;

    /**
     * Class constructor.
     *
     * @param detectorName the name of the detector model
     */
    public EvioDetectorConditionsProcessor(final String detectorName) {
        if (detectorName == null) {
            throw new IllegalArgumentException("The detectorName argument is null.");
        }
        this.detectorName = detectorName;
    }

    /**
     * Process an <code>EvioEvent</code> and activate the conditions system if applicable.
     *
     * @param evioEvent the <code>EvioEvent</code> to process
     */
    @Override
    public void process(final EvioEvent evioEvent) throws Exception {
        
        // Get the head head bank from event.
        final BaseStructure headBank = EvioEventUtilities.getHeadBank(evioEvent);

        // Initialize from head bank.
        if (headBank != null) {            
            initializeConditions(headBank.getIntData()[1]);
        }
        
        // Initialize from PRESTART.
        if (EventTagConstant.PRESTART.matches(evioEvent)) {
            int runNumber = EvioEventUtilities.getControlEventData(evioEvent)[1];
            initializeConditions(runNumber);
        }
    }

    private void initializeConditions(final int runNumber) {
        // Initialize the conditions system from the detector name and run number.
        try {
            ConditionsManager.defaultInstance().setDetector(this.detectorName, runNumber);
        } catch (final ConditionsNotFoundException e) {
            throw new RuntimeException("Error setting up conditions from EVIO head bank.", e);
        }
    }

    /**
     * Start of run action.
     * <p>
     * This will only activate if the evioEvent is a PRESTART event.
     *
     * @param evioEvent the <code>EvioEvent</code> to process
     */
    @Override
    // FIXME: not activated by EvioLoop
    public void startRun(final EvioEvent evioEvent) {
        // System.out.println("EvioDetectorConditionsProcessor.startRun");
        if (EvioEventUtilities.isPreStartEvent(evioEvent)) {
            // Get the pre start event's data bank.
            final int[] data = EvioEventUtilities.getControlEventData(evioEvent);

            // Get the run number from the bank.
            final int runNumber = data[1];

            // Initialize the conditions system from the detector name and run number.
            try {
                // System.out.println("  setting up conditions from pre start: " + detectorName + " #" + runNumber);
                ConditionsManager.defaultInstance().setDetector(this.detectorName, runNumber);
            } catch (final ConditionsNotFoundException e) {
                throw new RuntimeException("Error setting up conditions from EVIO pre start event.", e);
            }
        }
    }
}