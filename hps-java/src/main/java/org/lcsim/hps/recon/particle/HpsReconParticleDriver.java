package org.lcsim.hps.recon.particle;

//--- java ---//
import java.util.ArrayList;
import java.util.List;

//--- lcsim ---//
import org.lcsim.event.ReconstructedParticle;
import org.lcsim.event.base.BaseReconstructedParticle;
import org.lcsim.fit.helicaltrack.HelicalTrackFit;
import org.lcsim.recon.tracking.seedtracker.SeedCandidate;
import org.lcsim.recon.tracking.seedtracker.SeedTrack;

//--- hps-java ---//
import org.lcsim.hps.recon.vertexing.BilliorVertexer;
import org.lcsim.hps.recon.vertexing.BilliorVertex;
import org.lcsim.hps.recon.vertexing.BilliorTrack;

/**
 * 
 * @author Omar Moreno <omoreno1@ucsc.edu>
 * @version $Id: HpsReconParticleDriver.java,v 1.1 2013/04/15 07:14:32 omoreno Exp $
 */
public class HpsReconParticleDriver extends ReconParticleDriver {
	
	public HpsReconParticleDriver(){}

	@Override
	protected void startOfData(){
		
		candidatesCollectionName          = "AprimeUnconstrained";
		candidatesBeamConCollectionName   = "AprimeBeamspotConstrained";
		candidatesTargetConCollectionName = "AprimeTargetConstrained";	
	}
	
	@Override
	void vertexParticles(List<ReconstructedParticle> electrons, List<ReconstructedParticle> positrons) {
		
		BilliorVertexer vtxFitter = new BilliorVertexer(bField);
		vtxFitter.doBeamSpotConstraint(false); 
		vtxFitter.setBeamSize(beamsize);
		

		BilliorVertexer vtxFitterCon = new BilliorVertexer(bField);
		vtxFitterCon.doBeamSpotConstraint(true); 
		vtxFitterCon.setBeamSize(beamsize);
		
		BilliorVertexer vtxFitterTarget = new BilliorVertexer(bField);
		vtxFitterTarget.doTargetConstraint(true);
		vtxFitterCon.setBeamSize(beamsize);
		
		// Loop through both electrons and positrons and try to vertex them
		ReconstructedParticle apCand       = null; 
		for(ReconstructedParticle positron : positrons){
			for(ReconstructedParticle electron : electrons){
				
				// Get the tracks associated with the electron and the positron
				SeedTrack electronTrack = (SeedTrack) electron.getTracks().get(0); 
				SeedTrack positronTrack = (SeedTrack) positron.getTracks().get(0); 
				
				// Covert the tracks to BilliorTracks used by the vertexer
				BilliorTrack electronBTrack = this.getBilliorTrack(electronTrack); 
				BilliorTrack positronBTrack = this.getBilliorTrack(positronTrack);
			
				List<BilliorTrack> billiorTracks = new ArrayList<BilliorTrack>();
				billiorTracks.add(electronBTrack);
				billiorTracks.add(positronBTrack);
				
				BilliorVertex vtxFit = vtxFitter.fitVertex(billiorTracks);
				BilliorVertex vtxFitCon = vtxFitterCon.fitVertex(billiorTracks);
				BilliorVertex vtxFitTarget = vtxFitterTarget.fitVertex(billiorTracks);
				
				apCand = new BaseReconstructedParticle(); 
				((BaseReconstructedParticle) apCand).setStartVertex(vtxFit);
				apCand.addParticle(electron);
				apCand.addParticle(positron);
				candidates.add(apCand); 
				
				apCand = new BaseReconstructedParticle(); 
				((BaseReconstructedParticle) apCand).setStartVertex(vtxFitCon);
				apCand.addParticle(electron);
				apCand.addParticle(positron);
				candidatesBeamCon.add(apCand);
				
				apCand = new BaseReconstructedParticle(); 
				((BaseReconstructedParticle) apCand).setStartVertex(vtxFitTarget);
				apCand.addParticle(electron);
				apCand.addParticle(positron);
				candidatesTargetCon.add(apCand);
				
			}
		}
	}

	/**
	 * 
	 */
    private BilliorTrack getBilliorTrack(SeedTrack seedTrack) {
    	
    	SeedCandidate seedCan = seedTrack.getSeedCandidate(); 
    	HelicalTrackFit trackFit = seedCan.getHelix(); 
    	return new BilliorTrack(trackFit);
    }
}
