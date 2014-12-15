package org.hps.recon.ecal.cluster;

import java.util.List;

import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.Cluster;
import org.lcsim.geometry.subdetector.HPSEcal3;
import org.lcsim.geometry.subdetector.HPSEcal3.NeighborMap;

public abstract class AbstractClusterer implements Clusterer {
    
    HPSEcal3 ecal;
    NeighborMap neighborMap;
    
    public void setEcalSubdetector(HPSEcal3 ecal) {
        this.ecal = ecal;
        this.neighborMap = ecal.getNeighborMap();
    }
    
    public abstract List<Cluster> createClusters(List<CalorimeterHit> hits);    
}
