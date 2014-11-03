package org.hps.recon.particle;

import java.util.List;

import org.lcsim.event.ReconstructedParticle;
import org.lcsim.event.Track;
import org.lcsim.event.Vertex;
import org.lcsim.event.base.BaseReconstructedParticle;

import org.hps.recon.vertexing.TwoTrackVertexer;

/**
 * Method creates reconstructed particles from tracks and clusters for
 * test run data. Also generates candidate A' reconstructed particles.
 * This method does not generate a separate vertex collection.
 * 
 * @author Omar Moreno <omoreno1@ucsc.edu>
 * @version $Id$
 */
public class TestRunReconParticleDriver extends ReconParticleDriver {
    /**
     * Generates reconstructed V0 candidate particles from electron
     * and positron pairs.
     * @param electrons - The list of electrons.
     * @param positrons - The list of positrons.
     */
    @Override
    protected void findVertices(List<ReconstructedParticle> electrons, List<ReconstructedParticle> positrons) {
        // Create a vertex fitter.
        TwoTrackVertexer vtxFitter = new TwoTrackVertexer();
        
        // Iterate over the electrons and positrons and try to generate
        // an A' candidate from them.
        for(ReconstructedParticle positron : positrons){
            for(ReconstructedParticle electron : electrons){
                // Get the electron and positron tracks.
                Track electronTrack = electron.getTracks().get(0);
                Track positronTrack = positron.getTracks().get(0);
                
                // Feed the tracks to the vertex fitter.
                vtxFitter.setTracks(electronTrack, positronTrack);
                vtxFitter.fitVertex();
                
                // Get the reconstructed vertex.
                Vertex vertex = vtxFitter.getFittedVertex();
                
                // Create a reconstructed particle for the candidate
                // particle generated from the electron/positron pair.
                ReconstructedParticle candidate = new BaseReconstructedParticle(); 
                ((BaseReconstructedParticle) candidate).setStartVertex(vertex);
                candidate.addParticle(electron);
                candidate.addParticle(positron);
                
                // Add the candidate particle to list.
                unconstrainedV0Candidates.add(candidate); 
            }
        }
    }
    
    /**
     * Sets the unconstrained A' candidate particle collection name if
     * it has not already been defined.
     */
    @Override
    protected void startOfData(){
        if(unconstrainedV0CandidatesColName == null) { unconstrainedV0CandidatesColName = "V0Candidates"; }
    }
}
