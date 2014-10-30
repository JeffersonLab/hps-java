package org.hps.recon.particle;

import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.BasicHepLorentzVector;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.HepLorentzVector;
import hep.physics.vec.VecOp;

import java.util.ArrayList;
import java.util.List;

import org.lcsim.event.ReconstructedParticle;
import org.lcsim.event.Track;
import org.lcsim.event.base.BaseReconstructedParticle;
import org.lcsim.fit.helicaltrack.HelicalTrackFit;
import org.lcsim.recon.tracking.seedtracker.SeedTrack;

import org.hps.recon.tracking.CoordinateTransformations;
import org.hps.recon.vertexing.BilliorTrack;
import org.hps.recon.vertexing.BilliorVertex;
import org.hps.recon.vertexing.BilliorVertexer;

/**
 * The main HPS implementation of ReconParticleDriver. Method generates
 * V0 candidates and does vertex fits.
 * 
 * @author Omar Moreno <omoreno1@ucsc.edu>
 * @version $Id$
 */
public class HpsReconParticleDriver extends ReconParticleDriver {
    /**
     * Represents a type of constraint for vertex fitting.
     * 
     * @author Omar Moreno <omoreno1@ucsc.edu>
     */
    private enum Constraint { 
        /** Represents a fit with no constraints. */
        UNCONSTRAINED, 
        /** Represents a fit with beam spot constraints. */
        BS_CONSTRAINED, 
        /** Represents a fit with target constraints. */
        TARGET_CONSTRAINED
    }
    
