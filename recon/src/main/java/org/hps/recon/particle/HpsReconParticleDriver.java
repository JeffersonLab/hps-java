package org.hps.recon.particle;

import java.util.ArrayList;
import java.util.List;

import org.hps.recon.vertexing.BilliorTrack;
import org.hps.recon.vertexing.BilliorVertex;
import org.hps.recon.vertexing.BilliorVertexer;

import org.lcsim.event.ReconstructedParticle;
import org.lcsim.event.Track;
import org.lcsim.event.base.BaseReconstructedParticle;
import org.lcsim.fit.helicaltrack.HelicalTrackFit;
import org.lcsim.recon.tracking.seedtracker.SeedTrack;

/**
 * 
 * @author Omar Moreno <omoreno1@ucsc.edu>
 * @version $Id$
 */
public class HpsReconParticleDriver extends ReconParticleDriver {
	
	public HpsReconParticleDriver(){}

	@Override
	protected void startOfData(){
		
		unconstrainedV0CandidatesColName    = "UnconstrainedV0Candidates";
		beamConV0CandidatesColName   		= "BeamspotConstrainedV0Candidates";
		targetV0ConCandidatesColName 		= "TargetConstrainedV0Candidates";	
	}

	/**
	 * 
	 */
	@Override
	void vertexParticles(List<ReconstructedParticle> electrons, List<ReconstructedParticle> positrons) {
		
		BilliorVertexer unconstrainedVtxFitter = new BilliorVertexer(bField);
		unconstrainedVtxFitter.doBeamSpotConstraint(false); 
		unconstrainedVtxFitter.setBeamSize(beamsize);
		

		BilliorVertexer beamConVtxFitter = new BilliorVertexer(bField);
		beamConVtxFitter.doBeamSpotConstraint(true); 
		beamConVtxFitter.setBeamSize(beamsize);
		
		BilliorVertexer targetConVtxFitter = new BilliorVertexer(bField);
		targetConVtxFitter.doTargetConstraint(true);
		targetConVtxFitter.setBeamSize(beamsize);
		
		ReconstructedParticle candidate = null; 
		// Loop through both electrons and positrons and try to find a common vertex
		for(ReconstructedParticle positron : positrons){
			for(ReconstructedParticle electron : electrons){
				
				// Get the tracks associated with the electron and the positron
				Track electronTrack = electron.getTracks().get(0); 
				Track positronTrack = positron.getTracks().get(0); 
				
				// Covert the tracks to BilliorTracks used by the vertexer
				BilliorTrack electronBTrack = toBilliorTrack(electronTrack); 
				BilliorTrack positronBTrack = toBilliorTrack(positronTrack);
			
				List<BilliorTrack> billiorTracks = new ArrayList<BilliorTrack>();
				billiorTracks.add(electronBTrack);
				billiorTracks.add(positronBTrack);
				
				BilliorVertex vtxFit = unconstrainedVtxFitter.fitVertex(billiorTracks);
				BilliorVertex vtxFitCon = beamConVtxFitter.fitVertex(billiorTracks);
				BilliorVertex vtxFitTarget = targetConVtxFitter.fitVertex(billiorTracks);
				
				candidate = new BaseReconstructedParticle(); 
				((BaseReconstructedParticle) candidate).setStartVertex(vtxFit);
				candidate.addParticle(electron);
				candidate.addParticle(positron);
				unconstrainedV0Candidates.add(candidate); 
				
				candidate = new BaseReconstructedParticle(); 
				((BaseReconstructedParticle) candidate).setStartVertex(vtxFitCon);
				candidate.addParticle(electron);
				candidate.addParticle(positron);
				beamConV0Candidates.add(candidate);
				
				candidate = new BaseReconstructedParticle(); 
				((BaseReconstructedParticle) candidate).setStartVertex(vtxFitTarget);
				candidate.addParticle(electron);
				candidate.addParticle(positron);
				targetConV0Candidates.add(candidate);
				
			}
		}
	}

	/**
	 * 
	 */
    private BilliorTrack toBilliorTrack(Track track) {
    	
    	HelicalTrackFit trackFit = ((SeedTrack) track).getSeedCandidate().getHelix();
    	return new BilliorTrack(trackFit);
    }
}
