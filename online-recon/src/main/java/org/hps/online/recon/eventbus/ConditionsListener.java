package org.hps.online.recon.eventbus;

import org.hps.online.recon.properties.Property;
import org.hps.record.evio.EventTagConstant;
import org.hps.record.evio.EvioEventUtilities;
import org.jlab.coda.jevio.BaseStructure;
import org.jlab.coda.jevio.EvioEvent;
import org.lcsim.conditions.ConditionsManager;
import org.lcsim.conditions.ConditionsManager.ConditionsNotFoundException;

import com.google.common.eventbus.Subscribe;

/**
 * Activate lcsim conditions system from EVIO data
 * on the event bus.
 */
public class ConditionsListener {

    private OnlineEventBus eventbus;
    private Property<String> detectorProp = null;
    private Integer currentRun = null;
    private ConditionsManager mgr;

    ConditionsListener(OnlineEventBus eventbus) {
        this.eventbus = eventbus;
        detectorProp = eventbus.getStation().getProperties().get("lcsim.detector");
        if (!detectorProp.valid()) {
            throw new IllegalArgumentException("Cannot initialize ConditionsListener with invalid detector prop");
        }
        mgr = ConditionsManager.defaultInstance();
    }

    @Subscribe
    public void checkConditions(EvioEvent evioEvent) {

        if (EventTagConstant.PRESTART.matches(evioEvent)) {
            currentRun = EvioEventUtilities.getControlEventData(evioEvent)[1];
        } else {
            final BaseStructure headBank = EvioEventUtilities.getHeadBank(evioEvent);
            if (headBank != null) {
                currentRun = headBank.getIntData()[1];
            }
        }

        if (currentRun != null && currentRun != mgr.getRun()) {
            try {
                eventbus.getLogger().info("Setting conditions from EVIO: " + detectorProp.value() + ":" + currentRun);
                ConditionsManager.defaultInstance().setDetector(detectorProp.value(), currentRun);
            } catch (ConditionsNotFoundException e) {
                // Post fatal error because conditions are not found
                eventbus.post(new EventProcessingError(e, true));
            }
        }
    }
}
