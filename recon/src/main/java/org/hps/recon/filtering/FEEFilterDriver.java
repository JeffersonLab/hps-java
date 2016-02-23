package org.hps.recon.filtering;

import org.hps.record.triggerbank.AbstractIntData;
import org.hps.record.triggerbank.TIData;
import org.lcsim.event.GenericObject;

import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.util.Driver;
import org.hps.recon.ecal.cluster.ClusterUtilities;
import org.hps.record.epics.EpicsData;
public class FEEFilterDriver extends Driver
{
  public void process(EventHeader event) {

    // don't drop any events with EPICS data:
    // (could also do this via event tag=31)
    final EpicsData data = EpicsData.read(event);
    if (data != null) return;

    // only keep singles triggers:
    if (!event.hasCollection(GenericObject.class,"TriggerBank"))
      throw new Driver.NextEventException();
    boolean isSingles=false;
    for (GenericObject gob : event.get(GenericObject.class,"TriggerBank"))
    {
      if (!(AbstractIntData.getTag(gob) == TIData.BANK_TAG)) continue;
      TIData tid = new TIData(gob);
      if (tid.isSingle0Trigger()  || tid.isSingle1Trigger())
      {
        isSingles=true;
        break;
      }
    }
    if (!isSingles) throw new Driver.NextEventException();

    if (!event.hasCollection(Cluster.class, "EcalClusters"))
      throw new Driver.NextEventException();
    
    for (Cluster cc : event.get(Cluster.class,"EcalClusters"))
    {
      // try to drop clusters:
      //if (cc.getEnergy() < 0.6 ||
      //    ClusterUtilities.findSeedHit(cc).getRawEnergy() < 0.4)
      //  cc.Delete();

      // keep events with a cluster over 600 MeV with seed over 400 MeV (for 2015 running).
    	// keep events with cluster over 1.2 GeV and seed over 650 MeV for 2016 running.
      if (cc.getEnergy() > 1.2 && 
          ClusterUtilities.findSeedHit(cc).getCorrectedEnergy() > 0.65)
        return;
    }

    throw new Driver.NextEventException();
  }
}
