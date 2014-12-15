package org.hps.recon.ecal.cluster;

import java.util.List;

import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.Cluster;
import org.lcsim.geometry.subdetector.HPSEcal3;

public interface Clusterer {

    List<Cluster> createClusters(List<CalorimeterHit> hits);
}
