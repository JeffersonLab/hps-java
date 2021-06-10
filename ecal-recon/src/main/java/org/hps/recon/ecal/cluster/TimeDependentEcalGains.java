package org.hps.recon.ecal.cluster;

import org.lcsim.event.EventHeader;
import org.lcsim.event.base.BaseCluster;
import org.lcsim.util.Driver;

/**
 * Applies a time-dependent global correction factor to the energies of all ecal clusters.
 */
public abstract class TimeDependentEcalGains extends Driver{
    private String ecalClusterCollectionName = "EcalClustersCorr";
    public void setEcalClusterCollectionName(String name){
        this.ecalClusterCollectionName = name;
    }
    public void process(EventHeader event){
        double gain = getGain(event.getTimeStamp());
        for(BaseCluster c : event.get(BaseCluster.class, ecalClusterCollectionName)){
            double old_energy = c.getEnergy();
            double new_energy = old_energy*gain;
            c.setEnergy(new_energy);
        }
    }
    
    protected abstract double getGain(long timeStamp);
    
}
