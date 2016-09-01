package org.hps.recon.filtering;

import org.lcsim.event.EventHeader;
import org.lcsim.event.GenericObject;
import org.lcsim.util.Driver;
import org.hps.record.epics.EpicsData;
import org.hps.record.scalers.ScalerData;
import org.hps.record.triggerbank.AbstractIntData;
import org.hps.record.triggerbank.TIData;
/**
 * Keep events with exactly one trigger bit.
 * Also keep EPICS events, and Scaler events.
 * Drop all other events.
 * 
 * @author spaul
 *
 */
public class MonoTriggerFilterDriver extends Driver
{
  public void process(EventHeader event) {

    // 1. keep all events with EPICS data (could also use event tag = 31):
    if (EpicsData.read(event) != null) return;

    // 2. keep all events with Scaler data:
    if (ScalerData.read(event) != null) return;

    // 3. drop event if it doesn't have a TriggerBank
    if (!event.hasCollection(GenericObject.class,"TriggerBank"))
      throw new Driver.NextEventException();
  
    int triggers = 0;
    // 4. keep event if it only has one trigger bit set:
    for (GenericObject gob : event.get(GenericObject.class,"TriggerBank"))
    {
      if (!(AbstractIntData.getTag(gob) == TIData.BANK_TAG)) continue;
      TIData tid = new TIData(gob);
      if (tid.isSingle0Trigger()) triggers++;
      if (tid.isSingle1Trigger()) triggers++;
      if (tid.isPair0Trigger()) triggers++;
      if (tid.isPair1Trigger()) triggers++;
      if (tid.isPulserTrigger()) triggers++;
      if (tid.isCalibTrigger()) triggers++;
    }
    if(triggers == 1)
    /*tab*/return;
    
    // 5. Else, drop event:
    throw new Driver.NextEventException();
  }
}
