/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.lcsim.hps.recon.vertexing;

import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;

import org.lcsim.event.MCParticle;
import org.lcsim.hps.event.HPSTransformations;

/**
 *
 *Class that computes the vertex of two MC particles using the line vertexer class.
 *
 * @author phansson
 */
public class TwoParticleVertexer extends TwoLineVertexer {
    
	public TwoParticleVertexer() {
    }
    public void setParticle(MCParticle track1,MCParticle track2) {
        
        //Calculate using detector coord.
        Hep3Vector PA1 = track1.getOrigin();
        Hep3Vector PB1 = track2.getOrigin();
        Hep3Vector p1 = track1.getMomentum();
        Hep3Vector p2 = track2.getMomentum();
        
        //propagate to different position
        double dz = 20.0;
        Hep3Vector PA2 = this.propAlongLine(PA1, p1, dz);
        Hep3Vector PB2 = this.propAlongLine(PB1, p2, dz);
        
        if(_debug) {
        	System.out.printf("A1 %s p1 %s B1 %s p2 %s\n", PA1.toString(), p1.toString(), PB1.toString(), p2.toString());
        	System.out.printf("A2 %s B2 %s\n", PA2.toString(), PB2.toString());
        }
        
        //set the member variables
        A1 = HPSTransformations.transformVectorToTracking(PA1);
        A2 = HPSTransformations.transformVectorToTracking(PA2);
        B1 = HPSTransformations.transformVectorToTracking(PB1);
        B2 = HPSTransformations.transformVectorToTracking(PB2);

    }
    
    Hep3Vector propAlongLine(Hep3Vector org, Hep3Vector p, double dz) {
        double tanPxPz = p.x() / p.z();
        double tanPyPz = p.y() / p.z();
        double dx = dz * tanPxPz;
        double dy = dz * tanPyPz;
        return new BasicHep3Vector(org.x() + dx, org.y() + dy, org.z() + dz);
    }
    
    
}
