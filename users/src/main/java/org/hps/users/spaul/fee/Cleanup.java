package org.hps.users.spaul.fee;

import hep.physics.vec.Hep3Vector;

import java.util.ArrayList;
import java.util.List;

import org.hps.recon.tracking.TrackUtils;
import org.lcsim.event.ReconstructedParticle;
import org.lcsim.event.Track;
import org.lcsim.event.Cluster;

public class Cleanup {

	public static void purgeParticlesWithSameCluster(List<ReconstructedParticle> particles, List<Cluster> clusters) {
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
				
				if(p1.getClusters().get(0) == p2.getClusters().get(0)){ //tracks matched to same cluster
					ReconstructedParticle loser = getWorseParticle(p1, p2);
					//if(loser != null)
					  //  System.out.println("racoons are happy");
					trashcan.add(loser);
				}
				if(trashcan.contains(p1))
					continue outer;
				
			}
			
		}
		particles.removeAll(trashcan);
		if(test){
		for(ReconstructedParticle p1 : particles){
		    for(ReconstructedParticle p2 : particles){
		        if(p1 != p2 && p1.getClusters().get(0) == p2.getClusters().get(0)){
		            System.out.println("racoons are unhappy");
		            System.exit(0);
		        }
		    }
		    //System.out.println(p1.getClusters().get(0).getPosition()[1]);
		}
		if(particles.size() != clusters.size()){
		    System.out.println("racoons are getting fat off extra particles");
            System.exit(0);
		}
		
		for(Cluster c: clusters){
		    int found = 0;
		    for(ReconstructedParticle p : particles){
		        if(c == p.getClusters().get(0))
		            found ++;
		    }
		    if(found != 1){
		        System.out.println("racoons have been cloning themselves or killing each other.  I don't know which");
	            System.exit(0);
	        }
		    
		}
		}
	}
	public static boolean test = true;
	
	static ReconstructedParticle getWorseParticle(ReconstructedParticle p1, ReconstructedParticle p2){
		
	    if(p1.getType() > 31 && p2.getType() <= 31)
	        return p2;
	    if(p1.getType() <= 31 && p2.getType() > 31 )
	        return p1;
	    
		if(p1.getTracks().size() == 0 && p2.getTracks().size() == 1)
			return p1;
		if(p2.getTracks().size() == 0 && p1.getTracks().size() == 1)
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
