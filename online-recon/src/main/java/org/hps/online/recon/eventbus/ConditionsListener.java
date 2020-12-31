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

    OnlineEventBus eventbus;
    Property<String> detectorProp = null;

    ConditionsListener(OnlineEventBus eventbus) {
        this.eventbus = eventbus;
        detectorProp = eventbus.getStation().getProperties().get("lcsim.detector");
        if (!detectorProp.valid()) {
            throw new IllegalArgumentException("Cannot initialize ConditionsListener with invalid detector prop");
        }
    }

    @Subscribe
    public void checkConditions(EvioEvent evioEvent) {

        Integer run = null;

        if (EventTagConstant.PRESTART.matches(evioEvent)) {
            run = EvioEventUtilities.getControlEventData(evioEvent)[1];
        } else {
            final BaseStructure headBank = EvioEventUtilities.getHeadBank(evioEvent);
            if (headBank != null) {
                run = headBank.getIntData()[1];
            }
        }

        if (run != null) {
            try {
                eventbus.getLogger().info("Setting conditions from EVIO: " + detectorProp.value() + ":" + run);
                ConditionsManager.defaultInstance().setDetector(detectorProp.value(), run);
            } catch (ConditionsNotFoundException e) {
                // Post fatal error because conditions are not found
                eventbus.post(new EventProcessingError(e, true));
            }
        }
    }
}
