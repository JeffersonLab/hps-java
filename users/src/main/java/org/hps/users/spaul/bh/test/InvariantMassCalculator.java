package org.hps.users.spaul.bh.test;

import hep.physics.vec.Hep3Vector;

import java.util.List;

import org.lcsim.event.EventHeader;
import org.lcsim.event.ReconstructedParticle;
import org.lcsim.event.Vertex;

public class InvariantMassCalculator {
    static double Ebeam = 2.2;
    
    static String vertexCollectionName = "TargetConstrainedV0Vertices";
    
    static public double getMass(Vertex v){
        
            
            return getMassRecon(v.getAssociatedParticle().getParticles().get(0),
                    v.getAssociatedParticle().getParticles().get(1));
            
        
    }
    //special case:  we have the recoiled particle which is particle #3
    static double getMassRecon(ReconstructedParticle part1, ReconstructedParticle part2, ReconstructedParticle part3) {
        
        Hep3Vector p1 = part1.getMomentum();
        Hep3Vector p2 = part2.getMomentum();
        
        double E1 = getCombinedEnergy(part1);
        double E2 = getCombinedEnergy(part2);
        double E3 = getCombinedEnergy(part3);
        if(E1 > E2){
            E1 = Ebeam - E2 - E3;
        }
        else if(E1 < E2){
            E2 = Ebeam - E1 - E3;
        }
        
        double denom = Math.sqrt(p1.magnitudeSquared()*p2.magnitudeSquared());
        double cos = (p1.x()*p2.x()+p1.y()*p2.y()+p1.z()*p2.z())
                        /denom;
        
        double sin2 = 
                    (Math.pow(p1.y()*p2.z()-p1.z()*p2.y(),2) +
                    Math.pow(p1.z()*p2.x()-p1.x()*p2.z(),2) +
                    Math.pow(p1.x()*p2.y()-p1.y()*p2.x(),2)
                    )/denom*denom; 
        
        //this is equivalent to 2*sin(theta/2)*sqrt(E1*E2);
        return Math.sqrt(2*E1*E2*sin2/(1+cos)); 
        
        //return Math.sqrt(2*E1*E2*(1-cos)); 
        
    }
    
    
    static double  getMassRecon(ReconstructedParticle part1, ReconstructedParticle part2) {
        
        Hep3Vector p1 = part1.getMomentum();
        Hep3Vector p2 = part2.getMomentum();
        
        double E1 = getCombinedEnergy(part1);
        double E2 = getCombinedEnergy(part2);
        
        double denom = Math.sqrt(p1.magnitudeSquared()*p2.magnitudeSquared());
        double cos = (p1.x()*p2.x()+p1.y()*p2.y()+p1.z()*p2.z())
                        /denom;
        double sin2 = 
                    (Math.pow(p1.y()*p2.z()-p1.z()*p2.y(),2) +
                    Math.pow(p1.z()*p2.x()-p1.x()*p2.z(),2) +
                    Math.pow(p1.x()*p2.y()-p1.y()*p2.x(),2)
                    )/denom*denom; 
        
        //this is equivalent to 2*sin(theta/2)*sqrt(E1*E2);
        //return Math.sqrt(2*E1*E2*sin2/(1+cos)); 
        
        return Math.sqrt(2*E1*E2*(1-cos)); 
        
    }
    
    static double getCombinedEnergy(ReconstructedParticle part){
        // @TODO figure out how to combine the Ecal measurement with the 
        // SVT measurement
        return part.getMomentum().magnitude();
    }
}
