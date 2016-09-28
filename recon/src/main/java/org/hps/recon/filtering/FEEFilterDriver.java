package org.hps.recon.filtering;

import org.lcsim.event.Cluster;
import org.hps.recon.ecal.cluster.ClusterUtilities;
import org.hps.record.epics.EpicsData;
import org.hps.record.triggerbank.AbstractIntData;
import org.hps.record.triggerbank.TIData;
import org.lcsim.event.EventHeader;
import org.lcsim.event.GenericObject;
import org.lcsim.geometry.Detector;

public class FEEFilterDriver extends EventReconFilter
{
    //Set min seed energy value, default to 2015 run 
    private double seedCut = 0; //= 0.4
    
    //set min cluster energy value, default to 2015 run
    private double clusterCut = 0.6;
    
    //minimum number of hits per cluster
    private int minHits = 0; // = 3;
    
    public void setMinHits(int minHits){
        this.minHits = minHits;
    }
    
    /**
     * Set the cut value for seed energy in GeV
     * @param seedCut
     */
    public void setSeedCut(double seedCut) {
        this.seedCut = seedCut;
    }
    
    /**
     * Set the cut value for cluster energy in GeV
     * @param clusterCut
     */
    public void setClusterCut(double clusterCut) {
        this.clusterCut = clusterCut;
    }
    
    
  public void process(EventHeader event) {

    // don't drop any events with EPICS data:
    // (could also do this via event tag=31)
    final EpicsData data = EpicsData.read(event);
    if (data != null) return;

    // only keep singles triggers:
    if (!event.hasCollection(GenericObject.class,"TriggerBank"))
        skipEvent();
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
    if (!isSingles) skipEvent();

    if (!event.hasCollection(Cluster.class, "EcalClusters"))
        skipEvent();
    
    for (Cluster cc : event.get(Cluster.class,"EcalClusters"))
    {
      // try to drop clusters:
      //if (cc.getEnergy() < 0.6 ||
      //    ClusterUtilities.findSeedHit(cc).getRawEnergy() < 0.4)
      //  cc.Delete();

      // keep events with a cluster over 600 MeV with seed over 400 MeV (for 2015 running).
        // keep events with cluster over 1.2 GeV and seed over 650 MeV for 2016 running.
      if (cc.getEnergy() > clusterCut && 
          ClusterUtilities.findSeedHit(cc).getCorrectedEnergy() > seedCut && cc.getCalorimeterHits().size() >= minHits)
        return;
    }

    skipEvent();
  }
  
  protected void detectorChanged(Detector detector){
      super.detectorChanged(detector);
  }
  
}
