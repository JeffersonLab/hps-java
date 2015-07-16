package org.hps.recon.filtering;

import org.hps.recon.ecal.triggerbank.AbstractIntData;
import org.hps.recon.ecal.triggerbank.TIData;
import org.lcsim.event.EventHeader;
import org.lcsim.event.GenericObject;

/**
 * Accept events with the desired trigger bit.
 *
 * @author Sho Uemura <meeg@slac.stanford.edu>
 * @version $Id: $
 */
public class TriggerTypeFilter extends EventReconFilter {

    TriggerType triggerType = TriggerType.all;

    public enum TriggerType {

        all, singles0, singles1, pairs0, pairs1
    }

    public void setTriggerType(String triggerType) {
        this.triggerType = TriggerType.valueOf(triggerType);
    }

    @Override
    public void process(EventHeader event) {
        incrementEventProcessed();
        if (event.hasCollection(GenericObject.class, "TriggerBank")) {
            for (GenericObject data : event.get(GenericObject.class, "TriggerBank")) {
                if (AbstractIntData.getTag(data) == TIData.BANK_TAG) {
                    TIData triggerData = new TIData(data);
                    if (!matchTriggerType(triggerData))//only process singles0 triggers...
                    {
                        skipEvent();
                    }
                }
            }
        } else {
            skipEvent();
        }
        incrementEventPassed();
    }

    public boolean matchTriggerType(TIData triggerData) {
        switch (triggerType) {
            case all:
                return true;
            case singles0:
                return triggerData.isSingle0Trigger();
            case singles1:
                return triggerData.isSingle1Trigger();
            case pairs0:
                return triggerData.isPair0Trigger();
            case pairs1:
                return triggerData.isPair1Trigger();
            default:
                return false;
        }
    }
}
