package org.hps.recon.ecal.cluster;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hps.recon.ecal.cluster.ClusterUtilities.UniqueEnergyComparator;
import static org.hps.recon.ecal.cluster.ClusterUtilities.isHardwareCluster;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.Cluster;
import org.lcsim.event.base.AbstractClusterPropertyCalculator;

/**
 * Cluster property calculator for hardware clusters.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class HardwareClusterPropertyCalculator extends AbstractClusterPropertyCalculator {

    @Override
    public void calculateProperties(List<CalorimeterHit> hits) {        
        reset();        
        List<CalorimeterHit> sortedHits = new ArrayList<CalorimeterHit>(hits);
        Collections.sort(sortedHits, new UniqueEnergyComparator());   
        CalorimeterHit seedHit = sortedHits.get(0);
        this.position = new double[] {seedHit.getPosition()[0], seedHit.getPosition()[1], seedHit.getPosition()[2]};
    }

    @Override
    public void calculateProperties(Cluster cluster) {        
        reset();       
        if (isHardwareCluster(cluster)) {
            setPositionFromSeedHit(cluster);
        } else {
            throw new IllegalArgumentException("The cluster has the wrong type: " + ClusterType.getClusterType(cluster.getType()).toString());
        }
    }
    
    void setPositionFromSeedHit(Cluster cluster) {
        CalorimeterHit seedHit = ClusterUtilities.findSeedHit(cluster);
        this.position = new double[] {seedHit.getPosition()[0], seedHit.getPosition()[1], seedHit.getPosition()[2]};
    }          
}
