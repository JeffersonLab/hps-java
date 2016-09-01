package org.hps.users.spaul.fee;

import hep.physics.vec.Hep3Vector;

import java.util.ArrayList;
import java.util.List;

import org.hps.recon.tracking.TrackUtils;
import org.lcsim.event.ReconstructedParticle;
import org.lcsim.event.Track;

public class Cleanup {

	public static void purgeParticlesWithSameCluster(List<ReconstructedParticle> particles) {
		ArrayList<ReconstructedParticle> trashcan = new ArrayList();
		outer : for(ReconstructedParticle p1 : particles){
			if(trashcan.contains(p1))
				continue;
			if(p1.getClusters().size() == 0){
				trashcan.add(p1);
				continue;
			}
			for(ReconstructedParticle p2 : particles){
				if(p1 == p2) continue;
				if(trashcan.contains(p2))
					continue;
				if(p2.getClusters().size() == 0)
				{
					trashcan.add(p2);
					continue;
				}
				if((p1.getType()>31)^(p2.getType()>31)){ //one is GBL, the other is seed.  
					continue;
				}
				if(p1.getClusters().get(0) == p2.getClusters().get(0)){ //tracks matched to same cluster
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
		//first check if the tracks come from the target:
		
		if(p1.getTracks().size() == 0)
			return p1;
		if(p2.getTracks().size() == 0)
			return p2;
		
		Track t1 = p1.getTracks().get(0);
		Track t2 = p2.getTracks().get(0);
		
		
		//first see if there is some clue that one of the tracks is better than the other:
		if(Math.abs(TrackUtils.getDoca(t1))>1 && Math.abs(TrackUtils.getDoca(t2))<1)
			return p2;
		if(Math.abs(TrackUtils.getDoca(t2))>1 && Math.abs(TrackUtils.getDoca(t1))<1)
			return p1;
		
		//if they are both ok, use the one with the track that extrapolates closes to the cluster.  
		if(Math.hypot(FeeHistogramDriver.getDx(p1), FeeHistogramDriver.getDy(p1))
				< Math.hypot(FeeHistogramDriver.getDx(p2), FeeHistogramDriver.getDy(p2)))
			return p2;
		return p1;
		
		
		//if they are both ok, then just use whichever one has the smaller chi2
		//if(p1.getTracks().get(0).getChi2() < p2.getTracks().get(0).getChi2())
			//return p2;
		//return p1;
		
	}
	
	
	
	
	
	

}
