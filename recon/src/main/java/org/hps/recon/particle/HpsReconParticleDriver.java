package org.hps.recon.particle;

import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.BasicHepLorentzVector;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.HepLorentzVector;
import hep.physics.vec.VecOp;

import java.util.ArrayList;
import java.util.List;

import org.lcsim.event.EventHeader;
import org.lcsim.event.ReconstructedParticle;
import org.lcsim.event.Track;
import org.lcsim.event.Vertex;
import org.lcsim.event.base.BaseReconstructedParticle;
import org.hps.recon.tracking.CoordinateTransformations;
import org.hps.recon.tracking.TrackType;
import org.hps.recon.vertexing.BilliorTrack;
import org.hps.recon.vertexing.BilliorVertex;
import org.hps.recon.vertexing.BilliorVertexer;

/**
 * The main HPS implementation of ReconParticleDriver. Method generates V0
 * candidates and does vertex fits.
 *
 * @author Omar Moreno <omoreno1@ucsc.edu>
 */
public class HpsReconParticleDriver extends ReconParticleDriver {

    /**
     * LCIO collection name for Moller candidate particles generated without
     * constraints.
     */
    protected String unconstrainedMollerCandidatesColName = "UnconstrainedMollerCandidates";
    /**
     * LCIO collection name for Moller candidate particles generated with beam
     * spot constraints.
     */
    protected String beamConMollerCandidatesColName = "BeamspotConstrainedMollerCandidates";
    ;
    /**
     * LCIO collection name for Moller candidate particles generated with target
     * constraints.
     */
    protected String targetConMollerCandidatesColName = "TargetConstrainedMollerCandidates";
    /**
     * LCIO collection name for Moller candidate vertices generated without
     * constraints.
     */
    protected String unconstrainedMollerVerticesColName = "UnconstrainedMollerVertices";
    /**
     * LCIO collection name for Moller candidate vertices generated with beam
     * spot constraints.
     */
    protected String beamConMollerVerticesColName = "BeamspotConstrainedMollerVertices";
    /**
     * LCIO collection name for Moller candidate vertices generated with target
     * constraints.
     */
    protected String targetConMollerVerticesColName = "TargetConstrainedMollerVertices";

    /**
     * Stores reconstructed Moller candidate particles generated without
     * constraints.
     */
    protected List<ReconstructedParticle> unconstrainedMollerCandidates;
    /**
     * Stores reconstructed Moller candidate particles generated with beam spot
     * constraints.
     */
    protected List<ReconstructedParticle> beamConMollerCandidates;
    /**
     * Stores reconstructed Moller candidate particles generated with target
     * constraints.
     */
    protected List<ReconstructedParticle> targetConMollerCandidates;
    /**
     * Stores reconstructed Moller candidate vertices generated without
     * constraints.
     */
    protected List<Vertex> unconstrainedMollerVertices;
    /**
     * Stores reconstructed Moller candidate vertices generated with beam spot
     * constraints.
     */
    protected List<Vertex> beamConMollerVertices;
    /**
     * Stores reconstructed Moller candidate vertices generated with target
     * constraints.
     */
    protected List<Vertex> targetConMollerVertices;

    /**
     * Represents a type of constraint for vertex fitting.
     *
     * @author Omar Moreno <omoreno1@ucsc.edu>
     */
    private enum Constraint {

        /**
         * Represents a fit with no constraints.
         */
        UNCONSTRAINED,
        /**
         * Represents a fit with beam spot constraints.
         */
        BS_CONSTRAINED,
        /**
         * Represents a fit with target constraints.
         */
        TARGET_CONSTRAINED
    }

    /**
     * Processes the track and cluster collections in the event into
     * reconstructed particles and V0 candidate particles and vertices. These
     * reconstructed particles are then stored in the event.
     *
     * @param event - The event to process.
     */
    @Override
    protected void process(EventHeader event) {

        unconstrainedMollerCandidates = new ArrayList<ReconstructedParticle>();
        beamConMollerCandidates = new ArrayList<ReconstructedParticle>();
        targetConMollerCandidates = new ArrayList<ReconstructedParticle>();
        unconstrainedMollerVertices = new ArrayList<Vertex>();
        beamConMollerVertices = new ArrayList<Vertex>();
        targetConMollerVertices = new ArrayList<Vertex>();

        super.process(event);

        event.put(unconstrainedMollerCandidatesColName, unconstrainedMollerCandidates, ReconstructedParticle.class, 0);
        event.put(beamConMollerCandidatesColName, beamConMollerCandidates, ReconstructedParticle.class, 0);
        event.put(targetConMollerCandidatesColName, targetConMollerCandidates, ReconstructedParticle.class, 0);
        event.put(unconstrainedMollerVerticesColName, unconstrainedMollerVertices, Vertex.class, 0);
        event.put(beamConMollerVerticesColName, beamConMollerVertices, Vertex.class, 0);
        event.put(targetConMollerVerticesColName, targetConMollerVertices, Vertex.class, 0);
    }

