package org.hps.recon.particle;

//--- java ---//
import java.util.List;

import org.hps.recon.vertexing.TwoTrackVertexer;
//--- lcsim ---//
import org.lcsim.event.ReconstructedParticle;
import org.lcsim.event.Track;
import org.lcsim.event.Vertex;
import org.lcsim.event.base.BaseReconstructedParticle;
//--- hps-java ---//

/**
 * 
 * @author Omar Moreno <omoreno1@ucsc.edu>
 * @version $Id$
 */
public class TestRunReconParticleDriver extends ReconParticleDriver {

	public TestRunReconParticleDriver(){}; 
	
	@Override
	protected void startOfData(){
		unconstrainedV0CandidatesColName = "V0Candidates";
	}
	
	@Override
	void vertexParticles(List<ReconstructedParticle> electrons, List<ReconstructedParticle> positrons) {
		
		TwoTrackVertexer vtxFitter = new TwoTrackVertexer(); 
	
		// Loop through both electrons and positrons and try to vertex them
		for(ReconstructedParticle positron : positrons){
			for(ReconstructedParticle electron : electrons){
				
				// Get the tracks associated with the electrons and positrons
				Track electronTrack = electron.getTracks().get(0); 
				Track positronTrack = positron.getTracks().get(0); 
				vtxFitter.setTracks(electronTrack, positronTrack);
				vtxFitter.fitVertex();
				Vertex vertex = vtxFitter.getFittedVertex();
				
				ReconstructedParticle candidate = new BaseReconstructedParticle(); 
				((BaseReconstructedParticle) candidate).setStartVertex(vertex);
				candidate.addParticle(electron);
				candidate.addParticle(positron);
				unconstrainedV0Candidates.add(candidate); 
			}
		}
	}
}