    /**
     * Creates reconstructed V0 candidate particles and vertices for
     * electron positron pairs using no constraints, beam constraints,
     * and target constraints. These are saved to the appropriate lists
     * in the super class.
     * @param electrons - The list of electrons.
     * @param positrons - The list of positrons.
     */
    @Override
    protected void findVertices(List<ReconstructedParticle> electrons, List<ReconstructedParticle> positrons) {
        // Iterate over the positrons and electrons to perform vertexing
        // on the pairs.
        for(ReconstructedParticle positron : positrons) {
            for(ReconstructedParticle electron : electrons) {
                // Get the tracks associated with the electron and
                // the positron.
                Track electronTrack = electron.getTracks().get(0); 
                Track positronTrack = positron.getTracks().get(0); 
                
                // Covert the tracks to BilliorTracks.
                BilliorTrack electronBTrack = toBilliorTrack(electronTrack); 
                BilliorTrack positronBTrack = toBilliorTrack(positronTrack);
                
                // Create candidate particles for each constraint.
                for(Constraint constraint : Constraint.values()) {
                    // Generate a candidate vertex and particle.
                    BilliorVertex vtxFit = fitVertex(constraint, electronBTrack, positronBTrack);
                    ReconstructedParticle candidate = makeReconstructedParticle(electron, positron, vtxFit); 
                    
                    // Add the candidate vertex and particle to the
                    // appropriate LCIO collection.
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
     * Sets the default LCIO collection names if they are not already
     * defined previously.
     */
    @Override
    protected void startOfData(){
        // If the LCIO collection names have not been defined, assign
        // them to the default names.
        if(unconstrainedV0CandidatesColName == null) { unconstrainedV0CandidatesColName = "UnconstrainedV0Candidates"; }
        if(beamConV0CandidatesColName == null) { beamConV0CandidatesColName = "BeamspotConstrainedV0Candidates"; }
        if(targetConV0CandidatesColName == null) { targetConV0CandidatesColName = "TargetConstrainedV0Candidates";     }
        if(unconstrainedV0VerticesColName == null) { unconstrainedV0VerticesColName = "UnconstrainedV0Vertices"; }
        if(beamConV0VerticesColName == null) { beamConV0VerticesColName = "BeamspotConstrainedV0Vertices"; }
        if(targetConV0VerticesColName == null) { targetConV0VerticesColName = "TargetConstrainedV0Vertices"; }
    }
    
    /**
     * Fits a vertex from an electron/positron track pair using the
     * indicated constraint.
     * @param constraint - The constraint type to use.
     * @param electron - The electron track.
     * @param positron - The positron track.
     * @return Returns the reconstructed vertex as a <code>BilliorVertex
     * </code> object.
     */
    private BilliorVertex fitVertex(Constraint constraint, BilliorTrack electron, BilliorTrack positron){
        // Create a vertex fitter from the magnetic field.
        BilliorVertexer vtxFitter = new BilliorVertexer(bField);
        // TODO: The beam size should come from the conditions database.
        vtxFitter.setBeamSize(beamSize);
        
        // Perform the vertexing based on the specified constraint.
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
        
        // Add the electron and positron tracks to a track list for
        // the vertex fitter.
        List<BilliorTrack> billiorTracks = new ArrayList<BilliorTrack>();
        billiorTracks.add(electron);
        billiorTracks.add(positron);
        
        // Find and return a vertex based on the tracks.
        return vtxFitter.fitVertex(billiorTracks);
    }
    
    /**
     * Creates a reconstructed V0 candidate particle from an electron,
     * positron, and billior vertex.
     * @param electron - The electron.
     * @param positron - The positron.
     * @param vtxFit - The billior vertex.
     * @return Returns a reconstructed particle with properties generated
     * from the child particles and vertex given as an argument.
     */
    private ReconstructedParticle makeReconstructedParticle(ReconstructedParticle electron, ReconstructedParticle positron, BilliorVertex vtxFit){
        // Create a new reconstructed particle to represent the V0
        // candidate and populate it with the electron and positron.
        ReconstructedParticle candidate = new BaseReconstructedParticle();
        ((BaseReconstructedParticle) candidate).setStartVertex(vtxFit);
        candidate.addParticle(electron);
        candidate.addParticle(positron);
        
        // TODO: The calculation of the total fitted momentum should be
        //       done within BilloirVertex.
        // Calculate the candidate particle momentum and associate it
        // with the reconstructed candidate particle.
        ((BaseReconstructedParticle) candidate).setMass(vtxFit.getParameters().get("invMass"));
        Hep3Vector fittedMomentum = new BasicHep3Vector(vtxFit.getParameters().get("p1X"), 
                                                           vtxFit.getParameters().get("p1Y"), 
                                                        vtxFit.getParameters().get("p1Z"));
        fittedMomentum = VecOp.add(fittedMomentum, new BasicHep3Vector(vtxFit.getParameters().get("p2X"), 
                                                                          vtxFit.getParameters().get("p2Y"),
                                                                       vtxFit.getParameters().get("p2Z")));
        fittedMomentum = CoordinateTransformations.transformVectorToDetector(fittedMomentum);
        HepLorentzVector fourVector = new BasicHepLorentzVector(0, 0, 0, 0); 
        ((BasicHepLorentzVector) fourVector).setV3(fourVector.t(), fittedMomentum);
        ((BaseReconstructedParticle) candidate).set4Vector(fourVector);
        
        // VERBOSE :: Output the fitted momentum data.
        printDebug("Fitted momentum in tracking frame: " + fittedMomentum.toString());
        printDebug("Fitted momentum in detector frame: " + fittedMomentum.toString());
        
        // Add the ReconstructedParticle to the vertex.
        vtxFit.setAssociatedParticle(candidate);
        
        // Return the V0 candidate.
        return candidate;
    }
    
    /**
     * Converts a <code>Track</code> object to a <code>BilliorTrack
     * </code> object.
     * @param track - The original track.
     * @return Returns the original track as a <code>BilliorTrack
     * </code> object.
     */
    private BilliorTrack toBilliorTrack(Track track) {
        // Convert the track to a helical track fit.
        HelicalTrackFit trackFit = ((SeedTrack) track).getSeedCandidate().getHelix();
        
        // Generate and return the billior track.
        return new BilliorTrack(trackFit);
    }
}