    /**
     * Creates reconstructed V0 candidate particles and vertices for electron
     * positron pairs using no constraints, beam constraints, and target
     * constraints. These are saved to the appropriate lists in the super class.
     *
     * @param electrons - The list of electrons.
     * @param positrons - The list of positrons.
     */
    @Override
    protected void findVertices(List<ReconstructedParticle> electrons, List<ReconstructedParticle> positrons) {

        // Iterate over the positrons and electrons to perform vertexing
        // on the pairs.
        for (ReconstructedParticle positron : positrons) {
            for (ReconstructedParticle electron : electrons) {

                // Don't vertex a GBL track with a SeedTrack.
                if (TrackType.isGBL(positron.getType()) != TrackType.isGBL(electron.getType())) {
                    continue;
                }
                // Only vertex two particles if at least one strategy found both tracks. Take out this check once we reduce the number of tracks.
                // This is dumb so I took it out. - Matt Solt
                /*if ((positron.getType() & electron.getType() & 0x1f) == 0) {
                    continue;
                }*/

                // Make V0 candidates
                this.makeV0Candidates(electron, positron);
            }
        }

        List<ReconstructedParticle> topElectrons = new ArrayList<ReconstructedParticle>();
        List<ReconstructedParticle> botElectrons = new ArrayList<ReconstructedParticle>();
        for (ReconstructedParticle electron : electrons) {
            if (electron.getTracks().get(0).getTrackStates().get(0).getTanLambda() > 0) {
                topElectrons.add(electron);
            } else {
                botElectrons.add(electron);
            }
        }

        // Iterate over the collection of electrons and create e-e- pairs 
        for (ReconstructedParticle topElectron : topElectrons) {

            for (ReconstructedParticle botElectron : botElectrons) {
                // Don't vertex a GBL track with a SeedTrack.
                if (TrackType.isGBL(topElectron.getType()) != TrackType.isGBL(botElectron.getType())) {
                    continue;
                }

                // Only vertex two particles if at least one strategy found both tracks. Take out this check once we reduce the number of tracks.
                if ((topElectron.getType() & botElectron.getType() & 0x1f) == 0) {
                    continue;
                }

                // Make Moller candidates
                this.makeMollerCandidates(topElectron, botElectron);
            }
        }
    }

    /**
     * Sets the default LCIO collection names if they are not already defined
     * previously.
     */
    @Override
    protected void startOfData() {
        // If the LCIO collection names have not been defined, assign
        // them to the default names.
        if (unconstrainedV0CandidatesColName == null) {
            unconstrainedV0CandidatesColName = "UnconstrainedV0Candidates";
        }
        if (beamConV0CandidatesColName == null) {
            beamConV0CandidatesColName = "BeamspotConstrainedV0Candidates";
        }
        if (targetConV0CandidatesColName == null) {
            targetConV0CandidatesColName = "TargetConstrainedV0Candidates";
        }
        if (unconstrainedV0VerticesColName == null) {
            unconstrainedV0VerticesColName = "UnconstrainedV0Vertices";
        }
        if (beamConV0VerticesColName == null) {
            beamConV0VerticesColName = "BeamspotConstrainedV0Vertices";
        }
        if (targetConV0VerticesColName == null) {
            targetConV0VerticesColName = "TargetConstrainedV0Vertices";
        }
    }

