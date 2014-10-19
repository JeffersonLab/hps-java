package org.hps.recon.particle;

import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.BasicHepLorentzVector;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.HepLorentzVector;
import hep.physics.vec.VecOp;

import java.util.ArrayList;
import java.util.List;

import org.hps.recon.particle.ReconParticleDriver;
import org.hps.recon.particle.ReconParticleDriverIC;
import org.hps.recon.tracking.CoordinateTransformations;
import org.hps.recon.vertexing.BilliorTrack;
import org.hps.recon.vertexing.BilliorVertex;
import org.hps.recon.vertexing.BilliorVertexer;
import org.lcsim.event.ReconstructedParticle;
import org.lcsim.event.Track;
import org.lcsim.event.base.BaseReconstructedParticle;
import org.lcsim.fit.helicaltrack.HelicalTrackFit;
import org.lcsim.recon.tracking.seedtracker.SeedTrack;


/**
 * This uses ReconParticleDriverIC which improves the clustering
 * corrections. 
 * 
 * @author Omar Moreno <omoreno1@ucsc.edu>
 * @author Holly Szumila <hvanc001@odu.edu>
 * @version $Id$
 */
public class HpsReconParticleDriverIC extends ReconParticleDriverIC {
	
	private enum Constraint { 
		UNCONSTRAINED, 
		BS_CONSTRAINED, 
		TARGET_CONSTRAINED
	}
	
	public HpsReconParticleDriverIC(){}		

	@Override
	protected void startOfData(){
		
		unconstrainedV0CandidatesColName    = "UnconstrainedV0Candidates";
		beamConV0CandidatesColName   		= "BeamspotConstrainedV0Candidates";
		targetConV0CandidatesColName 		= "TargetConstrainedV0Candidates";	
		unconstrainedV0VerticesColName 		= "UnconstrainedV0Vertices";
		beamConV0VerticesColName 			= "BeamspotConstrainedV0Vertices";
		targetConV0VerticesColName			= "TargetConstrainedV0Vertices";
	}

	/**
	 * 
	 */
	@Override
	void findVertices(List<ReconstructedParticle> electrons, List<ReconstructedParticle> positrons) {
		
		ReconstructedParticle candidate = null; 
		BilliorVertex vtxFit = null;
		// Loop through both electrons and positrons and try to find a common vertex
		for(ReconstructedParticle positron : positrons){
			for(ReconstructedParticle electron : electrons){
				
				// Get the tracks associated with the electron and the positron
				Track electronTrack = electron.getTracks().get(0); 
				Track positronTrack = positron.getTracks().get(0); 
				
				// Covert the tracks to BilliorTracks used by the vertexer
				BilliorTrack electronBTrack = toBilliorTrack(electronTrack); 
				BilliorTrack positronBTrack = toBilliorTrack(positronTrack);
			
				for(Constraint constraint : Constraint.values()){
					
					vtxFit = fitVertex(constraint, electronBTrack, positronBTrack);
				
					candidate = makeReconstructedParticle(electron, positron, vtxFit); 
					
					
					switch(constraint){
						case UNCONSTRAINED: 
							unconstrainedV0Vertices.add(vtxFit);
							unconstrainedV0Candidates.add(candidate); 
							break;
						case BS_CONSTRAINED:
							beamConV0Vertices.add(vtxFit);
							beamConV0Candidates.add(candidate);
							break;
						case TARGET_CONSTRAINED:
							targetConV0Vertices.add(vtxFit);
							targetConV0Candidates.add(candidate);
							break;
					}
				}
			}
		}
	}
	
	/**
	 * 
	 */
	BilliorVertex fitVertex(Constraint constraint, BilliorTrack electron, BilliorTrack positron){
		
		BilliorVertexer vtxFitter = new BilliorVertexer(bField);
		// TODO: The beam size should come from the conditions database
		vtxFitter.setBeamSize(beamsize);
		
		switch(constraint){
			case UNCONSTRAINED: 
				vtxFitter.doBeamSpotConstraint(false); 
				break;
			case BS_CONSTRAINED:
				vtxFitter.doBeamSpotConstraint(true); 
				break;
			case TARGET_CONSTRAINED:
				vtxFitter.doTargetConstraint(true);
				break;
		}
				
		List<BilliorTrack> billiorTracks = new ArrayList<BilliorTrack>();
		billiorTracks.add(electron);
		billiorTracks.add(positron);
		
		return vtxFitter.fitVertex(billiorTracks);
	}
	
	/**
	 * 
	 */
	ReconstructedParticle makeReconstructedParticle(ReconstructedParticle electron, ReconstructedParticle positron, BilliorVertex vtxFit){
		
		ReconstructedParticle candidate = new BaseReconstructedParticle();
		((BaseReconstructedParticle) candidate).setStartVertex(vtxFit);
		candidate.addParticle(electron);
		candidate.addParticle(positron);
					
		// TODO: The calculation of the total fitted momentum should be done within 
		// 		 BilloirVertex
		((BaseReconstructedParticle) candidate).setMass(vtxFit.getParameters().get("invMass"));
		Hep3Vector fittedMomentum = new BasicHep3Vector(vtxFit.getParameters().get("p1X"), 
		   											    vtxFit.getParameters().get("p1Y"), 
														vtxFit.getParameters().get("p1Z"));
		fittedMomentum = VecOp.add(fittedMomentum, new BasicHep3Vector(vtxFit.getParameters().get("p2X"), 
																   	   vtxFit.getParameters().get("p2Y"),
																       vtxFit.getParameters().get("p2Z")));
		this.printDebug("Fitted momentum in tracking frame: " + fittedMomentum.toString());
		fittedMomentum = CoordinateTransformations.transformVectorToDetector(fittedMomentum);
		this.printDebug("Fitted momentum in detector frame: " + fittedMomentum.toString());
		HepLorentzVector fourVector = new BasicHepLorentzVector(0, 0, 0, 0); 
		((BasicHepLorentzVector) fourVector).setV3(fourVector.t(), fittedMomentum);
		((BaseReconstructedParticle) candidate).set4Vector(fourVector);
		
		// Add the ReconstructedParticle to the Vertex 
		vtxFit.setAssociatedParticle(candidate);
		
		return candidate;
		
	}
	

	/**
	 * 
	 */
    private BilliorTrack toBilliorTrack(Track track) {
    	
    	HelicalTrackFit trackFit = ((SeedTrack) track).getSeedCandidate().getHelix();
    	return new BilliorTrack(trackFit);
    }
}
