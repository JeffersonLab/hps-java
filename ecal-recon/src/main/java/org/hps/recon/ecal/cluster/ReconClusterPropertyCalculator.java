package org.hps.recon.ecal.cluster;

import hep.physics.vec.Hep3Vector;

import java.util.List;

import org.hps.detector.ecal.EcalCrystal;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.Cluster;
import org.lcsim.event.base.AbstractClusterPropertyCalculator;

class ReconClusterPropertyCalculator extends AbstractClusterPropertyCalculator {

    public void calculateProperties(List<CalorimeterHit> hits) {
        // This is unsupported because the cluster's energy is required for the position algorithm. 
        throw new IllegalArgumentException("This method is not supported for recon clustering.");
    }
    
    /**
     * Calculates the position of each cluster with no correction for particle type, 
     * as documented in HPS Note 2014-001.
     * @param cluster The input Cluster.
     */
    public void calculateProperties(Cluster cluster) {
        
        reset();
        
        // Calculate the position of the cluster.
        calculatePosition(cluster);
    }

    private void calculatePosition(Cluster cluster) {
        final double w0 = 3.1;
        double xCl = 0.0;
        // calculated cluster y position
        double yCl = 0.0;
        double eNumX = 0.0;
        double eNumY = 0.0;
        double eDen = 0.0;
        List<CalorimeterHit> clusterHits = cluster.getCalorimeterHits();
        for (CalorimeterHit hit : clusterHits) {
            
            EcalCrystal crystal = (EcalCrystal) hit.getDetectorElement();
            Hep3Vector crystalPosition = crystal.getPositionFront();
            
            double wi = Math.max(0.0, (w0 + Math.log(hit.getCorrectedEnergy() / cluster.getEnergy())));
            eNumX += wi * (crystalPosition.x() / 10.0);
            eNumY += wi * (crystalPosition.y() / 10.0);
            eDen += wi;

        } // end for iteration through clusterHits

        xCl = eNumX / eDen;
        yCl = eNumY / eDen;

        position[0] = xCl * 10.0;// mm
        position[1] = yCl * 10.0;// mm        
        CalorimeterHit seedHit = clusterHits.get(0);
        this.position[2] = ((EcalCrystal)seedHit.getDetectorElement()).getPositionFront().z();
    }    
}