package org.hps.recon.filtering;

import org.lcsim.event.EventHeader;
import org.lcsim.event.GenericObject;
import org.lcsim.util.Driver;
import org.hps.record.epics.EpicsData;
import org.hps.record.scalers.ScalerData;
import org.hps.record.triggerbank.AbstractIntData;
import org.hps.record.triggerbank.TSData2019;

/**
 * Keep pulser triggered events. Also keep EPICS events, and Scaler events. Drop all other events.
 */
public class UnbiasedTriggerFilterDriver extends Driver {

    public void process(EventHeader event) {

        // 1. keep all events with EPICS data (could also use event tag = 31):
        if (EpicsData.read(event) != null)
            return;

        // 2. keep all events with Scaler data:
        if (ScalerData.read(event) != null)
            return;

        // 3. drop event if it doesn't have a TriggerBank
        if (!event.hasCollection(GenericObject.class, "TSBank"))
            throw new Driver.NextEventException();

        // 4. keep event if it was from a Pulser trigger:
        for (GenericObject gob : event.get(GenericObject.class, "TSBank")) {
            if (!(AbstractIntData.getTag(gob) == TSData2019.BANK_TAG))
                continue;
            TSData2019 tsd = new TSData2019(gob);
            if (tsd.isPulserTrigger() || tsd.isFaradayCupTrigger())
                return;
        }

        // 5. Else, drop event:
        throw new Driver.NextEventException();
    }
}