    /**
     * Fits a vertex from an electron/positron track pair using the indicated
     * constraint.
     *
     * @param constraint - The constraint type to use.
     * @param electron - The electron track.
     * @param positron - The positron track.
     * @return Returns the reconstructed vertex as a <code>BilliorVertex
     * </code> object.
     */
    private BilliorVertex fitVertex(Constraint constraint, BilliorTrack electron, BilliorTrack positron) {
        // Create a vertex fitter from the magnetic field.
        BilliorVertexer vtxFitter = new BilliorVertexer(bField);
        // TODO: The beam size should come from the conditions database.
        vtxFitter.setBeamSize(beamSize);
        vtxFitter.setBeamPosition(beamPosition);
        
        vtxFitter.setDebug(debug);

        // Perform the vertexing based on the specified constraint.
        switch (constraint) {
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
     *
     */
    private void makeV0Candidates(ReconstructedParticle electron, ReconstructedParticle positron) {

        // Covert the tracks to BilliorTracks.
        BilliorTrack electronBTrack = toBilliorTrack(electron.getTracks().get(0));
        BilliorTrack positronBTrack = toBilliorTrack(positron.getTracks().get(0));

        // Create candidate particles for each constraint.
        for (Constraint constraint : Constraint.values()) {

            // Generate a candidate vertex and particle.
            BilliorVertex vtxFit = fitVertex(constraint, electronBTrack, positronBTrack);
            ReconstructedParticle candidate = this.makeReconstructedParticle(electron, positron, vtxFit);

            // Add the candidate vertex and particle to the
            // appropriate LCIO collection.
            switch (constraint) {

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

    /**
     *
     */
    private void makeMollerCandidates(ReconstructedParticle topElectron, ReconstructedParticle botElectron) {

        // Covert the tracks to BilliorTracks.
        BilliorTrack firstElectronBTrack = toBilliorTrack(topElectron.getTracks().get(0));
        BilliorTrack secondElectronBTrack = toBilliorTrack(botElectron.getTracks().get(0));

        // Create candidate particles for each constraint.
        for (Constraint constraint : Constraint.values()) {

            // Generate a candidate vertex and particle.
            BilliorVertex vtxFit = fitVertex(constraint, firstElectronBTrack, secondElectronBTrack);
            ReconstructedParticle candidate = makeReconstructedParticle(topElectron, botElectron, vtxFit);

            // Add the candidate vertex and particle to the
            // appropriate LCIO collection.
            switch (constraint) {

                case UNCONSTRAINED:
                    unconstrainedMollerVertices.add(vtxFit);
                    unconstrainedMollerCandidates.add(candidate);
                    break;

                case BS_CONSTRAINED:
                    beamConMollerVertices.add(vtxFit);
                    beamConMollerCandidates.add(candidate);
                    break;

                case TARGET_CONSTRAINED:
                    targetConMollerVertices.add(vtxFit);
                    targetConMollerCandidates.add(candidate);
                    break;

            }
        }
    }

    /**
     * Creates a reconstructed V0 candidate particle from an electron, positron,
     * and billior vertex.
     *
     * @param electron - The electron.
     * @param positron - The positron.
     * @param vtxFit - The billior vertex.
     * @return Returns a reconstructed particle with properties generated from
     * the child particles and vertex given as an argument.
     */
    public static ReconstructedParticle makeReconstructedParticle(ReconstructedParticle electron, ReconstructedParticle positron, BilliorVertex vtxFit) {

        // Create a new reconstructed particle to represent the V0
        // candidate and populate it with the electron and positron.
        ReconstructedParticle candidate = new BaseReconstructedParticle();
        ((BaseReconstructedParticle) candidate).setStartVertex(vtxFit);
        candidate.addParticle(electron);
        candidate.addParticle(positron);

        // Set the type of the V0 particle.  This will only be needed for pass 2.
        ((BaseReconstructedParticle) candidate).setType(electron.getType());

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

        // If both the electron and positron have an associated Ecal cluster,
        // calculate the total energy and assign it to the V0 particle
        double v0Energy = 0;
        if (!electron.getClusters().isEmpty() && !positron.getClusters().isEmpty()) {
            v0Energy += electron.getClusters().get(0).getEnergy();
            v0Energy += positron.getClusters().get(0).getEnergy();
        }

        HepLorentzVector fourVector = new BasicHepLorentzVector(v0Energy, fittedMomentum);
        //((BasicHepLorentzVector) fourVector).setV3(fourVector.t(), fittedMomentum);
        ((BaseReconstructedParticle) candidate).set4Vector(fourVector);

        // Set the charge of the particle
        double particleCharge = electron.getCharge() + positron.getCharge();
        ((BaseReconstructedParticle) candidate).setCharge(particleCharge);

        // VERBOSE :: Output the fitted momentum data.
//        printDebug("Fitted momentum in tracking frame: " + fittedMomentum.toString());
//        printDebug("Fitted momentum in detector frame: " + fittedMomentum.toString());
        // Add the ReconstructedParticle to the vertex.
        vtxFit.setAssociatedParticle(candidate);

        // Set the vertex position as the reference point of the V0 particle
        ((BaseReconstructedParticle) candidate).setReferencePoint(vtxFit.getPosition());

        // Return the V0 candidate.
        return candidate;
    }

    /**
     * Converts a <code>Track</code> object to a <code>BilliorTrack
     * </code> object.
     *
     * @param track - The original track.
     * @return Returns the original track as a <code>BilliorTrack
     * </code> object.
     */
    private BilliorTrack toBilliorTrack(Track track) {
        // Generate and return the billior track.
        return new BilliorTrack(track);
    }
}
