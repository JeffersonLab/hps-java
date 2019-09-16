package org.hps.analysis.MC;

import hep.physics.matrix.SymmetricMatrix;
import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.BasicHepLorentzVector;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.HepLorentzVector;
import hep.physics.vec.VecOp;
import static java.lang.Math.sqrt;

import java.util.ArrayList;
import java.util.List;

import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.ReconstructedParticle;
import org.lcsim.event.Track;
import org.lcsim.event.TrackerHit;
import org.lcsim.event.Vertex;
import org.lcsim.event.base.BaseReconstructedParticle;
import org.hps.recon.tracking.TrackType;
import org.hps.recon.tracking.TrackUtils;
import org.hps.recon.vertexing.BilliorTrack;
import org.hps.recon.vertexing.BilliorVertex;
import org.hps.recon.vertexing.BilliorVertexer;
import org.lcsim.event.TrackState;
import org.lcsim.event.base.BaseTrackState;
import org.lcsim.fit.helicaltrack.HelicalTrackFit;
import org.lcsim.fit.helicaltrack.HelixUtils;

/**
 * The is adapted from the main HPS implementation of ReconParticleDriver, but for ReconParticleDriverForTrackRefit
 * Method generates V0 candidates and does vertex fits for a custom set of tracks
 *
 * @author Omar Moreno <omoreno1@ucsc.edu>
 * @author Matt Solt <mrsolt@slac.stanford.edu>
 */
public class HpsReconParticleDriverForTrackRefit extends ReconParticleDriverForTrackRefit {

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
    // #dof for fit for each Constraint
    private static final int[] DOF = {7,5,4};
    
