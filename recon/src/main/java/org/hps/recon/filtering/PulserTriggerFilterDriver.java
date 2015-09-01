package org.hps.recon.filtering;

import org.lcsim.event.EventHeader;
import org.lcsim.event.GenericObject;
import org.lcsim.util.Driver;
import org.hps.record.epics.EpicsData;
import org.hps.record.triggerbank.AbstractIntData;
import org.hps.record.triggerbank.TIData;

public class PulserTriggerFilterDriver extends Driver
{
  public void process(EventHeader event) {

    // 1. keep all events with EPICS data (could also use event tag = 31):
    if (EpicsData.read(event) != null) return;

    // 2. drop event if it doesn't have a TriggerBank
    if (!event.hasCollection(GenericObject.class,"TriggerBank"))
      throw new Driver.NextEventException();
  
    // 3. keep event if it was from a Pulser trigger:
    for (GenericObject gob : event.get(GenericObject.class,"TriggerBank"))
    {
      if (!(AbstractIntData.getTag(gob) == TIData.BANK_TAG)) continue;
      TIData tid = new TIData(gob);
      if (tid.isPulserTrigger()) return;
    }
    
    // 4. Else, drop event:
    throw new Driver.NextEventException();
  }
}
