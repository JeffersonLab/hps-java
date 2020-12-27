package org.hps.online.recon.eventbus;

import org.hps.conditions.database.DatabaseConditionsManager;
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
 */
public class ConditionsListener {

    OnlineEventBus eventbus;

    ConditionsListener(OnlineEventBus eventbus) {
        this.eventbus = eventbus;
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
            DatabaseConditionsManager mgr = DatabaseConditionsManager.getInstance();
            if (mgr.getRun() != run || !mgr.isInitialized()) {
                try {
                    Property<String> detector =
                            eventbus.getStation().getProperties().get("lcsim.detector");
                    if (detector.valid()) {
                        ConditionsManager.defaultInstance().setDetector(detector.value(), run);
                    } else {
                        eventbus.getLogger().warning("Could not initialize conditions from EVIO because detector "
                                + "name is not valid.");
                    }
                } catch (final ConditionsNotFoundException e) {
                    eventbus.error(e, true);
                }
            }
        }
    }
}
