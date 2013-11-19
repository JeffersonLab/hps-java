package org.lcsim.hps.recon.particle;

//--- java ---//
import java.util.List;

//--- lcsim ---//
import org.lcsim.event.ReconstructedParticle;
import org.lcsim.event.Track;
import org.lcsim.event.Vertex;
import org.lcsim.event.base.BaseReconstructedParticle;

//--- hps-java ---//
import org.lcsim.hps.recon.vertexing.TwoTrackVertexer;

/**
 * 
 * @author Omar Moreno <omoreno1@ucsc.edu>
 * @version $Id: TestRunReconParticleDriver.java,v 1.3 2013/10/18 16:46:01 phansson Exp $
 */
public class TestRunReconParticleDriver extends ReconParticleDriver {

	public TestRunReconParticleDriver(){}; 
	
	@Override
	protected void startOfData(){
		candidatesCollectionName = "VertexedReconParticles";
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
				candidates.add(candidate); 
			}
		}
	}
}
