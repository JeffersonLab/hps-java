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
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;
import org.hps.recon.ecal.HPSEcalCluster;
import org.hps.recon.tracking.CoordinateTransformations;
import org.hps.recon.tracking.TrackUtils;

/**
 * Driver that matches SVT Tracks and Ecal Clusters and creates
 * ReconstructedParticles.
 *
 * @author Mathew Graham <mgraham@slac.stanford.edu>
 * @author Omar Moreno <omoreno1@ucsc.edu>
 * @version $Id$
 */
public abstract class ReconParticleDriver extends Driver {

    // Flags
    boolean debug = false;

    // Reconstructed particle collections
    List<ReconstructedParticle> finalStateParticles;
    List<ReconstructedParticle> unconstrainedV0Candidates;
    List<ReconstructedParticle> beamConV0Candidates;
    List<ReconstructedParticle> targetConV0Candidates;
    List<ReconstructedParticle> electrons;
    List<ReconstructedParticle> positrons;
    List<Vertex> unconstrainedV0Vertices;
    List<Vertex> beamConV0Vertices; 
    List<Vertex> targetConV0Vertices;

    // Collections
    String ecalClustersCollectionName = "EcalClusters";
    String tracksCollectionName = "MatchedTracks";
    String finalStateParticlesColName = "FinalStateParticles";
    String unconstrainedV0CandidatesColName = null;
    String beamConV0CandidatesColName = null;
    String targetConV0CandidatesColName = null;
    String vertexCandidatesColName = null;
    String vertexBeamConsCandidatesName = null;
	String unconstrainedV0VerticesColName = null;
	String beamConV0VerticesColName = null;
	String targetConV0VerticesColName = null;
	
    // The beamsize array is in the tracking frame
    /* TODO  mg-May 14, 2014:  the the beam size from the conditions db...also beam position!  */
    double[] beamsize = {0.001, 0.2, 0.02};
    double maxTrackClusterDistance = 10000; // [mm] 
    double bField;

    //  flipSign is a kludge...
    //  HelicalTrackFitter doesn't deal with B-fields in -ive Z correctly
    //  so we set the B-field in +iveZ and flip signs of fitted tracks
    //  
    //  Note:  This should be -1 for test run configurations and +1 for 
    //         prop-2014 configurations 
    int flipSign = 1;

