package org.hps.users.spaul.dimuon;

import java.util.List;

import org.hps.record.triggerbank.AbstractIntData;
import org.hps.record.triggerbank.TIData;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.GenericObject;
import org.lcsim.event.Track;
import org.lcsim.util.Driver;

public class DimuonFilterDriver extends Driver{
      public void process(EventHeader event) {

            

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
            
            

            if (!event.hasCollection(Track.class, "GBLTracks"))
                throw new Driver.NextEventException();
            
            List<Track> tracks = event.get(Track.class,"GBLTracks");
            
            if(tracks.size()<2)
                throw new Driver.NextEventException();
            
            boolean foundPlus = false;
            for(Track t : tracks){
                if(t.getCharge()==1)
                    foundPlus = true;
            }
            
            
            
            
          }


}
