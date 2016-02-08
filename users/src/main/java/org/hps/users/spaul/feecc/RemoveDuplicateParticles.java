package org.hps.users.spaul.feecc;

import java.util.ArrayList;
import java.util.List;

import org.hps.recon.tracking.TrackType;
import org.lcsim.event.ReconstructedParticle;

public class RemoveDuplicateParticles {
	public static List<ReconstructedParticle> removeDuplicateParticles(List<ReconstructedParticle> input){
		//System.out.println("\n" + input.size());
		List<ReconstructedParticle> output = new ArrayList();
		for(ReconstructedParticle p : input){
			boolean add = true;
			boolean replace = false;
			ReconstructedParticle duplicate = null;
			/*if(p.getTracks().size() == 0){
				//System.out.println("no track");
				continue;
			}
			if(!TrackType.isGBL(p.getTracks().get(0).getType())){
				//System.out.println("non gbl track");
				continue;
			}*/
			inner : for(ReconstructedParticle p2 : output){
				if(p.getEnergy() == p2.getEnergy()){
					
					if(betterThan(p, p2)){
						duplicate = p2;
						replace = true;
					}
					else{
						add = false; //keep the one that is already there
					}
					break inner;
				}
			}
			if(replace){
				output.remove(duplicate);
				//System.out.println("replaced");
				output.add(p);
			}else if(add){
				output.add(p);
				//System.out.println("added");
			}else{
				//System.out.println("retained");
			}
		}
		//System.out.println(output.size());
		return output;
	}

	private static boolean betterThan(ReconstructedParticle p,
			ReconstructedParticle p2) {
		if(p.getTracks().size() == 0)
			return false;
		if(p.getTracks().size() == 1 && p.getTracks().size() == 0)
			return true;
		if(TrackType.isGBL(p.getTracks().get(0).getType()) && !TrackType.isGBL(p2.getTracks().get(0).getType()))
				return true;
		
		if(p.getTracks().get(0).getChi2() < p2.getTracks().get(0).getChi2()){
			return true;
		}
		return false;
	}
}
