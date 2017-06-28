package org.hps.recon.utils;

import java.util.List;

import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.Cluster;

import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;

public class SnapToEdge2 {

    public Hep3Vector snapToEdge(Hep3Vector tPos, Cluster c) {
        List<CalorimeterHit> hits = c.getCalorimeterHits();
        boolean upperEdgeCrystalsOnly = true;
        boolean lowerEdgeCrystalsOnly = true;
        for(CalorimeterHit hit : hits){
            int ix = hit.getIdentifierFieldValue("ix");
            int iy = hit.getIdentifierFieldValue("iy");
            
            if(!(iy == -1 || (iy == -2 && ix >= -10 && ix <= -2) || iy == 5)){
                upperEdgeCrystalsOnly = false;
            }
            
            if(!(iy == 1 || (iy == 2 && ix >= -10 && ix <= -2) || iy == -5)){
                lowerEdgeCrystalsOnly = false;
            }
            
            
            if(!upperEdgeCrystalsOnly && !lowerEdgeCrystalsOnly)
                break;
        }
        if(upperEdgeCrystalsOnly && tPos.y() >= c.getPosition()[1])
            return new BasicHep3Vector(tPos.x(), c.getPosition()[1], tPos.z());
        if(lowerEdgeCrystalsOnly && tPos.y() <= c.getPosition()[1])
            return new BasicHep3Vector(tPos.x(), c.getPosition()[1], tPos.z());
        else 
            return tPos;
    }
}