    private boolean _patchVertexTrackParameters = false;
    private boolean _storeCovTrkMomList = true;
    
    
    /**
     * Processes the track and cluster collections in the event into
     * reconstructed particles and V0 candidate particles and vertices. These
     * reconstructed particles are then stored in the event.
     *
     * @param event - The event to process.
     */
    @Override
    protected void process(EventHeader event) {
        
        super.process(event);
    }

    
    public void setStoreVertexCovars(boolean input) {
        _storeCovTrkMomList = input;
    }
    
    
    public void findV0s(List<ReconstructedParticle> electrons, List<ReconstructedParticle> positrons,  List<ReconstructedParticle> uncV0Candidates, List<Vertex> uncV0Vertices, List<ReconstructedParticle> bscV0Candidates, List<Vertex> bscV0Vertices, List<ReconstructedParticle> targV0Candidates, List<Vertex> targV0Vertices) {
        for (ReconstructedParticle positron : positrons) {
            for (ReconstructedParticle electron : electrons) {
                // Don't vertex a GBL track with a SeedTrack.
                if (TrackType.isGBL(positron.getType()) != TrackType.isGBL(electron.getType()))
                    continue;
                // Make V0 candidates
                this.makeV0Candidates(electron, positron, uncV0Candidates, uncV0Vertices, bscV0Candidates, bscV0Vertices, targV0Candidates, targV0Vertices);
            }
        }
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
    protected void findVertices(List<ReconstructedParticle> electrons, List<ReconstructedParticle> positrons, List<ReconstructedParticle> uncV0Candidates, List<Vertex> uncV0Vertices, List<ReconstructedParticle> bscV0Candidates, List<Vertex> bscV0Vertices, List<ReconstructedParticle> targV0Candidates, List<Vertex> targV0Vertices) {

        // Iterate over the positrons and electrons to perform vertexing
        // on the pairs.
        
        findV0s(electrons, positrons, uncV0Candidates, uncV0Vertices, bscV0Candidates, bscV0Vertices, targV0Candidates, targV0Vertices);

    }

    /**
     * Sets the default LCIO collection names if they are not already defined
     * previously.
     */
    @Override
    protected void startOfData() {
        super.startOfData();
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
     * mg--8/14/17--add the displaced vertex refit for the UNCONSTRAINED and
     * BS_CONSTRAINED fits
     */
    private BilliorVertex fitVertex(Constraint constraint, ReconstructedParticle electron, ReconstructedParticle positron) {

        // Covert the tracks to BilliorTracks.
        BilliorTrack electronBTrack = toBilliorTrack(electron.getTracks().get(0));
        BilliorTrack positronBTrack = toBilliorTrack(positron.getTracks().get(0));

        // Create a vertex fitter from the magnetic field.
        BilliorVertexer vtxFitter = new BilliorVertexer(bField);
        // TODO: The beam size should come from the conditions database.
        vtxFitter.setBeamSize(beamSize);
        vtxFitter.setBeamPosition(beamPosition);
        vtxFitter.setStoreCovTrkMomList(_storeCovTrkMomList);
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
        billiorTracks.add(electronBTrack);
        billiorTracks.add(positronBTrack);

        // Find a vertex based on the tracks.
        BilliorVertex vtx = vtxFitter.fitVertex(billiorTracks);
        
        int minLayEle = 6;
        int minLayPos = 6;
        List<TrackerHit> allTrackHits = electron.getTracks().get(0).getTrackerHits();
        for (TrackerHit temp : allTrackHits) {
            // Retrieve the sensor associated with one of the hits. This will
            // be used to retrieve the layer number
            HpsSiSensor sensor = (HpsSiSensor) ((RawTrackerHit) temp.getRawHits().get(0)).getDetectorElement();

            // Retrieve the layer number by using the sensor
            int layer = (sensor.getLayerNumber() + 1) / 2;
            if (layer < minLayEle)
                minLayEle = layer;
        }
        allTrackHits = positron.getTracks().get(0).getTrackerHits();
        for (TrackerHit temp : allTrackHits) {
            // Retrieve the sensor associated with one of the hits. This will
            // be used to retrieve the layer number
            HpsSiSensor sensor = (HpsSiSensor) ((RawTrackerHit) temp.getRawHits().get(0)).getDetectorElement();

            // Retrieve the layer number by using the sensor
            int layer = (sensor.getLayerNumber() + 1) / 2;
            if (layer < minLayPos)
                minLayPos = layer;
        }
        vtx.setLayerCode(minLayPos + minLayEle);
        vtx.setProbability(DOF[constraint.ordinal()]);

        // mg 8/14/17 
        // if this is an unconstrained or BS constrained vertex, propogate the 
        // tracks to the vertex found in previous fit and do fit again
        //  ...  this is required because the vertex fit assumes trajectories 
        // change linearly about the reference point (which we initially guess to be 
        // (0,0,0) while for long-lived decays there is significant curvature
        if (constraint == Constraint.BS_CONSTRAINED || constraint == Constraint.UNCONSTRAINED) {
            List<ReconstructedParticle> recoList = new ArrayList<ReconstructedParticle>();
            recoList.add(electron);
            recoList.add(positron);
            List<BilliorTrack> shiftedTracks = shiftTracksToVertex(recoList, vtx.getPosition());
//            if (constraint == Constraint.BS_CONSTRAINED) {
//                Hep3Vector beamRelToNewRef = new BasicHep3Vector(-vtx.getPosition().z() + beamPosition[0], -vtx.getPosition().x() + beamPosition[1], 0);
////                vtxFitter.setBeamPosition(beamRelToNewRef.v());
//                //mg 5/11/2018:  use referencePostion, separate from beam position  
//                vtxFitter.setReferencePosition(beamRelToNewRef.v());
//            }
 //mg 5/11/2018:  use referencePosition, separate from beam position  
            Hep3Vector newRefPoint = new BasicHep3Vector(vtx.getPosition().z() , vtx.getPosition().x() , 0);          
            vtxFitter.setReferencePosition(newRefPoint.v());

            BilliorVertex vtxNew = vtxFitter.fitVertex(shiftedTracks);
            Hep3Vector vtxPosNew = VecOp.add(vtx.getPosition(), vtxNew.getPosition());//the refit vertex is measured wrt the original vertex position
            vtxNew.setPosition(vtxPosNew);//just change the position...the errors and momenta are correct in re-fit
            vtxNew.setLayerCode(vtx.getLayerCode());
            vtxNew.setProbability(DOF[constraint.ordinal()]);
            //  mg 5/11/2018:  correct for the beamspot position in the fitter instead of HPSReconParticle...
//            Hep3Vector vtxPosNew = VecOp.add(vtx.getPosition(), vtxNew.getPosition());//the refit vertex is measured wrt the original vertex position
//            vtxNew.setPosition(vtxPosNew);//just change the position...the errors and momenta are correct in re-fit

            return vtxNew;
        } else
            return vtx;
    }

    /**
     *
     */
    private void makeV0Candidates(ReconstructedParticle electron, ReconstructedParticle positron, List<ReconstructedParticle> uncV0Candidates, List<Vertex> uncV0Vertices, List<ReconstructedParticle> bscV0Candidates, List<Vertex> bscV0Vertices, List<ReconstructedParticle> targV0Candidates, List<Vertex> targV0Vertices) {
        boolean eleIsTop = (electron.getTracks().get(0).getTrackerHits().get(0).getPosition()[2] > 0);
        boolean posIsTop = (positron.getTracks().get(0).getTrackerHits().get(0).getPosition()[2] > 0);
        
        if ((eleIsTop == posIsTop))
            return;
        if (electron.getClusters() == null || positron.getClusters() == null)
            return;
        
        // Create candidate particles for each constraint.
        for (Constraint constraint : Constraint.values()) {

            // Generate a candidate vertex and particle.

            BilliorVertex vtxFit = fitVertex(constraint, electron, positron);
            ReconstructedParticle candidate = makeReconstructedParticle(electron, positron, vtxFit);
            // Add the candidate vertex and particle to the
            // appropriate LCIO collection.
            switch (constraint) {

                case UNCONSTRAINED:
                    // patch the track parameters at the found vertex
                    if (_patchVertexTrackParameters)
                        patchVertex(vtxFit);
                    if (eleIsTop != posIsTop) {
                        uncV0Vertices.add(vtxFit);
                        uncV0Candidates.add(candidate);
                    }
                    break;

                case BS_CONSTRAINED:
                    if (eleIsTop != posIsTop) {
                        bscV0Vertices.add(vtxFit);
                        bscV0Candidates.add(candidate);
                    }
                    break;

                case TARGET_CONSTRAINED:
                    if (eleIsTop != posIsTop) {
                        targV0Vertices.add(vtxFit);
                        targV0Candidates.add(candidate);
                    }
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
        //mg 10/24/2017...billiorvertex now returns momentum in JLAB frame
        //         fittedMomentum = CoordinateTransformations.transformVectorToDetector(fittedMomentum);

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

    /**
     * Converts a <code>TrackState</code> object to a <code>BilliorTrack
     * </code> object.
     *
     * @param track - The original track state
     * @return Returns the original track as a <code>BilliorTrack
     * </code> object.
     */
    private BilliorTrack toBilliorTrack(TrackState trackstate) {
        // Generate and return the billior track.
        return new BilliorTrack(trackstate, 0, 0); // track state doesn't store chi^2 info (stored in the Track object)
    }

    public void setPatchVertexTrackParameters(boolean b) {
        _patchVertexTrackParameters = b;
    }

    private void patchVertex(BilliorVertex v) {
        ReconstructedParticle rp = v.getAssociatedParticle();
        List<ReconstructedParticle> parts = rp.getParticles();
        ReconstructedParticle rp1 = parts.get(0);
        ReconstructedParticle rp2 = parts.get(1);
        //p1
        Track et = rp1.getTracks().get(0);
        double etrackMom = rp1.getMomentum().magnitude();
        HelicalTrackFit ehtf = TrackUtils.getHTF(et);
        // propagate this to the vertex z position...
        // Note that HPS y is lcsim z
        double es = HelixUtils.PathToZPlane(ehtf, v.getPosition().y());
        //Hep3Vector epointOnTrackAtVtx = HelixUtils.PointOnHelix(ehtf, es);
        Hep3Vector edirOfTrackAtVtx = HelixUtils.Direction(ehtf, es);
        Hep3Vector emomAtVtx = VecOp.mult(etrackMom, VecOp.unit(edirOfTrackAtVtx));
        //p2
        Track pt = rp2.getTracks().get(0);
        double ptrackMom = rp2.getMomentum().magnitude();
        HelicalTrackFit phtf = TrackUtils.getHTF(pt);
        // propagate this to the vertex z position...
        // Note that HPS y is lcsim z
        double ps = HelixUtils.PathToZPlane(phtf, v.getPosition().y());
        //Hep3Vector ppointOnTrackAtVtx = HelixUtils.PointOnHelix(phtf, ps);
        Hep3Vector pdirOfTrackAtVtx = HelixUtils.Direction(phtf, ps);
        Hep3Vector pmomAtVtx = VecOp.mult(ptrackMom, VecOp.unit(pdirOfTrackAtVtx));

        double mass = invMass(emomAtVtx, pmomAtVtx);
        v.setVertexTrackParameters(emomAtVtx, pmomAtVtx, mass);
    }

    private double invMass(Hep3Vector p1, Hep3Vector p2) {
        double me2 = 0.000511 * 0.000511;
        double esum = sqrt(p1.magnitudeSquared() + me2) + sqrt(p2.magnitudeSquared() + me2);
        double pxsum = p1.x() + p2.x();
        double pysum = p1.y() + p2.y();
        double pzsum = p1.z() + p2.z();

        double psum = Math.sqrt(pxsum * pxsum + pysum * pysum + pzsum * pzsum);
        double evtmass = esum * esum - psum * psum;

        if (evtmass > 0)
            return Math.sqrt(evtmass);
        else
            return -99;
    }

    private List<BilliorTrack> shiftTracksToVertex(List<ReconstructedParticle> particles, Hep3Vector vtxPos) {
        ///     Ok...shift the reference point....        
        double[] newRef = {vtxPos.z(), vtxPos.x(), 0.0};//the  TrackUtils.getParametersAtNewRefPoint method only shifts in xy tracking frame
        List<BilliorTrack> newTrks = new ArrayList<BilliorTrack>();
        for (ReconstructedParticle part : particles) {
            BaseTrackState oldTS = (BaseTrackState) part.getTracks().get(0).getTrackStates().get(0);
            double[] newParams = TrackUtils.getParametersAtNewRefPoint(newRef, oldTS);
            SymmetricMatrix newCov = TrackUtils.getCovarianceAtNewRefPoint(newRef, oldTS.getReferencePoint(), oldTS.getParameters(), new SymmetricMatrix(5, oldTS.getCovMatrix(), true));
            //mg...I don't like this re-casting, but toBilliorTrack only takes Track as input
            BaseTrackState newTS = new BaseTrackState(newParams, newRef, newCov.asPackedArray(true), TrackState.AtIP, bField);
            BilliorTrack electronBTrackShift = this.toBilliorTrack(newTS);
            newTrks.add(electronBTrackShift);
        }
        return newTrks;
    }
}
