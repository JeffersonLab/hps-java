package org.hps.users.spaul.fee;

import hep.physics.vec.Hep3Vector;

import java.util.ArrayList;
import java.util.List;

import org.lcsim.event.ReconstructedParticle;

public class Cleanup {

	public static void purgeDuplicates(List<ReconstructedParticle> particles) {
		ArrayList<ReconstructedParticle> trashcan = new ArrayList();
		outer : for(ReconstructedParticle p1 : particles){
			if(trashcan.contains(p1))
				continue;
			for(ReconstructedParticle p2 : particles){
				if(p1 == p2) continue;
				if(trashcan.contains(p2))
					continue;
				if((p1.getType()>31)^(p2.getType()>31)){ //one is GBL, the other is seed.  
					continue;
				}
				
				if(p1.getEnergy() == p2.getEnergy()){ //tracks matched to same cluster
					ReconstructedParticle loser = getWorseParticle(p1, p2);
					trashcan.add(loser);
				}
				if(trashcan.contains(p1))
					continue outer;
				
			}
			
		}
		particles.removeAll(trashcan);
	}
	
	static ReconstructedParticle getWorseParticle(ReconstructedParticle p1, ReconstructedParticle p2){
		if(p1.getTracks().get(0).getChi2() < p2.getTracks().get(0).getChi2())
			return p2;
		return p1;
	}
	
	
	
	
	
	static double angle(Hep3Vector v1, Hep3Vector v2){
		return Math.acos(v1.x()*v2.x() + v1.y()*v2.y() + v1.z()*v2.z())
			/(v1.magnitude()*v2.magnitude());
	}

}
