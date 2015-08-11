package org.hps.recon.filtering;

import org.hps.recon.ecal.triggerbank.AbstractIntData;
import org.hps.recon.ecal.triggerbank.TIData;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.GenericObject;
import org.lcsim.util.Driver;
import org.hps.recon.ecal.cluster.ClusterUtilities;
import org.hps.record.epics.EpicsData;

public class FEEFilterDriver extends Driver
{
	public void process(EventHeader event) {

/*	
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
*/
		
		// don't drop any events with EPICS data:
        // (could also do this via event tag=31)
		final EpicsData data = EpicsData.read(event);
        if (data != null) return;

		if (!event.hasCollection(Cluster.class, "EcalClusters"))
			throw new Driver.NextEventException();

		for (Cluster cc : event.get(Cluster.class,"EcalClusters"))
		{
			//if (cc.getEnergy() < 0.6 ||
			//    ClusterUtilities.findSeedHit(cc).getRawEnergy() < 0.4)
			//  cc.Delete();

			if (cc.getEnergy() > 0.6 &&
					ClusterUtilities.findSeedHit(cc).getRawEnergy() > 0.4) 
				return;

		}

		throw new Driver.NextEventException();
	}
}
