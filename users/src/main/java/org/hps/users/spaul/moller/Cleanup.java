package org.hps.users.spaul.moller;

import hep.physics.vec.Hep3Vector;

import java.util.ArrayList;
import java.util.List;

import org.hps.recon.tracking.TrackType;
import org.hps.recon.tracking.TrackUtils;
import org.lcsim.event.ReconstructedParticle;
import org.lcsim.event.Vertex;

public class Cleanup {

	public static void purgeDuplicates(List<Vertex> vertices) {
		ArrayList<Vertex> trashcan = new ArrayList();
		outer : for(Vertex p1 : vertices){
			if(trashcan.contains(p1))
				continue;
			for(Vertex p2 : vertices){
				if(p1 == p2) continue;
				if(trashcan.contains(p2))
					continue;
				
				
				if(p1.getAssociatedParticle().getEnergy() == p2.getAssociatedParticle().getEnergy()){ //tracks matched to same cluster
					Vertex loser = getWorseParticle(p1, p2);
					trashcan.add(loser);
				}
				if(trashcan.contains(p1))
					continue outer;
				
			}
			
		}
		vertices.removeAll(trashcan);
	}
	
	static Vertex getWorseParticle(Vertex v1, Vertex v2){
		
		ReconstructedParticle p1 = v1.getAssociatedParticle();
		ReconstructedParticle p2 = v2.getAssociatedParticle();
		if(TrackType.isGBL(p1.getType()) && !TrackType.isGBL(p2.getType()))
			return v2;
		if(TrackType.isGBL(p2.getType())&& !TrackType.isGBL(p1.getType()))
			return v1;
		
		if(v1.getChi2() < v2.getChi2())
			return v2;
		return v1;
	}
	
	
	
	
	
	

}