    public ReconParticleDriver() {
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public void setMaxTrackClusterDistance(double maxTrackClusterDistance) {
        this.maxTrackClusterDistance = maxTrackClusterDistance;
    }

    public void setBeamSigmaX(double sigma_x) {
        beamsize[1] = sigma_x;
    }

    public void setBeamSigmaY(double sigma_y) {
        beamsize[2] = sigma_y;
    }

    public void setEcalClusterCollectionName(String ecalClustersCollectionName) {
        this.ecalClustersCollectionName = ecalClustersCollectionName;
    }

    public void setTrackCollectoinName(String tracksCollectionName) {
        this.tracksCollectionName = tracksCollectionName;
    }

    @Override
    protected void detectorChanged(Detector detector) {

        Hep3Vector ip = new BasicHep3Vector(0., 0., 1.);
        bField = detector.getFieldMap().getField(ip).y();
        if (bField < 0)
            flipSign = -1;

    }

    public void process(EventHeader event) {

        // All events should have a collection of Ecal clusters.  If the event 
        // doesn't have one, skip the event.
        if (!event.hasCollection(HPSEcalCluster.class, ecalClustersCollectionName)) 
        	return;

        // Get the collection of Ecal clusters from the event. A triggered 
        // event should have Ecal clusters.  If it doesn't, skip the event.
        List<HPSEcalCluster> clusters = event.get(HPSEcalCluster.class, ecalClustersCollectionName);
        //if(clusters.isEmpty()) return;  
        this.printDebug("Number of Ecal clusters: " + clusters.size());

        // Get the collection of tracks from the event
        List<Track> tracks = event.get(Track.class, tracksCollectionName);
        this.printDebug("Number of Tracks: " + tracks.size());

        finalStateParticles = new ArrayList<ReconstructedParticle>();
        electrons = new ArrayList<ReconstructedParticle>();
        positrons = new ArrayList<ReconstructedParticle>();
        unconstrainedV0Candidates = new ArrayList<ReconstructedParticle>();
        beamConV0Candidates = new ArrayList<ReconstructedParticle>();
        targetConV0Candidates = new ArrayList<ReconstructedParticle>();
        unconstrainedV0Vertices = new ArrayList<Vertex>();
        beamConV0Vertices = new ArrayList<Vertex>();
        targetConV0Vertices = new ArrayList<Vertex>();

        // 
        finalStateParticles = this.makeReconstructedParticles(clusters, tracks);
        this.printDebug("Total number of Final State Particles: " + finalStateParticles.size());

        // Put all the reconstructed particles in the event
        event.put(finalStateParticlesColName, finalStateParticles, ReconstructedParticle.class, 0);

        // Loop through the list of final state particles and separate the
        // charged particles to either electrons or positrons.  These lists
        // will be used for vertexing purposes.
        for (ReconstructedParticle finalStateParticle : finalStateParticles) {
            if (finalStateParticle.getCharge() > 0) positrons.add(finalStateParticle);
            else if (finalStateParticle.getCharge() < 0) electrons.add(finalStateParticle);
        }
        this.printDebug("Number of Electrons: " + electrons.size());
        this.printDebug("Number of Positrons: " + positrons.size());
        
        // Vertex electron and positron candidates 
        findVertices(electrons, positrons);

        // If the list exist, put the vertexed candidates and vertices into the event
        if (unconstrainedV0CandidatesColName != null){
            this.printDebug("Total number of unconstrained V0 candidates: " + unconstrainedV0Candidates.size());
            event.put(unconstrainedV0CandidatesColName, unconstrainedV0Candidates, ReconstructedParticle.class, 0);
        }
        if (beamConV0CandidatesColName != null){
            this.printDebug("Total number of beam constrained V0 candidates: " + unconstrainedV0Candidates.size());
            event.put(beamConV0CandidatesColName, beamConV0Candidates, ReconstructedParticle.class, 0);
        }
        if (targetConV0CandidatesColName != null){
            this.printDebug("Total number of target constrained V0 candidates: " + unconstrainedV0Candidates.size());
            event.put(targetConV0CandidatesColName, targetConV0Candidates, ReconstructedParticle.class, 0);
        }
        if(unconstrainedV0VerticesColName != null){
        	this.printDebug("Total number of unconstrained V0 vertices: " + unconstrainedV0Vertices.size());
        	event.put(unconstrainedV0VerticesColName, unconstrainedV0Vertices, Vertex.class, 0);
        }
        if(beamConV0VerticesColName != null){
        	this.printDebug("Total number of beam constrained V0 vertices: " + beamConV0Vertices.size());
        	event.put(beamConV0VerticesColName, beamConV0Vertices, Vertex.class, 0);
        }
        if(targetConV0VerticesColName != null){
        	this.printDebug("Total number of target constrained V0 vertices: " + beamConV0Vertices.size());
        	event.put(targetConV0VerticesColName, targetConV0Vertices, Vertex.class, 0);
        }
    }

    /**
     *
     */
    abstract void findVertices(List<ReconstructedParticle> electrons, List<ReconstructedParticle> positrons);

    /**
     * make the final state particles from clusters & tracks
     * loop over the tracks first and try to match with clusters
     */
    protected List<ReconstructedParticle> makeReconstructedParticles(List<HPSEcalCluster> clusters, List<Track> tracks) {

        // Instantiate the list of reconstructed particles
        List<ReconstructedParticle> particles = new ArrayList<ReconstructedParticle>();

        // Instantiate the list of unmatched  clusters.  Remove if we find track match
        List<HPSEcalCluster> unmatchedClusters = new ArrayList<HPSEcalCluster>(clusters);
       

        for (Track track : tracks) {
            //make the containers for the reconstructed particle
            ReconstructedParticle particle = new BaseReconstructedParticle();
            HepLorentzVector fourVector = new BasicHepLorentzVector(0, 0, 0, 0);
            //add the track information
            particle.addTrack(track);
            Hep3Vector momentum = new BasicHep3Vector(track.getTrackStates().get(0).getMomentum());
            this.printDebug("Momentum in tracking frame: " + momentum.toString());
            momentum = CoordinateTransformations.transformVectorToDetector(momentum);
            this.printDebug("Momentum in detector frame: " + momentum.toString());
            ((BasicHepLorentzVector) fourVector).setV3(fourVector.t(), momentum);
            ((BaseReconstructedParticle) particle).setCharge(track.getCharge() * flipSign);
            if (particle.getCharge() > 0)
                ((BaseReconstructedParticle) particle).setParticleIdUsed(new SimpleParticleID(-11, 0, 0, 0));
            else if (particle.getCharge() < 0)
                ((BaseReconstructedParticle) particle).setParticleIdUsed(new SimpleParticleID(11, 0, 0, 0));

            HPSEcalCluster matchedCluster = null;
            for (HPSEcalCluster cluster : unmatchedClusters) {

                // Get the position of the Ecal cluster
                Hep3Vector clusterPosition = new BasicHep3Vector(cluster.getPosition());
                // Extrapolate the track to the Ecal cluster position
                Hep3Vector trackPosAtEcal = TrackUtils.extrapolateTrack(track, clusterPosition.z());
                this.printDebug("Ecal cluster position: " + clusterPosition.toString());

                double rMax = Double.MAX_VALUE;

                // Check if any of the extrapolated values are invalid.
                // TODO: There are some track whose extrapolated coordinates
                //        are NaN. The problem seems to be that the y-coordinate
                //        of the extrapolated helix is found to be non-real. This
                //        needs to be fixed.
                if (Double.isNaN(trackPosAtEcal.x()) || Double.isNaN(trackPosAtEcal.y()))
                    continue;
                this.printDebug("Track position at shower max: " + trackPosAtEcal.toString());

//                double r = VecOp.sub(trackPosAtEcal, clusterPosition).magnitude();
                //don't trust extrapolation...just do y-difference for now
                double r = Math.abs(clusterPosition.y() - trackPosAtEcal.y());
                this.printDebug("Distance between Ecal cluster and track position: " + r + " mm");

                // Check if the Ecal cluster and track are within the same 
                // detector volume i.e. both top or bottom
                if (clusterPosition.y() * trackPosAtEcal.y() < 0) {
                    this.printDebug("Track and Ecal cluster are in opposite volumes. Track Y @ ECAL = " + trackPosAtEcal.z());
                    continue;
                }

//                if (r < rMax && r <= maxTrackClusterDistance) {
                 if (r < rMax && isMatch(cluster,track)) {
                    rMax = r;
                    matchedCluster = cluster;
                }
            }
            if (matchedCluster != null) {
                particle.addCluster(matchedCluster);
                ((BasicHepLorentzVector) fourVector).setT(matchedCluster.getEnergy());
                unmatchedClusters.remove(matchedCluster);
            }
            ((BaseReconstructedParticle) particle).set4Vector(fourVector);
            particles.add(particle);
        }

        if (!unmatchedClusters.isEmpty())
            for (HPSEcalCluster unmatchedCluster : unmatchedClusters) {
                // Create a reconstructed particle and add it to the 
                // collection of particles
                ReconstructedParticle particle = new BaseReconstructedParticle();
                HepLorentzVector fourVector = new BasicHepLorentzVector(0, 0, 0, 0);

                particle.addCluster(unmatchedCluster);
                ((BasicHepLorentzVector) fourVector).setT(unmatchedCluster.getEnergy());
                ((BaseReconstructedParticle) particle).setCharge(0);
                ((BaseReconstructedParticle) particle).set4Vector(fourVector);
                ((BaseReconstructedParticle) particle).setParticleIdUsed(new SimpleParticleID(22, 0, 0, 0));
                particles.add(particle);
            }

        return particles;
    }

    /**
     *
     * @param debugMessage
     */
    protected void printDebug(String debugMessage) {
        if (debug)
            System.out.println(this.getClass().getSimpleName() + ": " + debugMessage);
    }

    boolean isMatch(HPSEcalCluster cluster, Track track) {
        Hep3Vector clusterPosition = new BasicHep3Vector(cluster.getPosition());
        // Extrapolate the track to the Ecal cluster position
        Hep3Vector trackPosAtEcal = TrackUtils.extrapolateTrack(track, clusterPosition.z());
        double trackMom = (new BasicHep3Vector(track.getMomentum())).magnitude();
        double clustEne = cluster.getEnergy();

        double dxCut = 20.0;
        double dyCut = 20.0;
        double dECut = 0.2;//20%
        double samplingFrac = 0.8;

        if (Math.abs(trackPosAtEcal.x() - clusterPosition.x()) > dxCut)
            return false;
        if (Math.abs(trackPosAtEcal.y() - clusterPosition.y()) > dyCut)
            return false;
//        if (Math.abs((samplingFrac*trackMom-clustEne)/(samplingFrac*trackMom)) > dECut)
//            return false;

        return true;
    }
}
